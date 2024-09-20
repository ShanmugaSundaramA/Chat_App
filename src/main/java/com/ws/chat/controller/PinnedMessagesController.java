package com.ws.chat.controller;

import org.springframework.web.bind.annotation.RestController;

import com.ws.chat.requestbody.PinnedMessageDTO;
import com.ws.chat.service.PinnedMessagesService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pinnedMessage")
public class PinnedMessagesController {

     private final PinnedMessagesService pinnedMessagesService;

     @PostMapping("/savePinnedMessage")
     public ResponseEntity<Object> savePinnedMessage(@RequestBody PinnedMessageDTO pinnedMessageDTO) {

          return new ResponseEntity<>(pinnedMessagesService.savePinnedMessage(pinnedMessageDTO), HttpStatus.OK);
     }

     @GetMapping("/getPinnedMessage")
     public ResponseEntity<Object> getPinnedMessage(
               @RequestParam String userId,
               @RequestParam String recipientId,
               @RequestParam(required = false) String groupId) {

          return new ResponseEntity<>(pinnedMessagesService.getPinnedMessage(userId, recipientId, groupId),
                    HttpStatus.OK);
     }

     @PostMapping("/deletePinnedMessage")
     public ResponseEntity<Object> deletePinnedMessage(@RequestParam String pinnedMessageId) {

          return new ResponseEntity<>(pinnedMessagesService.deleteById(pinnedMessageId), HttpStatus.OK);
     }

}