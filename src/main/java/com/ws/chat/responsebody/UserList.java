package com.ws.chat.responsebody;

import java.util.Date;
import java.util.List;

import com.ws.chat.model.PinnedRecipient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserList {

     private String userId;
     private String userName;
     private String userProfilePicture;
     private String userEmailId;
     private String userDesignation;
     private String userDepartment;
     private String colorCode;
     private String deviceToken;
     private String chatId;
     private Date pinnedAt;
     private List<PinnedRecipient> pinnedRecipientsId;
     private List<String> mutedRecipientIds;
     private boolean isSendByRecipient;
     private boolean isOnline;
     private boolean isPinnedChat;
     private boolean isMutedChat;
     private String groupId;
     private String groupName;
     private String groupProfilePicture;
     private LastMessage lastMessage;
}
