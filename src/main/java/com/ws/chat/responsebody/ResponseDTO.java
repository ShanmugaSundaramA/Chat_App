package com.ws.chat.responsebody;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDTO {

     private int statusCode;
     private String status;
     private Object data;
     private Date timeStamp;
}
