package com.ws.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.ws.chat.responsebody.ResponseDTO;

class ResponseServiceTest {

     @Test
     void testSuccessResponse() {

          int statusCode = 200;
          String status = "success";
          Object data = "Success Response Created";
          Date timestamp = new Date();

          ResponseDTO response = ResponseService.successResponse(statusCode, status, data);

          assertNotNull(response);
          assertEquals(statusCode, response.getStatusCode());
          assertEquals(status, response.getStatus());
          assertEquals(data, response.getData());
          assertNotNull(response.getTimeStamp());
          assertTrue(response.getTimeStamp().after(timestamp) || response.getTimeStamp().equals(timestamp));
     }
}
