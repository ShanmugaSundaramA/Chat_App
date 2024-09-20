package com.ws.chat.jwt;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JwtService {

     @Value("${spring.security.application.secretKey}")
     public String secretKey;

     @Value("${spring.security.application.jwtExpirationMs}")
     public int expirationMs;

     public String extractUsername(String token) {
          return extractClaim(token, Claims::getSubject);
     }

     public <R> R extractClaim(String token, Function<Claims, R> claimsResolver) {
          final Claims claims = extractAllClaims(token);
          return claimsResolver.apply(claims);
     }

     public String generateToken(UserDetails userDetails) {
          return generateToken(new HashMap<>(), userDetails);
     }

     public String generateToken(Map<String, Object> extractClaims, UserDetails userDetails) {
          return Jwts.builder()
                    .addClaims(extractClaims)
                    .setSubject(userDetails.getUsername())
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                    .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                    .compact();
     }

     public boolean isTokenValid(String token, UserDetails userDetails) {
          final String username = extractUsername(token);
          return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);

     }

     public boolean isTokenExpired(String token) {
          return extractExpiration(token).before(new Date());
     }

     public Date extractExpiration(String token) {
          return extractClaim(token, Claims::getExpiration);
     }

     public Claims extractAllClaims(String token) {
          return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
     }

     public Key getSignInKey() {
          byte[] keyBytes = Decoders.BASE64.decode(secretKey);
          return Keys.hmacShaKeyFor(keyBytes);
     }

     public String validateJwtToken(String authToken) {
          try {
               Jwts.parserBuilder()
                         .setSigningKey(getSignInKey())
                         .build()
                         .parseClaimsJws(authToken);
               return "valid";
          } catch (MalformedJwtException e) {
               return "Invalid JWT token: " + e.getMessage();
          } catch (ExpiredJwtException e) {
               deleteJwtTokenFromLocalStorage();
               return "JWT token is expired: " + e.getMessage();
          } catch (UnsupportedJwtException e) {
               return "JWT token is unsupported: " + e.getMessage();
          } catch (IllegalArgumentException e) {
               return "JWT claims string is empty: " + e.getMessage();
          } catch (Exception e) {
               return e.getMessage();
          }
     }

     public void deleteJwtTokenFromLocalStorage() {
          try {
               java.util.prefs.Preferences.userRoot().remove("jwtToken");
          } catch (Exception e) {
               log.error(e.getMessage());
          }
     }
}
