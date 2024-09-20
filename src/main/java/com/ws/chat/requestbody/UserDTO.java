package com.ws.chat.requestbody;

import org.springframework.web.multipart.MultipartFile;

import com.google.auto.value.AutoValue.Builder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {

     private String userId;
     private MultipartFile profilePicture;
}
