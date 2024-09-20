package com.ws.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.result.UpdateResult;
import com.ws.chat.model.InboxParticipants;
import com.ws.chat.repository.InboxParticipantsRepository;

@ExtendWith(MockitoExtension.class)
class InboxParticipantsServiceTest {

    @Mock
    private InboxParticipantsRepository inboxParticipantsRepository;
    @Mock
    MongoTemplate mongoTemplate;
    @Mock
    UpdateResult updateResult;
    @InjectMocks
    private InboxParticipantsService inboxParticipantsService;

    String chatId = "661f7e401a7cef5423668645_661f674e4757cf5c0a0dea0c";
    String senderId = "661f674e4757cf5c0a0dea0c";

    @Test
    void testSaveInboxParticipants() {

        InboxParticipants inboxParticipant = getInboxParticipants();
        List<InboxParticipants> inboxParticipants = new ArrayList<>();
        inboxParticipants.add(inboxParticipant);

        when(inboxParticipantsRepository.saveAll(inboxParticipants)).thenReturn(inboxParticipants);
        List<InboxParticipants> result = inboxParticipantsService.saveInboxParticipants(inboxParticipants);
        assertEquals(inboxParticipants, result);
        verify(inboxParticipantsRepository, times(1)).saveAll(inboxParticipants);
    }

    @Test
    void findByChatIdFoundTest() {

        InboxParticipants inboxParticipant = getInboxParticipants();
        List<InboxParticipants> inboxParticipants = new ArrayList<>();
        inboxParticipants.add(inboxParticipant);

        when(inboxParticipantsRepository.findByChatId(chatId)).thenReturn(inboxParticipants);
        List<InboxParticipants> result = inboxParticipantsService.findByChatId(chatId);
        assertEquals(inboxParticipants, result);
        verify(inboxParticipantsRepository, times(1)).findByChatId(chatId);
    }

    @Test
    void testDeleteByChatId() {

        inboxParticipantsService.deleteByChatId(senderId, chatId);

        verify(inboxParticipantsRepository, times(1)).deleteByChatIdAndSenderId(
                eq(chatId),
                eq(new ObjectId(senderId)));

    }

    InboxParticipants getInboxParticipants() {
        return InboxParticipants.builder()
                .id("6626360f51d3962995d4f3b1")
                .chatId(chatId)
                .senderId(new ObjectId("661f674e4757cf5c0a0dea0c")) // new Code ObjectId
                .recipientId(new ObjectId("661f674e4757cf5c0a0dea0b")) // new Code ObjectId
                .lastMessageId("1715249320016-919855")
                .isRead(false)
                .build();
    }
}
