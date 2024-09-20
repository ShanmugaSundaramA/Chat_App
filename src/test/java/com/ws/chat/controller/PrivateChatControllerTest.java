package com.ws.chat.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ws.chat.model.Type;
import com.ws.chat.responsebody.MessageRes;
import com.ws.chat.responsebody.PrivateChatRes;
import com.ws.chat.responsebody.ResponseDTO;
import com.ws.chat.service.PrivateChatService;

@ExtendWith(MockitoExtension.class)
class PrivateChatControllerTest {

     MockMvc mockMvc;
     @Mock
     PrivateChatService privateChatService;
     @InjectMocks
     PrivateChatController privateChatController;

     String senderId = "661f7e401a7cef5423668645";
     String recipientId = "661f7e401a7cef5423668645";

     @BeforeEach
     public void setup() {
          mockMvc = MockMvcBuilders
                    .standaloneSetup(privateChatController)
                    .build();
     }

     @Test
     void getMessagesTest() throws Exception {

          ArrayList<PrivateChatRes> privateChatRes = new ArrayList<>();
          PrivateChatRes chatRes = getPrivateChatRes();
          privateChatRes.add(chatRes);

          Map<String, Object> data = new HashMap<>();
          data.put("privateChatMessages", privateChatRes);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(data)
                    .build();

          when(privateChatService.getPrivateMessages(
                    senderId,
                    recipientId,
                    1,
                    10)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    get("/privateChat/getMessages")
                              .param("senderId", senderId)
                              .param("recipientId", recipientId)
                              .param("pageNo", "1")
                              .param("rowPerPage", "10")
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.privateChatMessages[0].id").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].senderId").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].senderName").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].senderProfilePicture").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].recipientId").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].recipientName").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].type").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].message.id").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].message.type").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].message.content").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].message.caption").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].message.size").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].message.name").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].message.deliveredAt").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].message.seenAt").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].message.updatedAt").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].replyTo").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].replySenderName").exists())
                    .andExpect(jsonPath("$.data.privateChatMessages[0].forwardMessage").exists());
     }

     @Test
     void updateSeenAtTest() throws Exception {

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data("1 messages updated")
                    .build();

          when(privateChatService.updateSeenAt(
                    senderId,
                    recipientId)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    get("/privateChat/updateSeenAt")
                              .param("senderId", senderId)
                              .param("recipientId", recipientId)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data").value("1 messages updated"));
     }

     @Test
     void deletePrivateChat() throws Exception {

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data("Private chat deleted successfully")
                    .build();

          when(privateChatService.deletePrivateChat(
                    recipientId,
                    senderId + "_" + recipientId)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    post("/privateChat/deletePrivateChat")
                              .param("senderId", senderId)
                              .param("chatId", senderId + "_" + recipientId)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data").value("Private chat deleted successfully"));
     }

     PrivateChatRes getPrivateChatRes() {
          PrivateChatRes privateChatRes = new PrivateChatRes();
          privateChatRes.setId("6626397f51d3962995d4f3de");
          privateChatRes.setSenderId("661f7e401a7cef5423668645");
          privateChatRes.setSenderName("arunprasad.s");
          privateChatRes.setSenderProfilePicture("");
          privateChatRes.setRecipientId("661f8dcb1a7cef5423668662");
          privateChatRes.setRecipientName("Jeeva");
          privateChatRes.setType(Type.TEXT);
          privateChatRes.setMessage(getMessageRes());
          privateChatRes.setReplySenderName("661f8dcb1a7cef5423668662");
          privateChatRes.setReplyTo(getMessageRes());
          privateChatRes.setForwardMessage(false);
          return privateChatRes;
     }

     MessageRes getMessageRes() {
          return MessageRes.builder()
                    .id("11111-2222-3333")
                    .type(Type.TEXT)
                    .content("Hello, world!")
                    .caption("Test caption")
                    .size(100L)
                    .name("Test Message")
                    .sendAt(new Date())
                    .deliveredAt(new Date())
                    .seenAt(new Date())
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();
     }
}
