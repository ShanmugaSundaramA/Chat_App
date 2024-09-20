package com.ws.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.springframework.data.mongodb.core.query.Query;

import com.ws.chat.exception.NotFound;
import com.ws.chat.kafka.KafkaConsumer;
import com.ws.chat.model.Group;
import com.ws.chat.model.GroupChat;
// import com.ws.chat.model.GroupParticipants;
import com.ws.chat.model.InboxParticipants;
import com.ws.chat.model.Member;
import com.ws.chat.model.Message;
import com.ws.chat.model.PrivateChat;
import com.ws.chat.model.Type;
import com.ws.chat.repository.GroupChatRepository;
import com.ws.chat.repository.GroupParticipantsRepo;
import com.ws.chat.repository.GroupRepository;
import com.ws.chat.repository.InboxParticipantsRepository;
import com.ws.chat.repository.MessageRepository;
import com.ws.chat.repository.PrivateChatRepo;
import com.ws.chat.requestbody.ForwardDTO;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.requestbody.RecipientId;

import com.ws.chat.responsebody.ResponseDTO;
import com.ws.chat.websocket.WebSocketService;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

     @InjectMocks
     MessageService messageService;
     @Mock
     MessageRepository messageRepository;
     @Mock
     PrivateChatRepo privateChatRepo;
     @Mock
     GroupChatRepository groupChatRepository;
     @Mock
     MongoTemplate mongoTemplate;
     @Mock
     InboxParticipantsRepository inboxParticipantsRepository;
     @Mock
     GroupParticipantsRepo groupParticipantsRepo;
     @Mock
     WebSocketService webSocketService;
     @Mock
     KafkaConsumer kafkaConsumer;
     @Mock
     PinnedMessagesService pinnedMessagesService;
     @Mock
     GroupRepository groupRepository;

     @Test
     void saveMessageTest() {
          Message message = getMessage();
          when(messageRepository.save(any(Message.class))).thenReturn(message);
          String messageId = messageService.saveMessage(message);
          assertNotNull(messageId);
     }

     @Test
     void findByIdMessageDoesNotExistThrowsNotFound() {

          String messageId = "1111-2222-3333";
          when(messageRepository.findById(anyString())).thenReturn(Optional.empty());
          NotFound exception = assertThrows(NotFound.class, () -> messageService.findById(messageId));
          assertEquals("Message does not exist : " + messageId, exception.getMessage());
     }

     @Test
     void updateMessageForPrivateTest() {

          MessageRequestBody messageRequestBody = getMessageBody(null, null);
          Message message = getMessage();

          when(messageRepository.findById("1715577263858-904491")).thenReturn(Optional.of(message));
          Object result = messageService.updateMessage(messageRequestBody);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          verify(webSocketService, times(1)).sendMessageToRecipient(
                    anyString(),
                    anyString(),
                    eq(true),
                    any());
          verify(webSocketService, times(0)).sendMessageToGroup(
                    anyString(),
                    anyString(),
                    anyString(),
                    eq(true),
                    any());
          verify(messageRepository, times(1)).save(message);
     }

     @Test
     void updateMessageForGroupTest() {

          MessageRequestBody messageRequestBody = getMessageBody(
                    "661f7e401a7cef5423668667",
                    "Mockito Testing team");

          Message message = getMessage();

          when(messageRepository.findById("1715577263858-904491")).thenReturn(Optional.of(message));
          Object result = messageService.updateMessage(messageRequestBody);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          verify(webSocketService, times(0)).sendMessageToRecipient(
                    anyString(),
                    anyString(),
                    eq(true),
                    any());
          verify(webSocketService, times(1)).sendMessageToGroup(
                    anyString(),
                    anyString(),
                    anyString(),
                    eq(true),
                    any());
          verify(messageRepository, times(1)).save(message);
     }

     @Test
     void deleteMessageForRecipientTest() {
          MessageRequestBody messageRequestBody = getMessageBody(null, null);
          PrivateChat privateChat = getPrivateChat();
          List<InboxParticipants> inboxParticipants = new ArrayList<>();
          inboxParticipants.add(new InboxParticipants());
          when(privateChatRepo.findByMessageId(messageRequestBody.getMessage().getId()))
                    .thenReturn(Optional.of(privateChat));
          when(inboxParticipantsRepository.findByChatId(anyString()))
                    .thenReturn(inboxParticipants);

          Object result = messageService.deleteMessage(messageRequestBody);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          verify(webSocketService, times(1)).sendMessageToRecipient(
                    anyString(),
                    anyString(),
                    eq(true),
                    any());
          verify(webSocketService, times(0)).sendMessageToGroup(
                    anyString(),
                    anyString(),
                    anyString(),
                    eq(true),
                    any());
     }

     @Test
     void deleteMessageLastMessageNotNullForRecipientTest() {
          MessageRequestBody messageRequestBody = getMessageBody(null, null);
          PrivateChat privateChat = getPrivateChat();

          List<InboxParticipants> inboxParticipants = new ArrayList<>();
          inboxParticipants.add(new InboxParticipants());

          when(privateChatRepo.findByMessageId(messageRequestBody.getMessage().getId()))
                    .thenReturn(Optional.of(privateChat));
          when(inboxParticipantsRepository.findByChatId(anyString()))
                    .thenReturn(inboxParticipants);

          when(mongoTemplate.findOne(any(Query.class), eq(PrivateChat.class))).thenReturn(new PrivateChat());

          Object result = messageService.deleteMessage(messageRequestBody);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          verify(webSocketService, times(1)).sendMessageToRecipient(
                    anyString(),
                    anyString(),
                    eq(true),
                    any());
          verify(webSocketService, times(0)).sendMessageToGroup(
                    anyString(),
                    anyString(),
                    anyString(),
                    eq(true),
                    any());
     }

     @Test
     void deleteMessageForPrivateChatNotFoundRecipientTest() {
          MessageRequestBody messageRequestBody = getMessageBody(null, null);

          when(privateChatRepo.findByMessageId(messageRequestBody.getMessage().getId()))
                    .thenReturn(Optional.empty());
          NotFound exception = assertThrows(NotFound.class, () -> messageService.deleteMessage(messageRequestBody));
          assertEquals("Message does not exist : " + messageRequestBody.getMessage().getId(), exception.getMessage());

     }

     // @Test
     // void deleteMessageForGroupTest() {

     // MessageRequestBody messageRequestBody = getMessageBody(
     // "661f7e401a7cef5423668667",
     // "Mockito Testing team");
     // GroupChat groupChat = getGroupChat();
     // when(groupRepository.findById(messageRequestBody.getGroupId())).thenReturn(Optional.of(getGroupObject()));
     // when(groupChatRepository.findByMessageId(messageRequestBody.getMessage().getId()))
     // .thenReturn(Optional.of(groupChat));
     // when(groupParticipantsRepo.findByGroupId(any()))
     // .thenReturn(Optional.of(new GroupParticipants()));

     // Object result = messageService.deleteMessage(messageRequestBody);
     // assertNotNull(result);
     // assertTrue(result instanceof ResponseDTO);
     // ResponseDTO response = (ResponseDTO) result;
     // assertEquals(200, response.getStatusCode());
     // assertEquals("success", response.getStatus());
     // verify(webSocketService, times(0)).sendMessageToRecipient(
     // anyString(),
     // anyString(),
     // eq(true),
     // any());
     // verify(webSocketService, times(1)).sendMessageToGroup(
     // anyString(),
     // anyString(),
     // anyString(),
     // eq(true),
     // any());
     // }

     // @Test
     // void deleteMessageIfLastMessageNotNullForGroupTest() {

     // MessageRequestBody messageRequestBody = getMessageBody(
     // "661f7e401a7cef5423668667",
     // "Mockito Testing team");
     // GroupChat groupChat = getGroupChat();
     // when(groupRepository.findById(messageRequestBody.getGroupId())).thenReturn(Optional.of(getGroupObject()));
     // when(groupChatRepository.findByMessageId(messageRequestBody.getMessage().getId()))
     // .thenReturn(Optional.of(groupChat));
     // when(groupParticipantsRepo.findByGroupId(any()))
     // .thenReturn(Optional.of(new GroupParticipants()));
     // when(mongoTemplate.findOne(any(Query.class),
     // eq(GroupChat.class))).thenReturn(new GroupChat());

     // Object result = messageService.deleteMessage(messageRequestBody);
     // assertNotNull(result);
     // assertTrue(result instanceof ResponseDTO);
     // ResponseDTO response = (ResponseDTO) result;
     // assertEquals(200, response.getStatusCode());
     // assertEquals("success", response.getStatus());
     // verify(webSocketService, times(0)).sendMessageToRecipient(
     // anyString(),
     // anyString(),
     // eq(true),
     // any());
     // verify(webSocketService, times(1)).sendMessageToGroup(
     // anyString(),
     // anyString(),
     // anyString(),
     // eq(true),
     // any());
     // }

     @Test
     void deleteMessageForGroupChatNotFoundRecipientTest() {
          MessageRequestBody messageRequestBody = getMessageBody(
                    "661f7e401a7cef5423668667",
                    "Mockito Testing team");
          when(groupRepository.findById(messageRequestBody.getGroupId())).thenReturn(Optional.of(getGroupObject()));
          when(groupChatRepository.findByMessageId(messageRequestBody.getMessage().getId()))
                    .thenReturn(Optional.empty());
          NotFound exception = assertThrows(NotFound.class, () -> messageService.deleteMessage(messageRequestBody));
          assertEquals("Message does not exist : " + messageRequestBody.getMessage().getId(), exception.getMessage());

     }

     @Test
     void forwardMessageForPrivateTest() {
          ForwardDTO forwardDTO = getForwardDTO("private");
          int count = forwardDTO.getRecipientIds().size();
          Message message = getMessage();
          when(messageRepository.save(any(Message.class))).thenReturn(message);

          doNothing().when(webSocketService).sendMessageToRecipient(
                    anyString(),
                    anyString(),
                    eq(false),
                    any());

          messageService.forwardMessage(forwardDTO);

          verify(messageRepository,
                    times(count)).save(any(Message.class));
          verify(webSocketService,
                    times(count)).sendMessageToRecipient(
                              anyString(),
                              anyString(),
                              eq(false),
                              any());
          verify(webSocketService, times(0)).sendMessageToGroup(
                    anyString(),
                    anyString(),
                    anyString(),
                    eq(false),
                    any());
     }

     @Test
     void forwardMessageForGroupTest() {
          ForwardDTO forwardDTO = getForwardDTO("group");
          int count = forwardDTO.getRecipientIds().size();
          Message message = getMessage();
          when(messageRepository.save(any(Message.class))).thenReturn(message);

          doNothing().when(webSocketService).sendMessageToGroup(
                    anyString(),
                    anyString(),
                    anyString(),
                    eq(false),
                    any());

          messageService.forwardMessage(forwardDTO);

          verify(messageRepository,
                    times(count)).save(any(Message.class));
          verify(webSocketService,
                    times(count)).sendMessageToGroup(
                              anyString(),
                              anyString(),
                              anyString(),
                              eq(false),
                              any());
          verify(webSocketService, times(0)).sendMessageToRecipient(
                    anyString(),
                    anyString(),
                    eq(false),
                    any());
     }

     Message getMessage() {

          return Message.builder()
                    .id("1715577263858-904491")
                    .content("sundar")
                    .caption("caption")
                    .type(Type.IMAGE)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .deliveredAt(new Date())
                    .sendAt(new Date())
                    .build();
     }

     MessageRequestBody getMessageBody(String groupId, String groupName) {

          return MessageRequestBody.builder()
                    .action("edit")
                    .senderId("661f7e401a7cef5423668645")
                    .senderName("Sundar")
                    .recipientId("661f7e401a7cef5423668645")
                    .recipientName("Jeeva")
                    .groupId(groupId)
                    .groupName(groupName)
                    .type(Type.TEXT)
                    .messageId("1715577263858-904491")
                    .message(getMessage())
                    .replySenderId("661f7e401a7cef5423668645")
                    .replyToMessageId("1715577263858-904492")
                    .isOnline(false)
                    .isForwardMessage(false)
                    .build();
     }

     ForwardDTO getForwardDTO(String typeForGroupOrPrivate) {

          List<Message> messages = new ArrayList<>();
          messages.add(getMessage());

          List<RecipientId> ids = new ArrayList<>();
          ids.add(new RecipientId(
                    "661f7e401a7cef5423668645",
                    "DeviceToken1234567890",
                    typeForGroupOrPrivate));

          return ForwardDTO.builder()
                    .senderId("661f7e401a7cef5423668645")
                    .senderName("Sundar")
                    .isForwardMessage(true)
                    .messages(messages)
                    .recipientIds(ids)
                    .build();
     }

     PrivateChat getPrivateChat() {
          return PrivateChat.builder()
                    .id("privateId123")
                    .senderId(new ObjectId("661f7e401a7cef5423668645"))
                    .recipientId(new ObjectId("661f7e401a7cef5423668645"))
                    .chatId("661f7e401a7cef5423668645_661f7e401a7cef5423668645")
                    .messageId("1715577263858-904491")
                    .isDeleted(false)
                    .isForwardMessage(false)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();
     }

     GroupChat getGroupChat() {

          return GroupChat.builder()
                    .groupId("661f7e401a7cef5423668633")
                    .senderId(new ObjectId("661f7e401a7cef5423668645"))
                    .messageId("1715577263858-904491")
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .isDeleted(false)
                    .isForwardMessage(false)
                    .build();
     }

     Group getGroupObject() {
          Group group = new Group();
          List<Member> members = new ArrayList<>();
          group.setId("661f7e401a7cef5423668667");
          group.setGroupName("Mockito");
          group.setDescription("Unit Testing");
          group.setMembers(members);
          group.setColorCode("#ffffff");
          group.setProfilePicture("http://profiles.com/profile.jpg");
          group.setCreatedBy("661f79041a7cef542366862f");
          return group;
     }
}
