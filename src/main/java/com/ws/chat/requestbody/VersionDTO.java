package com.ws.chat.requestbody;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VersionDTO {

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
}
