package com.ws.chat.responsebody;

import java.util.Date;
import java.util.List;

import com.ws.chat.model.GroupUser;
import com.ws.chat.model.PinnedGroup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupList {

     private String userId;
     private String userName;
     private String userProfilePicture;
     private String groupId;
     private String groupName;
     private String groupProfilePicture;
     private String colorCode;
     private List<GroupUser> members;
     private boolean isPinnedGroup;
     private boolean isMutedGroup;
     private List<PinnedGroup> pinnedByUserIds;
     private List<String> mutedByUserIds;
     private List<String> isReadBy;
     private Date pinnedAt;
     private LastMessage lastMessage;
}