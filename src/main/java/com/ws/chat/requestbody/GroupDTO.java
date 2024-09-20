package com.ws.chat.requestbody;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.ws.chat.model.Member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupDTO {

     private String id;
     private String groupName;
     private String colorCode;
     private String description;
     private MultipartFile profilePicture;
     private List<Member> members;
     private String createdBy;
}