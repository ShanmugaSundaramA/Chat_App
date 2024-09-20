package com.ws.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ws.chat.model.Message;
import com.ws.chat.model.PinnedMessages;
import com.ws.chat.model.Type;
import com.ws.chat.requestbody.PinnedMessageDTO;
import com.ws.chat.responsebody.ResponseDTO;
import com.ws.chat.service.PinnedMessagesService;

@ExtendWith(MockitoExtension.class)
class PinnedMessagesControllerTest {

     @Autowired
     MockMvc mockMvc;

     @Mock
     PinnedMessagesService pinnedMessagesService;

     @InjectMocks
     PinnedMessagesController pinnedMessagesController;

     private static final String userId = "6603d703294632cc703d47d9";
     private static final String recipientId = "6603d703294632cc703d47d9";
     private static final String groupId = "6603d703294632cc703d47d9";

     @BeforeEach
     public void setup() {
          mockMvc = MockMvcBuilders
                    .standaloneSetup(pinnedMessagesController)
                    .build();
     }

     @Test
     void savePinnedMessageTest() throws Exception {
          PinnedMessages pinnedMessages = getPinnedMessages();
          PinnedMessageDTO pinnedMessageDTO = new PinnedMessageDTO();
          BeanUtils.copyProperties(pinnedMessages, pinnedMessageDTO);
          pinnedMessageDTO.setUserId(userId);
          pinnedMessageDTO.setRecipientId(recipientId);

          ObjectMapper mapper = new ObjectMapper();
          String pinnedMessageInString = mapper.writeValueAsString(pinnedMessageDTO);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(pinnedMessages)
                    .build();

          when(pinnedMessagesService.savePinnedMessage(any(PinnedMessageDTO.class))).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    post("/pinnedMessage/savePinnedMessage")
                              .content(pinnedMessageInString)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.chatId").exists())
                    .andExpect(jsonPath("$.data.messageId").exists());
     }

     @Test
     void getPinnedMessageTest() throws Exception {
          PinnedMessages pinnedMessages = getPinnedMessages();
          PinnedMessageDTO pinnedMessageDTO = new PinnedMessageDTO();
          BeanUtils.copyProperties(pinnedMessages, pinnedMessageDTO);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(pinnedMessages)
                    .build();

          when(pinnedMessagesService.getPinnedMessage(userId, recipientId, groupId)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    get("/pinnedMessage/getPinnedMessage")
                              .param("userId", userId)
                              .param("recipientId", recipientId)
                              .param("groupId", groupId)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.messageId").exists())
                    .andExpect(jsonPath("$.data.message.id").exists())
                    .andExpect(jsonPath("$.data.message.type").exists())
                    .andExpect(jsonPath("$.data.message.caption").exists())
                    .andExpect(jsonPath("$.data.message.content").exists())
                    .andExpect(jsonPath("$.data.message.size").exists())
                    .andExpect(jsonPath("$.data.message.sendAt").exists())
                    .andExpect(jsonPath("$.data.message.deliveredAt").exists())
                    .andExpect(jsonPath("$.data.message.updatedAt").exists());
     }

     @Test
     void deletePinnedMessageTest() throws Exception {
          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data("Deleted successfully")
                    .build();

          when(pinnedMessagesService.deleteById(userId)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    post("/pinnedMessage/deletePinnedMessage")
                              .param("pinnedMessageId", userId)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data").value("Deleted successfully"));
     }

     private Message getMessage() {
          return Message.builder()
                    .id("1715577263858-904491")
                    .content("sundar")
                    .caption("caption")
                    .type(Type.TEXT)
                    .size(10L)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .deliveredAt(new Date())
                    .sendAt(new Date())
                    .build();
     }

     private PinnedMessages getPinnedMessages() {
          return PinnedMessages.builder()
                    .id("1111-222-333")
                    .chatId(userId + "_" + recipientId)
                    .groupId(groupId)
                    .messageId("1713780240727-571149")
                    .message(getMessage())
                    .build();
     }
}
