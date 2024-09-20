package com.ws.chat.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ws.chat.exception.NotFound;

import com.ws.chat.kafka.KafkaConsumer;
import com.ws.chat.model.Group;
import com.ws.chat.model.GroupChat;
import com.ws.chat.model.InboxParticipants;
import com.ws.chat.model.Message;
import com.ws.chat.model.PrivateChat;
import com.ws.chat.repository.GroupChatRepository;
import com.ws.chat.repository.GroupRepository;
import com.ws.chat.repository.InboxParticipantsRepository;
import com.ws.chat.repository.MessageRepository;
import com.ws.chat.repository.PrivateChatRepo;
import com.ws.chat.requestbody.ForwardDTO;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.websocket.WebSocketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

     private final MessageRepository messageRepository;
     private final PrivateChatRepo privateChatRepo;
     private final GroupChatRepository groupChatRepo;
     private final MongoTemplate mongoTemplate;
     private final InboxParticipantsRepository inboxParticipantsRepository;
     private final GroupParticipentService groupParticipentService;
     private final WebSocketService webSocketService;
     private final KafkaConsumer consumer;
     private final PinnedMessagesService pinnedMessagesService;
     private final GroupRepository groupRepository;
     private final ChatRoomService chatRoomService;

     private static final String MESSAGENOTFOUND = "Message does not exist : ";
     private static final String GROUPNOTFOUND = "Group does not exist : ";
     private static final String SUCCESS = "success";

     public String saveMessage(Message message) {
          return messageRepository.save(message).getId();
     }

     public Message findById(String messageId) {
          return messageRepository.findById(messageId)
                    .orElseThrow(() -> new NotFound(MESSAGENOTFOUND + messageId));
     }

     public Object updateMessage(MessageRequestBody messageReqBody) {

          Message message = this.findById(messageReqBody.getMessage().getId());
          messageReqBody.setAction("edit");
          ObjectNode messageText = this.convertObjectToStringify(messageReqBody);
          if (StringUtils.isEmpty(messageReqBody.getGroupId())) {
               webSocketService.sendMessageToRecipient(
                         messageReqBody.getRecipientId(),
                         messageText,
                         true,
                         messageReqBody);
          } else {
               webSocketService.sendMessageToGroup(
                         messageReqBody.getGroupId(),
                         messageReqBody.getSenderId(),
                         messageText,
                         true,
                         messageReqBody);
          }
          message.setContent(messageReqBody.getMessage().getContent());
          message.setType(messageReqBody.getMessage().getType());
          message.setSendAt(messageReqBody.getMessage().getSendAt());
          message.setDeliveredAt(new Date());
          messageRepository.save(message);
          return ResponseService.successResponse(200, SUCCESS, messageReqBody);
     }

     public Object deleteMessage(MessageRequestBody messageReqBody) {

          List<String> isDeletedBy = new ArrayList<>();
          messageReqBody.setAction("delete");
          String msgId = messageReqBody.getMessage().getId();
          if (messageReqBody.getGroupId() == null || messageReqBody.getGroupId().isEmpty()) {
               PrivateChat privateChat = privateChatRepo.findByMessageId(msgId)
                         .orElseThrow(() -> new NotFound(MESSAGENOTFOUND + msgId));
               isDeletedBy.add(messageReqBody.getSenderId());
               isDeletedBy.add(messageReqBody.getRecipientId());
               privateChat.setIsDeletedBy(isDeletedBy);
               privateChatRepo.save(privateChat);
               Query query = new Query(Criteria
                         .where("chatId").is(privateChat.getChatId())
                         .and("isDeletedBy").nin(messageReqBody.getSenderId()))
                         .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                         .limit(1);
               PrivateChat latestChat = mongoTemplate.findOne(
                         query,
                         PrivateChat.class);
               List<InboxParticipants> inboxParticipants = inboxParticipantsRepository.findByChatId(
                         privateChat.getChatId());
               ObjectNode messageText = this.convertObjectToStringify(messageReqBody);
               if (latestChat != null) {
                    inboxParticipants.forEach(i -> {
                         i.setSendByRecipient(!latestChat.getSenderId().equals(i.getSenderId()));
                         i.setLastMessageId(latestChat.getMessageId());
                         inboxParticipantsRepository.save(i);
                    });
               } else {
                    inboxParticipantsRepository.deleteByLastMessageId(msgId);
               }
               webSocketService.sendMessageToRecipient(
                         messageReqBody.getRecipientId(),
                         messageText,
                         true,
                         messageReqBody);
          } else {
               Group group = groupRepository.findById(messageReqBody.getGroupId())
                         .orElseThrow(() -> new NotFound(GROUPNOTFOUND + messageReqBody.getGroupId()));
               GroupChat groupChat = groupChatRepo.findByMessageId(msgId)
                         .orElseThrow(() -> new NotFound(MESSAGENOTFOUND + msgId));
               isDeletedBy = group.getMembers().stream()
                         .map(m -> m.getUserId().toHexString())
                         .collect(Collectors.toList());
               groupChat.setIsDeletedBy(isDeletedBy);
               groupChatRepo.save(groupChat);
               Query query = new Query(Criteria
                         .where("groupId").is(messageReqBody.getGroupId())
                         .and("isDeletedBy").nin(messageReqBody.getSenderId()))
                         .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                         .limit(1);
               GroupChat latestChat = mongoTemplate.findOne(
                         query,
                         GroupChat.class);
               ObjectNode messageText = this.convertObjectToStringify(messageReqBody);
               if (latestChat != null) {
                    groupParticipentService.updateLastMessageId(
                              new ObjectId(messageReqBody.getGroupId()),
                              latestChat.getSenderId(),
                              latestChat.getMessageId());
               } else {
                    groupParticipentService.updateLastMessageId(
                              new ObjectId(messageReqBody.getGroupId()),
                              null,
                              null);
               }
               webSocketService.sendMessageToGroup(
                         messageReqBody.getGroupId(),
                         messageReqBody.getSenderId(),
                         messageText,
                         true,
                         messageReqBody);
          }
          pinnedMessagesService.deleteByMessageId(msgId);
          return ResponseService.successResponse(200, SUCCESS, messageReqBody);
     }

     public Object forwardMessage(ForwardDTO forwardDTO) {
          List<Object> res = new ArrayList<>();
          if (forwardDTO != null && forwardDTO.getMessages() != null) {
               forwardDTO.getMessages().forEach(message -> {
                    String content = message.getContent();
                    if (Arrays.asList("AUDIO", "VIDEO", "DOCUMENT", "IMAGE").contains(message.getType().toString())) {
                         content = message.getName();
                    }
                    message.setContent(content);
                    res.add(sendMessage(
                              message,
                              forwardDTO));
               });
          }
          return ResponseService.successResponse(200, SUCCESS, "Forward message saved successfully.");
     }

     public Object sendMessage(
               Message message,
               ForwardDTO forwardDTO) {

          this.sendforwardMessage(
                    forwardDTO,
                    message);
          log.info("FORWARD MESSAGE SAVED SUCCESSFULLY.....");
          return null;
     }

     public void sendforwardMessage(
               ForwardDTO forwardDTO,
               Message msg) {

          if (forwardDTO.getRecipientIds() != null && !forwardDTO.getRecipientIds().isEmpty()) {
               forwardDTO.getRecipientIds().stream().forEach(recipientId -> {
                    UUID uniqueId = UUID.randomUUID();
                    msg.setId(uniqueId.toString());
                    msg.setCreatedAt(new Date());
                    msg.setUpdatedAt(new Date());
                    msg.setSendAt(new Date());
                    msg.setDeliveredAt(new Date());
                    messageRepository.save(msg);

                    if (recipientId.getType().equals("private")) {
                         MessageRequestBody messageReqBody = this.buildMessageRequestBody(
                                   null,
                                   recipientId.getId(),
                                   recipientId.getDeviceToken(),
                                   msg.getId(),
                                   msg,
                                   forwardDTO);
                         String chatId = chatRoomService.getChatRoomId(
                                   forwardDTO.getSenderId(),
                                   recipientId.getId(),
                                   true).orElseThrow(() -> new NotFound("Chat room not found"));
                         messageReqBody.setChatId(chatId);
                         ObjectNode messageText = this.convertObjectToStringify(messageReqBody);
                         webSocketService.sendMessageToRecipient(
                                   recipientId.getId(),
                                   messageText,
                                   false,
                                   messageReqBody);
                         consumer.consumeSingleMsg(messageReqBody);
                    } else {
                         MessageRequestBody messageReqBody = this.buildMessageRequestBody(
                                   recipientId.getId(),
                                   null,
                                   null,
                                   msg.getId(),
                                   msg,
                                   forwardDTO);
                         ObjectNode messageText = this.convertObjectToStringify(messageReqBody);
                         webSocketService.sendMessageToGroup(
                                   recipientId.getId(),
                                   forwardDTO.getSenderId(),
                                   messageText,
                                   false,
                                   messageReqBody);
                         consumer.consumeGroupMsg(messageReqBody);
                    }

               });
          }
     }

     public MessageRequestBody buildMessageRequestBody(
               String groupId,
               String recipientId,
               String deviceToken,
               String messageId,
               Message message,
               ForwardDTO forwardDTO) {

          return MessageRequestBody.builder()
                    .action("new")
                    .senderId(forwardDTO.getSenderId())
                    .senderName(forwardDTO.getSenderName())
                    .groupId(groupId)
                    .recipientId(recipientId)
                    .deviceToken(deviceToken)
                    .type(message.getType())
                    .messageId(messageId)
                    .message(message)
                    .isForwardMessage(true)
                    .build();
     }

     public ObjectNode convertObjectToStringify(MessageRequestBody messageReqBody) {
          ObjectMapper objectMapper = new ObjectMapper();
          return objectMapper.convertValue(messageReqBody, ObjectNode.class);
     }
}
