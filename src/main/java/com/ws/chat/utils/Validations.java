package com.ws.chat.utils;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Validations {

     public boolean isValidImage(MultipartFile file) {
          return file.getContentType() != null &&
                    file.getContentType().startsWith("image") &&
                    file.getSize() > 0;
     }
}
