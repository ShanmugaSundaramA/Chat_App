package com.ws.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ws.chat.model.Group;
import com.ws.chat.model.Member;
import com.ws.chat.model.Setting;
import com.ws.chat.model.Type;
import com.ws.chat.model.User;
import com.ws.chat.requestbody.GroupDTO;

import com.ws.chat.responsebody.GroupChatRes;
import com.ws.chat.responsebody.GroupList;
import com.ws.chat.responsebody.LastMessage;
import com.ws.chat.responsebody.MessageRes;
import com.ws.chat.responsebody.ResponseDTO;
import com.ws.chat.service.GroupService;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

     MockMvc mockMvc;
     @Mock
     GroupService groupService;
     @Mock
     ObjectMapper mapper;
     @InjectMocks
     GroupController groupController;

     @BeforeEach
     public void setup() {
          mockMvc = MockMvcBuilders
                    .standaloneSetup(groupController)
                    .build();
     }

     @SuppressWarnings("unchecked")
     @Test
     void createGroupTest() throws Exception {
          Group group = getGroupObject();
          String groupName = group.getGroupName();
          String description = group.getDescription();
          String membersList = this.objectToString(group.getMembers());
          String createdBy = group.getCreatedBy();
          MockMultipartFile profilePicture = new MockMultipartFile("profilePicture", "test.jpg", "image/jpeg",
                    new byte[1000]);
          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(getGroupList())
                    .build();
          when(mapper.readValue(
                    eq(membersList),
                    any(TypeReference.class))).thenReturn(group.getMembers());
          when(groupService.createGroup(
                    profilePicture,
                    groupName,
                    description,
                    group.getMembers(),
                    createdBy)).thenReturn(responseDTO);
          ResultActions result = mockMvc.perform(
                    MockMvcRequestBuilders.multipart("/group/createGroup")
                              .file(profilePicture)
                              .param("groupName", groupName)
                              .param("description", description)
                              .param("members", membersList)
                              .param("createdBy", createdBy)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.userName").exists())
                    .andExpect(jsonPath("$.data.userProfilePicture").exists())
                    .andExpect(jsonPath("$.data.groupId").exists())
                    .andExpect(jsonPath("$.data.groupName").exists())
                    .andExpect(jsonPath("$.data.groupProfilePicture").exists())
                    .andExpect(jsonPath("$.data.lastMessage.senderId").exists())
                    .andExpect(jsonPath("$.data.lastMessage.senderName").exists())
                    .andExpect(jsonPath("$.data.lastMessage.senderProfilePicture").exists())
                    .andExpect(jsonPath("$.data.lastMessage.content").exists())
                    .andExpect(jsonPath("$.data.lastMessage.type").exists())
                    .andExpect(jsonPath("$.data.lastMessage.createdAt").exists())
                    .andExpect(jsonPath("$.data.lastMessage.updatedAt").exists())
                    .andExpect(jsonPath("$.data.lastMessage.read").exists());
     }

     // @Test
     void addGroupMemberTest() throws Exception {
          GroupDTO groupDTO = getGroupDTOObject();
          String groupId = groupDTO.getId();
          List<Member> members = groupDTO.getMembers();

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data("1 members added successfully")
                    .build();

          when(groupService.addGroupMember(
                    groupId,
                    members)).thenReturn(responseDTO);

          String content = objectToString(groupDTO);

          ResultActions result = mockMvc.perform(
                    post("/group/addGroupMember")
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(content)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data").value("1 members added successfully"));
     }

     @Test
     void removeUserFromGroupTest() throws Exception {

          GroupDTO groupDTO = getGroupDTOObject();
          String groupId = groupDTO.getId();
          String userId = "661f674e4757cf5c0a0dea0c";

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data("Removed successfully")
                    .build();

          when(groupService.removeUserFromGroup(
                    groupId,
                    userId)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    post("/group/removeUserFromGroup")
                              .param("groupId", groupId)
                              .param("userId", userId)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data").value("Removed successfully"));
     }

     @Test
     void deleteGroupTest() throws Exception {
          GroupDTO groupDTO = getGroupDTOObject();
          String groupId = groupDTO.getId();

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data("Group deleted successfully")
                    .build();

          when(groupService.deleteGroup(
                    groupId)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    post("/group/deleteGroup/" + groupId)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data").value("Group deleted successfully"));
     }

     @Test
     void getGroupsTest() throws Exception {
          String userId = "661f674e4757cf5c0a0dea0c";

          ArrayList<GroupList> groupLists = new ArrayList<>();
          groupLists.add(getGroupList());

          Map<String, Object> data = new HashMap<>();
          data.put("chatGroupsList", groupLists);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(data)
                    .build();

          when(groupService.getGroups(
                    userId,
                    "",
                    1,
                    10,
                    false)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    get("/group/getGroups")
                              .param("userId", userId)
                              .param("groupName", "")
                              .param("pageNo", "1")
                              .param("rowPerPage", "10")
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.chatGroupsList[0].groupId").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].groupName").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].groupProfilePicture").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].userId").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].userName").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].userProfilePicture").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].lastMessage.senderId").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].lastMessage.senderName").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].lastMessage.senderProfilePicture").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].lastMessage.content").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].lastMessage.type").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].lastMessage.createdAt").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].lastMessage.updatedAt").exists())
                    .andExpect(jsonPath("$.data.chatGroupsList[0].lastMessage.read").exists());
     }

     @Test
     void getGroupDetailsTest() throws Exception {
          Group group = getGroupObject();
          String groupId = group.getId();
          String userId = "661f674e4757cf5c0a0dea0c";

          ArrayList<User> users = new ArrayList<>();
          users.add(getUser());
          ArrayList<GroupChatRes> medias = new ArrayList<>();
          medias.add(getGroupChatRes(Type.IMAGE));
          ArrayList<GroupChatRes> documents = new ArrayList<>();
          documents.add(getGroupChatRes(Type.DOCUMENT));

          Map<String, Object> groupDetails = new HashMap<>();
          groupDetails.put("id", group.getId());
          groupDetails.put("groupName", group.getGroupName());
          groupDetails.put("groupProfilePicture", group.getProfilePicture());
          groupDetails.put("description", group.getDescription());
          groupDetails.put("colorCode", group.getColorCode());
          groupDetails.put("createdAt", group.getCreatedBy());
          groupDetails.put("members", users);
          groupDetails.put("media", medias);
          groupDetails.put("document", documents);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(groupDetails)
                    .build();

          when(groupService.getGroupDetails(
                    groupId,
                    userId)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    get("/group/getGroupDetails")
                              .param("userId", userId)
                              .param("groupId", groupId)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.groupName").exists())
                    .andExpect(jsonPath("$.data.groupProfilePicture").exists())
                    .andExpect(jsonPath("$.data.description").exists())
                    .andExpect(jsonPath("$.data.colorCode").exists())
                    .andExpect(jsonPath("$.data.createdAt").exists())
                    .andExpect(jsonPath("$.data.members[0].id").exists())
                    .andExpect(jsonPath("$.data.members[0].fullname").exists())
                    .andExpect(jsonPath("$.data.members[0].email").exists())
                    .andExpect(jsonPath("$.data.members[0].designation").exists())
                    .andExpect(jsonPath("$.data.members[0].department").exists())
                    .andExpect(jsonPath("$.data.members[0].colorCode").exists())
                    .andExpect(jsonPath("$.data.members[0].setting").exists())
                    .andExpect(jsonPath("$.data.members[0].role").exists())
                    .andExpect(jsonPath("$.data.members[0].online").exists())
                    .andExpect(jsonPath("$.data.media[0].id").exists())
                    .andExpect(jsonPath("$.data.media[0].senderId").exists())
                    .andExpect(jsonPath("$.data.media[0].senderName").exists())
                    .andExpect(jsonPath("$.data.media[0].senderProfilePicture").exists())
                    .andExpect(jsonPath("$.data.media[0].groupId").exists())
                    .andExpect(jsonPath("$.data.media[0].groupName").exists())
                    .andExpect(jsonPath("$.data.media[0].recipientId").exists())
                    .andExpect(jsonPath("$.data.media[0].recipientName").exists())
                    .andExpect(jsonPath("$.data.media[0].type").exists())
                    .andExpect(jsonPath("$.data.media[0].repliesCount").exists())
                    .andExpect(jsonPath("$.data.media[0].forwardMessage").exists())
                    .andExpect(jsonPath("$.data.media[0].message").exists())
                    .andExpect(jsonPath("$.data.media[0].message.id").exists())
                    .andExpect(jsonPath("$.data.media[0].message.type").exists())
                    .andExpect(jsonPath("$.data.media[0].message.content").exists())
                    .andExpect(jsonPath("$.data.media[0].message.caption").exists())
                    .andExpect(jsonPath("$.data.media[0].message.size").exists())
                    .andExpect(jsonPath("$.data.media[0].message.name").exists())
                    .andExpect(jsonPath("$.data.media[0].message.deliveredAt").exists())
                    .andExpect(jsonPath("$.data.media[0].message.updatedAt").exists())
                    .andExpect(jsonPath("$.data.document[0].id").exists())
                    .andExpect(jsonPath("$.data.document[0].senderId").exists())
                    .andExpect(jsonPath("$.data.document[0].senderName").exists())
                    .andExpect(jsonPath("$.data.document[0].senderProfilePicture").exists())
                    .andExpect(jsonPath("$.data.document[0].groupId").exists())
                    .andExpect(jsonPath("$.data.document[0].groupName").exists())
                    .andExpect(jsonPath("$.data.document[0].recipientId").exists())
                    .andExpect(jsonPath("$.data.document[0].recipientName").exists())
                    .andExpect(jsonPath("$.data.document[0].type").exists())
                    .andExpect(jsonPath("$.data.document[0].repliesCount").exists())
                    .andExpect(jsonPath("$.data.document[0].forwardMessage").exists())
                    .andExpect(jsonPath("$.data.document[0].message").exists())
                    .andExpect(jsonPath("$.data.document[0].message.id").exists())
                    .andExpect(jsonPath("$.data.document[0].message.type").exists())
                    .andExpect(jsonPath("$.data.document[0].message.content").exists())
                    .andExpect(jsonPath("$.data.document[0].message.caption").exists())
                    .andExpect(jsonPath("$.data.document[0].message.size").exists())
                    .andExpect(jsonPath("$.data.document[0].message.name").exists())
                    .andExpect(jsonPath("$.data.document[0].message.deliveredAt").exists())
                    .andExpect(jsonPath("$.data.document[0].message.updatedAt").exists());
     }

     @Test
     void getGroupMembersTest() throws Exception {
          Group group = getGroupObject();
          String groupId = group.getId();

          ArrayList<User> users = new ArrayList<>();
          users.add(getUser());

          Map<String, Object> data = new HashMap<>();
          data.put("groupMembers", users);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(data)
                    .build();

          when(groupService.getGroupMember(
                    groupId,
                    "sundar",
                    1,
                    10,
                    false)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    get("/group/getGroupMembers")
                              .param("groupId", groupId)
                              .param("searchValue", "sundar")
                              .param("pageNo", "1")
                              .param("rowPerPage", "10")
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.groupMembers[0].id").exists())
                    .andExpect(jsonPath("$.data.groupMembers[0].fullname").exists())
                    .andExpect(jsonPath("$.data.groupMembers[0].email").exists())
                    .andExpect(jsonPath("$.data.groupMembers[0].designation").exists())
                    .andExpect(jsonPath("$.data.groupMembers[0].department").exists())
                    .andExpect(jsonPath("$.data.groupMembers[0].colorCode").exists())
                    .andExpect(jsonPath("$.data.groupMembers[0].profilePicture").exists())
                    .andExpect(jsonPath("$.data.groupMembers[0].setting").exists())
                    .andExpect(jsonPath("$.data.groupMembers[0].role").exists())
                    .andExpect(jsonPath("$.data.groupMembers[0].online").exists());
     }

     @Test
     void updateGroupMemberRoleTest() throws Exception {

          Group group = getGroupObject();

          String groupId = group.getId();
          String userId = "661f674e4757cf5c0a0dea0c";
          String isAdmin = "true";

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(1)
                    .build();

          when(groupService.updateGroupMemberRole(groupId, userId, isAdmin)).thenReturn(responseDTO);

          ResultActions result = mockMvc.perform(
                    post("/group/updateGroupMemberRole")
                              .param("groupId", groupId)
                              .param("userId", userId)
                              .param("isAdmin", isAdmin)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));

          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data").value(1));
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
                    .setting(new Setting())
                    .role("USER")
                    .isOnline(true)
                    .build();
     }

     GroupList getGroupList() {
          return GroupList.builder()
                    .groupId("661f8dcb1a7cef5423668662")
                    .groupName("Mockito")
                    .groupProfilePicture("http://profiles.com/profile.jpg")
                    .userId("661f8dcb1a7cef5423668662")
                    .userName("Sundar")
                    .userProfilePicture("https://profile.com/profile.jpg")
                    .colorCode("#ffffff")
                    .lastMessage(getLastMessage())
                    .build();
     }

     LastMessage getLastMessage() {
          return LastMessage.builder()
                    .senderId("661f8dcb1a7cef5423668662")
                    .senderName("Sundar")
                    .senderProfilePicture("https://profile.com/profile.jpg")
                    .content("Hi sundar")
                    .type(Type.TEXT.name())
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();
     }

     Group getGroupObject() {
          Group group = new Group();
          List<Member> members = getMembersObject();
          group.setId("661f8dcb1a7cef5423668662");
          group.setGroupName("Mockito");
          group.setDescription("Unit Testing");
          group.setMembers(members);
          group.setColorCode("#ffffff");
          group.setProfilePicture("http://profiles.com/profile.jpg");
          group.setCreatedBy("661f79041a7cef542366862f");
          return group;
     }

     List<Member> getMembersObject() {
          List<Member> members = new ArrayList<>();
          members.add(new Member(
                    new ObjectId("661f8dcb2d90c9179aff0826"),
                    "USER",
                    new Date()));
          return members;
     }

     GroupDTO getGroupDTOObject() {
          GroupDTO groupDTO = new GroupDTO();
          List<Member> members = getMembersObject();
          groupDTO.setId("661f8dcb1a7cef5423668662");
          groupDTO.setGroupName("Mockito");
          groupDTO.setDescription("Unit Testing");
          groupDTO.setMembers(members);
          groupDTO.setColorCode("#ffffff");
          groupDTO.setCreatedBy("661f79041a7cef542366862f");
          return groupDTO;
     }

     GroupChatRes getGroupChatRes(Type type) {
          GroupChatRes groupChatRes = new GroupChatRes();
          groupChatRes.setId("6626397f51d3962995d4f3de");
          groupChatRes.setSenderId("661f7e401a7cef5423668645");
          groupChatRes.setSenderName("arunprasad.s");
          groupChatRes.setSenderProfilePicture("");
          groupChatRes.setGroupId("661f8dcb1a7cef5423668662");
          groupChatRes.setGroupName("Fullstack");
          groupChatRes.setRecipientId("661f8dcb1a7cef5423668662");
          groupChatRes.setRecipientName("Jeeva");
          groupChatRes.setType(type);
          groupChatRes.setMessage(getMessageRes(type));
          groupChatRes.setRepliesCount(0);
          groupChatRes.setForwardMessage(false);
          return groupChatRes;
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

     String objectToString(Object obj) throws JsonProcessingException {
          ObjectMapper objMapper = new ObjectMapper();
          return objMapper.writeValueAsString(obj);
     }

}
