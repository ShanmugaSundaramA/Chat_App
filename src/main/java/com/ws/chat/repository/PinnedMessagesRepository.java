package com.ws.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ws.chat.model.PinnedMessages;

public interface PinnedMessagesRepository extends MongoRepository<PinnedMessages, String> {

     void deleteByMessageId(String messageId);

     void deleteByChatId(String chatId);

     void deleteByGroupId(String groupId);
}