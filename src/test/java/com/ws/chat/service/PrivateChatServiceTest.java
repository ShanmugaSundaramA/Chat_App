package com.ws.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
import com.ws.chat.model.InboxParticipants;
import com.ws.chat.model.PrivateChat;
import com.ws.chat.model.Type;
import com.ws.chat.repository.PrivateChatRepo;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.responsebody.MessageRes;
import com.ws.chat.responsebody.PrivateChatRes;
import com.ws.chat.responsebody.ResponseDTO;

@ExtendWith(MockitoExtension.class)
class PrivateChatServiceTest {

     @InjectMocks
     PrivateChatService privateChatService;

     @Mock
     MongoTemplate mongoTemplate;
     @Mock
     ChatRoomService chatRoomService;
     @Mock
     PrivateChatRepo privateChatRepo;
     @Mock
     InboxParticipantsService inboxParticipantsService;
     @Mock
     AzureBlobAdapter azureBlobAdapter;
     @Mock
     private UpdateResult updateResult;

     String senderId = "661f7e401a7cef5423668645";
     String recipientId = "661f674e4757cf5c0a0dea0c";
     String chatId = "661f7e401a7cef5423668645_661f674e4757cf5c0a0dea0c";

     @SuppressWarnings("unchecked")
     @Test
     void getPrivateMessagesTest() throws StorageException, URISyntaxException {

          when(chatRoomService.getChatRoomId(
                    senderId,
                    recipientId,
                    false)).thenReturn(Optional.of(chatId));

          AggregationResults<PrivateChatRes> aggregationResultsMock = mock(AggregationResults.class);
          when(aggregationResultsMock.getMappedResults()).thenReturn(Arrays.asList(getPrivateChatRes()));

          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    eq("privateChats"),
                    eq(PrivateChatRes.class))).thenReturn(aggregationResultsMock);

          Object result = privateChatService.getPrivateMessages(
                    senderId,
                    recipientId,
                    1,
                    10);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
     }

     @Test
     void getPrivateMessagesIfChatIdIsNullTest() throws StorageException, URISyntaxException {

          when(chatRoomService.getChatRoomId(
                    senderId,
                    recipientId,
                    false)).thenReturn(Optional.empty());

          Object result = privateChatService.getPrivateMessages(
                    senderId,
                    recipientId,
                    1,
                    10);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          assertEquals(Arrays.asList(), response.getData());
     }

     @Test
     void savePrivateChatTest() {

          MessageRequestBody messageRes = new MessageRequestBody();
          messageRes.setSenderId("661f674e4757cf5c0a0dea0c");
          messageRes.setRecipientId("661f674e4757cf5c0a0dea0c");
          messageRes.setType(Type.TEXT);
          messageRes.setMessageId("messageId1");
          messageRes.setReplyToMessageId("replyToMessageId");
          messageRes.setReplySenderId("661f674e4757cf5c0a0dea0c");

          when(chatRoomService.getChatRoomId(
                    messageRes.getSenderId(),
                    messageRes.getRecipientId(),
                    true))
                    .thenReturn(Optional.of(chatId));

          PrivateChat privateChat = PrivateChat.builder()
                    .chatId(chatId)
                    .senderId(new ObjectId(messageRes.getSenderId()))
                    .recipientId(new ObjectId(messageRes.getRecipientId()))
                    .type(messageRes.getType())
                    .messageId(messageRes.getMessageId())
                    .replyTo(messageRes.getReplyToMessageId())
                    .replySenderId(new ObjectId(messageRes.getReplySenderId()))
                    .isDeleted(false)
                    .isForwardMessage(false)
                    .build();

          when(privateChatRepo.save(any(PrivateChat.class))).thenReturn(privateChat);

          InboxParticipants inboxParticipant = InboxParticipants.builder()
                    .chatId(chatId)
                    .senderId(new ObjectId(messageRes.getSenderId())) // new Code ObjectId
                    .recipientId(new ObjectId(messageRes.getRecipientId())) // new Code ObjectId
                    .lastMessageId(messageRes.getMessageId())
                    .isRead(false)
                    .build();
          List<InboxParticipants> inboxParticipants = new ArrayList<>();
          inboxParticipants.add(inboxParticipant);

          when(inboxParticipantsService.saveInboxParticipants(
                    any())).thenReturn(inboxParticipants);

          PrivateChat chat = privateChatService.save(messageRes);
          assertNotNull(chat);

     }

     // @Test
     void updateSeenAtTest() {

          when(chatRoomService.getChatRoomId(senderId, recipientId, false)).thenReturn(Optional.of(chatId));
          when(updateResult.getMatchedCount()).thenReturn(1L);
          when(mongoTemplate.updateMulti(
                    any(Query.class),
                    any(Update.class),
                    eq(InboxParticipants.class))).thenReturn(updateResult);

          Object result = privateChatService.updateSeenAt(senderId, recipientId);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          assertEquals("1 messages updated ", response.getData());
     }

     // @Test
     void updateSeenAtIfChatIdNotExistTest() {

          when(chatRoomService.getChatRoomId(senderId, recipientId, false)).thenReturn(Optional.empty());

          Object result = privateChatService.updateSeenAt(senderId, recipientId);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          assertEquals("0 messages updated ", response.getData());
     }

     @Test
     void deletePrivateChatTest() {

          when(mongoTemplate.updateMulti(
                    any(Query.class),
                    any(Update.class),
                    eq(PrivateChat.class))).thenReturn(updateResult);

          Object result = privateChatService.deletePrivateChat(recipientId, chatId);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          assertEquals("Private chat deleted successfully", response.getData());
     }

     MessageRes getMessage() {
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

     PrivateChatRes getPrivateChatRes() {
          PrivateChatRes privateChatRes = new PrivateChatRes();
          privateChatRes.setId("privateId123");
          privateChatRes.setSenderId("661f7e401a7cef5423668645");
          privateChatRes.setSenderName("sundar");
          privateChatRes.setRecipientId("661f7e401a7cef5423668645");
          privateChatRes.setRecipientName("Jeeva");
          privateChatRes.setGroupId("661f7e401a7cef5423668645");
          privateChatRes.setGroupName("Mockito Testing");
          privateChatRes.setMessage(getMessage());
          privateChatRes.setForwardMessage(false);
          return privateChatRes;
     }

}
