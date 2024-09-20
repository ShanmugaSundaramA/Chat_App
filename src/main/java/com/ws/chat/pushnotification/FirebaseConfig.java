package com.ws.chat.pushnotification;

import java.io.IOException;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
public class FirebaseConfig {

     @Bean
     FirebaseMessaging firebaseMessaging() throws IOException {

          GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(new ClassPathResource("trove-firebase.json").getInputStream());

          FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                    .setCredentials(googleCredentials)
                    .build();

          String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
          String appName = "Trove mobile " + randomSuffix;

          FirebaseApp app = FirebaseApp.initializeApp(firebaseOptions, appName);
          return FirebaseMessaging.getInstance(app);
     }

}
