package com.ws.chat.kafka;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.service.GroupChatService;
import com.ws.chat.service.PrivateChatService;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

     @Mock
     PrivateChatService privateChatService;
     @Mock
     GroupChatService groupChatService;
     @InjectMocks
     KafkaConsumer kafkaConsumer;

     @Test
     void consumeSingleMsgTest() {

          MessageRequestBody messageReqBody = new MessageRequestBody();
          kafkaConsumer.consumeSingleMsg(messageReqBody);
          verify(privateChatService).save(messageReqBody);
     }

     @Test
     void consumeGroupMsgTest() {

          MessageRequestBody messageReqBody = new MessageRequestBody();
          kafkaConsumer.consumeGroupMsg(messageReqBody);
          verify(groupChatService).save(messageReqBody);
     }

     @Test
     void consumeGroupMsgReplyTest() {

          MessageRequestBody messageReqBody = new MessageRequestBody();
          kafkaConsumer.consumeGroupMsgReply(messageReqBody);
          verify(groupChatService).updateReply(messageReqBody);
     }
}
