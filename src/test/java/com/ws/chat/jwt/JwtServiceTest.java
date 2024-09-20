
package com.ws.chat.jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

     @Mock
     private UserDetails userDetails;

     @InjectMocks
     private JwtService jwtService;

     private String secretKey = "mySecretKey12345678901234567890123456789012";
     private int expirationMs = 3600000;

     @BeforeEach
     void setUp() {
          ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
          ReflectionTestUtils.setField(jwtService, "expirationMs", expirationMs);
          when(userDetails.getUsername()).thenReturn("testuser");
     }

     @Test
     void testGenerateToken() {
          String token = jwtService.generateToken(userDetails);
          assertNotNull(token);
     }

     @Test
     void testExtractUsername() {
          String token = jwtService.generateToken(userDetails);
          String username = jwtService.extractUsername(token);
          assertEquals("testuser", username);
     }

     @Test
     void testExtractClaim() {
          String token = jwtService.generateToken(userDetails);
          Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);
          assertNotNull(issuedAt);
     }

     @Test
     void testIsTokenValid() {
          String token = jwtService.generateToken(userDetails);
          assertTrue(jwtService.isTokenValid(token, userDetails));
     }
}
