package com.ws.chat.service;

import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.storage.StorageException;
import com.mongodb.client.result.UpdateResult;
import com.ws.chat.azureblob.AzureBlobAdapter;
import com.ws.chat.exception.NotFound;
import com.ws.chat.model.Group;
import com.ws.chat.model.Member;
import com.ws.chat.model.Message;
import com.ws.chat.model.Type;
import com.ws.chat.model.User;
import com.ws.chat.repository.GroupRepository;
import com.ws.chat.repository.MessageRepository;
import com.ws.chat.requestbody.GroupDTO;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.responsebody.GroupList;
import com.ws.chat.responsebody.LastMessage;
import com.ws.chat.utils.Validations;
import com.ws.chat.websocket.WebSocketService;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupService {

     private final GroupRepository groupRepository;
     private final MongoTemplate mongoTemplate;
     private final GroupChatService groupChatService;
     private final AzureBlobAdapter azureBlobAdapter;
     private final WebSocketService webSocketService;
     private final MessageRepository messageRepository;
     private final Validations validations;

     @Value("${spring.application.colors}")
     private final String[] colours;

     LocalDate defaultLocalDate = LocalDate.of(1997, Month.JANUARY, 1);
     Date defaultDate = Date.from(defaultLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
     private final SecureRandom secureRandom = new SecureRandom();

     private static final String USERS = "users";
     private static final String MESSAGES = "messages";

     private static final String SENDER_ID = "senderId";
     private static final String LAST_MESSAGE_ID = "lastMessageId";

     private static final String SENDER = "sender";
     private static final String LASTMESSAGE = "lastMessage";
     private static final String IS_READ = "isRead";

     private static final String CREATED_AT = "createdAt";
     private static final String UPDATED_AT = "updatedAt";

     private static final String MEMBERS = "members";
     private static final String SUCCESS = "success";

     public Group save(Group group) {

          return groupRepository.save(group);
     }

     public Optional<Group> findById(String groupId) {

          return groupRepository.findById(groupId);
     }

     public List<Member> getUserIdsByGroupId(String groupId) {

          Optional<Group> groupOptional = this.findById(groupId);
          if (groupOptional.isPresent()) {
               return groupOptional.get().getMembers();
          } else {
               return new ArrayList<>();
          }
     }

     public Object createGroup(
               MultipartFile profilePicture,
               String groupName,
               String description,
               List<Member> members,
               String createdBy) throws IllegalArgumentException {

          String groupProfileName = null;
          String groupProfilePictureURI = null;
          if (profilePicture != null) {
               if (validations.isValidImage(profilePicture)) {
                    groupProfileName = azureBlobAdapter.upload(profilePicture);
               } else {
                    throw new IllegalArgumentException("Content is not an image");
               }
          }
          Member member = new Member(new ObjectId(createdBy), "ADMIN", new Date());
          members.add(member);
          Group group = Group.builder()
                    .groupName(groupName)
                    .profilePicture(groupProfileName)
                    .colorCode(colours[secureRandom.nextInt(colours.length)])
                    .description(description)
                    .members(members)
                    .createdBy(createdBy)
                    .build();
          Group savedGroup = groupRepository.save(group);
          if (groupProfileName != null) {
               groupProfilePictureURI = azureBlobAdapter.getBlobUri(groupProfileName).toString();
          }
          GroupList groupList = GroupList.builder()
                    .groupId(savedGroup.getId())
                    .groupName(savedGroup.getGroupName())
                    .groupProfilePicture(groupProfilePictureURI)
                    .colorCode(savedGroup.getColorCode())
                    .lastMessage(new LastMessage())
                    .build();
          UUID uniqueId = UUID.randomUUID();
          Message msg = new Message();
          msg.setId(uniqueId.toString());
          msg.setType(Type.NEWGROUP);
          msg.setCreatedAt(new Date());
          msg.setUpdatedAt(new Date());
          msg.setSendAt(new Date());
          msg.setDeliveredAt(new Date());
          Message savedMessage = messageRepository.save(msg);
          MessageRequestBody messageRequestBody = MessageRequestBody.builder()
                    .action("newGroup")
                    .senderId(createdBy)
                    .senderName("")
                    .senderProfilePicture("")
                    .department("")
                    .designation("")
                    .colorCode("")
                    .groupId(savedGroup.getId())
                    .groupName(savedGroup.getGroupName())
                    .groupProfilePicture(groupProfilePictureURI)
                    .messageId(savedMessage.getId())
                    .type(Type.NEWGROUP)
                    .message(savedMessage)
                    .build();
          ObjectNode messageText = webSocketService.convertObjectToStringify(messageRequestBody);
          groupChatService.saveGroupChatWhenGroupCreated(
                    members,
                    messageRequestBody);
          webSocketService.sendMessageToGroup(
                    savedGroup.getId(),
                    createdBy,
                    messageText,
                    false,
                    messageRequestBody);
          return ResponseService.successResponse(200, SUCCESS, groupList);
     }

     public Object addGroupMember(
               String groupId,
               List<Member> newMembers) {

          Query query = new Query(Criteria.where("_id").is(groupId));
          Update update = new Update();
          update.push(MEMBERS).each(newMembers);
          mongoTemplate.updateFirst(
                    query,
                    update,
                    Group.class);
          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    newMembers.size() + " members added successfully");
     }

     public Object removeUserFromGroup(
               String groupId,
               String userId) {

          Query query = new Query(Criteria.where("_id").is(groupId));
          Update update = new Update().pull(MEMBERS, Query.query(Criteria.where("userId").is(new ObjectId(userId))));
          mongoTemplate.updateFirst(query, update, Group.class);
          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    "Removed successfully");
     }

     public Object leaveGroup(
               String groupId,
               String userId) {

          Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new NotFound("Group does not exist"));
          List<Member> remainingMembers = group.getMembers()
                    .stream()
                    .filter(member -> !member.getUserId().toHexString().equals(userId))
                    .collect(Collectors.toList());
          boolean adminExists = remainingMembers
                    .stream()
                    .anyMatch(member -> "ADMIN".equals(member.getRole()));
          if (!adminExists && !remainingMembers.isEmpty()) {
               remainingMembers.sort(
                         Comparator.comparing(Member::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
               Member newAdmin = remainingMembers.get(0);
               newAdmin.setRole("ADMIN");
          }
          group.setMembers(remainingMembers);
          groupRepository.save(group);

          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    "Removed successfully");
     }

     public Object deleteGroup(String groupId) {

          groupRepository.deleteById(groupId);
          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    "Group deleted successfully");
     }

     public List<Group> getGroupsByUserId(String userId, String groupName) {

          Query query = new Query();
          if (groupName != null && !groupName.isEmpty()) {
               query.addCriteria(Criteria.where("groupName").regex(".*" + groupName + ".*", "i"));
          }
          query.addCriteria(Criteria.where("members.userId").in(userId));
          return mongoTemplate.find(query, Group.class);
     }

     public List<GroupList> getGroupList(
               String userId,
               List<Group> groups) {

          List<String> groupIds = groups.stream()
                    .map(Group::getId)
                    .collect(Collectors.toList());
          MatchOperation groupsMatch = Aggregation.match(
                    Criteria.where("groupId").in(groupIds).and("isDeletedBy").nin(userId));
          LookupOperation lookupMessages = LookupOperation.newLookup()
                    .from(MESSAGES)
                    .localField(LAST_MESSAGE_ID)
                    .foreignField("_id")
                    .as(LASTMESSAGE);
          LookupOperation lookupSender = LookupOperation.newLookup()
                    .from(USERS)
                    .localField(SENDER_ID)
                    .foreignField("_id")
                    .as(SENDER);
          ProjectionOperation project = Aggregation.project()
                    .and("groupId").as("groupId")
                    .and("isReadBy").as("isReadBy")
                    .and(ArrayOperators.ArrayElemAt.arrayOf(LASTMESSAGE).elementAt(0))
                    .as(LASTMESSAGE)
                    .and(ArrayOperators.ArrayElemAt.arrayOf(SENDER).elementAt(0))
                    .as(SENDER)
                    .and(IS_READ).as(IS_READ)
                    .and(CREATED_AT).as(CREATED_AT)
                    .and(UPDATED_AT).as(UPDATED_AT);
          Aggregation aggregation = Aggregation.newAggregation(
                    groupsMatch,
                    lookupMessages,
                    lookupSender,
                    project);
          AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(
                    aggregation,
                    "groupParticipants",
                    Document.class);
          List<Document> results = aggregationResults.getMappedResults();
          Map<String, Document> resultMap = results.stream()
                    .collect(Collectors.toMap(res -> res.getString("groupId"), res -> res));
          return groups.stream().map(group -> {
               Document result = resultMap.get(group.getId());
               LastMessage lastMessageForGroup = new LastMessage();
               String groupProfilePictureURI = null;
               if (result != null) {
                    Document lastMessageDoc = result.get(LASTMESSAGE, Document.class);
                    Document senderDoc = result.get(SENDER, Document.class);
                    Object obj = result.get("isReadBy");
                    @SuppressWarnings("unchecked")
                    boolean isReadBy = obj != null && ((List<String>) obj).contains(userId);
                    lastMessageForGroup = LastMessage.builder()
                              .isRead(isReadBy)
                              .senderId(senderDoc.getObjectId("_id").toHexString())
                              .senderName(senderDoc.getString("fullname"))
                              .senderProfilePicture(senderDoc.getString("profilePicture"))
                              .type(lastMessageDoc.getString("type"))
                              .content(lastMessageDoc.getString("content"))
                              .createdAt(result.getDate("createdAt"))
                              .updatedAt(result.getDate("updatedAt"))
                              .build();
               }
               if (group.getProfilePicture() != null && !group.getProfilePicture().isEmpty()) {
                    groupProfilePictureURI = azureBlobAdapter.getBlobUri(group.getProfilePicture()).toString();
               }
               return GroupList.builder()
                         .groupId(group.getId())
                         .groupName(group.getGroupName())
                         .groupProfilePicture(groupProfilePictureURI)
                         .colorCode(group.getColorCode())
                         .mutedByUserIds(group.getMutedByUserIds())
                         .pinnedByUserIds(group.getPinnedByUserIds())
                         .lastMessage(lastMessageForGroup)
                         .build();
          }).collect(Collectors.toList());
     }

     public Member findMemberInGroup(
               String groupId,
               String userId) {

          Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("_id").is(groupId)),
                    Aggregation.unwind(MEMBERS),
                    Aggregation.match(Criteria.where("members.userId").is(new ObjectId(userId))),
                    Aggregation.project()
                              .andExclude("_id")
                              .and("members.userId").as("userId")
                              .and("members.role").as("role")
                              .and("members.createdAt").as("createdAt"));

          AggregationResults<Member> results = mongoTemplate.aggregate(
                    aggregation,
                    "groups",
                    Member.class);

          return !results.getMappedResults().isEmpty() ? results.getMappedResults().get(0) : new Member();
     }

     @SuppressWarnings("unchecked")
     public Object getGroupDetails(
               String groupId,
               String userId) throws StorageException, URISyntaxException {

          Member member = this.findMemberInGroup(groupId, userId);
          boolean isAdmin = member.getRole().equals("ADMIN");

          List<User> groupMembers = null;
          String groupProfilePictureURI = null;
          Optional<Group> groupOpt = this.findById(groupId);
          Object medias = groupChatService.getMediaForGroupDetails(
                    groupId,
                    userId,
                    Arrays.asList("IMAGE", "VIDEO"),
                    member.getCreatedAt());
          Object documents = groupChatService.getMediaForGroupDetails(
                    groupId,
                    userId,
                    Arrays.asList("DOCUMENT"),
                    member.getCreatedAt());
          if (groupOpt.isPresent()) {
               Group group = groupOpt.get();
               if (group.getProfilePicture() != null) {
                    groupProfilePictureURI = azureBlobAdapter.getBlobUri(group.getProfilePicture()).toString();
               }
               groupMembers = (List<User>) this.getGroupMember(
                         groupId,
                         "",
                         1,
                         5,
                         true);
               Map<String, Object> groupDetails = new HashMap<>();
               groupDetails.put("id", group.getId());
               groupDetails.put("groupName", group.getGroupName());
               groupDetails.put("groupProfilePicture", groupProfilePictureURI);
               groupDetails.put("description", group.getDescription());
               groupDetails.put("colorCode", group.getColorCode());
               groupDetails.put("isAdmin", isAdmin);
               groupDetails.put(CREATED_AT, group.getCreatedAt());
               groupDetails.put("membersCount", group.getMembers().size());
               groupDetails.put(MEMBERS, groupMembers.subList(0, Math.min(5, groupMembers.size())));
               groupDetails.put("media", medias);
               groupDetails.put("document", documents);
               return ResponseService.successResponse(200, SUCCESS, groupDetails);
          }
          return ResponseService.successResponse(200, SUCCESS, new ArrayList<>());
     }

     public Object updateGroupMemberRole(
               String groupId,
               String userId,
               String isAdmin) {

          Query query = new Query();
          Update update = new Update();

          query.addCriteria(Criteria
                    .where("id").is(groupId)
                    .and("members.userId").is(new ObjectId(userId)));
          update.set("members.$.role", isAdmin);

          UpdateResult result = mongoTemplate.updateFirst(query, update, Group.class);

          if (result.getModifiedCount() > 0) {
               return ResponseService.successResponse(
                         200,
                         SUCCESS,
                         result.getModifiedCount());
          } else {
               return ResponseService.successResponse(
                         304,
                         "notModified",
                         "No modification occurred");
          }
     }

     public Object updateGroup(GroupDTO groupDTO) {

          Group group = this.findById(groupDTO.getId())
                    .orElseThrow(() -> new NotFound("Group does not exist."));
          if (groupDTO.getProfilePicture() != null) {
               String groupProfileName = azureBlobAdapter.upload(groupDTO.getProfilePicture());
               group.setProfilePicture(groupProfileName);
          }
          if (groupDTO.getGroupName() != null &&
                    !groupDTO.getGroupName().isEmpty() &&
                    !groupDTO.getGroupName().equals("null")) {
               group.setGroupName(groupDTO.getGroupName());
          }
          if (groupDTO.getDescription() != null &&
                    !groupDTO.getDescription().isEmpty()) {
               group.setDescription(groupDTO.getDescription());
          }
          Group savedGroup = groupRepository.save(group);
          String groupProfilePictureURI = azureBlobAdapter.getBlobUri(savedGroup.getProfilePicture()).toString();
          GroupList groupList = GroupList.builder()
                    .groupId(savedGroup.getId())
                    .groupName(savedGroup.getGroupName())
                    .groupProfilePicture(groupProfilePictureURI)
                    .colorCode(savedGroup.getColorCode())
                    .lastMessage(new LastMessage())
                    .build();
          return ResponseService.successResponse(
                    200,
                    SUCCESS,
                    groupList);
     }

     public Object getGroupMember(
               String groupId,
               String searchValue,
               int pageNo,
               int rowPerPage,
               boolean isComingFromGroupDetailsMethod) throws StorageException, URISyntaxException {

          Optional<Group> groupOptional = this.findById(groupId);

          long totalCount = 0;
          int totalNumberOfPages = 0;
          List<User> groupMembers = new ArrayList<>();
          if (groupOptional.isPresent()) {
               AggregationOperation match = Aggregation.match(Criteria.where("_id").is(groupId));
               AggregationOperation unwind = Aggregation.unwind("members");
               AggregationOperation lookup = Aggregation.lookup(
                         "users",
                         "members.userId",
                         "_id",
                         "userDetails");
               AggregationOperation matchSearch = Aggregation.match(
                         Criteria.where("userDetails.fullname").regex(searchValue, "i"));
               AggregationOperation sort = Aggregation.sort(Sort.by(
                         Sort.Order.asc("members.role")));
               AggregationOperation skip = Aggregation.skip((long) (pageNo - 1) * rowPerPage);
               AggregationOperation limit = Aggregation.limit(rowPerPage);
               AggregationOperation project = Aggregation.project()
                         .andExclude("_id")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails._id").elementAt(0))
                         .as("userId")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.fullname").elementAt(0))
                         .as("fullname")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.email").elementAt(0))
                         .as("email")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.profilePicture").elementAt(0))
                         .as("profilePicture")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.department").elementAt(0))
                         .as("department")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.designation").elementAt(0))
                         .as("designation")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.deviceToken").elementAt(0))
                         .as("deviceToken")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.mobileNo").elementAt(0))
                         .as("mobileNo")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.colorCode").elementAt(0))
                         .as("colorCode")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.isOnline").elementAt(0))
                         .as("isOnline")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.pinnedRecipientsId").elementAt(0))
                         .as("pinnedRecipientsId")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.mutedRecipientIds").elementAt(0))
                         .as("mutedRecipientIds")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.createdAt").elementAt(0))
                         .as("createdAt")
                         .and(ArrayOperators.ArrayElemAt.arrayOf("userDetails.updatedAt").elementAt(0))
                         .as("updatedAt")
                         .and("members.role").as("role");
               Aggregation aggregation = Aggregation.newAggregation(
                         match,
                         unwind,
                         lookup,
                         matchSearch,
                         sort,
                         skip,
                         limit,
                         project);
               AggregationResults<User> results = mongoTemplate.aggregate(
                         aggregation,
                         "groups",
                         User.class);
               groupMembers = results.getMappedResults();

               String sasToken = azureBlobAdapter.generateContainerSasToken();
               String uri = "https://packagingapp.blob.core.windows.net/trove/";
               groupMembers.forEach(g -> {
                    if (g.getProfilePicture() != null && !g.getProfilePicture().isEmpty()) {
                         g.setProfilePicture(uri + g.getProfilePicture() + "?" + sasToken);
                    }
               });
               if (isComingFromGroupDetailsMethod) {
                    return groupMembers;
               }
               totalCount = mongoTemplate.aggregate(Aggregation.newAggregation(
                         match,
                         unwind,
                         lookup,
                         matchSearch),
                         "groups", Document.class).getMappedResults().size();
               totalNumberOfPages = (int) Math.ceil((double) totalCount / rowPerPage);
          }
          Map<String, Object> response = new HashMap<>();

          response.put("totalCount", totalCount);
          response.put("groupMembers", groupMembers);
          response.put("totalNumberOfPages", totalNumberOfPages);

          return ResponseService.successResponse(200, SUCCESS, response);
     }

     public Object getGroups(
               String userId,
               String groupName,
               int pageNo,
               int rowPerPage,
               boolean fetchAll) throws StorageException, URISyntaxException {

          List<AggregationOperation> aggregationOperations = new ArrayList<>();
          if (groupName != null && !groupName.isEmpty()) {
               MatchOperation groupsMatch = Aggregation.match(
                         Criteria.where("groupName").regex(".*" + groupName + ".*", "i"));
               aggregationOperations.add(groupsMatch);
          }
          MatchOperation memberMatch = Aggregation.match(
                    Criteria.where("members.userId").is(new ObjectId(userId)));
          aggregationOperations.add(memberMatch);
          LookupOperation lookupMember = LookupOperation.newLookup()
                    .from("users")
                    .localField("members.userId")
                    .foreignField("_id")
                    .as("member");
          aggregationOperations.add(lookupMember);
          LookupOperation lookupGroupParticipants = LookupOperation.newLookup()
                    .from("groupParticipants")
                    .localField("_id")
                    .foreignField("groupId")
                    .as("groupPrac");
          aggregationOperations.add(lookupGroupParticipants);
          UnwindOperation unwindGroupPrac = Aggregation.unwind("groupPrac");
          aggregationOperations.add(unwindGroupPrac);
          MatchOperation senderMatch = Aggregation.match(
                    Criteria.where("groupPrac.userId").is(new ObjectId(userId)));
          aggregationOperations.add(senderMatch);
          LookupOperation lookupMessages = LookupOperation.newLookup()
                    .from(MESSAGES)
                    .localField("groupPrac.lastMessageId")
                    .foreignField("_id")
                    .as("lastMessages");
          aggregationOperations.add(lookupMessages);
          LookupOperation lookupSender = LookupOperation.newLookup()
                    .from(USERS)
                    .localField("groupPrac.senderId")
                    .foreignField("_id")
                    .as(SENDER);
          aggregationOperations.add(lookupSender);
          ProjectionOperation project = Aggregation.project()
                    .andInclude(
                              "groupName",
                              "colorCode",
                              "description",
                              "pinnedByUserIds",
                              "mutedByUserIds",
                              "createdBy",
                              "createdAt",
                              UPDATED_AT)
                    .and("_id").as("groupId")
                    .and("profilePicture").as("groupProfilePicture")
                    .and("member").as("members")
                    .and("sender._id").as("lastMessage.senderId")
                    .and("sender.fullname").as("lastMessage.senderName")
                    .and("sender.profilePicture").as("lastMessage.senderProfilePicture")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.id").elementAt(0))
                    .as("lastMessage.id")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.content").elementAt(0))
                    .as("lastMessage.content")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.type").elementAt(0))
                    .as("lastMessage.type")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.createdAt").elementAt(0))
                    .as("lastMessage.createdAt")
                    .and(ArrayOperators.ArrayElemAt.arrayOf("lastMessages.updatedAt").elementAt(0))
                    .as("lastMessage.updatedAt")
                    .and("groupPrac.isRead").as("lastMessage.isRead")
                    .and("groupPrac.unreadMessageCount").as("lastMessage.unreadMessageCount")
                    .and("groupPrac.pinnedAt").as("pinnedAt");
          aggregationOperations.add(project);

          if (fetchAll) {
               Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
               AggregationResults<GroupList> aggregationResults = mongoTemplate.aggregate(
                         aggregation,
                         "groups",
                         GroupList.class);
               return aggregationResults.getMappedResults();
          }

          List<AggregationOperation> countOperations = new ArrayList<>(aggregationOperations);
          countOperations.add(Aggregation.count().as("count"));
          Aggregation countAggregation = Aggregation.newAggregation(countOperations);
          AggregationResults<Document> countResults = mongoTemplate.aggregate(
                    countAggregation,
                    "groups",
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
          AggregationResults<GroupList> aggregationResults = mongoTemplate.aggregate(
                    aggregation,
                    "groups",
                    GroupList.class);
          List<GroupList> groupLists = aggregationResults.getMappedResults();

          String sasToken = azureBlobAdapter.generateContainerSasToken();
          String uri = "https://packagingapp.blob.core.windows.net/trove/";

          groupLists.stream().forEach(groupList -> {
               if (groupList.getGroupProfilePicture() != null && !groupList.getGroupProfilePicture().isEmpty()) {
                    groupList.setGroupProfilePicture(uri + groupList.getGroupProfilePicture() + "?" + sasToken);
               }
               if (groupList.getMutedByUserIds() != null && !groupList.getMutedByUserIds().isEmpty()) {
                    groupList.setMutedGroup(groupList.getMutedByUserIds().contains(userId));
               }
               if (groupList.getPinnedByUserIds() != null && !groupList.getPinnedByUserIds().isEmpty()) {
                    groupList.setPinnedGroup(groupList.getPinnedByUserIds().stream()
                              .anyMatch(pinnedByUserIds -> pinnedByUserIds.getUserId().equals(userId)));
               }
          });
          int totalNumberOfPages = (int) Math.ceil((double) totalCount / rowPerPage);
          Map<String, Object> response = new HashMap<>();
          response.put("totalCount", totalCount);
          response.put("chatGroupsList", groupLists);
          response.put("totalNumberOfPages", totalNumberOfPages);
          return ResponseService.successResponse(200, "success", response);
     }

     public Object getTotalUnreadMessageCountForCircles(
               String userId) {

          List<AggregationOperation> aggregationOperations = new ArrayList<>();
          aggregationOperations.add(Aggregation.match(
                    new Criteria().andOperator(
                              Criteria.where("userId").is(new ObjectId(userId)),
                              Criteria.where("isRead").is(false))));
          // aggregationOperations.add(Aggregation.group().sum("unreadMessageCount").as("count"));
          aggregationOperations.add(Aggregation.count().as("count"));
          Aggregation countAggregation = Aggregation.newAggregation(aggregationOperations);
          AggregationResults<Document> countResults = mongoTemplate.aggregate(
                    countAggregation,
                    "groupParticipants",
                    Document.class);
          Integer totalCount = 0;
          if (!countResults.getMappedResults().isEmpty()) {
               totalCount = (Integer) countResults.getMappedResults().get(0).get("count");
          }
          Map<String, Object> response = new HashMap<>();
          response.put("totalCount", totalCount);
          return ResponseService.successResponse(200, "success", response);
     }
}
