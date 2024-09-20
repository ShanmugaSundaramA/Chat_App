package com.ws.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.ws.chat.requestbody.MessageRequestBody;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseMessagingService {

     private final FirebaseMessaging firebaseMessaging;
     private final EncryptionService encryptionService;

     public void sendNotification(
               String deviceToken,
               boolean isGroupMessage,
               MessageRequestBody messagePayload) {

          if (deviceToken == null || deviceToken.isEmpty() || deviceToken.equals("null")) {
               log.info(messagePayload.getRecipientName() + " User Logout");
               return;
          }

          String groupId = "";
          String groupName = "";
          String groupProfilePicture = "";
          String recipientId = "";
          String recipientName = "";
          String recipientProfilePicture = "";
          String chatId = "";

          String decryptedString = encryptionService.decrypt(messagePayload.getMessage().getContent());

          if (isGroupMessage) {
               groupId = messagePayload.getGroupId() != null ? messagePayload.getGroupId() : "";
               groupName = messagePayload.getGroupName() != null ? messagePayload.getGroupName() : "";
               groupProfilePicture = messagePayload.getGroupProfilePicture() != null
                         ? messagePayload.getGroupProfilePicture()
                         : "";
          } else {
               recipientId = messagePayload.getRecipientId() != null ? messagePayload.getRecipientId() : "";
               recipientName = messagePayload.getRecipientName() != null ? messagePayload.getRecipientName() : "";
               recipientProfilePicture = messagePayload.getRecipientProfilePicture() != null
                         ? messagePayload.getRecipientProfilePicture()
                         : "";
               chatId = messagePayload.getChatId() != null ? messagePayload.getChatId()
                         : "";
          }
          try {
               Map<String, String> chatRes = new HashMap<>();

               chatRes.put("userId", messagePayload.getSenderId());
               chatRes.put("userName", messagePayload.getSenderName());
               chatRes.put("userProfilePicture", messagePayload.getSenderProfilePicture());
               chatRes.put("userDesignation", messagePayload.getDepartment());
               chatRes.put("userDepartment", messagePayload.getDepartment());
               chatRes.put("colorCode", messagePayload.getColorCode());
               chatRes.put("groupId", groupId);
               chatRes.put("groupName", groupName);
               chatRes.put("groupProfilePicture", groupProfilePicture);
               chatRes.put("recipientId", recipientId);
               chatRes.put("recipientName", recipientName);
               chatRes.put("recipientProfilePicture", recipientProfilePicture);
               chatRes.put("chatId", chatId);
               Map<String, Object> lastMessage = new HashMap<>();
               lastMessage.put("senderId", messagePayload.getSenderId());
               lastMessage.put("senderName", messagePayload.getSenderName());
               lastMessage.put("senderProfilePicture", messagePayload.getSenderProfilePicture());
               lastMessage.put("content", messagePayload.getMessage().getContent());
               lastMessage.put("type", messagePayload.getMessage().getType());
               lastMessage.put("createdAt", messagePayload.getMessage().getCreatedAt());
               lastMessage.put("updatedAt", messagePayload.getMessage().getUpdatedAt());
               lastMessage.put("read", "false");
               chatRes.put("lastMessage", convertObjectToStringify(lastMessage));
               log.info("ChatRes  : " + chatRes);

               Notification notification = Notification.builder()
                         .setTitle(isGroupMessage ? messagePayload.getGroupName() : messagePayload.getSenderName())
                         .setBody(decryptedString)
                         .build();

               AndroidConfig androidConfig = AndroidConfig.builder()
                         .setPriority(AndroidConfig.Priority.HIGH)
                         .setNotification(AndroidNotification.builder()
                                   .setSound("default")
                                   .build())
                         .build();

               Message message = Message.builder()
                         .setToken(deviceToken)
                         .setNotification(notification)
                         .setAndroidConfig(androidConfig)
                         .putAllData(chatRes)
                         .build();
               firebaseMessaging.send(message);
          } catch (Exception e) {
               log.error("Error sending notification", e);
          }

     }

     public String convertObjectToStringify(Map<String, Object> lastMessage) {
          ObjectMapper objectMapper = new ObjectMapper();
          String messageText = null;
          try {
               messageText = objectMapper.writeValueAsString(lastMessage);
          } catch (JsonProcessingException e) {
               log.info(e.getMessage());
          }
          return messageText;
     }

}