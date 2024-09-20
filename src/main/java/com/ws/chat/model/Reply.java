package com.ws.chat.model;

import java.util.Date;

import org.bson.types.ObjectId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Reply {

     private ObjectId senderId;
     private ObjectId recipientId;
     private String replyMessageId;
     private Date createdAt;
     private Date updatedAt;
}