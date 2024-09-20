package com.ws.chat.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "groupParticipants")
public class GroupParticipants {

     @Id
     private String id;
     private ObjectId groupId;
     private ObjectId userId;
     private ObjectId senderId;
     private Type type;
     private String lastMessageId;
     private int unreadMessageCount;
     private boolean isRead;
     private Date pinnedAt;
     @CreatedDate
     private Date createdAt;
     @LastModifiedDate
     private Date updatedAt;
}
