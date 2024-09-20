package com.ws.chat.model;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Version {

     @Id
     private String id;
     private String appName;
     private String title;
     private String description;
     private String androidVersion;
     private String iosVersion;
     private String androidAppLink;
     private String iosAppLink;
     private boolean isUpdate;
     private boolean isMandatoryUpdate;
     @CreatedDate
     private Date createdAt;
     @LastModifiedDate
     private Date updateAt;

}