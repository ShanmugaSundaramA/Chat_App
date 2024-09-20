package com.ws.chat.controller;

import org.springframework.web.bind.annotation.RestController;

import com.ws.chat.requestbody.VersionDTO;
import com.ws.chat.service.VersionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/version")
public class VersionControler {

     private final VersionService versionService;

     @PostMapping("/saveVersion")
     public ResponseEntity<Object> saveVersion(@RequestBody VersionDTO versionDTO) {
          return new ResponseEntity<>(versionService.saveVersion(versionDTO), HttpStatus.OK);
     }

     @GetMapping("/getVersion")
     public ResponseEntity<Object> getVersion() {
          return new ResponseEntity<>(versionService.getVersion(), HttpStatus.OK);
     }

     /* For All Mobile Apps .Not only for Trove. */
     @PostMapping("/saveVersionForAllApps")
     public ResponseEntity<Object> saveVersionForAllApps(
               @RequestBody VersionDTO versionDTO) {

          return new ResponseEntity<>(versionService.saveVersionForAllApps(versionDTO), HttpStatus.OK);
     }

     @PostMapping("/getVersionForAllApps")
     public ResponseEntity<Object> getVersionForAllApps(
               @RequestBody VersionDTO versionDTO) {

          return new ResponseEntity<>(versionService.getVersionForAllApps(versionDTO.getAppName()), HttpStatus.OK);
     }

}
