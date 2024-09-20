package com.ws.chat.requestbody;

import java.util.Date;
import java.util.List;

import com.ws.chat.model.Message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForwardDTO {

     private String senderId;
     private String senderName;
     private List<RecipientId> recipientIds;
     private boolean isForwardMessage;
     private List<Message> messages;
     private Date sendAt;
}