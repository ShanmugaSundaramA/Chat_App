package com.ws.chat.responsebody;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReplyRes {

     private String senderId;
     private String recipientId;
     private String senderName;
     private String recipientName;
     private String senderProfilePicture;
     private String recipientProfilePicture;
     private String replyMessageId;
     private Object message;
}