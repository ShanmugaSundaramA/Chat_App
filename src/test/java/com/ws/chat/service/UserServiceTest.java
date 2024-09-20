package com.ws.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.microsoft.azure.storage.StorageException;
import com.ws.chat.azureblob.AzureBlobAdapter;
import com.ws.chat.exception.NotFound;
import com.ws.chat.model.Group;
import com.ws.chat.model.InboxParticipants;
import com.ws.chat.model.Member;
import com.ws.chat.model.Type;
import com.ws.chat.model.User;
import com.ws.chat.repository.UserRepository;
import com.ws.chat.responsebody.InboxParticipantRes;
import com.ws.chat.responsebody.LastMessage;
import com.ws.chat.responsebody.ResponseDTO;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

     @InjectMocks
     UserService userService;
     @Mock
     UserRepository userRepository;
     @Mock
     MongoTemplate mongoTemplate;
     @Mock
     GroupService groupService;
     @Mock
     AzureBlobAdapter azureBlobAdapter;

     @Value("${spring.application.colors}")
     private String[] colours;

     String[] mockColours = {
               "#AB47BC", "#42A5F5", "#26A69A", "#7E57C2", "#EC407A", "#EC407A", "#EC407A",
               "#FFC069", "#5CDBD3", "#69B1FF", "#B37FEB", "#FF85C0" };

     @Test
     void findByEmail() {

          User user = getUser();
          when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

          User userOutput = userService.findUserByEmail(
                    user.getUsername(),
                    user.getEmail(),
                    user.getDesignation(),
                    user.getDepartment(),
                    user.getMobileNo(),
                    user.getDeviceToken());
          assertEquals(user.getEmail(), userOutput.getEmail());
     }

     @Test
     void findByEmailForUserNotExist() {

          User user = getUser();
          when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
          ReflectionTestUtils.setField(userService, "colours", mockColours);
          when(userRepository.save(any(User.class))).thenReturn(user);

          User userOutput = userService.findUserByEmail(
                    user.getUsername(),
                    user.getEmail(),
                    user.getDesignation(),
                    user.getDepartment(),
                    user.getMobileNo(),
                    user.getDeviceToken());
          assertEquals(user.getEmail(), userOutput.getEmail());
     }

     @Test
     void findById() {

          User user = getUser();
          when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

          ResponseDTO responseDTO = userService.getUser(user.getId());
          User userOutput = (User) responseDTO.getData();
          assertEquals(user.getId(), userOutput.getId());
     }

     @Test
     void findByIdUserNotFoundTest() {

          User user = getUser();
          when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

          NotFound notFound = assertThrows(NotFound.class, () -> userService.findById(user.getId()));
          assertEquals("User not found with id: " + user.getId(), notFound.getMessage());
     }

     @Test
     void saveUserTest() {

          User user = getUser();
          when(userRepository.save(user)).thenReturn(user);
          User userOutput = userService.saveUser(user);
          assertNotNull(userOutput.getId());
     }

     // @Test
     void findAllTest() throws StorageException, URISyntaxException {

          List<User> users = new ArrayList<>();
          users.add(getUser());
          when(mongoTemplate.find(
                    any(Query.class),
                    eq(User.class))).thenReturn(users);
          when(groupService.findById(
                    "groupId123")).thenReturn(Optional.of(getGroupObject()));
          when(azureBlobAdapter.generateContainerSasToken()).thenReturn(anyString());
          Object result = userService.findAll("userId12", "groupId123", "sundar", 1, 10);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
     }

     // @Test
     void findAllIfGroupNotExistTest() throws StorageException, URISyntaxException {

          List<User> users = new ArrayList<>();
          users.add(getUser());
          when(mongoTemplate.find(
                    any(Query.class),
                    eq(User.class))).thenReturn(users);
          when(groupService.findById(
                    "groupId123")).thenReturn(Optional.empty());
          Object result = userService.findAll("userId12", "groupId123", null, 1, 10);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
     }

     // @Test
     void findAllWithoutGroupIdTest() throws StorageException, URISyntaxException {

          List<User> users = new ArrayList<>();
          users.add(getUser());
          when(mongoTemplate.find(
                    any(Query.class),
                    eq(User.class))).thenReturn(users);
          Object result = userService.findAll("669115f9c5176057331739a7", null, null, 1, 10);

          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
     }

     @Test
     void findUsersByEmailOrNameTest() {

          List<User> users = new ArrayList<>();
          users.add(getUser());
          when(mongoTemplate.find(any(Query.class), eq(User.class))).thenReturn(users);

          List<User> userOutput = userService.findUsersByEmailOrName("user123", "sundar");
          assertEquals(users, userOutput);
     }

     @SuppressWarnings("unchecked")
     // @Test
     void findRecipientsForUserTest() throws StorageException, URISyntaxException {

          List<User> users = new ArrayList<>();
          users.add(getUser());

          List<InboxParticipantRes> inboxParticipantRes = new ArrayList<>();
          inboxParticipantRes.add(getInboxParticipantsRes());
          User user = getUser();
          when(userRepository.findById("669115f9c5176057331739a7")).thenReturn(Optional.of(user));
          when(mongoTemplate.find(any(Query.class), eq(User.class))).thenReturn(users);
          AggregationResults<InboxParticipantRes> mockAggregationResults = mock(AggregationResults.class);
          when(mockAggregationResults.getMappedResults()).thenReturn(inboxParticipantRes);
          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    eq("inboxParticipants"),
                    eq(InboxParticipantRes.class))).thenReturn(mockAggregationResults);

          Object result = userService.findRecipientsForUser("669115f9c5176057331739a7", "sundar", 1, 10, false);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
     }

     @SuppressWarnings("unchecked")
     // @Test
     void findRecipientsForUserIfInboxParticipantsNullTest() throws StorageException, URISyntaxException {

          List<User> users = new ArrayList<>();
          users.add(getUser());

          List<InboxParticipantRes> inboxParticipantRes = new ArrayList<>();
          User user = getUser();
          when(userRepository.findById("669115f9c5176057331739a7")).thenReturn(Optional.of(user));
          when(mongoTemplate.find(any(Query.class), eq(User.class))).thenReturn(users);
          AggregationResults<InboxParticipantRes> mockAggregationResults = mock(AggregationResults.class);
          when(mockAggregationResults.getMappedResults()).thenReturn(inboxParticipantRes);
          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    eq("inboxParticipants"),
                    eq(InboxParticipantRes.class))).thenReturn(mockAggregationResults);

          Object result = userService.findRecipientsForUser("669115f9c5176057331739a7", "sundar", 1, 10, false);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
     }

     @SuppressWarnings("unchecked")
     // @Test
     void findAllRecipientAndGroupsTest() throws StorageException, URISyntaxException {

          List<User> users = new ArrayList<>();
          users.add(getUser());

          List<InboxParticipantRes> inboxParticipantRes = new ArrayList<>();
          inboxParticipantRes.add(getInboxParticipantsRes());

          when(mongoTemplate.find(
                    any(Query.class),
                    eq(User.class))).thenReturn(users);
          AggregationResults<InboxParticipantRes> mockAggregationResults = mock(AggregationResults.class);
          when(mockAggregationResults.getMappedResults()).thenReturn(inboxParticipantRes);
          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    eq("inboxParticipants"),
                    eq(InboxParticipantRes.class))).thenReturn(mockAggregationResults);
          Object result = userService.findAllRecipientsAndGroups("669115f9c5176057331739a7", "sundar", 1, 10);
          assertNotNull(result);
          assertTrue(result instanceof ResponseDTO);
          ResponseDTO response = (ResponseDTO) result;
          assertEquals(200, response.getStatusCode());
          assertEquals("success", response.getStatus());
     }

     User getUser() {

          return User.builder()
                    .id("669115f9c5176057331739a7")
                    .fullname("Sundar Vikram")
                    .email("sundarvikram.a@cavininfotech.com")
                    .designation("Employee")
                    .department("IT")
                    .role("USER")
                    .build();
     }

     List<Member> getMembersObject() {

          List<Member> members = new ArrayList<>();
          members.add(new Member(new ObjectId("669115f9c5176057331739a9"), "USER", new Date()));
          members.add(new Member(new ObjectId("669115f9c5176057331739a9"), "USER", new Date()));
          return members;
     }

     Group getGroupObject() {

          Group group = new Group();
          List<Member> members = getMembersObject();
          group.setId("group123");
          group.setGroupName("Mockito Test Group");
          group.setDescription("Test Description");
          group.setMembers(members);
          group.setColorCode("#ffffff");
          group.setProfilePicture("profile.jpg");
          return group;
     }

     InboxParticipants getInboxParticipants() {
          return InboxParticipants.builder()
                    .id("6626360f51d3962995d4f3b1")
                    .chatId("661f674e4757cf5c0a0dea0c_661f674e4757cf5c0a0dea0b")
                    .senderId(new ObjectId("661f674e4757cf5c0a0dea0c"))
                    .recipientId(new ObjectId("661f674e4757cf5c0a0dea0b"))
                    .lastMessageId("1715249320016-919855")
                    .isRead(false)
                    .build();
     }

     InboxParticipantRes getInboxParticipantsRes() {
          return InboxParticipantRes.builder()
                    .id("6626360f51d3962995d4f3b1")
                    .chatId("661f674e4757cf5c0a0dea0c_661f674e4757cf5c0a0dea0b")
                    .senderId("661f674e4757cf5c0a0dea0c")
                    .recipientId("661f674e4757cf5c0a0dea0b")
                    .lastMessage(getLastMessage())
                    .isRead(false)
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
}