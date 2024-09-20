package com.ws.chat.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ws.chat.model.Version;

public interface VersionRepository extends MongoRepository<Version, String> {
     
     Optional<Version> findByAppName(String appName);
}
