package com.ws.chat.jwt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

     private final JwtService jwtService;
     private final UserDetailsService userDetailsService;

     @Override
     protected void doFilterInternal(
               @NonNull HttpServletRequest request,
               @NonNull HttpServletResponse response,
               @NonNull FilterChain filterChain) throws IOException, ServletException {

          final String authHeader = request.getHeader("Authorization");
          final String jwt;
          final String userEmail;

          if (authHeader == null || !authHeader.startsWith("Bearer ")) {
               filterChain.doFilter(request, response);
               return;
          }
          jwt = authHeader.substring(7);
          String validationResult = jwtService.validateJwtToken(jwt);

          if (validationResult.equals("valid")) {
               userEmail = jwtService.extractUsername(jwt);
               if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                         
                         UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                   userDetails,
                                   null,
                                   userDetails.getAuthorities());
                         authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                         SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
               }
               filterChain.doFilter(request, response);

          } else {

               Map<String, Object> customErrorResponse = new HashMap<>();
               customErrorResponse.put("statusCode", 401);
               customErrorResponse.put("status", "Unauthorized");
               customErrorResponse.put("data", validationResult);

               String errorResponseJson = new ObjectMapper().writeValueAsString(customErrorResponse);

               response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
               response.setContentType("application/json");
               response.getWriter().write(errorResponseJson);
          }

     }

}
