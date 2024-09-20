package com.ws.chat.websocket;

import com.ws.chat.kafka.KafkaConsumer;
import com.ws.chat.repository.MessageRepository;
import com.ws.chat.repository.UserRepository;
import com.ws.chat.service.GroupService;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.socket.*;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class WebSocketServiceTest {

     @Mock
     GroupService groupService;
     @Mock
     KafkaConsumer consumer;
     @Mock
     MessageRepository messageRepository;
     @Mock
     MongoTemplate mongoTemplate;
     @Mock
     UserRepository userRepository;
     @Mock
     private TextMessage textMessage;
     @Mock
     private BinaryMessage binaryMessage;
     @InjectMocks
     WebSocketService webSocketService;
     WebSocketSession session;

     @BeforeEach
     void setUp() {

          session = mock(WebSocketSession.class);
          try {
               clearSessions();
          } catch (Exception e) {
               log.warn(e.getMessage());
          }
     }

     @SuppressWarnings("unchecked")
     Map<String, Map<String, WebSocketSession>> getSessionsField() throws NoSuchFieldException,
               IllegalAccessException {

          var field = WebSocketService.class.getDeclaredField("sessions");
          field.setAccessible(true);
          return (Map<String, Map<String, WebSocketSession>>) field.get(webSocketService);
     }

     void clearSessions() throws Exception {

          Map<String, Map<String, WebSocketSession>> sessions = getSessionsField();
          sessions.clear();
     }

     @Test
     void afterConnectionEstablished() throws Exception {

          when(session.getUri()).thenReturn(new URI("wss:trovechat.ckdigital.in/ws/669115f9c5176057331739a7/device123"));
          webSocketService.afterConnectionEstablished(session);
          Map<String, Map<String, WebSocketSession>> sessions = getSessionsField();

          assertTrue(sessions.containsKey("669115f9c5176057331739a7"));
          assertTrue(sessions.get("669115f9c5176057331739a7").containsKey("device123"));
     }

     @Test
     void createConnectionWithoutUserIdAndDeviceIdTest() throws Exception {

          when(session.getUri()).thenReturn(new URI("wss:trovechat.ckdigital.in/ws"));
          webSocketService.afterConnectionEstablished(session);
          verify(session, never()).sendMessage(any(TextMessage.class));
     }

     @Test
     void createConnectionWithoutDeviceIdTest() throws Exception {

          when(session.getUri()).thenReturn(new URI("wss:trovechat.ckdigital.in/ws/user123"));
          webSocketService.afterConnectionEstablished(session);
          verify(session, never()).sendMessage(any(TextMessage.class));
     }

     @Test
     void afterConnectionEstablishedWithAnotherDeviceId() throws Exception {

          when(session.getUri()).thenReturn(new URI("ws://localhost/ws/669115f9c5176057331739a7/device124"));
          Map<String, Map<String, WebSocketSession>> sessions = getSessionsField();
          WebSocketSession existingSession = mock(WebSocketSession.class);
          Map<String, WebSocketSession> existingDevices = new HashMap<>();
          existingDevices.put("device123", existingSession);
          sessions.put("669115f9c5176057331739a7", existingDevices);

          webSocketService.afterConnectionEstablished(session);

          assertTrue(sessions.containsKey("669115f9c5176057331739a7"));
          assertTrue(sessions.get("669115f9c5176057331739a7").containsKey("device123"));
          assertTrue(sessions.get("669115f9c5176057331739a7").containsKey("device124"));
     }

}
