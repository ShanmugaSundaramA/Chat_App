package com.ws.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "messages")
public class Message {

     @Id
     private String id;
     private Type type;
     private String content;
     private String caption;
     private String name;
     private long size;
     private Date sendAt;
     private Date deliveredAt;
     private Date seenAt;
     @CreatedDate
     private Date createdAt;
     @LastModifiedDate
     private Date updatedAt;

}