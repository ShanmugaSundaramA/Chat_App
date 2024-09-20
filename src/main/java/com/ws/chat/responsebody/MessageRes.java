package com.ws.chat.responsebody;

import java.util.Date;

import com.ws.chat.model.Type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageRes {

     private String id;
     private Type type;
     private Object content;
     private String caption;
     private Long size;
     private String name;
     private Date sendAt;
     private Date deliveredAt;
     private Date seenAt;
     private Date createdAt;
     private Date updatedAt;

}
