package com.ws.chat.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ws.chat.model.ChatRooms;
import com.ws.chat.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

     private final ChatRoomRepository chatRoomRepository;

     public Optional<String> getChatRoomId(String senderId, String recipientId, boolean createNewRoomIfNotExists) {
          return chatRoomRepository.findBySenderIdAndRecipientId(senderId, recipientId)
                    .map(ChatRooms::getChatId)
                    .or(() -> createNewRoomIfNotExists ? Optional.of(createChatId(senderId, recipientId))
                              : Optional.empty());
     }

     public String createChatId(String senderId, String recipientId) {
          String chatId = String.format("%s_%s", senderId, recipientId);
          ChatRooms senderRecipient = ChatRooms.builder()
                    .chatId(chatId)
                    .senderId(senderId)
                    .recipientId(recipientId)
                    .build();
          ChatRooms recipientSender = ChatRooms.builder()
                    .chatId(chatId)
                    .senderId(recipientId)
                    .recipientId(senderId)
                    .build();
          chatRoomRepository.save(senderRecipient);
          chatRoomRepository.save(recipientSender);
          return chatId;
     }
}
