package com.ws.chat.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.ws.chat.responsebody.GroupChatRes;
import com.ws.chat.responsebody.MessageRes;
import com.ws.chat.responsebody.ReplyRes;
import com.ws.chat.responsebody.ResponseDTO;
import com.ws.chat.service.GroupChatService;

@ExtendWith(MockitoExtension.class)
class GroupChatControllerTest {

     MockMvc mockMvc;
     @Mock
     GroupChatService groupChatService;
     @InjectMocks
     GroupChatController privateChatController;

     String senderId = "661f7e401a7cef5423668645";
     String groupId = "661f7e401a7cef5423668645";

     @BeforeEach
     public void setup() {
          mockMvc = MockMvcBuilders
                    .standaloneSetup(privateChatController)
                    .build();
     }

     @Test
     void getMessagesTest() throws Exception {

          ArrayList<GroupChatRes> groupChatRes = new ArrayList<>();
          GroupChatRes chatRes = getGroupChatRes(Type.TEXT);
          groupChatRes.add(chatRes);

          Map<String, Object> data = new HashMap<>();
          data.put("groupChatMessages", groupChatRes);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(data)
                    .build();

          when(groupChatService.getGroupChat(
                    groupId,
                    senderId,
                    1,
                    10)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    get("/groupChat/getMessages")
                              .param("userId", senderId)
                              .param("groupId", groupId)
                              .param("pageNo", "1")
                              .param("rowPerPage", "10")
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.groupChatMessages[0].id").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].senderId").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].senderName").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].senderProfilePicture").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].groupId").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].groupName").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].type").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.id").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.type").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.content").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.caption").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.size").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.name").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.deliveredAt").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.seenAt").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.updatedAt").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].repliesCount").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].forwardMessage").exists());
     }

     @Test
     void getRepliesForMessageTest() throws Exception {

          String messageId = "1713849090976-193408";

          ArrayList<ReplyRes> replyRes = new ArrayList<>();
          ReplyRes res = getReplyRes();
          replyRes.add(res);

          Map<String, Object> data = new HashMap<>();
          data.put("groupThreadMessages", replyRes);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(data)
                    .build();

          when(groupChatService.getReplyForMessage(
                    groupId,
                    messageId,
                    1,
                    10)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    get("/groupChat/getRepliesForMessage")
                              .param("messageId", messageId)
                              .param("groupId", groupId)
                              .param("pageNo", "1")
                              .param("rowPerPage", "10")
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].senderId").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].senderName").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].senderProfilePicture").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].recipientId").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].recipientName").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].recipientProfilePicture").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].message.id").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].message.type").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].message.content").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].message.caption").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].message.size").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].message.name").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].message.deliveredAt").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].message.seenAt").exists())
                    .andExpect(jsonPath("$.data.groupThreadMessages[0].message.updatedAt").exists());
     }

     @Test
     void getMediaMessagesTest() throws Exception {

          ArrayList<GroupChatRes> groupChatRes = new ArrayList<>();
          GroupChatRes chatRes = getGroupChatRes(Type.IMAGE);
          groupChatRes.add(chatRes);

          Map<String, Object> data = new HashMap<>();
          data.put("groupChatMessages", groupChatRes);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(data)
                    .build();

          when(groupChatService.getMediaMessages(
                    groupId,
                    senderId,
                    Arrays.asList("IMAGE", "VIDEO"),
                    1,
                    10)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    get("/groupChat/getMediaMessages")
                              .param("userId", senderId)
                              .param("groupId", groupId)
                              .param("type", "IMAGE", "VIDEO")
                              .param("pageNo", "1")
                              .param("rowPerPage", "10")
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.groupChatMessages[0].id").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].senderId").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].senderName").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].senderProfilePicture").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].groupId").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].groupName").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].type").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.id").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.type").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.content").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.caption").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.size").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.name").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.deliveredAt").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.seenAt").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].message.updatedAt").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].repliesCount").exists())
                    .andExpect(jsonPath("$.data.groupChatMessages[0].forwardMessage").exists());
     }

     @Test
     void deleteGroupChat() throws Exception {

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data("Group chat deleted successfully")
                    .build();

          when(groupChatService.deleteGroupChat(
                    senderId,
                    groupId)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    post("/groupChat/deleteGroupChat")
                              .param("userId", senderId)
                              .param("groupId", groupId)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data").value("Group chat deleted successfully"));
     }

     GroupChatRes getGroupChatRes(Type type) {
          GroupChatRes groupChatRes = new GroupChatRes();
          groupChatRes.setId("6626397f51d3962995d4f3de");
          groupChatRes.setSenderId("661f7e401a7cef5423668645");
          groupChatRes.setSenderName("arunprasad.s");
          groupChatRes.setSenderProfilePicture("");
          groupChatRes.setGroupId("6626397f51d3962995d4f3de");
          groupChatRes.setGroupName("Mockito Junit");
          groupChatRes.setType(type);
          groupChatRes.setMessage(getMessageRes(type));
          groupChatRes.setRepliesCount(10);
          groupChatRes.setForwardMessage(false);
          return groupChatRes;
     }

     ReplyRes getReplyRes() {

          return ReplyRes.builder()
                    .senderId(senderId)
                    .senderName(senderId)
                    .senderProfilePicture(senderId)
                    .recipientId(groupId)
                    .recipientName(groupId)
                    .recipientProfilePicture(groupId)
                    .message(getMessageRes(Type.TEXT))
                    .build();
     }

     MessageRes getMessageRes(Type type) {
          return MessageRes.builder()
                    .id("11111-2222-3333")
                    .type(type)
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
