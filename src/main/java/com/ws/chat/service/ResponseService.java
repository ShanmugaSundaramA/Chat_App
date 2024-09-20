package com.ws.chat.service;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.ws.chat.responsebody.ResponseDTO;

@Service
public class ResponseService {

     private ResponseService() {
     }

     public static ResponseDTO successResponse(int statusCode, String status, Object data) {
          return ResponseDTO.builder()
                    .status(status)
                    .statusCode(statusCode)
                    .data(data)
                    .timeStamp(new Date())
                    .build();
     }
}
