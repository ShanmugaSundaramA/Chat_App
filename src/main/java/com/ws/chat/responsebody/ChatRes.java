package com.ws.chat.responsebody;

import com.ws.chat.model.Type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRes {

     private String id;
     private String senderId;
     private String senderName;
     private String senderProfilePicture;
     private String groupId;
     private String groupName;
     private String recipientId;
     private String recipientName;
     private Type type;
     private MessageRes message;
     private boolean isForwardMessage;

}
