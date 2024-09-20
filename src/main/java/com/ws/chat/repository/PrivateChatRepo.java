package com.ws.chat.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ws.chat.model.PrivateChat;

public interface PrivateChatRepo extends MongoRepository<PrivateChat, String> {

     long countByChatIdAndIsDeleted(
               String chatId,
               boolean isDeleted);

     Optional<PrivateChat> findByMessageId(String msgId);

     void deleteByChatId(String chatId);

}