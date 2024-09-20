package com.ws.chat.responsebody;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class PrivateChatRes extends ChatRes {

     private MessageRes replyTo;
     private String replySenderName;
     private String replySenderProfilePicture;
}
