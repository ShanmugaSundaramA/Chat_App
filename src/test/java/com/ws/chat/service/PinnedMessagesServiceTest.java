package com.ws.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.ws.chat.exception.NotFound;
import com.ws.chat.model.Message;
import com.ws.chat.model.PinnedMessages;
import com.ws.chat.model.Type;
import com.ws.chat.repository.PinnedMessagesRepository;
import com.ws.chat.requestbody.PinnedMessageDTO;
import com.ws.chat.responsebody.ResponseDTO;

@ExtendWith(MockitoExtension.class)
class PinnedMessagesServiceTest {

     @Mock
     PinnedMessagesRepository pinnedMessagesRepository;
     @Mock
     MongoTemplate mongoTemplate;
     @Mock
     private ChatRoomService chatRoomService;
     @InjectMocks
     PinnedMessagesService pinnedMessagesService;

     private static final String userId = "6603d703294632cc703d47d9";
     private static final String recipientId = "6603d703294632cc703d47d9";
     private static final String groupId = "6603d703294632cc703d47d9";

     @Test
     void savePinnedMessageIfIdNullTest() {

          PinnedMessages pinnedMessages = getPinnedMessages();
          pinnedMessages.setId(null);

          PinnedMessageDTO pinnedMessageDTO = new PinnedMessageDTO();
          BeanUtils.copyProperties(pinnedMessages, pinnedMessageDTO);
          pinnedMessageDTO.setUserId(userId);
          pinnedMessageDTO.setRecipientId(recipientId);
          when(chatRoomService.getChatRoomId(userId, recipientId, true))
                    .thenReturn(Optional.of(userId + "_" + recipientId));
          when(pinnedMessagesRepository.save(pinnedMessages)).thenReturn(pinnedMessages);
          ResponseDTO result = (ResponseDTO) pinnedMessagesService.savePinnedMessage(pinnedMessageDTO);
          assertEquals(200, result.getStatusCode());
          assertEquals("success", result.getStatus());
          assertEquals(pinnedMessages, result.getData());
     }

     @Test
     void savePinnedMessageIfIdNotNullTest() {
          PinnedMessages pinnedMessages = getPinnedMessages();

          PinnedMessageDTO pinnedMessageDTO = new PinnedMessageDTO();
          BeanUtils.copyProperties(pinnedMessages, pinnedMessageDTO);
          pinnedMessageDTO.setUserId(userId);
          pinnedMessageDTO.setRecipientId(recipientId);

          when(chatRoomService.getChatRoomId(userId, recipientId, true))
                    .thenReturn(Optional.of(userId + "_" + recipientId));
          when(pinnedMessagesRepository.findById(pinnedMessageDTO.getId())).thenReturn(Optional.of(pinnedMessages));
          when(pinnedMessagesRepository.save(pinnedMessages)).thenReturn(pinnedMessages);

          ResponseDTO result = (ResponseDTO) pinnedMessagesService.savePinnedMessage(pinnedMessageDTO);
          assertEquals(200, result.getStatusCode());
          assertEquals("success", result.getStatus());
          assertEquals(pinnedMessages, result.getData());
     }

     @SuppressWarnings("unchecked")
     @Test
     void getPinnedMessageTest() {

          List<PinnedMessages> pinnedMessagesList = Arrays.asList(getPinnedMessages(), getPinnedMessages());
          AggregationResults<PinnedMessages> aggregationResults = mock(AggregationResults.class);

          when(aggregationResults.getMappedResults()).thenReturn(pinnedMessagesList);
          when(mongoTemplate.aggregate(
                    any(Aggregation.class),
                    eq("pinnedMessages"),
                    eq(PinnedMessages.class))).thenReturn(aggregationResults);
          ResponseDTO result = (ResponseDTO) pinnedMessagesService.getPinnedMessage(userId, recipientId, groupId);
          assertEquals(200, result.getStatusCode());
          assertEquals("success", result.getStatus());
          assertEquals(pinnedMessagesList, result.getData());
     }

     @Test
     void deleteByIdTest() {
          String pinnedMessageId = "1715577263858-904491";

          doNothing().when(pinnedMessagesRepository).deleteById(pinnedMessageId);
          ResponseDTO result = (ResponseDTO) pinnedMessagesService.deleteById(pinnedMessageId);
          assertEquals(200, result.getStatusCode());
          assertEquals("success", result.getStatus());
          assertEquals("Deleted successfully", result.getData());
     }

     @Test
     void deleteByMessageIdTest() {
          String pinnedMessageId = "1715577263858-904491";

          doNothing().when(pinnedMessagesRepository).deleteByMessageId(pinnedMessageId);
          pinnedMessagesService.deleteByMessageId(pinnedMessageId);
          verify(pinnedMessagesRepository, times(1)).deleteByMessageId(pinnedMessageId);
     }

     @Test
     void handleExceptionIfIdNotExist() {
          String pinnedMessageId = "1715577263858-904491";

          when(pinnedMessagesRepository.findById(pinnedMessageId))
                    .thenReturn(Optional.empty());
          assertThrows(NotFound.class, () -> pinnedMessagesService.findById(pinnedMessageId));
          verify(pinnedMessagesRepository, times(1)).findById(pinnedMessageId);
     }

     private Message getMessage() {
          return Message.builder()
                    .id("1715577263858-904491")
                    .content("sundar")
                    .caption("caption")
                    .type(Type.TEXT)
                    .size(10L)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .deliveredAt(new Date())
                    .sendAt(new Date())
                    .build();
     }

     private PinnedMessages getPinnedMessages() {
          return PinnedMessages.builder()
                    .id("1111-222-3333")
                    .chatId(userId + "_" + recipientId)
                    .messageId("1713780240727-571149")
                    .message(getMessage())
                    .build();
     }

}
