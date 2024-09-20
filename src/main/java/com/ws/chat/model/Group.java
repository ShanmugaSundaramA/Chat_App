package com.ws.chat.model;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "groups")
public class Group {

     @Id
     private String id;
     private String groupName;
     private String profilePicture;
     private String colorCode;
     private String description;
     private List<Member> members;
     private String createdBy;
     private List<PinnedGroup> pinnedByUserIds;
     private List<String> mutedByUserIds;
     @CreatedDate
     private Date createdAt;
     @LastModifiedDate
     private Date updatedAt;
}