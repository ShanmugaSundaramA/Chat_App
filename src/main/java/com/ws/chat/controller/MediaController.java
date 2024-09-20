package com.ws.chat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ws.chat.requestbody.MediaDTO;
import com.ws.chat.service.MediaService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
@CrossOrigin(origins = "*")
public class MediaController {

     private final MediaService mediaService;

     @PostMapping("/post")
     public ResponseEntity<Object> postMedia(@RequestBody MediaDTO mediaDTO) {

          return new ResponseEntity<>(mediaService.saveMedia(mediaDTO), HttpStatus.OK);
     }
}
