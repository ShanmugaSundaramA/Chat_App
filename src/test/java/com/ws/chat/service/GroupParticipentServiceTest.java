package com.ws.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.client.result.UpdateResult;
import com.ws.chat.model.GroupParticipants;
import com.ws.chat.model.Type;
import com.ws.chat.repository.GroupParticipantsRepo;

@ExtendWith(MockitoExtension.class)
class GroupParticipentServiceTest {

     @Mock
     private GroupParticipantsRepo groupParticipantsRepo;
     @Mock
     MongoTemplate mongoTemplate;
     @Mock
     UpdateResult updateResult;

     @InjectMocks
     private GroupParticipentService groupParticipentService;

     String groupId = "661f7e401a7cef5423668645";
     String userId = "661f674e4757cf5c0a0dea0c";

     @Test
     void saveInboxParticipantsTest() {

          GroupParticipants groupParticipants = getGroupParticipants();

          when(groupParticipantsRepo.save(groupParticipants)).thenReturn(groupParticipants);

          GroupParticipants result = groupParticipentService.saveGroupParticipants(groupParticipants);
          assertEquals(groupParticipants, result);
          verify(groupParticipantsRepo, times(1)).save(groupParticipants);
     }

     // @Test
     // void findByChatIdFoundTest() {

     // GroupParticipants groupParticipants = getGroupParticipants();

     // when(groupParticipantsRepo.findByGroupId(new
     // ObjectId(groupId))).thenReturn(Optional.of(groupParticipants));
     // GroupParticipants result = groupParticipentService.findByGroupId(groupId);
     // assertEquals(groupParticipants, result);
     // verify(groupParticipantsRepo, times(1)).findByGroupId(new ObjectId(groupId));
     // }

     // @Test
     // void findByChatIdNotFoundTest() {

     // when(groupParticipantsRepo.findByGroupId(new
     // ObjectId(groupId))).thenReturn(Optional.empty());
     // GroupParticipants result = groupParticipentService.findByGroupId(groupId);
     // assertNotNull(result);
     // verify(groupParticipantsRepo, times(1)).findByGroupId(new ObjectId(groupId));
     // }

     @Test
     void testDeleteByChatId() {

          when(updateResult.getModifiedCount()).thenReturn(1L);
          when(mongoTemplate.updateFirst(
                    any(Query.class),
                    any(Update.class),
                    eq(GroupParticipants.class))).thenReturn(updateResult);

          Long result = groupParticipentService.deleteByGroupId(userId, groupId);
          assertEquals(1L, result);
     }

     GroupParticipants getGroupParticipants() {
          return GroupParticipants.builder()
                    .id("6626360f51d3962995d4f3b1")
                    .groupId(new ObjectId(groupId))
                    .senderId(new ObjectId("661f674e4757cf5c0a0dea0c"))
                    .type(Type.IMAGE)
                    .lastMessageId("1715249320016-919855")
                    .isRead(false)
                    .build();
     }

}
