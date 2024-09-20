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
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "inboxParticipants")
public class InboxParticipants {

     @Id
     private String id;
     private String chatId;
     private ObjectId senderId; 
     private ObjectId recipientId;
     private String lastMessageId;
     private boolean isSendByRecipient; 
     private Date pinnedAt; 
     private boolean isRead;
     private int unreadMessageCount;
     private boolean isDeleted; 
     private List<String> isDeletedBy;
     @CreatedDate
     private Date createdAt;
     @LastModifiedDate
     private Date updatedAt;
}