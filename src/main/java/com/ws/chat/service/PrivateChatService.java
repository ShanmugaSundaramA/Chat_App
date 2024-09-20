package com.ws.chat.service;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.stereotype.Service;

import com.microsoft.azure.storage.StorageException;
import com.ws.chat.azureblob.AzureBlobAdapter;
import com.ws.chat.model.InboxParticipants;
import com.ws.chat.model.PrivateChat;
import com.ws.chat.repository.PrivateChatRepo;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.responsebody.MessageRes;
import com.ws.chat.responsebody.PrivateChatRes;

import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrivateChatService {

     private final MongoTemplate mongoTemplate;
     private final ChatRoomService chatRoomService;
     private final PrivateChatRepo privateChatRepo;
     private final InboxParticipantsService inboxParticipantsService;
     private final PinnedMessagesService pinnedMessagesService;
     private final AzureBlobAdapter azureBlobAdapter;

     private static final String USERS = "users";
     private static final String MESSAGES = "messages";

     private static final String SENDER_ID = "senderId";
     private static final String RECIPIENT_ID = "recipientId";
     private static final String MESSAGE_ID = "messageId";
     private static final String REPLY_SENDER_ID = "replySenderId";

     private static final String SENDER_NAME = "senderName";
     private static final String RECIPIENT_NAME = "recipientName";
     private static final String REPLY_SENDER_NAME = "replySenderName";
     private static final String REPLY_SENDER_PROFILEPICTURE = "replySenderProfilePicture";

     private static final String MESSAGE = "message";
     private static final String REPLY_TO = "replyTo";

     private static final String SENDER_PROFILE_PICTURE = "senderProfilePicture";

     private static final String IS_FORWARD_MESSAGE = "isForwardMessage";
     private static final String IS_DELETED_BY = "isDeletedBy";
     private static final String CREATED_AT = "createdAt";
     private static final String UPDATED_AT = "updatedAt";

     private static final String SENDER = "sender";
     private static final String RECIPIENT = "recipient";
     private static final String REPLY_SENDER = "replySender";

     private static final String CHAT_ID = "chatId";
     private static final String SUCCESS = "success";

     public Object getPrivateMessages(
               String senderId,
               String recipientId,
               int page,
               int rowPerPage) throws StorageException, URISyntaxException {

          int skip = page * rowPerPage;
          var chatIdOptional = chatRoomService.getChatRoomId(
                    senderId,
                    recipientId,
                    false);
          if (chatIdOptional.isEmpty()) {
               return ResponseService.successResponse(200, SUCCESS, Collections.emptyList());
          }
          String chatId = chatIdOptional.get();
          LookupOperation lookupMessages = LookupOperation.newLookup()
                    .from(MESSAGES)
                    .localField(MESSAGE_ID)
                    .foreignField("_id")
                    .as(MESSAGE);
          LookupOperation lookupReplyMessages = LookupOperation.newLookup()
                    .from(MESSAGES)
                    .localField(REPLY_TO)
                    .foreignField("_id")
                    .as(REPLY_TO);
          LookupOperation lookupUser = LookupOperation.newLookup()
                    .from(USERS)
                    .localField(SENDER_ID)
                    .foreignField("_id")
                    .as(SENDER);
          LookupOperation lookupRecipient = LookupOperation.newLookup()
                    .from(USERS)
                    .localField(RECIPIENT_ID)
                    .foreignField("_id")
                    .as(RECIPIENT);
          LookupOperation lookupReplySenderId = LookupOperation.newLookup()
                    .from(USERS)
                    .localField(REPLY_SENDER_ID)
                    .foreignField("_id")
                    .as(REPLY_SENDER);
          Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(Criteria
                              .where(CHAT_ID).is(chatId)
                              .and(IS_DELETED_BY).nin(senderId)),
                    lookupMessages,
                    lookupReplyMessages,
                    lookupUser,
                    lookupRecipient,
                    lookupReplySenderId,
                    Aggregation.project()
                              .and(ArrayOperators.ArrayElemAt.arrayOf(SENDER).elementAt(0)).as(SENDER)
                              .and("sender._id").as(SENDER_ID)
                              .and("sender.fullname").as(SENDER_NAME)
                              .and("sender.profilePicture").as(SENDER_PROFILE_PICTURE)
                              .and(ArrayOperators.ArrayElemAt.arrayOf(RECIPIENT).elementAt(0)).as(RECIPIENT)
                              .and("recipient._id").as(RECIPIENT_ID)
                              .and("recipient.fullname").as(RECIPIENT_NAME)
                              .and("type").as("type")
                              .and(ArrayOperators.ArrayElemAt.arrayOf(MESSAGE).elementAt(0)).as(MESSAGE)
                              .and(ArrayOperators.ArrayElemAt.arrayOf(REPLY_TO).elementAt(0)).as(REPLY_TO)
                              .and(ArrayOperators.ArrayElemAt.arrayOf(REPLY_SENDER).elementAt(0)).as(REPLY_SENDER)
                              .and("replySender.fullname").as(REPLY_SENDER_NAME)
                              .and("replySender.profilePicture").as(REPLY_SENDER_PROFILEPICTURE)
                              .andExpression(IS_FORWARD_MESSAGE).as(IS_FORWARD_MESSAGE)
                              .andExpression(UPDATED_AT).as(UPDATED_AT)
                              .andExpression(CREATED_AT).as(CREATED_AT),
                    Aggregation.sort(Sort.by(Sort.Direction.DESC, CREATED_AT)),
                    Aggregation.skip(skip),
                    Aggregation.limit(rowPerPage));
          AggregationResults<PrivateChatRes> results = mongoTemplate.aggregate(
                    aggregation,
                    "privateChats",
                    PrivateChatRes.class);

          String sasToken = azureBlobAdapter.generateContainerSasToken();
          String uri = "https://packagingapp.blob.core.windows.net/trove/";

          results.getMappedResults().forEach(p -> {
               if (p.getSenderProfilePicture() != null && !p.getSenderProfilePicture().isEmpty()) {
                    p.setSenderProfilePicture(uri + p.getSenderProfilePicture() + "?" + sasToken);
               }
               if (p.getReplySenderProfilePicture() != null && !p.getReplySenderProfilePicture().isEmpty()) {
                    p.setReplySenderProfilePicture(uri + p.getReplySenderProfilePicture() + "?" + sasToken);
               }
               MessageRes msg = p.getMessage();
               String contentType = msg.getType().toString();
               if (Arrays.asList("AUDIO", "VIDEO", "DOCUMENT", "IMAGE").contains(contentType)) {
                    msg.setName(msg.getContent().toString());
                    msg.setContent(uri + msg.getContent() + "?" + sasToken);
                    p.setMessage(msg);
               }
          });

          Query query = new Query();
          query.addCriteria(Criteria
                    .where(CHAT_ID).is(chatId)
                    .and(IS_DELETED_BY).nin(senderId));
          long totalCount = mongoTemplate.count(
                    query,
                    PrivateChat.class);

          int totalNumberOfPages = (int) Math.ceil((double) totalCount / rowPerPage);
          Map<String, Object> response = new HashMap<>();
          response.put("totalCount", totalCount);
          response.put("privateChatMessages", results.getMappedResults());
          response.put("totalNumberOfPages", totalNumberOfPages);

          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    response);
     }

     public PrivateChat save(MessageRequestBody messageRes) {

          String senderId = messageRes.getSenderId();
          String recipientId = messageRes.getRecipientId();
          ObjectId replySenderId = messageRes.getReplySenderId() != null ? new ObjectId(messageRes.getReplySenderId())
                    : null;
          String chatId = messageRes.getChatId();
          PrivateChat privateChat = PrivateChat.builder()
                    .chatId(chatId)
                    .senderId(new ObjectId(senderId))
                    .recipientId(new ObjectId(recipientId))
                    .type(messageRes.getType())
                    .messageId(messageRes.getMessageId())
                    .replyTo(messageRes.getReplyToMessageId())
                    .replySenderId(replySenderId)
                    .isDeleted(false)
                    .isForwardMessage(messageRes.isForwardMessage())
                    // .isMentionedMessage(messageRes.isMentionedMessage())
                    // .mentionedUserIds(messageRes.getMentions())
                    .build();
          try {
               privateChatRepo.save(privateChat);
          } catch (Exception e) {
               System.out.println("error while saving private chat message :  " + e.getMessage());
          }
          List<InboxParticipants> inboxParticipants = inboxParticipantsService.findByChatId(chatId);
          ObjectId senderObjectId = new ObjectId(senderId);
          ObjectId recipientObjectId = new ObjectId(recipientId);

          if (inboxParticipants.isEmpty()) {
               InboxParticipants senderInboxParticipant = InboxParticipants.builder()
                         .chatId(chatId)
                         .senderId(senderObjectId)
                         .recipientId(recipientObjectId)
                         .lastMessageId(messageRes.getMessageId())
                         .isRead(true)
                         .unreadMessageCount(0)
                         .isDeletedBy(new ArrayList<>())
                         .isDeleted(false)
                         .isSendByRecipient(false)
                         .build();
               InboxParticipants recipientInboxParticipant = InboxParticipants.builder()
                         .chatId(chatId)
                         .senderId(recipientObjectId)
                         .recipientId(senderObjectId)
                         .lastMessageId(messageRes.getMessageId())
                         .isRead(false)
                         .unreadMessageCount(1)
                         .isDeletedBy(new ArrayList<>())
                         .isDeleted(false)
                         .isSendByRecipient(true)
                         .build();
               inboxParticipantsService.saveInboxParticipants(
                         Arrays.asList(
                                   senderInboxParticipant,
                                   recipientInboxParticipant));
          } else {
               if (inboxParticipants.size() == 1) {
                    InboxParticipants existingParticipant = inboxParticipants.get(0);
                    InboxParticipants newParticipant;
                    if (existingParticipant.getSenderId().equals(senderObjectId)) {
                         newParticipant = InboxParticipants.builder()
                                   .chatId(chatId)
                                   .senderId(recipientObjectId)
                                   .recipientId(senderObjectId)
                                   .lastMessageId(messageRes.getMessageId())
                                   .isRead(false)
                                   .unreadMessageCount(existingParticipant.getUnreadMessageCount() + 1)
                                   .isDeletedBy(new ArrayList<>())
                                   .isDeleted(false)
                                   .isSendByRecipient(true)
                                   .build();
                         existingParticipant.setSendByRecipient(false);
                         existingParticipant.setUnreadMessageCount(0);
                    } else {
                         newParticipant = InboxParticipants.builder()
                                   .chatId(chatId)
                                   .senderId(senderObjectId)
                                   .recipientId(recipientObjectId)
                                   .lastMessageId(messageRes.getMessageId())
                                   .isRead(false)
                                   .unreadMessageCount(existingParticipant.getUnreadMessageCount() + 1)
                                   .isDeletedBy(new ArrayList<>())
                                   .isDeleted(false)
                                   .isSendByRecipient(false)
                                   .build();
                         existingParticipant.setSendByRecipient(true);
                         existingParticipant.setUnreadMessageCount(0);
                    }
                    existingParticipant.setLastMessageId(messageRes.getMessageId());
                    existingParticipant.setRead(false);
                    existingParticipant.setIsDeletedBy(new ArrayList<>());
                    inboxParticipants.add(newParticipant);
                    inboxParticipantsService.saveInboxParticipants(inboxParticipants);
               } else {
                    for (InboxParticipants participant : inboxParticipants) {
                         if (participant.getSenderId().equals(senderObjectId)) {
                              participant.setRecipientId(recipientObjectId);
                              participant.setSendByRecipient(false);
                              participant.setUnreadMessageCount(0);
                              participant.setRead(true);
                         } else {
                              participant.setSenderId(recipientObjectId);
                              participant.setRecipientId(senderObjectId);
                              participant.setSendByRecipient(true);
                              participant.setUnreadMessageCount(participant.getUnreadMessageCount() + 1);
                              participant.setRead(false);
                         }
                         participant.setChatId(chatId);
                         participant.setLastMessageId(messageRes.getMessageId());
                         participant.setIsDeletedBy(new ArrayList<>());
                    }
                    inboxParticipantsService.saveInboxParticipants(inboxParticipants);
               }
          }
          return privateChat;
     }

     public Object updateSeenAt(
               String senderId,
               String recipientId) {

          Query query = new Query(new Criteria().andOperator(Criteria
                    .where("senderId").is(new ObjectId(senderId))
                    .and("recipientId").is(new ObjectId(recipientId))));
          Update update = new Update();
          update.set("isRead", true);
          update.set("unreadMessageCount", 0);

          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    mongoTemplate.updateMulti(
                              query,
                              update,
                              InboxParticipants.class).getMatchedCount() + " messages updated ");
     }

     public Object deletePrivateChat(
               String senderId,
               String chatId) {

          Query query = new Query(
                    Criteria.where(CHAT_ID).is(chatId)
                              .and("isDeletedBy").nin(senderId));
          Update update = new Update();
          update.push("isDeletedBy").each(senderId);
          mongoTemplate.updateMulti(
                    query,
                    update,
                    PrivateChat.class);
          inboxParticipantsService.deleteByChatId(
                    senderId,
                    chatId);
          pinnedMessagesService.deleteByChatId(chatId);
          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    "Private chat deleted successfully");
     }
}
