package com.ws.chat.controller;

import java.net.URISyntaxException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.microsoft.azure.storage.StorageException;
import com.ws.chat.requestbody.UserDTO;
import com.ws.chat.service.UserService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

     private final UserService userService;

     @GetMapping("/getUser")
     public ResponseEntity<Object> getUser(@RequestParam String userId) {

          return new ResponseEntity<>(userService.getUser(userId), HttpStatus.OK);
     }

     @GetMapping("/findUserByEmail")
     public ResponseEntity<Object> findUserByEmail(
               @RequestParam String userName,
               @RequestParam String emailId,
               @RequestParam String designation,
               @RequestParam String department,
               @RequestParam String deviceToken,
               @RequestParam String mobileNo) {

          return new ResponseEntity<>(userService.findUserByEmail(
                    userName,
                    emailId,
                    designation,
                    department,
                    deviceToken,
                    mobileNo), HttpStatus.OK);
     }

     @PostMapping("/logoutUser")
     public ResponseEntity<Object> logout(
               @RequestParam String userId) {

          return new ResponseEntity<>(userService.logout(userId), HttpStatus.OK);
     }

     @PostMapping("/updateUser")
     public ResponseEntity<Object> postMethodName(
               @RequestBody UserDTO userDTO) {

          return new ResponseEntity<>(userService.updateUserDetails(userDTO),
                    HttpStatus.OK);
     }

     @PostMapping("/pinRecipientsOrGroupsForUser")
     public ResponseEntity<Object> pinRecipientsForUser(
               @RequestParam String userId,
               @RequestParam String recipientId,
               @RequestParam String groupId) {

          return new ResponseEntity<>(userService.pinRecipientsForUser(
                    userId,
                    recipientId,
                    groupId), HttpStatus.OK);
     }

     @PostMapping("/unpinRecipientsOrGroupsForUser")
     public ResponseEntity<Object> unpinRecipientsForUser(
               @RequestParam String userId,
               @RequestParam String recipientId,
               @RequestParam String groupId) {

          return new ResponseEntity<>(userService.unpinRecipientsForUser(
                    userId,
                    recipientId,
                    groupId), HttpStatus.OK);
     }

     @PostMapping("/muteRecipientsOrGroupsForUser")
     public ResponseEntity<Object> muteRecipientsForUser(
               @RequestParam String userId,
               @RequestParam String recipientId,
               @RequestParam String groupId) {

          return new ResponseEntity<>(userService.muteRecipientsForUser(
                    userId,
                    recipientId,
                    groupId), HttpStatus.OK);
     }

     @PostMapping("/unmuteRecipientsOrGroupsForUser")
     public ResponseEntity<Object> unmuteRecipientsForUser(
               @RequestParam String userId,
               @RequestParam String recipientId,
               @RequestParam String groupId) {

          return new ResponseEntity<>(userService.unmuteRecipientsForUser(
                    userId,
                    recipientId,
                    groupId), HttpStatus.OK);
     }

     @GetMapping("/findRecipientsForUser")
     public ResponseEntity<Object> findRecipientsForUser(
               @RequestParam String userId,
               @RequestParam String searchValue,
               @RequestParam(required = false, defaultValue = "false") Boolean unread,
               @RequestParam int pageNo,
               @RequestParam int rowPerPage) throws StorageException, URISyntaxException {

          return new ResponseEntity<>(userService.findRecipientsForUser(
                    userId,
                    searchValue,
                    unread,
                    pageNo,
                    rowPerPage,
                    false), HttpStatus.OK);
     }

     @GetMapping("/findAllRecipientsAndGroups")
     public ResponseEntity<Object> findAllRecipientsAndGroups(
               @RequestParam String userId,
               @RequestParam String searchValue,
               @RequestParam int pageNo,
               @RequestParam int rowPerPage) throws StorageException, URISyntaxException {

          return new ResponseEntity<>(userService.findAllRecipientsAndGroups(
                    userId,
                    searchValue,
                    pageNo,
                    rowPerPage), HttpStatus.OK);
     }

     @GetMapping("/findAll")
     public ResponseEntity<Object> findAll(
               @RequestParam String userId,
               @RequestParam(required = false) String groupId,
               @RequestParam String searchValue,
               @RequestParam int pageNo,
               @RequestParam int rowPerPage) throws StorageException, URISyntaxException {

          return new ResponseEntity<>(userService.findAll(
                    userId,
                    groupId,
                    searchValue,
                    pageNo,
                    rowPerPage), HttpStatus.OK);
     }

     @GetMapping("/getTotalUnreadMessageCountForDMs")
     public ResponseEntity<Object> getTotalUnreadMessageCountForDMs(
               @RequestParam String userId) {

          return new ResponseEntity<>(userService.getTotalUnreadMessageCountForDMs(
                    userId), HttpStatus.OK);
     }
}
