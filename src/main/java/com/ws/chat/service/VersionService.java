package com.ws.chat.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ws.chat.model.Version;
import com.ws.chat.repository.VersionRepository;
import com.ws.chat.requestbody.VersionDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VersionService {

     private final VersionRepository versionRepository;

     private static final String SUCCESS = "success";

     public Object saveVersion(VersionDTO versionDTO) {

          String id = "65583cfd9ab11825ae0f3f92";
          Optional<Version> versionOptional = versionRepository.findById(id);

          Version version;
          if (versionOptional.isPresent()) {
               version = versionOptional.get();
          } else {
               version = new Version();
               version.setId(id);
          }
          updateVersionFields(version, versionDTO);
          versionRepository.save(version);

          return ResponseService.successResponse(200, SUCCESS, version);
     }

     private void updateVersionFields(
               Version version,
               VersionDTO versionDTO) {

          version.setTitle(versionDTO.getTitle());
          version.setAppName(versionDTO.getAppName());
          version.setDescription(versionDTO.getDescription());
          version.setAndroidVersion(versionDTO.getAndroidVersion());
          version.setAndroidAppLink(versionDTO.getAndroidAppLink());
          version.setIosVersion(versionDTO.getIosVersion());
          version.setIosAppLink(versionDTO.getIosAppLink());
          version.setUpdate(versionDTO.isUpdate());
          version.setMandatoryUpdate(versionDTO.isMandatoryUpdate());
     }

     public Object getVersion() {

          String id = "65583cfd9ab11825ae0f3f92";
          Optional<Version> versionOptional = versionRepository.findById(id);
          Version version;
          if (versionOptional.isPresent()) {
               version = versionOptional.get();
          } else {
               version = new Version();
          }
          return ResponseService.successResponse(200, SUCCESS, version);
     }

     /* For All Mobile Apps .Not only for Trove. */
     public Object saveVersionForAllApps(VersionDTO versionDTO) {

          Optional<Version> versionOptional = versionRepository.findByAppName(versionDTO.getAppName());
          Version version;
          if (versionOptional.isPresent()) {
               version = versionOptional.get();
          } else {
               version = new Version();
          }
          updateVersionFields(version, versionDTO);
          return ResponseService.successResponse(200, SUCCESS, versionRepository.save(version));
     }

     public Object getVersionForAllApps(String appName) {
          Optional<Version> versionOptional = versionRepository.findByAppName(appName);
          Version version;
          if (versionOptional.isPresent()) {
               version = versionOptional.get();
          } else {
               version = new Version();
          }
          return ResponseService.successResponse(200, SUCCESS, version);
     }

}
