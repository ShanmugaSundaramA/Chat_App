package com.ws.chat.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

// import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ws.chat.exception.NotFound;
import com.ws.chat.kafka.KafkaConsumer;
import com.ws.chat.model.Group;
import com.ws.chat.model.InboxParticipants;
import com.ws.chat.model.Member;
// import com.ws.chat.model.Mention;
import com.ws.chat.model.Message;
import com.ws.chat.model.Type;
import com.ws.chat.model.User;
import com.ws.chat.repository.GroupRepository;
import com.ws.chat.repository.MessageRepository;
import com.ws.chat.repository.UserRepository;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.service.ChatRoomService;
import com.ws.chat.service.FirebaseMessagingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService extends AbstractWebSocketHandler {

     private final GroupRepository groupRepository;
     private final KafkaConsumer consumer;
     private final MessageRepository messageRepository;
     private final MongoTemplate mongoTemplate;
     private final UserRepository userRepository;
     private final FirebaseMessagingService firebaseService;
     private final ChatRoomService chatRoomService;

     private static final String GROUPID = "groupId";

     private static final Map<String, Map<String, WebSocketSession>> sessions = new HashMap<>();

     private String getUserIdFromSessionUri(WebSocketSession session) {

          URI uri = session.getUri();
          String[] userIdAndDeviceId = {};
          if (uri != null) {
               userIdAndDeviceId = uri.toString().split("/ws/");
          }
          return userIdAndDeviceId.length > 1 ? userIdAndDeviceId[1] : null;
     }

     @Override
     public void afterConnectionEstablished(WebSocketSession session) throws Exception {

          String userIdAndDeviceId = getUserIdFromSessionUri(session);
          if (userIdAndDeviceId == null) {
               return;
          }
          String[] parts = userIdAndDeviceId.split("/");
          if (parts.length < 2) {
               return;
          }
          String userId = parts[0];
          String deviceId = parts[1];
          updateisOnlineStatus(userId, true);
          if (sessions.containsKey(userId)) {
               Map<String, WebSocketSession> existingDevices = sessions.get(userId);
               existingDevices.put(deviceId, session);
          } else {
               Map<String, WebSocketSession> devices = new HashMap<>();
               devices.put(deviceId, session);
               sessions.put(userId, devices);
          }
     }

     @Override
     public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {

          if (message instanceof TextMessage textMessage) {
               handleTextMessage(session, textMessage);
          } else if (message instanceof BinaryMessage binaryMessage) {
               handleBinaryMessage(session, binaryMessage);
          } else {
               throw new IllegalStateException("UNEXPECTED WEBSOCKET MESSAGE TYPE : " + message);
          }
     }

     @Override
     protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

          ObjectMapper mapper = new ObjectMapper();

          String groupId = null;
          String groupName = null;
          String groupProfilePicture = null;
          String replyToMessageId = null;
          String replySenderId = null;

          // List<Mention> mentions = new ArrayList<>();

          try {
               JsonNode jsonNode = mapper.readTree(message.getPayload());

               Type type = Type.valueOf(jsonNode.get("type").asText());
               String action = jsonNode.get("action").asText();

               /* Sender Informations */
               String senderId = jsonNode.get("senderId").asText();
               String senderName = jsonNode.get("senderName").asText();
               String senderProfilePicture = jsonNode.get("senderProfilePicture").asText();
               String designation = jsonNode.get("designation").asText();
               String department = jsonNode.get("department").asText();
               String colorCode = jsonNode.get("colorCode").asText();
               String id = mapper.readTree(jsonNode.get("message").asText()).get("id").asText();
               String content = mapper.readTree(jsonNode.get("message").asText()).get("content").asText();
               Date sendAt = new Date();

               /*
                * for Mentioned Message
                *
                * log.info("before getting isMentioned Message : ");
                * boolean isMentionedMessage =
                * Boolean.parseBoolean(jsonNode.get("isMentionedMessage").asText());
                * log.info("after getting isMentioned Message : ");
                * log.info("isMentionedMessage : " + isMentionedMessage);
                */

               /*
                * log.info("before mentions : " + mentions);
                * if (isMentionedMessage) {
                * mentions = mapper.readValue(jsonNode.get("mentions").toString(),
                * new TypeReference<List<Mention>>() {
                * });
                * }
                * log.info("after mentions : " + mentions);
                */

               /*
                * Group information
                */
               if (jsonNode.has(GROUPID) && !jsonNode.get(GROUPID).asText().isEmpty()) {
                    groupId = jsonNode.get(GROUPID).asText();
                    groupName = jsonNode.get("groupName").asText();
                    groupProfilePicture = jsonNode.get("groupProfilePicture").asText();
               }

               /*
                * Reply Message Information
                */
               if (action.equals("reply")) {
                    replyToMessageId = mapper.readTree(jsonNode.get("replyTo").asText()).get("id").asText();
                    replySenderId = jsonNode.get("replySenderId").asText();
               }

               Message msg = Message.builder()
                         .id(id)
                         .type(type)
                         .content(content)
                         .sendAt(sendAt)
                         .deliveredAt(new Date())
                         .seenAt(null)
                         .createdAt(new Date())
                         .build();
               id = messageRepository.save(msg).getId();
               ObjectNode updatedPayload = (ObjectNode) jsonNode;

               if (groupId != null) {
                    MessageRequestBody messageReqBody = MessageRequestBody.builder()
                              .senderId(senderId)
                              .senderName(senderName)
                              .senderProfilePicture(senderProfilePicture)
                              .designation(designation)
                              .department(department)
                              .colorCode(colorCode)
                              .groupId(groupId)
                              .groupName(groupName)
                              .groupProfilePicture(groupProfilePicture)
                              .type(type)
                              .messageId(id)
                              .message(msg)
                              // .isMentionedMessage(isMentionedMessage)
                              // .mentions(mentions)
                              .replyToMessageId(replyToMessageId)
                              .replySenderId(replySenderId)
                              .build();
                    sendMessageToGroup(
                              groupId,
                              senderId,
                              updatedPayload,
                              false,
                              messageReqBody);
                    if (replyToMessageId != null && !replyToMessageId.isEmpty()) {
                         consumer.consumeGroupMsgReply(messageReqBody);
                    } else {
                         consumer.consumeGroupMsg(messageReqBody);
                    }
               } else {
                    /*
                     * Recipient Informations
                     */
                    String recipientId = jsonNode.get("recipientId").asText();
                    String recipientName = jsonNode.get("recipientName").asText();
                    // String deviceToken = jsonNode.get("deviceToken").asText();
                    
                    String chatId = chatRoomService.getChatRoomId(
                              senderId,
                              recipientId,
                              true).orElseThrow(() -> new NotFound("Chat room not found"));
                    updatedPayload.put("chatId", chatId);

                    MessageRequestBody messageReqBody = MessageRequestBody.builder()
                              .senderId(senderId)
                              .senderName(senderName)
                              .senderProfilePicture(senderProfilePicture)
                              .designation(designation)
                              .department(department)
                              .colorCode(colorCode)
                              // .deviceToken(deviceToken)
                              .chatId(chatId)
                              .recipientId(recipientId)
                              .recipientName(recipientName)
                              .type(type)
                              .messageId(id)
                              .message(msg)
                              // .isMentionedMessage(isMentionedMessage)
                              // .mentions(mentions)
                              .replyToMessageId(replyToMessageId)
                              .replySenderId(replySenderId)
                              .build();
                    sendMessageToRecipient(
                              recipientId,
                              updatedPayload,
                              false,
                              messageReqBody);
                    consumer.consumeSingleMsg(messageReqBody);
               }
          } catch (IOException e) {
               log.info(e.getMessage());
          }
     }

     @Override
     public void afterConnectionClosed(
               WebSocketSession session,
               CloseStatus status) throws Exception {

          String userIdAndDeviceId = getUserIdFromSessionUri(session);
          if (userIdAndDeviceId == null) {
               log.info("SESSION DOES NOT CONTAIN USER ID AND DEVICE ID");
               return;
          }
          String[] parts = userIdAndDeviceId.split("/");
          if (parts.length < 2) {
               log.info("SESSION DOES NOT CONTAIN BOTH USER ID AND DEVICE ID");
               return;
          }
          String userId = parts[0];
          updateisOnlineStatus(userId, false);
          Map<String, WebSocketSession> existingDevices = sessions.get(userId);
          if (existingDevices != null) {
               Iterator<Map.Entry<String, WebSocketSession>> iterator = existingDevices.entrySet().iterator();
               while (iterator.hasNext()) {
                    Map.Entry<String, WebSocketSession> entry = iterator.next();
                    if (entry.getValue().equals(session)) {
                         iterator.remove();
                         break;
                    }
               }
               if (existingDevices.isEmpty()) {
                    sessions.remove(userId);
               }
          } else {
               log.info("NO SESSIONS FOUND FOR USER ID: " + userId);
          }
     }

     public void sendMessageToRecipient(
               String recipientId,
               ObjectNode messageContent,
               boolean onlineFlag,
               MessageRequestBody messageRequestBody) {

          Optional<User> recipientOpt = userRepository.findById(recipientId);
          if (recipientOpt.isEmpty()) {
               log.info("RECIPIENT DOES NOT EXIST : " + recipientId);
               return;
          }
          User recipient = recipientOpt.get();
          
          boolean isMutedRecipient = recipient.getMutedRecipientIds() != null
                    && recipient.getMutedRecipientIds().contains(messageRequestBody.getSenderId());
          messageContent.put("mutedChat", isMutedRecipient);

          Map<String, WebSocketSession> recipientSessions = sessions.get(recipientId);
          if (recipientSessions != null && !recipientSessions.isEmpty()) {
               for (WebSocketSession recipientSession : recipientSessions.values()) {
                    if (recipientSession != null && recipientSession.isOpen()) {
                         sendMessageToSession(
                                   recipientSession,
                                   messageContent.toString());
                    }
               }
          }
          if (onlineFlag || isMutedRecipient) {
               return;
          }

          this.sendOfflineMessage(
                    recipient.getDeviceToken(),
                    false,
                    messageRequestBody);

     }

     public void sendMessageToGroup(
               String groupId,
               String userId,
               ObjectNode messageContent,
               boolean isOnline,
               MessageRequestBody messageRequestBody) {

          List<Member> members;
          List<String> groupMutedByUserId;
          Optional<Group> groupOptional = groupRepository.findById(groupId);
          if (groupOptional.isPresent()) {
               members = groupOptional.get().getMembers();
               groupMutedByUserId = groupOptional.get().getMutedByUserIds();
          } else {
               log.info("GROUP DOES NOT EXIST");
               return;
          }
          List<String> userIds = members.stream()
                    .map(m -> m.getUserId().toHexString())
                    .collect(Collectors.toList());

          List<User> membersWithDetails = userRepository.findAllById(userIds);
          ExecutorService executor = Executors.newCachedThreadPool();
          membersWithDetails.stream()
                    .filter(member -> !member.getId().equals(userId))
                    .forEach(member -> executor.submit(
                              () -> sendMessageToMember(
                                        member.getId(),
                                        messageContent,
                                        member.getDeviceToken(),
                                        isOnline,
                                        messageRequestBody,
                                        groupMutedByUserId)));
          executor.shutdown();
     }

     private void sendMessageToMember(
               String memberId,
               ObjectNode messageContent,
               String deviceToken,
               boolean isOnline,
               MessageRequestBody messageRequestBody,
               List<String> groupMutedByUserId) {
                    
          boolean isMutedGroup = groupMutedByUserId != null && groupMutedByUserId.contains(memberId);
          messageContent.put("mutedGroup", isMutedGroup);

          Map<String, WebSocketSession> recipientSessions = sessions.get(memberId);
          if (recipientSessions != null && !recipientSessions.isEmpty()) {
               sendToOpenSessions(
                         recipientSessions.values(),
                         messageContent);
          }
          if (isOnline || isMutedGroup) {
               return;
          }
          this.sendOfflineMessage(
                    deviceToken,
                    true,
                    messageRequestBody);

     }

     private void sendToOpenSessions(
               Collection<WebSocketSession> sessions,
               ObjectNode messageContent) {

          sessions.stream().filter(WebSocketSession::isOpen)
                    .forEach(session -> sendMessageToSession(
                              session,
                              messageContent.toString()));
     }

     private void sendMessageToSession(
               WebSocketSession session,
               String messageContent) {
          try {
               session.sendMessage(new TextMessage(messageContent));
          } catch (IOException e) {
               log.info(e.getMessage());
          }
     }

     private void sendOfflineMessage(
               String deviceToken,
               boolean isGroupMessage,
               MessageRequestBody messageRequestBody) {

          firebaseService.sendNotification(
                    deviceToken,
                    isGroupMessage,
                    messageRequestBody);

     }

     public void updateisOnlineStatus(
               String newUserId,
               boolean isOnline) {

          MessageRequestBody messageRequestBody = MessageRequestBody.builder()
                    .action("checkOnline")
                    .recipientId(newUserId)
                    .isOnline(isOnline)
                    .build();
          ObjectNode messageText = convertObjectToStringify(messageRequestBody);

          Optional<User> userOptional = userRepository.findById(newUserId);
          if (userOptional.isPresent()) {
               User user = userOptional.get();
               user.setOnline(isOnline);
               userRepository.save(user);
          }
          Query query = new Query();
          query.addCriteria(Criteria.where("senderId").is(new ObjectId(newUserId)));
          List<InboxParticipants> inboxParticipants = mongoTemplate.find(query, InboxParticipants.class);

          Query queryGroup = new Query();
          queryGroup.addCriteria(Criteria.where("members.userId").in(new ObjectId(newUserId)));
          List<Group> groups = mongoTemplate.find(queryGroup, Group.class);

          Set<String> recipientIds = new HashSet<>();
          for (Group group : groups) {
               List<Member> roles = group.getMembers();
               Set<String> users = roles.stream()
                         .filter(m -> !m.getUserId().toHexString().equals(newUserId))
                         .map(m -> m.getUserId().toHexString()).collect(Collectors.toSet());
               recipientIds.addAll(users);
          }
          for (InboxParticipants inboxParticipant : inboxParticipants) {
               recipientIds.add(inboxParticipant.getRecipientId().toHexString());
          }
          try {
               ExecutorService executor = Executors.newCachedThreadPool();
               for (String recipientId : recipientIds) {
                    executor.submit(() -> sendMessageToRecipient(
                              recipientId,
                              messageText,
                              true,
                              messageRequestBody));
               }
               executor.shutdown();
          } catch (Exception e) {
               log.info(e.getMessage());
          }
     }

     public ObjectNode convertObjectToStringify(MessageRequestBody messageReqBody) {
          ObjectMapper objectMapper = new ObjectMapper();
          return objectMapper.convertValue(messageReqBody, ObjectNode.class);
     }

}