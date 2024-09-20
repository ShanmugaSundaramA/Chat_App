package com.ws.chat.repository;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.ws.chat.model.InboxParticipants;

public interface InboxParticipantsRepository extends MongoRepository<InboxParticipants, String> {

     List<InboxParticipants> findByChatId(String chatId);

     void deleteByLastMessageId(String messageId);

     void deleteByChatIdAndSenderId(String chatId, ObjectId senderId);

     Optional<InboxParticipants> findBySenderIdAndRecipientId(ObjectId senderId, ObjectId recipientId);
}