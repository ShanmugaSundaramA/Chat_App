package com.ws.chat.controller;

import org.springframework.web.bind.annotation.RestController;

import com.microsoft.azure.storage.StorageException;
import com.ws.chat.service.PrivateChatService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URISyntaxException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/privateChat")
@CrossOrigin(origins = "*")
public class PrivateChatController {

     private final PrivateChatService privateChatService;

     @GetMapping("/getMessages")
     public ResponseEntity<Object> getPrivateMessages(
               @RequestParam String senderId,
               @RequestParam String recipientId,
               @RequestParam int pageNo,
               @RequestParam int rowPerPage) throws StorageException, URISyntaxException {

          return new ResponseEntity<>(privateChatService.getPrivateMessages(
                    senderId,
                    recipientId,
                    pageNo,
                    rowPerPage), HttpStatus.OK);
     }

     @GetMapping("/updateSeenAt")
     public ResponseEntity<Object> updateSeenAt(
               @RequestParam String senderId,
               @RequestParam String recipientId) {

          return new ResponseEntity<>(privateChatService.updateSeenAt(
                    senderId,
                    recipientId), HttpStatus.OK);
     }

     @PostMapping("/deletePrivateChat")
     public ResponseEntity<Object> deletePrivateChat(
               @RequestParam String senderId,
               @RequestParam String chatId) {

          return new ResponseEntity<>(privateChatService.deletePrivateChat(
                    senderId,
                    chatId), HttpStatus.OK);
     }

}
