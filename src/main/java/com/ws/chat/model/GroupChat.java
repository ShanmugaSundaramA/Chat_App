package com.ws.chat.model;

import java.util.Date;
import java.util.List;

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
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "groupChats")
public class GroupChat {

     @Id
     private String id;
     private String groupId;
     private ObjectId senderId;
     private Type type;
     private String messageId;
     private List<Reply> replies;
     private boolean isDeleted;
     private boolean isForwardMessage;
     private List<String> isDeletedBy;
     @CreatedDate
     private Date createdAt;
     @LastModifiedDate
     private Date updatedAt;
}
