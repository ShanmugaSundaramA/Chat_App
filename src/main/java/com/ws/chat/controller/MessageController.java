package com.ws.chat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ws.chat.requestbody.ForwardDTO;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.service.MessageService;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@AllArgsConstructor
@RequestMapping("/message")
@CrossOrigin(origins = "*")
public class MessageController {

     private final MessageService messageService;

     @PostMapping("/updateMessage")
     public ResponseEntity<Object> updateMessage(@RequestBody MessageRequestBody messageRequestBody) {
          return new ResponseEntity<>(messageService.updateMessage(messageRequestBody), HttpStatus.OK);
     }

     @PostMapping("/deleteMessage")
     public ResponseEntity<Object> deleteMessage(@RequestBody MessageRequestBody messageRequestBody) {
          return new ResponseEntity<>(messageService.deleteMessage(messageRequestBody), HttpStatus.OK);
     }

     @PostMapping("/forwardMessage")
     public ResponseEntity<Object> forwardMessage(@RequestBody ForwardDTO forwardDTO) {
          return new ResponseEntity<>(messageService.forwardMessage(forwardDTO), HttpStatus.OK);
     }
}
