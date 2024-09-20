package com.ws.chat.service;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.ws.chat.exception.NotFound;
import com.ws.chat.model.GroupParticipants;
import com.ws.chat.repository.GroupParticipantsRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupParticipentService {

     private final GroupParticipantsRepo groupParticipantsRepo;
     private final MongoTemplate mongoTemplate;

     // public GroupParticipants findByGroupId(String groupId) {

     // return groupParticipantsRepo.findByGroupId(
     // new ObjectId(groupId)).orElseGet(GroupParticipants::new);
     // }

     public GroupParticipants saveGroupParticipants(GroupParticipants groupParticipant) {
          return groupParticipantsRepo.save(groupParticipant);
     }

     public List<GroupParticipants> saveAllGroupParticipants(List<GroupParticipants> groupParticipants) {
          return groupParticipantsRepo.saveAll(groupParticipants);
     }

     public Long deleteByGroupId(
               String userId,
               String groupId) {

          GroupParticipants groupParticipant = groupParticipantsRepo.findByGroupIdAndUserId(
                    new ObjectId(groupId),
                    new ObjectId(userId)).orElseThrow(() -> new NotFound("GroupParticipant not found."));
          groupParticipant.setLastMessageId(null);
          groupParticipant.setSenderId(null);
          groupParticipant.setUnreadMessageCount(0);
          groupParticipantsRepo.save(groupParticipant);
          return 1L;
     }

     public void updateLastMessageId(
               ObjectId groupId,
               ObjectId senderId,
               String lastMessageId) {

          Query groupQuery = new Query();
          groupQuery.addCriteria(Criteria.where("groupId").is(groupId));

          Update groupPraticipantsUpdate = new Update();
          groupPraticipantsUpdate.set("senderId", senderId);
          groupPraticipantsUpdate.set("lastMessageId", lastMessageId);
          groupPraticipantsUpdate.set("updatedAt", new Date());

          BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GroupParticipants.class);
          bulkOps.updateMulti(groupQuery, groupPraticipantsUpdate);
          bulkOps.execute();
     }

}
