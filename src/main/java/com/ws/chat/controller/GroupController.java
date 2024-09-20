package com.ws.chat.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.storage.StorageException;
import com.ws.chat.model.Member;
import com.ws.chat.requestbody.GroupDTO;
import com.ws.chat.service.GroupService;

import lombok.RequiredArgsConstructor;

import java.net.URISyntaxException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
@CrossOrigin(origins = "*")
public class GroupController {

     private final GroupService groupService;
     private final ObjectMapper mapper;

     @PostMapping("/createGroup")
     public ResponseEntity<Object> createGroup(
               @RequestParam(required = false) MultipartFile profilePicture,
               @RequestParam(required = false) String groupName,
               @RequestParam(required = false) String description,
               @RequestParam(required = false) String members,
               @RequestParam(required = false) String createdBy) throws JsonProcessingException {

          List<Member> membersList = mapper.readValue(
                    members,
                    new TypeReference<List<Member>>() {
                    });

          return new ResponseEntity<>(groupService.createGroup(
                    profilePicture,
                    groupName,
                    description,
                    membersList,
                    createdBy), HttpStatus.OK);
     }

     @PostMapping("/updateGroup")
     public ResponseEntity<Object> updateGroup(
               @io.swagger.v3.oas.annotations.parameters.RequestBody GroupDTO groupDTO) {

          return new ResponseEntity<>(groupService.updateGroup(groupDTO), HttpStatus.OK);
     }

     @PostMapping("/updateGroupMemberRole")
     public ResponseEntity<Object> updateGroupMemberRole(
               @RequestParam String groupId,
               @RequestParam String userId,
               @RequestParam String isAdmin) {

          return new ResponseEntity<>(groupService.updateGroupMemberRole(
                    groupId,
                    userId,
                    isAdmin), HttpStatus.OK);
     }

     @PostMapping("/addGroupMember")
     public ResponseEntity<Object> addGroupMember(
               @RequestBody GroupDTO groupDTO) {

          return new ResponseEntity<>(groupService.addGroupMember(
                    groupDTO.getId(),
                    groupDTO.getMembers()), HttpStatus.OK);
     }

     @PostMapping("/removeUserFromGroup")
     public ResponseEntity<Object> removeUserFromGroup(
               @RequestParam String groupId,
               @RequestParam String userId) {

          return new ResponseEntity<>(groupService.removeUserFromGroup(
                    groupId,
                    userId), HttpStatus.OK);
     }

     @PostMapping("/leaveGroup")
     public ResponseEntity<Object> leaveGroup(
               @RequestParam String groupId,
               @RequestParam String userId) {

          return new ResponseEntity<>(groupService.leaveGroup(
                    groupId,
                    userId), HttpStatus.OK);
     }

     @PostMapping("/deleteGroup/{groupId}")
     public ResponseEntity<Object> deleteGroup(@PathVariable String groupId) {

          return new ResponseEntity<>(groupService.deleteGroup(groupId), HttpStatus.OK);
     }

     @GetMapping("/getGroupDetails")
     public ResponseEntity<Object> getGroupDetails(
               @RequestParam String userId,
               @RequestParam String groupId) throws StorageException, URISyntaxException {

          return new ResponseEntity<>(groupService.getGroupDetails(
                    groupId,
                    userId), HttpStatus.OK);
     }

     @GetMapping("/getGroups")
     public ResponseEntity<Object> getGroups(
               @RequestParam String userId,
               @RequestParam String groupName,
               @RequestParam int pageNo,
               @RequestParam int rowPerPage) throws StorageException, URISyntaxException {

          return new ResponseEntity<>(groupService.getGroups(
                    userId,
                    groupName,
                    pageNo,
                    rowPerPage,
                    false), HttpStatus.OK);
     }

     @GetMapping("/getGroupMembers")
     public ResponseEntity<Object> getGroupMember(
               @RequestParam String groupId,
               @RequestParam String searchValue,
               @RequestParam int pageNo,
               @RequestParam int rowPerPage) throws StorageException, URISyntaxException {

          return new ResponseEntity<>(groupService.getGroupMember(
                    groupId,
                    searchValue,
                    pageNo,
                    rowPerPage,
                    false), HttpStatus.OK);
     }

     @GetMapping("/getTotalUnreadMessageCountForCircles")
     public ResponseEntity<Object> getTotalUnreadMessageCountForCircles(
               @RequestParam String userId) {

          return new ResponseEntity<>(groupService.getTotalUnreadMessageCountForCircles(
                    userId), HttpStatus.OK);
     }
}
