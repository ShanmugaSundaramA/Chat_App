package com.ws.chat.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ws.chat.azureblob.AzureBlobAdapter;
import com.ws.chat.exception.NotFound;
import com.ws.chat.kafka.KafkaConsumer;
import com.ws.chat.model.Message;
import com.ws.chat.model.Type;
import com.ws.chat.requestbody.LinkAndNum;
import com.ws.chat.requestbody.MediaDTO;
import com.ws.chat.requestbody.MediaFile;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.websocket.WebSocketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

     private final AzureBlobAdapter azureBlobAdapter;
     private final MessageService messageService;
     private final KafkaConsumer consumer;
     private final WebSocketService webSocketService;
     private final ChatRoomService chatRoomService;

     public Object saveMedia(MediaDTO mediaDTO) {

          List<MessageRequestBody> res = new ArrayList<>();
          this.handleMedia(mediaDTO.getImages(), Type.IMAGE, mediaDTO, res);
          this.handleMedia(mediaDTO.getVideos(), Type.VIDEO, mediaDTO, res);
          this.handleMedia(mediaDTO.getAudios(), Type.AUDIO, mediaDTO, res);
          this.handleMedia(mediaDTO.getDocuments(), Type.DOCUMENT, mediaDTO, res);
          this.handleLinksAndNums(mediaDTO.getContacts(), Type.CONTACT, mediaDTO, res);
          this.handleLinksAndNums(mediaDTO.getLinks(), Type.LINK, mediaDTO, res);
          return ResponseService.successResponse(200, "success", res);
     }

     public void handleMedia(
               List<MediaFile> medias,
               Type type,
               MediaDTO mediaDTO,
               List<MessageRequestBody> res) {

          if (medias != null && !medias.isEmpty()) {
               for (MediaFile mediaObj : medias) {
                    MultipartFile media = mediaObj.getMedia();
                    if (media != null && !media.isEmpty()) {
                         String uri = azureBlobAdapter.upload(media);
                         long size = media.getSize();
                         String messageId = mediaObj.getId();
                         String caption = mediaObj.getCaption();
                         res.add(sendMessage(
                                   type,
                                   size,
                                   messageId,
                                   uri,
                                   caption,
                                   mediaDTO));
                    }
               }
          }
     }

     public void handleLinksAndNums(
               List<LinkAndNum> linkAndNums,
               Type type,
               MediaDTO mediaDTO,
               List<MessageRequestBody> res) {

          if (linkAndNums != null && !linkAndNums.isEmpty()) {
               for (LinkAndNum mediaObj : linkAndNums) {
                    String media = mediaObj.getMedia();
                    if (media != null && !media.isEmpty()) {
                         res.add(sendMessage(
                                   type,
                                   0L,
                                   mediaObj.getId(),
                                   media,
                                   null,
                                   mediaDTO));
                    }
               }
          }
     }

     public MessageRequestBody sendMessage(
               Type type,
               Long size,
               String messageId,
               String content,
               String caption,
               MediaDTO mediaDTO) {

          MessageRequestBody messageRequestBody = new MessageRequestBody();
          Message msg = Message.builder()
                    .id(messageId)
                    .type(type)
                    .size(size)
                    .content(content)
                    .caption(caption)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .sendAt(new Date())
                    .deliveredAt(new Date())
                    .build();

          String msgId = messageService.saveMessage(msg);
          Object uriOrMsg = msg.getContent();
          if (Arrays.asList("AUDIO", "VIDEO", "DOCUMENT", "IMAGE").contains(msg.getType().toString())) {
               uriOrMsg = azureBlobAdapter.getBlobUri(msg.getContent());
          }
          msg.setName(content);
          msg.setContent(uriOrMsg.toString());
          messageRequestBody.setSenderId(mediaDTO.getSenderId());
          messageRequestBody.setSenderName(mediaDTO.getSenderName());
          messageRequestBody.setSenderProfilePicture(mediaDTO.getSenderProfilePicture());
          messageRequestBody.setDesignation(mediaDTO.getDesignation());
          messageRequestBody.setDepartment(mediaDTO.getDepartment());
          messageRequestBody.setColorCode(mediaDTO.getColorCode());
          messageRequestBody.setType(type);
          messageRequestBody.setMessageId(msgId);
          messageRequestBody.setMessage(msg);
          messageRequestBody.setReplyToMessageId(mediaDTO.getReplyTo());

          if (StringUtils.isEmpty(mediaDTO.getGroupId())) {

               messageRequestBody.setRecipientId(mediaDTO.getRecipientId());
               messageRequestBody.setRecipientName(mediaDTO.getRecipientName());
               String chatId = chatRoomService.getChatRoomId(
                         mediaDTO.getSenderId(),
                         mediaDTO.getRecipientId(),
                         true).orElseThrow(() -> new NotFound("Chat room not found"));
               messageRequestBody.setChatId(chatId);
               messageRequestBody.setDeviceToken(mediaDTO.getDeviceToken());
               ObjectNode messageContent = messageService.convertObjectToStringify(messageRequestBody);
               webSocketService.sendMessageToRecipient(
                         messageRequestBody.getRecipientId(),
                         messageContent,
                         false,
                         messageRequestBody);
               consumer.consumeSingleMsg(messageRequestBody);
          } else {

               messageRequestBody.setGroupId(mediaDTO.getGroupId());
               messageRequestBody.setGroupName(mediaDTO.getGroupName());
               messageRequestBody.setGroupProfilePicture(mediaDTO.getGroupProfilePicture());
               ObjectNode messageContent = messageService.convertObjectToStringify(messageRequestBody);
               webSocketService.sendMessageToGroup(
                         messageRequestBody.getGroupId(),
                         messageRequestBody.getSenderId(),
                         messageContent,
                         false,
                         messageRequestBody);
               consumer.consumeGroupMsg(messageRequestBody);
          }
          return messageRequestBody;
     }
}
