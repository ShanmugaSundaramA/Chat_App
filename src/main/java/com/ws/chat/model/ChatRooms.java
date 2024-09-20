package com.ws.chat.model;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "chatRoom")
public class ChatRooms {

     @Id
     private String id;
     private String chatId;
     private String senderId;
     private String recipientId;
     @CreatedDate
     private Date createdAt;
     @LastModifiedDate
     private Date updatedAt;
}