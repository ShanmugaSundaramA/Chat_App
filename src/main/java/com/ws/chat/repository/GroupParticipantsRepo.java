package com.ws.chat.repository;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.ws.chat.model.GroupParticipants;

public interface GroupParticipantsRepo extends MongoRepository<GroupParticipants, String> {

     List<GroupParticipants> findByGroupId(ObjectId groupId);

     void deleteByLastMessageId(String msgId);

     void deleteByGroupId(String groupId);

     Optional<GroupParticipants> findByGroupIdAndUserId(ObjectId groupId, ObjectId userId);
}