package com.ws.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ws.chat.model.ChatRooms;
import com.ws.chat.repository.ChatRoomRepository;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

     @Mock
     private ChatRoomRepository chatRoomRepository;

     @InjectMocks
     private ChatRoomService chatRoomService;

     private String senderId = "661f674e4757cf5c0a0dea0c";
     private String recipientId = "661f674e4757cf5c0a0dea0d";
     private String chatId;

     @BeforeEach
     void setUp() {
          chatId = senderId + "_" + recipientId;
     }

     @Test
     void testGetChatRoomIdWhenRoomExists() {
          ChatRooms chatRooms = ChatRooms.builder()
                    .chatId(chatId)
                    .senderId(senderId)
                    .recipientId(recipientId)
                    .build();

          when(chatRoomRepository.findBySenderIdAndRecipientId(senderId, recipientId))
                    .thenReturn(Optional.of(chatRooms));

          Optional<String> result = chatRoomService.getChatRoomId(senderId, recipientId, false);
          assertTrue(result.isPresent());
          assertEquals(chatId, result.get());
     }

     @Test
     void testGetChatRoomIdWhenRoomDoesNotExistAndCreateNewRoomIfNotExistsIsFalse() {
          when(chatRoomRepository.findBySenderIdAndRecipientId(senderId, recipientId))
                    .thenReturn(Optional.empty());

          Optional<String> result = chatRoomService.getChatRoomId(senderId, recipientId, false);
          assertFalse(result.isPresent());
     }

     @Test
     void testGetChatRoomIdWhenRoomDoesNotExistAndCreateNewRoomIfNotExistsIsTrue() {
          when(chatRoomRepository.findBySenderIdAndRecipientId(senderId, recipientId))
                    .thenReturn(Optional.empty());

          Optional<String> result = chatRoomService.getChatRoomId(senderId, recipientId, true);
          assertTrue(result.isPresent());
          assertEquals(chatId, result.get());

          verify(chatRoomRepository, times(2)).save(any(ChatRooms.class));
     }

     @Test
     void testCreateChatId() {
          String result = chatRoomService.createChatId(senderId, recipientId);
          assertEquals(chatId, result);

          verify(chatRoomRepository, times(2)).save(any(ChatRooms.class));
          verify(chatRoomRepository).save(ChatRooms.builder()
                    .chatId(chatId)
                    .senderId(senderId)
                    .recipientId(recipientId)
                    .build());
          verify(chatRoomRepository).save(ChatRooms.builder()
                    .chatId(chatId)
                    .senderId(recipientId)
                    .recipientId(senderId)
                    .build());
     }
}
