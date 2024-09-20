package com.ws.chat.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ws.chat.model.ChatRooms;

public interface ChatRoomRepository extends MongoRepository<ChatRooms, String> {
     
     Optional<ChatRooms> findBySenderIdAndRecipientId(String senderId, String recipientId);
}
