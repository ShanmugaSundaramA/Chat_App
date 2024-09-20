package com.ws.chat.service;

import org.springframework.stereotype.Service;

import com.ws.chat.exception.NotFound;
import com.ws.chat.model.PinnedMessages;
import com.ws.chat.repository.PinnedMessagesRepository;
import com.ws.chat.requestbody.PinnedMessageDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;

@Service
@RequiredArgsConstructor
public class PinnedMessagesService {

     private final PinnedMessagesRepository pinnedMessagesRepository;
     private final MongoTemplate mongoTemplate;
     private final ChatRoomService chatRoomService;

     private static final String USERID = "userId";
     private static final String GROUPID = "groupId";
     private static final String CHATID = "chatId";
     private static final String SUCCESS = "success";
     private static final String MESSAGE = "message";
     private static final String MESSAGEID = "messageId";

     public Object savePinnedMessage(PinnedMessageDTO pinnedMessageDTO) {
          String chatId = null;
          if (pinnedMessageDTO.getGroupId() == null || pinnedMessageDTO.getGroupId().isEmpty()) {
               chatId = chatRoomService.getChatRoomId(
                         pinnedMessageDTO.getUserId(),
                         pinnedMessageDTO.getRecipientId(),
                         true).orElseThrow(() -> new NotFound("Chat room not found"));
          }
          PinnedMessages pinnedMessagesReqBody = PinnedMessages.builder()
                    .id(pinnedMessageDTO.getId())
                    .groupId(pinnedMessageDTO.getGroupId())
                    .chatId(chatId)
                    .messageId(pinnedMessageDTO.getMessageId())
                    .message(pinnedMessageDTO.getMessage())
                    .build();
          String id = pinnedMessagesReqBody.getId();

          PinnedMessages savedPinnedMessage = null;
          if (id != null && !id.isBlank()) {
               PinnedMessages pinnedMessages = this.findById(id);
               pinnedMessages.setMessageId(pinnedMessagesReqBody.getMessageId());
               savedPinnedMessage = pinnedMessagesRepository.save(pinnedMessages);
          } else {
               savedPinnedMessage = pinnedMessagesRepository.save(pinnedMessagesReqBody);
          }
          return ResponseService.successResponse(200, SUCCESS, savedPinnedMessage);
     }

     public Object getPinnedMessage(String userId, String recipientId, String groupId) {

          String chatId = null;
          if (groupId == null || groupId.isEmpty()) {
               Optional<String> chatIdOpt = chatRoomService.getChatRoomId(
                         userId,
                         recipientId,
                         false);
               if (chatIdOpt.isPresent()) {
                    chatId = chatIdOpt.get();
               } else {
                    return ResponseService.successResponse(200, SUCCESS, new ArrayList<>());
               }
          }
          List<AggregationOperation> operations = new ArrayList<>();
          if (groupId != null && !groupId.isEmpty()) {
               operations.add(Aggregation.match(Criteria.where(GROUPID).is(groupId)));
          }
          if (chatId != null && !chatId.isEmpty()) {
               operations.add(Aggregation.match(Criteria.where("chatId").is(chatId)));
          }
          LookupOperation lookupMessages = LookupOperation.newLookup()
                    .from("messages")
                    .localField(MESSAGEID)
                    .foreignField("_id")
                    .as(MESSAGE);
          operations.add(lookupMessages);
          operations.add(Aggregation.project()
                    .and(USERID).as(USERID)
                    .and(CHATID).as(CHATID)
                    .and(GROUPID).as(GROUPID)
                    .and(MESSAGEID).as(MESSAGEID)
                    .and(ArrayOperators.ArrayElemAt.arrayOf(MESSAGE).elementAt(0)).as(MESSAGE)
                    .and("createdAt").as("createdAt")
                    .and("updatedAt").as("updatedAt"));
          Aggregation aggregation = Aggregation.newAggregation(operations);
          AggregationResults<PinnedMessages> results = mongoTemplate.aggregate(
                    aggregation,
                    "pinnedMessages",
                    PinnedMessages.class);
          return ResponseService.successResponse(200, SUCCESS, results.getMappedResults());
     }

     public PinnedMessages findById(String id) {
          return pinnedMessagesRepository.findById(id).orElseThrow(
                    () -> new NotFound("Message not found with id: " + id));
     }

     public Object deleteById(String id) {
          pinnedMessagesRepository.deleteById(id);
          return ResponseService.successResponse(200, SUCCESS, "Deleted successfully");
     }

     public void deleteByMessageId(String messageId) {
          pinnedMessagesRepository.deleteByMessageId(messageId);
     }

     public void deleteByChatId(String chatId) {
          pinnedMessagesRepository.deleteByChatId(chatId);
     }

     public void deleteByGroupId(String groupId) {
          pinnedMessagesRepository.deleteByGroupId(groupId);
     }

}
