package com.ws.chat.jwt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

     @Override
     public void commence(
               HttpServletRequest request,
               HttpServletResponse response,
               AuthenticationException authException)
               throws IOException, ServletException {

          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.setContentType("application/json");

          Map<String, Object> customErrorResponse = new HashMap<>();
          customErrorResponse.put("statusCode", 401);
          customErrorResponse.put("status", "Unauthorized");
          customErrorResponse.put("data", authException.getMessage());

          ObjectMapper objectMapper = new ObjectMapper();
          objectMapper.writeValue(response.getWriter(), customErrorResponse);
     }
}