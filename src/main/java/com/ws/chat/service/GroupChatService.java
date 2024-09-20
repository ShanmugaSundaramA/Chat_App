package com.ws.chat.service;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.microsoft.azure.storage.StorageException;
import com.mongodb.client.result.UpdateResult;
import com.ws.chat.azureblob.AzureBlobAdapter;
import com.ws.chat.model.GroupChat;
import com.ws.chat.model.GroupParticipants;
import com.ws.chat.model.Member;
import com.ws.chat.model.Reply;
import com.ws.chat.repository.GroupChatRepository;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.responsebody.GroupChatRes;
import com.ws.chat.responsebody.MessageRes;
import com.ws.chat.responsebody.ReplyRes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupChatService {

     private static final String SENDERID = "senderId";
     private static final String SENDERNAME = "senderName";
     private static final String GROUPID = "groupId";
     private static final String ISDELETED = "isDeleted";
     private static final String MESSAGE = "message";
     private static final String MESSAGEID = "messageId";
     private static final String CREATEDAT = "createdAt";
     private static final String UPDATEDAT = "updatedAt";
     private static final String USERS = "users";
     private static final String SENDER = "sender";
     private static final String SUCCESS = "success";

     private final MongoTemplate mongoTemplate;
     private final GroupChatRepository groupChatRepository;
     private final GroupParticipentService groupParticipentService;
     private final AzureBlobAdapter azureBlobAdapter;
     private final PinnedMessagesService pinnedMessagesService;

     public Member findMemberInGroup(
               String groupId,
               String userId) {

          Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("_id").is(groupId)),
                    Aggregation.unwind("members"),
                    Aggregation.match(Criteria.where("members.userId").is(new ObjectId(userId))),
                    Aggregation.project()
                              .andExclude("_id")
                              .and("members.userId").as("userId")
                              .and("members.role").as("role")
                              .and("members.createdAt").as("createdAt"));

          AggregationResults<Member> results = mongoTemplate.aggregate(
                    aggregation,
                    "groups",
                    Member.class);
          return !results.getMappedResults().isEmpty() ? results.getMappedResults().get(0) : new Member();
     }

     public Map<String, Object> getGroupMessages(
               String groupId,
               String userId,
               List<String> types,
               int pageNo,
               int rowPerPage,
               Date createdAt) throws StorageException, URISyntaxException {

          int skip = (pageNo - 1) * rowPerPage;
          Criteria criteria = Criteria
                    .where(GROUPID).is(groupId)
                    .and("isDeletedBy").nin(userId)
                    .and("type").in(types);
          if (createdAt != null) {
               criteria = criteria.and(CREATEDAT).gte(createdAt);
          }
          LookupOperation lookupUser = LookupOperation.newLookup()
                    .from(USERS)
                    .localField(SENDERID)
                    .foreignField("_id")
                    .as(SENDER);
          LookupOperation lookupMessages = LookupOperation.newLookup()
                    .from("messages")
                    .localField(MESSAGEID)
                    .foreignField("_id")
                    .as(MESSAGE);
          Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(criteria),
                    lookupMessages,
                    lookupUser,
                    Aggregation.project().and(ArrayOperators.ArrayElemAt.arrayOf(SENDER).elementAt(0)).as(SENDER)
                              .and("sender._id").as(SENDERID)
                              .and("sender.fullname").as(SENDERNAME)
                              .and("sender.profilePicture").as("senderProfilePicture")
                              .and("chatId").as("chatId")
                              .and(GROUPID).as(GROUPID).and("type").as("type")
                              .and(ArrayOperators.ArrayElemAt.arrayOf(MESSAGE).elementAt(0)).as(MESSAGE)
                              .andExpression("size(replies)").as("repliesCount").and("isForwardMessage")
                              .as("isForwardMessage").and(UPDATEDAT).as(UPDATEDAT),
                    Aggregation.sort(Sort.by(Sort.Direction.DESC, UPDATEDAT)), Aggregation.skip(skip),
                    Aggregation.limit(rowPerPage));

          AggregationResults<GroupChatRes> results = mongoTemplate.aggregate(
                    aggregation,
                    "groupChats",
                    GroupChatRes.class);

          String sasToken = azureBlobAdapter.generateContainerSasToken();
          String uri = "https://packagingapp.blob.core.windows.net/trove/";

          results.getMappedResults().forEach(g -> {
               MessageRes msg = g.getMessage();
               String contentType = msg.getType().toString();
               if (g.getSenderProfilePicture() != null && !g.getSenderProfilePicture().isEmpty()) {
                    g.setSenderProfilePicture(uri + g.getSenderProfilePicture() + "?" + sasToken);
               }
               if (Arrays.asList("AUDIO", "VIDEO", "DOCUMENT", "IMAGE").contains(contentType)) {
                    msg.setName(msg.getContent().toString());
                    msg.setContent(uri + msg.getContent() + "?" + sasToken);
               }
               g.setMessage(msg);
          });

          Query query = new Query();
          query.addCriteria(Criteria.where("groupId").is(groupId)
                    .and("isDeleted").is(false)
                    .and("type").in(types)
                    .and("createdAt").gte(createdAt)
                    .and("isDeletedBy").nin(userId));
          long totalCount = mongoTemplate.count(
                    query,
                    GroupChat.class);

          int totalNumberOfPages = (int) Math.ceil((double) totalCount / rowPerPage);

          Map<String, Object> response = new HashMap<>();
          response.put("totalCount", totalCount);
          response.put("groupChatMessages", results.getMappedResults());
          response.put("totalNumberOfPages", totalNumberOfPages);

          return response;
     }

     public Object getGroupChat(
               String groupId,
               String userId,
               int pageNo,
               int rowPerPage) throws StorageException, URISyntaxException {

          Member member = this.findMemberInGroup(groupId, userId);
          Map<String, Object> response = this.getGroupMessages(
                    groupId,
                    userId,
                    Arrays.asList("TEXT", "GIF", "AUDIO", "VIDEO", "DOCUMENT", "IMAGE", "LINK", "CONTACT", "NEWGROUP"),
                    pageNo,
                    rowPerPage,
                    member.getCreatedAt());
          return ResponseService.successResponse(200, SUCCESS, response);
     }

     public Object getMediaForGroupDetails(
               String groupId,
               String userId,
               List<String> types,
               Date createdAt) throws StorageException, URISyntaxException {

          Map<String, Object> response = this.getGroupMessages(
                    groupId,
                    userId,
                    types,
                    1,
                    5,
                    createdAt);
          return response.get("groupChatMessages");
     }

     public Object getMediaMessages(
               String groupId,
               String userId,
               List<String> types,
               int pageNo,
               int rowPerPage) throws StorageException, URISyntaxException {

          Member member = this.findMemberInGroup(groupId, userId);
          Map<String, Object> response = this.getGroupMessages(
                    groupId,
                    userId,
                    types,
                    pageNo,
                    rowPerPage,
                    member.getCreatedAt());
          return ResponseService.successResponse(200, SUCCESS, response);
     }

     public GroupChat save(MessageRequestBody messageRes) {

          GroupChat groupChat = GroupChat.builder()
                    .groupId(messageRes.getGroupId())
                    .senderId(new ObjectId(messageRes.getSenderId()))
                    .type(messageRes.getType())
                    .messageId(messageRes.getMessageId())
                    .replies(new ArrayList<>())
                    .isDeleted(false)
                    .isForwardMessage(messageRes.isForwardMessage())
                    .build();

          Query groupQuery = new Query();
          groupQuery.addCriteria(Criteria.where("groupId").is(new ObjectId(messageRes.getGroupId())));

          Update groupPraticipantsUpdate = new Update();
          groupPraticipantsUpdate.set("senderId", new ObjectId(messageRes.getSenderId()));
          groupPraticipantsUpdate.set("lastMessageId", messageRes.getMessageId());
          groupPraticipantsUpdate.set("isRead", false);
          groupPraticipantsUpdate.set("updatedAt", new Date());
          groupPraticipantsUpdate.inc("unreadMessageCount", 1);

          Query senderQuery = new Query();
          senderQuery.addCriteria(Criteria.where("groupId").is(new ObjectId(messageRes.getGroupId())));
          senderQuery.addCriteria(Criteria.where("userId").is(new ObjectId(messageRes.getSenderId())));

          Update senderUpdate = new Update();
          senderUpdate.set("isRead", true);
          senderUpdate.set("unreadMessageCount", 0);

          BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GroupParticipants.class);
          bulkOps.updateMulti(groupQuery, groupPraticipantsUpdate);
          bulkOps.updateOne(senderQuery, senderUpdate);
          bulkOps.execute();

          groupChatRepository.save(groupChat);
          return groupChat;
     }

     public GroupChat saveGroupChatWhenGroupCreated(
               List<Member> members,
               MessageRequestBody messageRequestBody) {
          List<GroupParticipants> groupParticipants = new ArrayList<>();
          GroupChat groupChat = GroupChat.builder()
                    .groupId(messageRequestBody.getGroupId())
                    .senderId(new ObjectId(messageRequestBody.getSenderId()))
                    .type(messageRequestBody.getType())
                    .messageId(messageRequestBody.getMessageId())
                    .replies(new ArrayList<>())
                    .isDeleted(false)
                    .isForwardMessage(messageRequestBody.isForwardMessage())
                    .build();
          members.forEach(m -> {
               boolean isUser = messageRequestBody.getSenderId().equals(m.getUserId().toHexString());
               groupParticipants.add(GroupParticipants.builder()
                         .groupId(new ObjectId(messageRequestBody.getGroupId()))
                         .userId(m.getUserId())
                         .senderId(new ObjectId(messageRequestBody.getSenderId()))
                         .lastMessageId(messageRequestBody.getMessageId())
                         .isRead(isUser)
                         .unreadMessageCount(isUser ? 0 : 1)
                         .pinnedAt(null)
                         .build());
          });
          groupParticipentService.saveAllGroupParticipants(groupParticipants);
          groupChatRepository.save(groupChat);
          return groupChat;
     }

     public Object updateReply(MessageRequestBody messageRequestBody) {

          ObjectId recipientId = messageRequestBody.getRecipientId() != null
                    ? new ObjectId(messageRequestBody.getRecipientId())
                    : null;
          Reply reply = Reply.builder()
                    .senderId(new ObjectId(messageRequestBody.getSenderId()))
                    .recipientId(recipientId)
                    .replyMessageId(messageRequestBody.getMessageId())
                    .createdAt(new Date()).updatedAt(new Date())
                    .build();
          Query query = new Query(
                    Criteria.where(MESSAGEID).is(messageRequestBody.getReplyToMessageId()));
          Update update = new Update()
                    .push("replies", reply);
          UpdateResult updateResult = mongoTemplate.updateFirst(
                    query,
                    update,
                    GroupChat.class);

          log.info("Reply added successfully: " + updateResult.getModifiedCount());
          return updateResult.getModifiedCount();
     }

     public Object getReplyForMessage(
               String groupId,
               String messageId,
               int pageNo,
               int rowPerPage) {

          Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where(GROUPID).is(groupId)),
                    Aggregation.match(Criteria.where(MESSAGEID).is(messageId)),
                    Aggregation.match(Criteria.where(ISDELETED).is(false)), Aggregation.unwind("replies"),
                    LookupOperation.newLookup().from(USERS).localField("replies.senderId").foreignField("_id")
                              .as(SENDER),
                    LookupOperation.newLookup().from(USERS).localField("replies.recipientId").foreignField("_id")
                              .as("recipient"),
                    LookupOperation.newLookup().from("messages").localField("replies.replyMessageId")
                              .foreignField("_id")
                              .as(MESSAGE),
                    Aggregation.project().and(ArrayOperators.ArrayElemAt.arrayOf(MESSAGE).elementAt(0)).as(MESSAGE)
                              .and(ArrayOperators.ArrayElemAt.arrayOf("sender._id").elementAt(0)).as(SENDERID)
                              .and(ArrayOperators.ArrayElemAt.arrayOf("sender.fullname").elementAt(0)).as(SENDERNAME)
                              .and(ArrayOperators.ArrayElemAt.arrayOf("sender.profilePicture").elementAt(0))
                              .as("senderProfilePicture")
                              .and(ArrayOperators.ArrayElemAt.arrayOf("recipient._id").elementAt(0)).as("recipientId")
                              .and(ArrayOperators.ArrayElemAt.arrayOf("recipient.fullname").elementAt(0))
                              .as("recipientName")
                              .and(ArrayOperators.ArrayElemAt.arrayOf("recipient.profilePicture").elementAt(0))
                              .as("recipientProfilePicture").and("message.createdAt").as(CREATEDAT),
                    Aggregation.sort(Sort.by(Sort.Direction.DESC, CREATEDAT)));
          List<ReplyRes> replies = mongoTemplate.aggregate(aggregation, "groupChats", ReplyRes.class)
                    .getMappedResults();

          int totalCount = replies.size();
          int fromIndex = Math.min((pageNo - 1) * rowPerPage, totalCount);
          int toIndex = Math.min(((pageNo - 1) * rowPerPage) + rowPerPage, totalCount);
          int totalNumberOfPages = (int) Math.ceil((double) totalCount / rowPerPage);
          List<ReplyRes> subReplyRes = replies.subList(fromIndex, toIndex);
          subReplyRes.forEach(res -> {
               if (res.getSenderProfilePicture() != null
                         && !res.getSenderProfilePicture().isEmpty()) {
                    String senderProfilePicture = azureBlobAdapter.getBlobUri(res.getSenderProfilePicture())
                              .toString();
                    res.setSenderProfilePicture(senderProfilePicture);
               }
          });
          Map<String, Object> response = new HashMap<>();
          response.put("totalCount", totalCount);
          response.put("groupThreadMessages", subReplyRes);
          response.put("totalNumberOfPages", totalNumberOfPages);

          return ResponseService.successResponse(200, SUCCESS, response);
     }

     public Object deleteGroupChat(
               String userId,
               String groupId) {

          Query query = new Query(
                    Criteria.where(GROUPID)
                              .is(groupId)
                              .and("isDeletedBy").nin(userId));
          Update update = new Update();
          update.push("isDeletedBy").each(userId);
          mongoTemplate.updateMulti(
                    query,
                    update,
                    GroupChat.class);
          groupParticipentService.deleteByGroupId(
                    userId,
                    groupId);
          pinnedMessagesService.deleteByGroupId(groupId);
          return ResponseService.successResponse(200, SUCCESS, "Group chat deleted successfully");
     }

     public Object updateSeenAt(
               String userId,
               String groupId) {

          Query query = new Query(
                    Criteria.where(GROUPID)
                              .is(new ObjectId(groupId))
                              .and("userId").is(new ObjectId(userId)));
          Update update = new Update();
          update.set("isRead", "true");
          update.set("unreadMessageCount", 0);
          // update.push("isReadBy").each(userId);
          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    mongoTemplate.updateFirst(
                              query,
                              update,
                              GroupParticipants.class).getModifiedCount() + " message updated.");
     }

}
