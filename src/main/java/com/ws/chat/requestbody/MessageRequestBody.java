package com.ws.chat.requestbody;

import java.util.Date;
// import java.util.List;

// import com.ws.chat.model.Mention;
import com.ws.chat.model.Message;
import com.ws.chat.model.Type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageRequestBody {

     private Type type;
     private String action;
     private boolean isForwardMessage;
     private String senderId;
     private String senderName;
     private String senderProfilePicture;
     private String department;
     private String designation;
     private String colorCode;
     private String recipientId;
     private String recipientName;
     private String recipientProfilePicture;
     private String groupId;
     private String groupName;
     private String groupProfilePicture;
     private String messageId;
     private Message message;
     // private boolean isMentionedMessage;
     // private List<Mention> mentions;
     private String replySenderId;
     private String replyToMessageId;
     private String deviceToken;
     private String chatId;
     private Date sendAt;
     private boolean isOnline;
     private String userId;
     // private boolean deleteForEveryone;
}
