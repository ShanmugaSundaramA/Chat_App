package com.ws.chat.securityconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ws.chat.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApplicationConfig {

     private final UserRepository userRepository;

     @Bean
     UserDetailsService userDetailsService() {
          return username -> {
               try {
                    return userRepository.findByEmail(username).orElseThrow(() -> new Exception("Invalid Email"));
               } catch (Exception e) {
                    log.info(e.getMessage());
                    return null;
               }
          };
     }

     @Bean
     AuthenticationProvider authenticationProvider() {
          DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
          authenticationProvider.setUserDetailsService(userDetailsService());
          authenticationProvider.setPasswordEncoder(passwordEncoder());
          return authenticationProvider;
     }

     @Bean
     AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
          return config.getAuthenticationManager();
     }

     @Bean
     PasswordEncoder passwordEncoder() {
          return new BCryptPasswordEncoder();
     }

}
