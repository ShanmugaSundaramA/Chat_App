package com.ws.chat.service;

import java.util.ArrayList;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.microsoft.azure.storage.StorageException;
import com.ws.chat.azureblob.AzureBlobAdapter;
import com.ws.chat.exception.NotFound;
import com.ws.chat.model.Group;
import com.ws.chat.model.GroupParticipants;
import com.ws.chat.model.InboxParticipants;
import com.ws.chat.model.PinnedGroup;
import com.ws.chat.model.PinnedRecipient;
import com.ws.chat.model.User;
import com.ws.chat.repository.GroupParticipantsRepo;
import com.ws.chat.repository.InboxParticipantsRepository;
import com.ws.chat.repository.UserRepository;
import com.ws.chat.requestbody.UserDTO;
import com.ws.chat.responsebody.GroupList;
import com.ws.chat.responsebody.InboxParticipantRes;
import com.ws.chat.responsebody.LastMessage;
import com.ws.chat.responsebody.ResponseDTO;
import com.ws.chat.responsebody.UserList;

import lombok.RequiredArgsConstructor;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;

import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class UserService {

     private final UserRepository userRepository;
     private final MongoTemplate mongoTemplate;
     private final GroupService groupService;
     private final GroupParticipantsRepo groupParticipantsRepo;
     private final AzureBlobAdapter azureBlobAdapter;
     private final InboxParticipantsRepository inboxParticipantsRepository;

     private static final String SENDERID = "senderId";
     private static final String RECIPIENTID = "recipientId";
     private static final String SUCCESS = "success";
     private static final String LASTMESSAGE = "lastMessage";
     private static final String TOTALCOUNT = "totalCount";
     private static final String TOTALNUMOFPAGES = "totalNumberOfPages";

     @Value("${spring.application.colors}")
     private final String[] colours;

     private final SecureRandom secureRandom = new SecureRandom();
     LocalDate defaultLocalDate = LocalDate.of(1997, Month.JANUARY, 1);
     Date defaultDate = Date.from(defaultLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

     public User saveUser(User user) {
          return userRepository.save(user);
     }

     public User findById(String id) {
          return userRepository.findById(id)
                    .orElseThrow(() -> new NotFound("User not found with id: " + id));
     }

     public User findUserByEmail(
               String userName,
               String email,
               String designation,
               String department,
               String deviceToken,
               String mobileNo) {

          Optional<User> userOpt = userRepository.findByEmail(email);
          if (userOpt.isPresent()) {
               User existingUser = userOpt.get();
               if (deviceToken != null && !deviceToken.isEmpty()) {
                    existingUser.setDeviceToken(deviceToken);
               }
               userRepository.save(existingUser);
               String profilePicture = null;
               if (existingUser.getProfilePicture() != null && !existingUser.getProfilePicture().isEmpty()) {
                    profilePicture = azureBlobAdapter.getBlobUri(existingUser.getProfilePicture()).toString();
               }
               existingUser.setProfilePicture(profilePicture);
               return userOpt.get();
          } else {
               int randomNumber = secureRandom.nextInt(colours.length);
               User user = User.builder()
                         .fullname(userName)
                         .email(email)
                         .isOnline(true)
                         .designation(designation)
                         .department(department)
                         .mobileNo(mobileNo)
                         .colorCode(colours[randomNumber])
                         .deviceToken(deviceToken)
                         .build();
               return userRepository.save(user);
          }
     }

     public Object logout(String userId) {

          User user = this.findById(userId);
          user.setDeviceToken(null);
          userRepository.save(user);
          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    "Device token removed successfully");
     }

     public ResponseDTO getUser(String userId) {
          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    this.findById(userId));
     }

     public List<User> findUsersByEmailOrName(
               String userId,
               String searchValue) {

          Query query = new Query();
          Criteria criteria = Criteria.where("id").ne(userId);
          if (searchValue != null && !searchValue.isEmpty()) {
               criteria.orOperator(Criteria.where("fullname").regex(".*" + searchValue + ".*", "i"),
                         Criteria.where("email").regex(".*" + searchValue + ".*", "i"));
          }
          query.addCriteria(criteria);
          return mongoTemplate.find(query, User.class);
     }

     public List<UserList> findUserList(
               List<User> recipients,
               String id,
               String searchValue) {

          List<ObjectId> recipientIds = recipients.stream()
                    .map(u -> new ObjectId(u.getId()))
                    .collect(Collectors.toList());
          MatchOperation senderRecipientMatch = Aggregation.match(
                    new Criteria().orOperator(
                              new Criteria().andOperator(
                                        Criteria.where(SENDERID).is(new ObjectId(id)),
                                        Criteria.where(RECIPIENTID).in(recipientIds)),
                              new Criteria().andOperator(
                                        Criteria.where(SENDERID).in(recipientIds),
                                        Criteria.where(RECIPIENTID).is(new ObjectId(id))))
                              .and("isDeletedBy").nin(id));
          LookupOperation lookupMessages = LookupOperation.newLookup()
                    .from("messages")
                    .localField("lastMessageId")
                    .foreignField("_id")
                    .as(LASTMESSAGE);
          ProjectionOperation project = Aggregation.project()
                    .and(ArrayOperators.ArrayElemAt.arrayOf(LASTMESSAGE).elementAt(0)).as(LASTMESSAGE)
                    .and("chatId").as("chatId")
                    .and(SENDERID).as(SENDERID)
                    .and(RECIPIENTID).as(RECIPIENTID)
                    .and("isRead").as("isRead");
          Aggregation aggregation = Aggregation.newAggregation(
                    senderRecipientMatch,
                    lookupMessages,
                    project);
          AggregationResults<InboxParticipantRes> aggregationResults = mongoTemplate.aggregate(
                    aggregation,
                    "inboxParticipants",
                    InboxParticipantRes.class);

          List<InboxParticipantRes> mappedResults = aggregationResults.getMappedResults();
          List<UserList> lists = new ArrayList<>();

          for (User recipient : recipients) {
               InboxParticipantRes inboxParticipant = mappedResults.stream()
                         .filter(res -> (res.getSenderId().equals(id) && res.getRecipientId().equals(recipient.getId()))
                                   ||
                                   (res.getSenderId().equals(recipient.getId()) && res.getRecipientId().equals(id)))
                         .findFirst()
                         .orElse(null);

               UserList userList = null;
               if (inboxParticipant != null) {
                    boolean isSendByRecipient = id.equals(inboxParticipant.getRecipientId());
                    userList = this.buildUserList(
                              recipient,
                              inboxParticipant.getChatId(),
                              inboxParticipant.getLastMessage(),
                              isSendByRecipient,
                              recipient.isOnline(),
                              inboxParticipant.isRead());
                    lists.add(userList);
               } else if (searchValue != null && !searchValue.isEmpty()) {
                    userList = this.buildUserList(
                              recipient,
                              null,
                              new LastMessage(),
                              false,
                              recipient.isOnline(),
                              false);
                    lists.add(userList);
               }
          }
          return lists;
     }

     public UserList buildUserList(
               User recipient,
               String chatId,
               LastMessage lastMessage,
               boolean isSendByRecipient,
               Boolean isOnline,
               boolean isRead) {

          lastMessage.setRead(isRead);
          String profilePicture = null;
          if (recipient.getProfilePicture() != null && !recipient.getProfilePicture().isEmpty()) {
               profilePicture = azureBlobAdapter.getBlobUri(recipient.getProfilePicture()).toString();
          }
          return UserList.builder()
                    .userId(recipient.getId())
                    .userName(recipient.getFullname())
                    .userEmailId(recipient.getEmail())
                    .userDesignation(recipient.getDesignation())
                    .userDepartment(recipient.getDepartment())
                    .colorCode(recipient.getColorCode())
                    .deviceToken(recipient.getDeviceToken())
                    .pinnedRecipientsId(recipient.getPinnedRecipientsId())
                    .mutedRecipientIds(recipient.getMutedRecipientIds())
                    .userProfilePicture(profilePicture)
                    .isOnline(isOnline)
                    .isSendByRecipient(isSendByRecipient)
                    .chatId(chatId)
                    .lastMessage(lastMessage)
                    .build();
     }

     public Object updateUserDetails(UserDTO userDTO) {

          String uri = azureBlobAdapter.upload(userDTO.getProfilePicture());
          User user = userRepository.findById(userDTO.getUserId())
                    .orElseThrow(() -> new NotFound("User does not exist."));
          user.setProfilePicture(uri);
          User savedUser = userRepository.save(user);

          String userProfilePictureURI = azureBlobAdapter.getBlobUri(savedUser.getProfilePicture()).toString();
          savedUser.setProfilePicture(userProfilePictureURI);

          return ResponseService.successResponse(200, SUCCESS, savedUser);
     }

     public Object pinRecipientsForUser(
               String userId,
               String recipientId,
               String groupId) {

          String value = "Recipient";
          if (groupId != null && !groupId.isEmpty()) {
               Group group = groupService.findById(groupId)
                         .orElseThrow(() -> new NotFound("Group not found with id: " + groupId));
               List<PinnedGroup> groupPinnedByUserIds = group.getPinnedByUserIds();
               if (groupPinnedByUserIds == null) {
                    groupPinnedByUserIds = new ArrayList<>();
               }
               boolean isGroupAlreadyPinned = groupPinnedByUserIds.stream()
                         .anyMatch(pinnedGroup -> pinnedGroup.getUserId().equals(userId));
               if (isGroupAlreadyPinned) {
                    return ResponseService.successResponse(
                              200,
                              SUCCESS,
                              "Groups already pinned");
               }
               groupPinnedByUserIds.add(PinnedGroup.builder()
                         .userId(userId)
                         .pinnedDate(new Date())
                         .build());
               group.setPinnedByUserIds(groupPinnedByUserIds);
               groupService.save(group);
               GroupParticipants groupParticipant = groupParticipantsRepo.findByGroupIdAndUserId(
                         new ObjectId(groupId),
                         new ObjectId(userId)).orElseThrow(() -> new NotFound("GroupParticipants not exists"));
               groupParticipant.setPinnedAt(new Date());
               groupParticipantsRepo.save(groupParticipant);
               value = "Group";
          } else {
               User user = this.findById(userId);
               List<PinnedRecipient> pinnedRecipients = user.getPinnedRecipientsId();
               if (pinnedRecipients == null) {
                    pinnedRecipients = new ArrayList<>();
               }
               boolean isRecipientAlreadyPinned = pinnedRecipients.stream()
                         .anyMatch(pinnedRecipient -> pinnedRecipient.getRecipientId().equals(recipientId));
               if (isRecipientAlreadyPinned) {
                    return ResponseService.successResponse(
                              200,
                              SUCCESS,
                              "Recipient already pinned");
               }
               Date pinnedAt = new Date();
               pinnedRecipients.add(PinnedRecipient.builder()
                         .pinnedDate(pinnedAt)
                         .recipientId(recipientId)
                         .build());
               user.setPinnedRecipientsId(pinnedRecipients);
               userRepository.save(user);
               InboxParticipants inboxParticipant = inboxParticipantsRepository.findBySenderIdAndRecipientId(
                         new ObjectId(userId),
                         new ObjectId(recipientId)).orElseThrow(() -> new NotFound("inboxParticipant now exist"));
               inboxParticipant.setPinnedAt(pinnedAt);
               inboxParticipantsRepository.save(inboxParticipant);
          }
          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    value + " pinned successfully");
     }

     public Object unpinRecipientsForUser(
               String userId,
               String recipientId,
               String groupId) {

          String groupMessage = "Group list is empty or group not pinned";
          String recipientMessage = "Recipient list is empty or recipient not pinned";

          if (groupId != null && !groupId.isEmpty()) {
               Group group = groupService.findById(groupId)
                         .orElseThrow(() -> new NotFound("User not found with id: " + groupId));
               List<PinnedGroup> groupPinnedByUserIds = group.getPinnedByUserIds();
               if (groupPinnedByUserIds == null) {
                    groupPinnedByUserIds = new ArrayList<>();
               }
               boolean isGroupAlreadyPinned = groupPinnedByUserIds.stream()
                         .anyMatch(pinnedGroup -> pinnedGroup.getUserId().equals(userId));
               if (isGroupAlreadyPinned) {
                    groupPinnedByUserIds = groupPinnedByUserIds.stream()
                              .filter(pinnedGroup -> !pinnedGroup.getUserId().equals(userId))
                              .collect(Collectors.toList());
                    group.setPinnedByUserIds(groupPinnedByUserIds);
                    groupService.save(group);
                    GroupParticipants groupParticipant = groupParticipantsRepo.findByGroupIdAndUserId(
                              new ObjectId(groupId),
                              new ObjectId(userId)).orElseThrow(() -> new NotFound("GroupParticipants not exists"));
                    groupParticipant.setPinnedAt(null);
                    groupParticipantsRepo.save(groupParticipant);
                    groupMessage = "Group unpinned successfully";
               }
               return ResponseService.successResponse(
                         200,
                         SUCCESS,
                         groupMessage);
          } else {
               User user = this.findById(userId);
               List<PinnedRecipient> pinnedRecipients = user.getPinnedRecipientsId();
               if (pinnedRecipients == null) {
                    pinnedRecipients = new ArrayList<>();
               }
               boolean isRecipientAlreadyPinned = pinnedRecipients.stream()
                         .anyMatch(pinnedRecipient -> pinnedRecipient.getRecipientId().equals(recipientId));
               if (!pinnedRecipients.isEmpty() && isRecipientAlreadyPinned) {
                    pinnedRecipients = pinnedRecipients.stream()
                              .filter(pinnedRecipient -> !pinnedRecipient.getRecipientId().equals(recipientId))
                              .collect(Collectors.toList());
                    user.setPinnedRecipientsId(pinnedRecipients);
                    userRepository.save(user);
                    InboxParticipants inboxParticipant = inboxParticipantsRepository.findBySenderIdAndRecipientId(
                              new ObjectId(userId),
                              new ObjectId(recipientId)).orElseThrow(() -> new NotFound("inboxParticipant now exist"));
                    inboxParticipant.setPinnedAt(null);
                    inboxParticipantsRepository.save(inboxParticipant);
                    recipientMessage = "Recipient unpinned successfully";
               }
               return ResponseService.successResponse(
                         200,
                         SUCCESS,
                         recipientMessage);
          }
     }

     public Object muteRecipientsForUser(
               String userId,
               String recipientId,
               String groupId) {

          String value = "Recipient";
          if (groupId != null && !groupId.isEmpty()) {
               Group group = groupService.findById(groupId)
                         .orElseThrow(() -> new NotFound("User not found with id: " + groupId));
               List<String> groupMutedByUserIds = group.getMutedByUserIds();
               if (groupMutedByUserIds == null) {
                    groupMutedByUserIds = new ArrayList<>();
               }
               if (groupMutedByUserIds.contains(userId)) {
                    return ResponseService.successResponse(
                              200,
                              SUCCESS,
                              "Groups already muted");
               }
               groupMutedByUserIds.add(userId);
               group.setMutedByUserIds(groupMutedByUserIds);
               groupService.save(group);
               value = "Group";
          } else {
               User user = this.findById(userId);
               List<String> mutedRecipients = user.getMutedRecipientIds();
               if (mutedRecipients == null) {
                    mutedRecipients = new ArrayList<>();
               }
               if (mutedRecipients.contains(recipientId)) {
                    return ResponseService.successResponse(
                              200,
                              SUCCESS,
                              "Recipient already pinned");
               }
               mutedRecipients.add(recipientId);
               user.setMutedRecipientIds(mutedRecipients);
               userRepository.save(user);
          }
          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    value + " muted successfully");
     }

     public Object unmuteRecipientsForUser(
               String userId,
               String recipientId,
               String groupId) {

          String groupMessage = "Group list is empty or group not muted";
          String recipientMessage = "Recipient list is empty or recipient not muted";

          if (groupId != null && !groupId.isEmpty()) {
               Group group = groupService.findById(groupId)
                         .orElseThrow(() -> new NotFound("User not found with id: " + groupId));
               List<String> groupMutedByUserIds = group.getMutedByUserIds();
               if (groupMutedByUserIds == null) {
                    groupMutedByUserIds = new ArrayList<>();
               }
               if (groupMutedByUserIds.contains(userId)) {
                    groupMutedByUserIds.remove(userId);
                    group.setMutedByUserIds(groupMutedByUserIds);
                    groupService.save(group);
                    groupMessage = "Group unmuted successfully";
               }
               return ResponseService.successResponse(
                         200,
                         SUCCESS,
                         groupMessage);
          } else {
               User user = this.findById(userId);
               List<String> mutedRecipients = user.getMutedRecipientIds();
               if (mutedRecipients == null) {
                    mutedRecipients = new ArrayList<>();
               }
               if (!mutedRecipients.isEmpty() && mutedRecipients.contains(recipientId)) {
                    mutedRecipients.remove(recipientId);
                    user.setMutedRecipientIds(mutedRecipients);
                    userRepository.save(user);
                    recipientMessage = "Recipient unmuted successfully";
               }
               return ResponseService.successResponse(
                         200,
                         SUCCESS,
                         recipientMessage);
          }
     }

     /*
      * new code
      */
     public Object findRecipientsForUser(
               String senderId,
               String searchValue,
               boolean unread,
               int pageNo,
               int rowPerPage,
               boolean fetchAll) throws StorageException, URISyntaxException {

          if (searchValue == null || searchValue.isEmpty()) {
               return findRecipientForUserIfSearchValueIsEmpty(
                         senderId,
                         unread,
                         pageNo,
                         rowPerPage,
                         fetchAll);
          }

          User sender = this.findById(senderId);
          List<AggregationOperation> aggregationOperations = new ArrayList<>();
          aggregationOperations.add(Aggregation.match(
                    Criteria.where("_id").ne(new ObjectId(senderId))));
          aggregationOperations.add(Aggregation.match(
                    Criteria.where("fullname").regex(".*" + searchValue + ".*", "i")));
          aggregationOperations.add(LookupOperation.newLookup()
                    .from("inboxParticipants")
                    .localField("_id")
                    .foreignField("recipientId")
                    .as("recipients"));
          aggregationOperations.add(
                    Aggregation.match(Criteria.where("recipients.isDeletedBy").nin(senderId)));
          aggregationOperations.add(Aggregation.project()
                    .and("_id").as("userId")
                    .and("fullname").as("userName")
                    .and("email").as("userEmailId")
                    .and("profilePicture").as("userProfilePicture")
                    .and("designation").as("userDesignation")
                    .and("department").as("userDepartment")
                    .and("pinnedRecipientsId").as("pinnedRecipientsId")
                    .and("mutedRecipientIds").as("mutedRecipientIds")
                    .and("colorCode").as("colorCode")
                    .and("deviceToken").as("deviceToken")
                    .and("isOnline").as("isOnline")
                    .and(ArrayOperators.Filter.filter("recipients")
                              .as("rec")
                              .by(ComparisonOperators.Eq.valueOf("$$rec.senderId")
                                        .equalToValue(new ObjectId(senderId))))
                    .as("filteredRecipients"));
          aggregationOperations.add(Aggregation.lookup(
                    "messages",
                    "filteredRecipients.lastMessageId",
                    "_id",
                    "lastMessages"));
          aggregationOperations.add(Aggregation.project(
                    "userId",
                    "userName",
                    "userEmailId",
                    "userProfilePicture",
                    "userDesignation",
                    "userDepartment",
                    "mutedRecipientIds",
                    "pinnedRecipientsId",
                    "colorCode",
                    "deviceToken",
                    "isOnline")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("filteredRecipients.chatId").elementAt(0))
                    .as("chatId")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("filteredRecipients.isSendByRecipient").elementAt(0))
                    .as("isSendByRecipient")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("filteredRecipients.pinnedAt").elementAt(0))
                    .as("pinnedAt")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.id").elementAt(0))
                    .as("lastMessage.id")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.type").elementAt(0))
                    .as("lastMessage.type")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.content").elementAt(0))
                    .as("lastMessage.content")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.createdAt").elementAt(0))
                    .as("lastMessage.createdAt")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.updatedAt").elementAt(0))
                    .as("lastMessage.updatedAt")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("filteredRecipients.isRead").elementAt(0))
                    .as("lastMessage.isRead")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("filteredRecipients.unreadMessageCount").elementAt(0))
                    .as("lastMessage.unreadMessageCount"));
          if (fetchAll) {
               Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
               AggregationResults<UserList> results = mongoTemplate.aggregate(
                         aggregation,
                         "users",
                         UserList.class);
               return results.getMappedResults();
          }
          List<AggregationOperation> countOperations = new ArrayList<>(aggregationOperations);
          countOperations.add(Aggregation.count().as("count"));
          Aggregation countAggregation = Aggregation.newAggregation(countOperations);
          AggregationResults<Document> countResults = mongoTemplate.aggregate(
                    countAggregation,
                    "users",
                    Document.class);
          Integer totalCount = 0;
          if (!countResults.getMappedResults().isEmpty()) {
               totalCount = (Integer) countResults.getMappedResults().get(0).get("count");
          }
          aggregationOperations.add(Aggregation.sort(Sort.by(
                    Sort.Order.desc("pinnedAt"),
                    Sort.Order.desc("lastMessage.createdAt"),
                    Sort.Order.asc("userName"))));
          aggregationOperations.add(Aggregation.skip((long) (pageNo - 1) * rowPerPage));
          aggregationOperations.add(Aggregation.limit(rowPerPage));

          Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
          AggregationResults<UserList> results = mongoTemplate.aggregate(
                    aggregation,
                    "users",
                    UserList.class);

          String sasToken = azureBlobAdapter.generateContainerSasToken();
          String uri = "https://packagingapp.blob.core.windows.net/trove/";

          List<UserList> userLists = results.getMappedResults();
          userLists.stream().forEach(u -> {
               if (u.getUserProfilePicture() != null && !u.getUserProfilePicture().isEmpty()) {
                    u.setUserProfilePicture(uri + u.getUserProfilePicture() + "?" + sasToken);
               }
               if (u.getPinnedAt() != null) {
                    u.setPinnedChat(true);
               }
               if (sender.getMutedRecipientIds() != null && !sender.getMutedRecipientIds().isEmpty()) {
                    u.setMutedChat(sender.getMutedRecipientIds().contains(u.getUserId()));
               }
               if (u.getLastMessage() == null) {
                    u.setLastMessage(new LastMessage());
               }
          });
          int totalNumberOfPages = (int) Math.ceil((double) totalCount / rowPerPage);
          Map<String, Object> response = new HashMap<>();
          response.put("totalCount", totalCount);
          response.put("chatMembersList", userLists);
          response.put("totalNumberOfPages", totalNumberOfPages);

          return ResponseService.successResponse(200, "success", response);
     }

     public Object findRecipientForUserIfSearchValueIsEmpty(
               String senderId,
               boolean unread,
               int pageNo,
               int rowPerPage,
               boolean fetchAll) throws StorageException, URISyntaxException {

          User sender = this.findById(senderId);
          List<AggregationOperation> aggregationOperations = new ArrayList<>();
          aggregationOperations.add(Aggregation.match(
                    Criteria.where("senderId").is(new ObjectId(senderId))));
          aggregationOperations.add(Aggregation.match(
                    Criteria.where("isDeletedBy").nin(senderId)));
          if (unread) {
               aggregationOperations.add(Aggregation.match(
                         Criteria.where("isRead").is(false)));
          }
          LookupOperation lookupMessages = LookupOperation.newLookup()
                    .from("messages")
                    .localField("lastMessageId")
                    .foreignField("_id")
                    .as("lastMessages");
          aggregationOperations.add(lookupMessages);
          LookupOperation lookupRecipient = LookupOperation.newLookup()
                    .from("users")
                    .localField("recipientId")
                    .foreignField("_id")
                    .as("recipients");
          aggregationOperations.add(lookupRecipient);
          aggregationOperations.add(Aggregation.project()
                    .and(ArrayOperators.ArrayElemAt.arrayOf("recipients._id").elementAt(0))
                    .as("userId")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("recipients.fullname").elementAt(0))
                    .as("userName")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("recipients.profilePicture").elementAt(0))
                    .as("userProfilePicture")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("recipients.email").elementAt(0))
                    .as("userEmailId")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("recipients.designation").elementAt(0))
                    .as("userDesignation")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("recipients.department").elementAt(0))
                    .as("userDepartment")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("recipients.pinnedRecipientsId").elementAt(0))
                    .as("pinnedRecipientsId")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("recipients.mutedRecipientIds").elementAt(0))
                    .as("mutedRecipientIds")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("recipients.colorCode").elementAt(0))
                    .as("colorCode")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("recipients.deviceToken").elementAt(0))
                    .as("deviceToken")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("recipients.isOnline").elementAt(0))
                    .as("isOnline")
                    .and("chatId").as("chatId")
                    .and("pinnedAt").as("pinnedAt")
                    .and("isSendByRecipient").as("isSendByRecipient")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.id").elementAt(0))
                    .as("lastMessage.id")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.type").elementAt(0))
                    .as("lastMessage.type")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.content").elementAt(0))
                    .as("lastMessage.content")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.createdAt").elementAt(0))
                    .as("lastMessage.createdAt")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.updatedAt").elementAt(0))
                    .as("lastMessage.updatedAt")
                    .and("unreadMessageCount").as("lastMessage.unreadMessageCount")
                    .and("isRead").as("lastMessage.isRead"));
          if (fetchAll) {
               Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
               AggregationResults<UserList> results = mongoTemplate.aggregate(
                         aggregation,
                         "inboxParticipants",
                         UserList.class);
               return results.getMappedResults();
          }
          List<AggregationOperation> countOperations = new ArrayList<>(aggregationOperations);
          countOperations.add(Aggregation.count().as("count"));
          Aggregation countAggregation = Aggregation.newAggregation(countOperations);
          AggregationResults<Document> countResults = mongoTemplate.aggregate(
                    countAggregation,
                    "inboxParticipants",
                    Document.class);
          Integer totalCount = 0;
          if (!countResults.getMappedResults().isEmpty()) {
               totalCount = (Integer) countResults.getMappedResults().get(0).get("count");
          }

          aggregationOperations.add(Aggregation.sort(Sort.by(
                    Sort.Order.desc("pinnedAt"),
                    Sort.Order.desc("lastMessage.createdAt"))));
          aggregationOperations.add(Aggregation.skip((long) (pageNo - 1) * rowPerPage));
          aggregationOperations.add(Aggregation.limit(rowPerPage));

          Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
          AggregationResults<UserList> results = mongoTemplate.aggregate(
                    aggregation,
                    "inboxParticipants",
                    UserList.class);

          String sasToken = azureBlobAdapter.generateContainerSasToken();
          String uri = "https://packagingapp.blob.core.windows.net/trove/";

          List<UserList> userLists = results.getMappedResults();
          userLists.stream().forEach(u -> {
               if (u.getUserProfilePicture() != null && !u.getUserProfilePicture().isEmpty()) {
                    u.setUserProfilePicture(uri + u.getUserProfilePicture() + "?" + sasToken);
               }
               if (u.getPinnedAt() != null) {
                    u.setPinnedChat(true);
               }
               if (sender.getMutedRecipientIds() != null && !sender.getMutedRecipientIds().isEmpty()) {
                    u.setMutedChat(sender.getMutedRecipientIds().contains(u.getUserId()));
               }
               if (u.getLastMessage() == null) {
                    u.setLastMessage(new LastMessage());
               }
          });

          int totalNumberOfPages = (int) Math.ceil((double) totalCount / rowPerPage);
          Map<String, Object> response = new HashMap<>();
          response.put("totalCount", totalCount);
          response.put("chatMembersList", userLists);
          response.put("totalNumberOfPages", totalNumberOfPages);

          return ResponseService.successResponse(200, "success", response);
     }

     public Object findAllRecipientsAndGroups(
               String senderId,
               String searchValue,
               int pageNo,
               int rowPerPage) throws StorageException, URISyntaxException {

          String sasToken = azureBlobAdapter.generateContainerSasToken();
          String uri = "https://packagingapp.blob.core.windows.net/trove/";

          @SuppressWarnings("unchecked")
          List<UserList> userLists = (List<UserList>) this.findRecipientsForUser(
                    senderId,
                    searchValue,
                    false,
                    pageNo,
                    rowPerPage,
                    true);
          @SuppressWarnings("unchecked")
          List<GroupList> groupLists = (List<GroupList>) groupService.getGroups(
                    senderId,
                    searchValue,
                    pageNo,
                    rowPerPage,
                    true);

          List<Object> objects = new ArrayList<>();
          objects.addAll(groupLists);
          objects.addAll(userLists);

          objects.sort(Comparator.<Object, Date>comparing(obj -> {
               if (obj instanceof GroupList groupList) {
                    LastMessage lastMessage = groupList.getLastMessage();
                    return lastMessage != null && lastMessage.getCreatedAt() != null ? lastMessage.getCreatedAt()
                              : defaultDate;
               } else if (obj instanceof UserList userList) {
                    LastMessage lastMessage = userList.getLastMessage();
                    return lastMessage != null && lastMessage.getCreatedAt() != null ? lastMessage.getCreatedAt()
                              : defaultDate;
               }
               return defaultDate;
          }, Comparator.nullsLast(Comparator.reverseOrder())));

          int fromIndex = Math.min((pageNo - 1) * rowPerPage, objects.size());
          int toIndex = Math.min(((pageNo - 1) * rowPerPage) + rowPerPage, objects.size());
          int totalNumberOfPages = (int) Math.ceil((double) objects.size() / rowPerPage);

          List<Object> subList = objects.subList(fromIndex, toIndex);
          subList.forEach(obj -> {
               if (obj instanceof GroupList g
                         && g.getGroupProfilePicture() != null
                         && !g.getGroupProfilePicture().isEmpty()) {
                    g.setGroupProfilePicture(uri + g.getGroupProfilePicture() + "?" + sasToken);
               } else if (obj instanceof UserList u
                         && u.getUserProfilePicture() != null
                         && !u.getUserProfilePicture().isEmpty()) {
                    u.setUserProfilePicture(uri + u.getUserProfilePicture() + "?" + sasToken);
               }
          });

          Map<String, Object> response = new HashMap<>();
          response.put(TOTALCOUNT, objects.size());
          response.put("chatMembers", subList);
          response.put(TOTALNUMOFPAGES, totalNumberOfPages);

          return ResponseService.successResponse(200, SUCCESS, response);
     }

     public Object findAll(
               String id,
               String groupId,
               String searchValue,
               int pageNo,
               int rowPerPage) throws StorageException, URISyntaxException {

          Set<String> usersId = new HashSet<>();
          usersId.add(id);
          if (groupId != null && !groupId.isBlank()) {
               Optional<Group> groupOpt = groupService.findById(groupId);
               if (groupOpt.isPresent()) {
                    Group group = groupOpt.get();
                    usersId.addAll(group.getMembers()
                              .stream()
                              .map(m -> m.getUserId().toHexString())
                              .collect(Collectors.toSet()));
               }
          }
          List<AggregationOperation> aggregationOperations = new ArrayList<>();
          aggregationOperations.add(Aggregation.match(
                    Criteria.where("_id").nin(usersId)));
          aggregationOperations.add(Aggregation.match(
                    Criteria.where("fullname").regex(".*" + searchValue + ".*", "i")));
          aggregationOperations.add(LookupOperation.newLookup()
                    .from("inboxParticipants")
                    .localField("_id")
                    .foreignField("recipientId")
                    .as("recipients"));
          aggregationOperations.add(Aggregation.project(
                    "_id",
                    "fullname",
                    "email",
                    "profilePicture",
                    "designation",
                    "department",
                    "role",
                    "pinnedRecipientsId",
                    "mutedRecipientIds",
                    "colorCode",
                    "deviceToken",
                    "mobileNo",
                    "isOnline",
                    "createdAt",
                    "updatedAt")
                    .and(ArrayOperators.Filter.filter("recipients")
                              .as("rec")
                              .by(ComparisonOperators.Eq.valueOf("$$rec.senderId")
                                        .equalToValue(new ObjectId(id))))
                    .as("filteredRecipients"));
          aggregationOperations.add(Aggregation.lookup(
                    "messages",
                    "filteredRecipients.lastMessageId",
                    "_id",
                    "lastMessages"));
          aggregationOperations.add(Aggregation.project(
                    "_id",
                    "fullname",
                    "email",
                    "profilePicture",
                    "designation",
                    "department",
                    "role",
                    "pinnedRecipientsId",
                    "mutedRecipientIds",
                    "colorCode",
                    "deviceToken",
                    "mobileNo",
                    "isOnline",
                    "createdAt",
                    "updatedAt")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.createdAt").elementAt(0))
                    .as("lastMessage.createdAt"));
          List<AggregationOperation> countOperations = new ArrayList<>(aggregationOperations);
          countOperations.add(Aggregation.count().as("count"));
          Aggregation countAggregation = Aggregation.newAggregation(countOperations);
          AggregationResults<Document> countResults = mongoTemplate.aggregate(
                    countAggregation,
                    "users",
                    Document.class);
          Integer totalCount = 0;
          if (!countResults.getMappedResults().isEmpty()) {
               totalCount = (Integer) countResults.getMappedResults().get(0).get("count");
          }
          aggregationOperations.add(Aggregation.sort(Sort.by(
                    Sort.Order.desc("lastMessage.createdAt"),
                    Sort.Order.asc("fullname"))));
          aggregationOperations.add(Aggregation.skip((long) (pageNo - 1) * rowPerPage));
          aggregationOperations.add(Aggregation.limit(rowPerPage));
          Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
          AggregationResults<User> results = mongoTemplate.aggregate(
                    aggregation,
                    "users",
                    User.class);
          String sasToken = azureBlobAdapter.generateContainerSasToken();
          String uri = "https://packagingapp.blob.core.windows.net/trove/";
          List<User> users = results.getMappedResults();
          users.forEach(u -> {
               if (u.getProfilePicture() != null && !u.getProfilePicture().isEmpty()) {
                    u.setProfilePicture(uri + u.getProfilePicture() + "?" + sasToken);
               }
          });
          int totalNumberOfPages = (int) Math.ceil((double) totalCount / rowPerPage);
          Map<String, Object> response = new HashMap<>();
          response.put(TOTALCOUNT, totalCount);
          response.put("membersList", users);
          response.put(TOTALNUMOFPAGES, totalNumberOfPages);

          return ResponseService.successResponse(200, SUCCESS, response);
     }

     public Object getTotalUnreadMessageCountForDMs(
               String userId) {

          List<AggregationOperation> aggregationOperations = new ArrayList<>();
          aggregationOperations.add(Aggregation.match(
                    new Criteria().andOperator(
                              Criteria.where("senderId").is(new ObjectId(userId)),
                              Criteria.where("isRead").is(false))));
          // aggregationOperations.add(Aggregation.group().sum("unreadMessageCount").as("count"));
          aggregationOperations.add(Aggregation.count().as("count"));
          Aggregation countAggregation = Aggregation.newAggregation(aggregationOperations);
          AggregationResults<Document> countResults = mongoTemplate.aggregate(
                    countAggregation,
                    "inboxParticipants",
                    Document.class);
          Integer totalCount = 0;
          if (!countResults.getMappedResults().isEmpty()) {
               totalCount = (Integer) countResults.getMappedResults().get(0).get("count");
          }
          Map<String, Object> response = new HashMap<>();
          response.put(TOTALCOUNT, totalCount);
          return ResponseService.successResponse(200, "success", response);

     }

}
