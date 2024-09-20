package com.ws.chat.requestbody;

import java.util.Date;
import java.util.List;

import com.ws.chat.model.Message;
import com.ws.chat.model.Type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaDTO {

     private List<MediaFile> audios;
     private List<MediaFile> videos;
     private List<MediaFile> documents;
     private List<MediaFile> images;
     private List<LinkAndNum> links;
     private List<LinkAndNum> contacts;
     private Type type;
     private String senderId;
     private String senderName;
     private String senderProfilePicture;
     private String department;
     private String designation;
     private String colorCode;
     private String recipientId;
     private String recipientName;
     private String groupId;
     private String groupName;
     private String groupProfilePicture;
     private String messageId;
     private Message message;
     private String replyTo;
     private Date sendAt;
     private String deviceToken;
}