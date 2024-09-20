package com.ws.chat.requestbody;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PinnedMessageDTO {

     private String id;
     private String userId;
     private String recipientId;
     private String groupId;
     private String messageId;
     private Object message;
}
