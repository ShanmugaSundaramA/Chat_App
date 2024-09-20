package com.ws.chat.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ws.chat.model.Message;
import com.ws.chat.model.Type;
import com.ws.chat.requestbody.ForwardDTO;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.requestbody.RecipientId;
import com.ws.chat.responsebody.ResponseDTO;
import com.ws.chat.service.MessageService;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

     @Autowired
     MockMvc mockMvc;
     @Mock
     MessageService messageService;
     @InjectMocks
     private MessageController messageController;
     ObjectMapper mapper = new ObjectMapper();

     @BeforeEach
     public void setup() {
          mockMvc = MockMvcBuilders
                    .standaloneSetup(messageController)
                    .build();
     }

     @Test
     void testUpdateMessage() throws Exception {
          MessageRequestBody messageRequestBody = getMessageBody("edit");
          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(messageRequestBody)
                    .build();

          String messageReqBodyInString = mapper.writeValueAsString(messageRequestBody);

          when(messageService.updateMessage(messageRequestBody)).thenReturn(responseDTO);
          ResultActions result = mockMvc.perform(
                    post("/message/updateMessage")
                              .content(messageReqBodyInString)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.senderId").exists())
                    .andExpect(jsonPath("$.data.senderName").exists())
                    .andExpect(jsonPath("$.data.recipientId").exists())
                    .andExpect(jsonPath("$.data.recipientName").exists())
                    .andExpect(jsonPath("$.data.type").exists())
                    .andExpect(jsonPath("$.data.action").exists())
                    .andExpect(jsonPath("$.data.online").exists())
                    .andExpect(jsonPath("$.data.forwardMessage").exists())
                    .andExpect(jsonPath("$.data.messageId").exists())
                    .andExpect(jsonPath("$.data.message.type").exists())
                    .andExpect(jsonPath("$.data.message.type").exists())
                    .andExpect(jsonPath("$.data.message.content").exists())
                    .andExpect(jsonPath("$.data.message.caption").exists())
                    .andExpect(jsonPath("$.data.message.sendAt").exists())
                    .andExpect(jsonPath("$.data.message.deliveredAt").exists())
                    .andExpect(jsonPath("$.data.message.createdAt").exists())
                    .andExpect(jsonPath("$.data.message.updatedAt").exists());
     }

     @Test
     void deleteMessageTest() throws Exception {
          MessageRequestBody messageRequestBody = getMessageBody("delete");
          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(messageRequestBody)
                    .build();
          String messageReqBodyInString = mapper.writeValueAsString(messageRequestBody);

          when(messageService.deleteMessage(messageRequestBody)).thenReturn(responseDTO);
          ResultActions result = mockMvc.perform(
                    post("/message/deleteMessage")
                              .content(messageReqBodyInString)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.senderId").exists())
                    .andExpect(jsonPath("$.data.senderName").exists())
                    .andExpect(jsonPath("$.data.recipientId").exists())
                    .andExpect(jsonPath("$.data.recipientName").exists())
                    .andExpect(jsonPath("$.data.type").exists())
                    .andExpect(jsonPath("$.data.action").exists())
                    .andExpect(jsonPath("$.data.online").exists())
                    .andExpect(jsonPath("$.data.forwardMessage").exists())
                    .andExpect(jsonPath("$.data.messageId").exists())
                    .andExpect(jsonPath("$.data.message.type").exists())
                    .andExpect(jsonPath("$.data.message.type").exists())
                    .andExpect(jsonPath("$.data.message.content").exists())
                    .andExpect(jsonPath("$.data.message.caption").exists())
                    .andExpect(jsonPath("$.data.message.sendAt").exists())
                    .andExpect(jsonPath("$.data.message.deliveredAt").exists())
                    .andExpect(jsonPath("$.data.message.createdAt").exists())
                    .andExpect(jsonPath("$.data.message.updatedAt").exists());
     }

     @Test
     void testForwardMessage() throws Exception {
          ForwardDTO forwardDTO = getForwardDTO();
          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data("Forward message saved successfully")
                    .build();
          String forwardDTOInString = mapper.writeValueAsString(forwardDTO);

          when(messageService.forwardMessage(forwardDTO)).thenReturn(responseDTO);
          ResultActions result = mockMvc.perform(
                    post("/message/forwardMessage")
                              .content(forwardDTOInString)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data").exists());
     }

     Message getMessage() {

          return Message.builder()
                    .id("1715577263858-904491")
                    .content("sundar")
                    .caption("caption")
                    .type(Type.IMAGE)
                    .size(10L)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .deliveredAt(new Date())
                    .sendAt(new Date())
                    .build();
     }

     MessageRequestBody getMessageBody(String action) {

          return MessageRequestBody.builder()
                    .action(action)
                    .senderId("661f7e401a7cef5423668645")
                    .senderName("Shanmuga Sundaram A")
                    .recipientId("661f7e401a7cef5423668645")
                    .recipientName("Jeeva")
                    .type(Type.IMAGE)
                    .messageId("1715577263858-904491")
                    .message(getMessage())
                    .isOnline(true)
                    .isForwardMessage(false)
                    .build();
     }

     ForwardDTO getForwardDTO() {
          List<Message> messages = new ArrayList<>();
          messages.add(getMessage());

          List<RecipientId> recipientIds = new ArrayList<>();
          recipientIds.add(getRecipientId());

          return ForwardDTO.builder()
                    .senderId("6601b56cf2dad67a6e84914a")
                    .senderName("Shanmuga Sundaram A")
                    .recipientIds(recipientIds)
                    .messages(messages)
                    .isForwardMessage(true)
                    .build();
     }

     RecipientId getRecipientId() {

          return RecipientId.builder()
                    .id("6601b56cf2dad67a6e84914b")
                    .type("private")
                    .build();
     }
}
