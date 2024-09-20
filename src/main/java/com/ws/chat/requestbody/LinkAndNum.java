package com.ws.chat.requestbody;

import java.util.Date;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkAndNum {

     private String id;
     private String media;
     private Date sendAt;
     private Date deliveredAt;
     private Date seenAt;
}