package com.ws.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.microsoft.azure.storage.StorageException;
import com.mongodb.client.result.UpdateResult;
import com.ws.chat.azureblob.AzureBlobAdapter;
import com.ws.chat.model.Group;
import com.ws.chat.model.Member;
import com.ws.chat.model.Type;
import com.ws.chat.model.User;
import com.ws.chat.repository.GroupRepository;
import com.ws.chat.repository.UserRepository;
import com.ws.chat.responsebody.GroupChatRes;
import com.ws.chat.responsebody.GroupList;
import com.ws.chat.responsebody.LastMessage;
import com.ws.chat.responsebody.MessageRes;
import com.ws.chat.responsebody.ResponseDTO;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

     @Mock
     GroupRepository groupRepository;
     @Mock
     MongoTemplate mongoTemplate;
     @Mock
     UserRepository userRepository;
     @Mock
     GroupChatService groupChatService;
     @Mock
     AzureBlobAdapter azureBlobAdapter;
     @Mock
     private UpdateResult updateResult;
     @InjectMocks
     GroupService groupService;

     @Value("${spring.application.colors}")
     private String[] colours;

     String[] mockColours = {
               "#AB47BC", "#42A5F5", "#26A69A", "#7E57C2", "#EC407A", "#EC407A", "#EC407A",
               "#FFC069", "#5CDBD3", "#69B1FF", "#B37FEB", "#FF85C0" };

     @Test
     void getUserIdsByGroupIdTest() {

          Group group = getGroupObject();
          when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

          List<Member> result = groupService.getUserIdsByGroupId(group.getId());
          assertEquals(group.getMembers(), result);
     }

     @Test
     void getUserIdsByGroupIdIfGroupNotFoundTest() {

          Group group = getGroupObject();
          when(groupRepository.findById(group.getId())).thenReturn(Optional.empty());

          List<Member> result = groupService.getUserIdsByGroupId(group.getId());
          assertEquals(new ArrayList<>(), result);
     }

     // @Test
     void createGroupTest() throws IllegalArgumentException, URISyntaxException {

          String groupName = "Mockito Test Group";
          String description = "Test Description";
          String createdBy = "65ba171bc69ae53a4c7f4491";
          String profilePictureName = "profile.jpg";
          String profilePictureURI = "https://example.com/profile.jpg";

          ReflectionTestUtils.setField(groupService, "colours", mockColours);

          List<Member> members = new ArrayList<>();

          MultipartFile imageFile = new MockMultipartFile(
                    "profile.jpg",
                    "profile.jpg",
                    "image/jpeg",
                    new byte[10000]);

          when(azureBlobAdapter.upload(imageFile)).thenReturn(profilePictureName);
          Group savedGroup = getGroupObject();
          when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
          when(azureBlobAdapter.getBlobUri(profilePictureName)).thenReturn(new URI(profilePictureURI));
          Object result = groupService.createGroup(
                    imageFile,
                    groupName,
                    description,
                    members,
                    createdBy);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          GroupList groupList = (GroupList) response.getData();
          assertNotNull(groupList);
          assertEquals("65ba171bc69ae53a4c7f4491", groupList.getGroupId());
          assertEquals(groupName, groupList.getGroupName());
          assertEquals(profilePictureURI, groupList.getGroupProfilePicture());
          assertNotNull(groupList.getColorCode());
          assertNotNull(groupList.getLastMessage());
     }

     @Test
     void createGroupWithNonImageProfilePictureTest() {

          String groupName = "Mockito Test Group";
          String description = "Test Description";
          String createdBy = "admin123";

          ReflectionTestUtils.setField(groupService, "colours", mockColours);

          List<Member> members = new ArrayList<>();

          MultipartFile imageFile = new MockMultipartFile(
                    "profile.jpg",
                    "profile.jpg",
                    "video/jpeg",
                    new byte[10000]);

          assertThrows(IllegalArgumentException.class,
                    () -> groupService.createGroup(imageFile, groupName, description, members, createdBy));

          verify(azureBlobAdapter, never()).upload(any(MultipartFile.class));
          verify(groupRepository, never()).save(any(Group.class));
     }

     @Test
     void updateGroupTest() throws URISyntaxException {

          String groupId = "65ba171bc69ae53a4c7f4491";
          String groupName = "Updated Group Name";
          String description = "Updated Description";
          String profilePictureName = "profile.jpg";
          String profilePictureURI = "https://example.com/profile.jpg";

          Group group = getGroupObject();

          MultipartFile imageFile = new MockMultipartFile(
                    "profile.jpg",
                    "profile.jpg",
                    "image/jpeg",
                    new byte[1]);

          when(groupService.findById(groupId)).thenReturn(Optional.of(group));
          when(azureBlobAdapter.upload(imageFile)).thenReturn(profilePictureName);
          when(groupRepository.save(any(Group.class))).thenReturn(group);
          when(azureBlobAdapter.getBlobUri(profilePictureName)).thenReturn(new URI(profilePictureURI));

          Object result = groupService.updateGroup(groupId, imageFile, groupName, description);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);

          ResponseDTO responseDTO = (ResponseDTO) result;
          assertEquals(200, responseDTO.getStatusCode());
          assertEquals("success", responseDTO.getStatus());

          GroupList groupList = (GroupList) responseDTO.getData();
          assertNotNull(groupList);
          assertEquals(groupId, groupList.getGroupId());
          assertEquals(groupName, groupList.getGroupName());
          assertEquals(profilePictureURI, groupList.getGroupProfilePicture());
          assertNotNull(groupList.getLastMessage());

     }

     @Test
     void updateGroupWithNonImageProfilePictureTest() {

          String groupId = "group123";
          String groupName = "Updated Group Name";
          String description = "Updated Description";
          MultipartFile imageFile = new MockMultipartFile(
                    "profile.jpg",
                    "profile.jpg",
                    "video/mp4",
                    new byte[1]);

          when(groupService.findById(groupId)).thenReturn(Optional.of(getGroupObject()));
          IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
                    () -> groupService.updateGroup(groupId, imageFile, groupName, description));

          assertEquals("Content is not an image", result.getMessage());
     }

     @Test
     void addGroupMembers() {
          String groupId = "669115f9c5176057331739a9";
          List<Member> members = getMembersObject();

          when(mongoTemplate.updateFirst(
                    any(Query.class),
                    any(Update.class),
                    eq(Group.class))).thenReturn(updateResult);

          Object result = groupService.addGroupMember(groupId, members);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);

          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());

          String message = (String) response.getData();
          assertEquals(members.size() + " members added successfully", message);
     }

     @Test
     void removeUserFromGroupTest() {

          String userId = "669115f9c5176057331739a8";
          String groupId = "669115f9c5176057331739a9";

          when(mongoTemplate.updateFirst(
                    any(Query.class),
                    any(Update.class),
                    eq(Group.class))).thenReturn(updateResult);
          Object result = groupService.removeUserFromGroup(groupId, userId);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);

          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          String message = (String) response.getData();
          assertEquals("Removed successfully", message);
     }

     @Test
     void deleteGroupTest() {
          String groupId = "group1234";
          groupService.deleteGroup(groupId);
          verify(groupRepository, times(1)).deleteById(groupId);
     }

     @Test
     void getGroupsByUserId() {

          Group group = getGroupObject();
          List<Group> groups = new ArrayList<>();
          groups.add(group);
          when(mongoTemplate.find(
                    any(Query.class),
                    eq(Group.class))).thenReturn(groups);

          List<Group> groupsOutput = groupService.getGroupsByUserId("user1234", "");
          assertNotNull(groupsOutput);
     }

     @SuppressWarnings("unchecked")
     // @Test
     void getGroupsTest() throws URISyntaxException, StorageException {

          URI uri = new URI("https://example.com/image.jpg");
          List<Group> groups = new ArrayList<>();
          groups.add(getGroupObject());
          // when(userRepository.findById("userId")).thenReturn(Optional.of(getUser("userId")));
          Document senderDoc = new Document()
                    .append("_id", new ObjectId("60d5f9f2f2f2f2f2f2f2f2f2"))
                    .append("fullname", "Shanmuga sundar A")
                    .append("profilePicture", "https://example.com/image.jpg");
          Document lastMessageDoc = new Document()
                    .append("type", "TEXT")
                    .append("content", "This is a test message");
          Document resultDoc = new Document("isRead", true)
                    .append("lastMessage", lastMessageDoc)
                    .append("sender", senderDoc)
                    .append("createdAt", new Date())
                    .append("updatedAt", new Date());
          List<Document> documents = new ArrayList<>();
          documents.add(resultDoc);

          when(mongoTemplate.find(
                    any(Query.class),
                    eq(Group.class))).thenReturn(groups);
          AggregationResults<Document> aggregationResultsMock = mock(AggregationResults.class);
          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    anyString(),
                    eq(Document.class))).thenReturn(aggregationResultsMock);
          when(aggregationResultsMock.getMappedResults()).thenReturn(documents);
          when(azureBlobAdapter.getBlobUri(any())).thenReturn(uri);
          Object result = groupService.getGroups(
                    "669115f9c5176057331739a8",
                    "669115f9c5176057331739a9",
                    1,
                    10,
                    false);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
     }

     // @Test
     // void getGroupMemberTest() {

     //      Group group = getGroupObject();
     //      when(groupService.findById("669115f9c5176057331739a9")).thenReturn(Optional.of(group));
     //      when(userRepository.findById("669115f9c5176057331739a9"))
     //                .thenReturn(Optional.of(getUser("669115f9c5176057331739a9")));

     //      Object result = groupService.getGroupMember(
     //                "669115f9c5176057331739a9",
     //                "sundar",
     //                1,
     //                1);
     //      assertNotNull(result);
     //      assertTrue(result instanceof ResponseDTO);
     //      ResponseDTO responseDTO = (ResponseDTO) result;
     //      assertEquals(200, responseDTO.getStatusCode());
     //      assertEquals("success", responseDTO.getStatus());
     // }

     @SuppressWarnings("unchecked")
     @Test
     void getGroupDetailsTest() throws URISyntaxException, StorageException {

          Group group = getGroupObject();
          Member member = new Member();
          member.setRole("ADMIN");
          member.setUserId(new ObjectId("669115f9c5176057331739a9"));
          List<Member> members = new ArrayList<>();
          members.add(member);

          AggregationResults<Member> aggregationResultsMock = mock(AggregationResults.class);
          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    anyString(),
                    eq(Member.class))).thenReturn(aggregationResultsMock);
          when(aggregationResultsMock.getMappedResults()).thenReturn(members);

          when(groupService.findById("669115f9c5176057331739a9")).thenReturn(Optional.of(group));
          when(azureBlobAdapter.getBlobUri("profile.jpg")).thenReturn(new URI("http://profiles.com/profile.com"));
          when(userRepository.findById("669115f9c5176057331739a9"))
                    .thenReturn(Optional.of(getUser("669115f9c5176057331739a9")));

          Object result = groupService.getGroupDetails("669115f9c5176057331739a9", "669115f9c5176057331739a9");
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
     }

     @SuppressWarnings("unchecked")
     // @Test
     void getGroupDetailsIfGroupNotExistTest() throws StorageException, URISyntaxException {

          Member member = new Member();
          member.setRole("ADMIN");
          member.setUserId(new ObjectId("669115f9c5176057331739a9"));
          List<Member> members = new ArrayList<>();
          members.add(member);

          AggregationResults<Member> aggregationResultsMock = mock(AggregationResults.class);
          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    anyString(),
                    eq(Member.class))).thenReturn(aggregationResultsMock);
          when(aggregationResultsMock.getMappedResults()).thenReturn(members);
          when(groupService.findById("group123")).thenReturn(Optional.empty());

          Object result = groupService.getGroupDetails(
                    "group123",
                    "user123");
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals(new ArrayList<>(), response.getData());
     }

     // @Test
     void updateGroupMemberRoleTest() {
          String groupId = "groupId123";
          String userId = "userId123";
          String role = "ADMIN";

          when(updateResult.getModifiedCount()).thenReturn(1L);
          when(mongoTemplate.updateFirst(
                    any(Query.class),
                    any(Update.class),
                    eq(Group.class))).thenReturn(updateResult);

          Object result = groupService.updateGroupMemberRole(groupId, userId, role);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
          Long message = (Long) response.getData();
          assertEquals(1, message);
     }

     // @Test
     void updateGroupMemberRoleNotModifiedTest() {
          String groupId = "groupId123";
          String userId = "userId123";
          String role = "ADMIN";

          when(updateResult.getModifiedCount()).thenReturn(0L);
          when(mongoTemplate.updateFirst(
                    any(Query.class),
                    any(Update.class),
                    eq(Group.class))).thenReturn(updateResult);

          Object result = groupService.updateGroupMemberRole(groupId, userId, role);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(304, response.getStatusCode());
          assertEquals("notModified", response.getStatus());
          assertEquals("No modification occurred", response.getData());
     }

     Group getGroupObject() {
          Group group = new Group();
          List<Member> members = getMembersObject();

          group.setId("65ba171bc69ae53a4c7f4491");
          group.setGroupName("Mockito Test Group");
          group.setDescription("Test Description");
          group.setMembers(members);
          group.setColorCode("#ffffff");
          group.setCreatedBy("65ba171bc69ae53a4c7f4491");
          group.setProfilePicture("profile.jpg");
          return group;
     }

     GroupList getGroupList() {
          return GroupList.builder()
                    .groupId("group123")
                    .groupName("Mockito Test Group")
                    .groupProfilePicture(null)
                    .userId("userId")
                    .userName("Sundar")
                    .userProfilePicture("https://profile.com/profile.jpg")
                    .colorCode("#ffffff")
                    .lastMessage(getMessage())
                    .build();
     }

     LastMessage getMessage() {
          return LastMessage.builder()
                    .content("sundar")
                    .type(Type.TEXT.name())
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();
     }

     List<Member> getMembersObject() {
          List<Member> members = new ArrayList<>();
          members.add(new Member(new ObjectId("669115f9c5176057331739a9"), "USER", new Date()));
          members.add(new Member(new ObjectId("669115f9c5176057331739a9"), "USER", new Date()));
          return members;
     }

     User getUser(String userId) {
          return User.builder()
                    .id(userId)
                    .fullname("Sundar Vikram")
                    .email("sundarvikram.a@cavininfotech.com")
                    .designation("Employee")
                    .department("IT")
                    .role("USER")
                    .build();
     }

     GroupChatRes getGroupChatRes() {
          GroupChatRes groupChatRes = new GroupChatRes();
          groupChatRes.setId("privateId123");
          groupChatRes.setSenderId("661f7e401a7cef5423668645");
          groupChatRes.setSenderName("sundar");
          groupChatRes.setRecipientId("661f7e401a7cef5423668645");
          groupChatRes.setRecipientName("Jeeva");
          groupChatRes.setGroupId("661f7e401a7cef5423668645");
          groupChatRes.setGroupName("Mockito Testing");
          groupChatRes.setMessage(getMessageRes());
          groupChatRes.setForwardMessage(false);
          groupChatRes.setRepliesCount(10);
          return groupChatRes;
     }

     MessageRes getMessageRes() {
          return MessageRes.builder()
                    .id("1715577263858-904491")
                    .content("Sundar")
                    .caption("caption")
                    .type(Type.IMAGE)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .deliveredAt(new Date())
                    .sendAt(new Date())
                    .build();
     }
}
