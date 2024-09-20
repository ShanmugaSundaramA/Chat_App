package com.ws.chat.kafka;

import org.springframework.stereotype.Service;

import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.service.GroupChatService;
import com.ws.chat.service.PrivateChatService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KafkaConsumer {

     private final PrivateChatService privateChatService;
     private final GroupChatService groupChatService;

     public void consumeSingleMsg(MessageRequestBody messageReqBody) {
          privateChatService.save(messageReqBody);
     }

     public void consumeGroupMsg(MessageRequestBody messageReqBody) {
          groupChatService.save(messageReqBody);
     }

     public void consumeGroupMsgReply(MessageRequestBody messageReqBody) {
          groupChatService.updateReply(messageReqBody);
     }

}