package com.ws.chat.responsebody;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InboxParticipantRes {

     private String id;
     private String senderId;
     private String recipientId;
     private String chatId;
     private LastMessage lastMessage;
     private boolean isRead;
     private Date createdAt;
     private Date updatedAt;
}