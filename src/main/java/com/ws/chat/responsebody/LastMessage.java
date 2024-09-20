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
public class LastMessage {

     private String senderId;
     private String senderName;
     private String senderProfilePicture;
     private String id;
     private String content;
     private String type;
     private boolean isRead;
     private int unreadMessageCount;
     private Date createdAt;
     private Date updatedAt;
}