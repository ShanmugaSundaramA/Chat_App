package com.ws.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ws.chat.model.Message;

public interface MessageRepository extends MongoRepository<Message, String> {

}