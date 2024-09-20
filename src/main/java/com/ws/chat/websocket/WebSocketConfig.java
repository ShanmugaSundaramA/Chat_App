package com.ws.chat.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

     private final WebSocketService webSocketService;

     @Override
     public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
          registry.addHandler(webSocketService, "/ws/{userId}/{deviceId}")
                    .setAllowedOrigins("*");
     }

}