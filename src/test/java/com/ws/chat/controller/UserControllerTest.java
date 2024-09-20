package com.ws.chat.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.ws.chat.model.Setting;
import com.ws.chat.model.Type;
import com.ws.chat.model.User;
import com.ws.chat.responsebody.LastMessage;
import com.ws.chat.responsebody.ResponseDTO;
import com.ws.chat.responsebody.UserList;
import com.ws.chat.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

     @Autowired
     private MockMvc mockMvc;
     @Mock
     private UserService userService;
     @InjectMocks
     private UserController userController;

     @BeforeEach
     public void setup() {
          mockMvc = MockMvcBuilders
                    .standaloneSetup(userController)
                    .build();
     }

     @Test
     void getUserTest() throws Exception {

          User user = getUser();
          String userId = user.getId();
          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(user)
                    .build();
          when(userService.getUser(userId)).thenReturn(responseDTO);
          ResultActions result = mockMvc.perform(
                    get("/getUser")
                              .param("userId", userId)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));
          result.andExpect(status().isOk());
          result.andExpect(jsonPath("$.statusCode").value(200));
          result.andExpect(jsonPath("$.status").value("success"));
          result.andExpect(jsonPath("$.data.id").value(user.getId()));
          result.andExpect(jsonPath("$.data.fullname").value(user.getFullname()));
          result.andExpect(jsonPath("$.data.email").value(user.getEmail()));
          result.andExpect(jsonPath("$.data.designation").value(user.getDesignation()));
          result.andExpect(jsonPath("$.data.department").value(user.getDepartment()));
     }

     @Test
     void findRecipientsForUserTest() throws Exception {

          String userId = "661f674e4757cf5c0a0dea0c";
          String searchValue = "";

          Map<String, Object> data = new HashMap<>();
          List<UserList> userLists = new ArrayList<>();
          userLists.add(getUserList());
          data.put("chatMembersList", userLists);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(data)
                    .build();

          when(userService.findRecipientsForUser(userId, searchValue, 1, 10, false)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    get("/findRecipientsForUser")
                              .param("userId", userId)
                              .param("searchValue", searchValue)
                              .param("pageNo", "1")
                              .param("rowPerPage", "10")
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));
          result.andExpect(status().isOk());
          result.andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.chatMembersList[0].userId").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].userName").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].userProfilePicture").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].userEmailId").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].userDesignation").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].userDepartment").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].colorCode").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].chatId").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].online").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].sendByRecipient").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].lastMessage.senderId").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].lastMessage.senderName").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].lastMessage.senderProfilePicture").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].lastMessage.content").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].lastMessage.type").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].lastMessage.createdAt").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].lastMessage.updatedAt").exists())
                    .andExpect(jsonPath("$.data.chatMembersList[0].lastMessage.read").exists());
     }

     @Test
     void findUserByEmailTest() throws Exception {

          User user = getUser();
          String userName = user.getFullname();
          String emailId = user.getEmail();
          String designation = user.getDesignation();
          String department = user.getDepartment();
          String deviceToken = user.getDeviceToken();
          String mobileNo = user.getMobileNo();

          when(userService.findUserByEmail(
                    userName,
                    emailId,
                    designation,
                    department,
                    deviceToken,
                    mobileNo)).thenReturn(user);
          ResultActions result = mockMvc.perform(
                    get("/findUserByEmail")
                              .param("userName", userName)
                              .param("emailId", emailId)
                              .param("designation", designation)
                              .param("department", department)
                              .param("mobileNo", mobileNo)
                              .param("deviceToken", deviceToken)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));
          result.andExpect(status().isOk());
          result.andExpect(jsonPath("$.id").exists());
          result.andExpect(jsonPath("$.fullname").value(userName));
          result.andExpect(jsonPath("$.email").value(emailId));
          result.andExpect(jsonPath("$.designation").value(designation));
          result.andExpect(jsonPath("$.department").value(department));
     }

     @Test
     void findAllTest() throws Exception {

          String userId = "661f674e4757cf5c0a0dea0c";
          String groupId = "661f79d91a7cef542366863d";
          String searchValue = "";

          Map<String, Object> data = new HashMap<>();
          List<User> users = new ArrayList<>();
          users.add(getUser());
          data.put("membersList", users);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(data)
                    .build();
          when(userService.findAll(
                    userId,
                    groupId,
                    searchValue,
                    1,
                    10)).thenReturn(responseDTO);
          ResultActions result = mockMvc.perform(
                    get("/findAll")
                              .param("userId", userId)
                              .param("groupId", groupId)
                              .param("searchValue", searchValue)
                              .param("pageNo", "1")
                              .param("rowPerPage", "10")
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));
          result.andExpect(status().isOk());
          result.andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.membersList[0].id").exists())
                    .andExpect(jsonPath("$.data.membersList[0].fullname").exists())
                    .andExpect(jsonPath("$.data.membersList[0].email").exists())
                    .andExpect(jsonPath("$.data.membersList[0].profilePicture").exists())
                    .andExpect(jsonPath("$.data.membersList[0].designation").exists())
                    .andExpect(jsonPath("$.data.membersList[0].department").exists())
                    .andExpect(jsonPath("$.data.membersList[0].online").exists())
                    .andExpect(jsonPath("$.data.membersList[0].colorCode").exists())
                    .andExpect(jsonPath("$.data.membersList[0].setting").exists())
                    .andExpect(jsonPath("$.data.membersList[0].role").exists());
     }

     @Test
     void findAllRecipientsAndGroups() throws Exception {

          String userId = "661f674e4757cf5c0a0dea0c";
          String searchValue = "";

          Map<String, Object> data = new HashMap<>();
          List<UserList> userLists = new ArrayList<>();
          userLists.add(getUserList());
          data.put("chatMembers", userLists);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(data)
                    .build();
          when(userService.findAllRecipientsAndGroups(userId, searchValue, 1, 10)).thenReturn(responseDTO);
          ResultActions result = mockMvc.perform(
                    get("/findAllRecipientsAndGroups")
                              .param("userId", userId)
                              .param("searchValue", searchValue)
                              .param("pageNo", "1")
                              .param("rowPerPage", "10")
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));
          result.andExpect(status().isOk());
          result.andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.chatMembers[0].userId").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].userName").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].userProfilePicture").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].userEmailId").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].userDesignation").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].userDepartment").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].colorCode").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].chatId").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].online").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].sendByRecipient").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].lastMessage.senderId").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].lastMessage.senderName").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].lastMessage.senderProfilePicture").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].lastMessage.content").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].lastMessage.type").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].lastMessage.createdAt").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].lastMessage.updatedAt").exists())
                    .andExpect(jsonPath("$.data.chatMembers[0].lastMessage.read").exists());

     }

     User getUser() {
          return User.builder()
                    .id("661f674e4757cf5c0a0dea0c")
                    .fullname("Sundar Vikram")
                    .email("sundarvikram.a@cavininfotech.com")
                    .profilePicture("http://profile.com/profile.png")
                    .designation("Employee")
                    .department("IT")
                    .colorCode("#ffffff")
                    .mobileNo("8939678300")
                    .setting(new Setting())
                    .role("USER")
                    .deviceToken("sundarvikramToken12345678")
                    .isOnline(true)
                    .build();
     }

     LastMessage getLastMessage() {
          return LastMessage.builder()
                    .senderId("661f8f221a7cef5423668667")
                    .senderName("saran.s")
                    .senderProfilePicture("http://profile.com/profile.png")
                    .content("sundar")
                    .type(Type.TEXT.name())
                    .isRead(false)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();
     }

     UserList getUserList() {
          return UserList.builder()
                    .userId("661f8f221a7cef5423668667")
                    .userName("saran.s")
                    .userEmailId("saran.s@hepl.com")
                    .userProfilePicture("http://profile.com/profile.png")
                    .userDesignation("Employee")
                    .userDepartment("IT")
                    .colorCode("#69B1FF")
                    .chatId("66309f4e40f5163ba5d89cc9_661f8f221a7cef5423668667")
                    .lastMessage(getLastMessage())
                    .isOnline(false)
                    .isSendByRecipient(false)
                    .build();
     }
}
