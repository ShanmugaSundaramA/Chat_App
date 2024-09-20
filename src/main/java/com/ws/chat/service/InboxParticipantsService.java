package com.ws.chat.service;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.ws.chat.model.InboxParticipants;
import com.ws.chat.repository.InboxParticipantsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InboxParticipantsService {

     private final InboxParticipantsRepository inboxParticipantsRepository;

     public List<InboxParticipants> saveInboxParticipants(
               List<InboxParticipants> inboxParticipants) {

          return inboxParticipantsRepository.saveAll(inboxParticipants);
     }

     public List<InboxParticipants> findByChatId(
               String chatId) {

          return inboxParticipantsRepository.findByChatId(chatId);
     }

     public void deleteByChatId(
               String senderId,
               String chatId) {

          inboxParticipantsRepository.deleteByChatIdAndSenderId(
                    chatId,
                    new ObjectId(senderId));
     }
}