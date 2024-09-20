package com.ws.chat.model;

import java.util.Date;

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
@Document(collection = "pinnedMessages")
public class PinnedMessages {

     @Id
     private String id;
     private String chatId;
     private String groupId;
     private String messageId;
     private Object message;
     @CreatedDate
     private Date createdAt;
     @LastModifiedDate
     private Date updatedAt;

}