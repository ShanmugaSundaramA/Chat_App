package com.ws.chat.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ws.chat.model.GroupChat;

public interface GroupChatRepository extends MongoRepository<GroupChat, String> {

     Optional<GroupChat> findByMessageId(String msgId);

     long countByGroupIdAndIsDeletedAndTypeInAndCreatedAtGreaterThanEqual(
               String groupId,
               boolean isDeleted,
               List<String> types,
               Date createdAt);

}