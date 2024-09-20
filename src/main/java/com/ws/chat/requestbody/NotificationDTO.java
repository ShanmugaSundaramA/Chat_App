package com.ws.chat.requestbody;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationDTO {

     private String recipientToken;
     private String title;
     private String body;
     private String image;
     private Map<String, String> data;
}
