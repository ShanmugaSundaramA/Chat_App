package com.ws.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ws.chat.model.Group;

public interface GroupRepository extends MongoRepository<Group, String> {
}
