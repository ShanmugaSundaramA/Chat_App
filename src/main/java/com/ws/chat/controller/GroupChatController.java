package com.ws.chat.controller;

import org.springframework.web.bind.annotation.RestController;

import com.microsoft.azure.storage.StorageException;
import com.ws.chat.service.GroupChatService;

import lombok.RequiredArgsConstructor;

import java.net.URISyntaxException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/groupChat")
@CrossOrigin(origins = "*")
public class GroupChatController {

     private final GroupChatService groupChatService;

     @GetMapping("/getMessages")
     public ResponseEntity<Object> getGroupChat(
               @RequestParam String groupId,
               @RequestParam String userId,
               @RequestParam int pageNo,
               @RequestParam int rowPerPage) throws StorageException, URISyntaxException {

          return new ResponseEntity<>(groupChatService.getGroupChat(
                    groupId,
                    userId,
                    pageNo,
                    rowPerPage), HttpStatus.OK);
     }

     @GetMapping("/getRepliesForMessage")
     public Object getRepliesForMessage(
               @RequestParam String groupId,
               @RequestParam String messageId,
               @RequestParam int pageNo,
               @RequestParam int rowPerPage) {

          return groupChatService.getReplyForMessage(
                    groupId,
                    messageId,
                    pageNo,
                    rowPerPage);
     }

     @GetMapping("/getMediaMessages")
     public ResponseEntity<Object> getMediaMessages(
               @RequestParam String groupId,
               @RequestParam String userId,
               @RequestParam("type") List<String> types,
               @RequestParam int pageNo,
               @RequestParam int rowPerPage) throws StorageException, URISyntaxException {

          return new ResponseEntity<>(groupChatService.getMediaMessages(
                    groupId,
                    userId,
                    types,
                    pageNo,
                    rowPerPage), HttpStatus.OK);
     }

     @PostMapping("/deleteGroupChat")
     public ResponseEntity<Object> deleteGroupChat(
               @RequestParam String userId,
               @RequestParam String groupId) {

          return new ResponseEntity<>(groupChatService.deleteGroupChat(
                    userId,
                    groupId), HttpStatus.OK);
     }

     @PostMapping("/updateSeenAt")
     public ResponseEntity<Object> updateSeenAt(
               @RequestParam String userId,
               @RequestParam String groupId) {

          return new ResponseEntity<>(groupChatService.updateSeenAt(
                    userId,
                    groupId), HttpStatus.OK);
     }

}
