package com.ws.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
@Slf4j
public class EncryptionService {

     @Value("${encryption.key}")
     private String encryptionKey;

     @Value("${encryption.cipherMode}")
     private String cipherMode;

     public String decrypt(String encryptedValue) {

          try {
               Cipher cipher = Cipher.getInstance(cipherMode);
               SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");

               byte[] encryptedData = Base64.getDecoder().decode(encryptedValue);

               cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
               byte[] decryptedBytes = cipher.doFinal(encryptedData);
               return new String(decryptedBytes);

          } catch (Exception e) {
               log.info("error : " + e.getMessage());
               return "";
          }
     }
}
