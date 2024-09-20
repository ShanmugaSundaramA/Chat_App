package com.ws.chat.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.ws.chat.azureblob.AzureBlobAdapter;
import com.ws.chat.kafka.KafkaConsumer;
import com.ws.chat.requestbody.LinkAndNum;
import com.ws.chat.requestbody.MediaDTO;
import com.ws.chat.requestbody.MediaFile;
import com.ws.chat.websocket.WebSocketService;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

     @InjectMocks
     MediaService mediaService;
     @Mock
     AzureBlobAdapter azureBlobAdapter;
     @Mock
     MessageService messageService;
     @Mock
     KafkaConsumer consumer;
     @Mock
     WebSocketService webSocketService;

     @Test
     void saveMediaForRecipientTest() throws URISyntaxException {
          MediaDTO mediaDTO = getMediaDTO();
          URI uri = new URI("https://example.com/image.jpg");

          when(azureBlobAdapter.getBlobUri(any()))
                    .thenReturn(uri);
          when(messageService.saveMessage(any()))
                    .thenReturn("messageId");
          mediaService.saveMedia(mediaDTO);

          verify(azureBlobAdapter, times(1)).getBlobUri(any());
          verify(messageService, times(2)).saveMessage(any());
     }

     @Test
     void saveMediaForGroupTest() throws URISyntaxException {
          MediaDTO mediaDTO = getMediaDTO();
          mediaDTO.setGroupId(null);

          URI uri = new URI("https://example.com/image.jpg");

          when(azureBlobAdapter.getBlobUri(any()))
                    .thenReturn(uri);
          when(messageService.saveMessage(any()))
                    .thenReturn("messageId");
          mediaService.saveMedia(mediaDTO);

          verify(azureBlobAdapter, times(1)).getBlobUri(any());
          verify(messageService, times(2)).saveMessage(any());

     }

     private MediaDTO getMediaDTO() {
          List<MediaFile> images = new ArrayList<>();
          MediaFile mediaFile = new MediaFile();
          mediaFile.setId("111-222-333-444");
          mediaFile.setMedia(
                    new MockMultipartFile(
                              "demo.png",
                              "demo.png",
                              "image/png",
                              new byte[10]));
          mediaFile.setCaption("Hiiii");
          images.add(mediaFile);

          List<LinkAndNum> linkAndNums = new ArrayList<>();
          linkAndNums.add(
                    LinkAndNum.builder()
                              .id("111-222-333-445")
                              .media("sundar.com")
                              .build());

          return MediaDTO.builder()
                    .senderId("661f7e401a7cef5423668645")
                    .senderName("Sundar")
                    .senderProfilePicture("http://localhost:8080/profile.png")
                    .recipientId("661f7e401a7cef5423668645")
                    .recipientName("Jeeva")
                    .groupId("661f7e401a7cef5423668645")
                    .images(images)
                    .links(linkAndNums)
                    .sendAt(new Date())
                    .build();
     }
}
