package com.ws.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.microsoft.azure.storage.StorageException;
import com.mongodb.client.result.UpdateResult;
import com.ws.chat.azureblob.AzureBlobAdapter;
import com.ws.chat.model.Group;
import com.ws.chat.model.GroupChat;
import com.ws.chat.model.GroupParticipants;
import com.ws.chat.model.Message;
import com.ws.chat.model.Type;
import com.ws.chat.repository.GroupChatRepository;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.responsebody.GroupChatRes;
import com.ws.chat.responsebody.MessageRes;
import com.ws.chat.responsebody.ReplyRes;
import com.ws.chat.responsebody.ResponseDTO;

@ExtendWith(MockitoExtension.class)
class GroupChatServiceTest {

     @Mock
     MongoTemplate mongoTemplate;
     @Mock
     GroupChatRepository groupChatRepository;
     @Mock
     GroupParticipentService groupParticipentService;
     @Mock
     AzureBlobAdapter azureBlobAdapter;
     @Mock
     UpdateResult updateResult;
     @InjectMocks
     GroupChatService groupChatService;

     String groupId = "661f8dcb1a7cef5423668662";
     String userId = "661f83f41a7cef5423668646";
     String messageId = "661f83f41a7cef5423668646";

     @SuppressWarnings("unchecked")
     @Test
     void getGroupChatTest() throws StorageException, URISyntaxException {

          Document role = new Document();
          role.put("createdAt", new Date());

          AggregationResults<Document> aggregationResultsRolesMock = mock(AggregationResults.class);
          when(aggregationResultsRolesMock.getMappedResults()).thenReturn(Arrays.asList(role));

          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    eq(Group.class),
                    eq(Document.class))).thenReturn(aggregationResultsRolesMock);

          AggregationResults<GroupChatRes> aggregationResultsMock = mock(AggregationResults.class);
          when(aggregationResultsMock.getMappedResults())
                    .thenReturn(Arrays.asList(getGroupChatRes(), getGroupChatRes()));

          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    eq("groupChats"),
                    eq(GroupChatRes.class))).thenReturn(aggregationResultsMock);

          Object result = groupChatService.getGroupChat(
                    groupId,
                    userId,
                    1,
                    10);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;

          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          assertNotNull(response.getData());
     }

     @SuppressWarnings("unchecked")
     @Test
     void getRepliesForMessageTest() {

          AggregationResults<ReplyRes> aggregationReplyResResults = mock(AggregationResults.class);

          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    anyString(),
                    eq(ReplyRes.class))).thenReturn(aggregationReplyResResults);

          Object result = groupChatService.getReplyForMessage(
                    groupId,
                    messageId,
                    1,
                    10);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          assertNotNull(response.getData());
     }

     @SuppressWarnings("unchecked")
     // @Test
     void getMediaForGroupDetailsTest() {

          // List<String> types = Arrays.asList("IMAGE", "VIDEO");

          Document role = new Document();
          role.put("createdAt", new Date());

          AggregationResults<Document> aggregationResultsRolesMock = mock(AggregationResults.class);
          when(aggregationResultsRolesMock.getMappedResults()).thenReturn(Arrays.asList(role));

          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    eq(Group.class),
                    eq(Document.class))).thenReturn(aggregationResultsRolesMock);

          AggregationResults<GroupChatRes> aggregationResultsMock = mock(AggregationResults.class);
          when(aggregationResultsMock.getMappedResults())
                    .thenReturn(Arrays.asList(getGroupChatRes(), getGroupChatRes()));

          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    eq("groupChats"),
                    eq(GroupChatRes.class))).thenReturn(aggregationResultsMock);

          // Object result = groupChatService.getMediaForGroupDetails(
          //           groupId,
          //           userId,
          //           types);
          // assertNotNull(result);
     }

     @SuppressWarnings("unchecked")
     @Test
     void getMediaMessagesTest() throws StorageException, URISyntaxException {
          AggregationResults<Document> aggregationResultsRolesMock = mock(AggregationResults.class);
          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    eq(Group.class),
                    eq(Document.class))).thenReturn(aggregationResultsRolesMock);

          AggregationResults<GroupChatRes> aggregationResultsMock = mock(AggregationResults.class);
          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    anyString(),
                    eq(GroupChatRes.class))).thenReturn(aggregationResultsMock);

          Object result = groupChatService.getMediaMessages(
                    groupId,
                    userId,
                    Arrays.asList("IMAGE", "VIDEO"),
                    1,
                    10);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          assertNotNull(response.getData());
     }

     // @Test
     void saveGroupChatTest() {

          MessageRequestBody messageRequestBody = getMessageBody();

          when(groupParticipentService.saveGroupParticipants(
                    any(GroupParticipants.class))).thenReturn(getGroupParticipants());
          when(groupChatRepository.save(any(GroupChat.class))).thenReturn(new GroupChat());
          GroupChat result = groupChatService.save(messageRequestBody);
          assertNotNull(result);
     }

     @Test
     void updateReply() {

          MessageRequestBody messageRequestBody = getMessageBody();

          when(updateResult.getModifiedCount()).thenReturn(1L);
          when(mongoTemplate.updateFirst(
                    any(Query.class),
                    any(Update.class),
                    eq(GroupChat.class))).thenReturn(updateResult);

          Object count = groupChatService.updateReply(messageRequestBody);
          assertNotNull(count);
     }

     @Test
     void deleteGroupChatTest() {

          groupChatService.deleteGroupChat(
                    "661f8dcb1a7cef5423668662",
                    "661f8dcb1a7cef5423668662");
          verify(mongoTemplate, times(1)).updateMulti(any(Query.class), any(Update.class), eq(GroupChat.class));
     }

     MessageRes getMessageRes() {
          return MessageRes.builder()
                    .id("1715577263858-904491")
                    .content("Sundar")
                    .caption("caption")
                    .type(Type.IMAGE)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .deliveredAt(new Date())
                    .sendAt(new Date())
                    .build();
     }

     GroupChatRes getGroupChatRes() {
          GroupChatRes groupChatRes = new GroupChatRes();
          groupChatRes.setId("privateId123");
          groupChatRes.setSenderId("661f7e401a7cef5423668645");
          groupChatRes.setSenderName("sundar");
          groupChatRes.setRecipientId("661f7e401a7cef5423668645");
          groupChatRes.setRecipientName("Jeeva");
          groupChatRes.setGroupId("661f7e401a7cef5423668645");
          groupChatRes.setGroupName("Mockito Testing");
          groupChatRes.setMessage(getMessageRes());
          groupChatRes.setForwardMessage(false);
          groupChatRes.setRepliesCount(10);
          return groupChatRes;
     }

     Message getMessage() {
          return Message.builder()
                    .id("1715577263858-904491")
                    .content("sundar")
                    .caption("caption")
                    .type(Type.TEXT)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .deliveredAt(new Date())
                    .sendAt(new Date())
                    .build();
     }

     MessageRequestBody getMessageBody() {

          return MessageRequestBody.builder()
                    .action("edit")
                    .senderId("661f7e401a7cef5423668645")
                    .senderName("Sundar")
                    .recipientId("661f7e401a7cef5423668645")
                    .recipientName("Jeeva")
                    .groupId("661f7e401a7cef5423668645")
                    .groupName(null)
                    .type(Type.TEXT)
                    .messageId("1715577263858-904491")
                    .message(getMessage())
                    .replySenderId("661f7e401a7cef5423668645")
                    .replyToMessageId("661f7e401a7cef5423668645")
                    .isOnline(false)
                    .isForwardMessage(false)
                    .build();
     }

     GroupParticipants getGroupParticipants() {
          return GroupParticipants.builder()
                    .id("661f7e401a7cef5423668645")
                    .groupId(new ObjectId("661f7e401a7cef5423668645"))
                    .senderId(new ObjectId("661f7e401a7cef5423668645"))
                    .type(Type.TEXT)
                    .lastMessageId("1715577263858-904491")
                    .isRead(false)
                    .build();
     }
}
