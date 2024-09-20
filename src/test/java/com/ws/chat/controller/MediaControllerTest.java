package com.ws.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ws.chat.model.Message;
import com.ws.chat.model.Type;
import com.ws.chat.requestbody.MediaDTO;
import com.ws.chat.requestbody.MediaFile;
import com.ws.chat.requestbody.MessageRequestBody;
import com.ws.chat.responsebody.ResponseDTO;
import com.ws.chat.service.MediaService;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

     MockMvc mockMvc;

     @Mock
     MediaService mediaService;

     @InjectMocks
     MediaController mediaController;

     @BeforeEach
     void setup() {
          mockMvc = MockMvcBuilders
                    .standaloneSetup(mediaController)
                    .build();
     }

     @Test
     void postMediaTest() throws Exception {
          MediaDTO mediaDTO = getMediaDTO();
          MessageRequestBody messageRequestBody = getMessageBody();

          ArrayList<MessageRequestBody> list = new ArrayList<>();
          list.add(messageRequestBody);

          ResponseDTO responseDTO = ResponseDTO.builder()
                    .statusCode(200)
                    .status("success")
                    .data(list)
                    .build();

          when(mediaService.saveMedia(any(MediaDTO.class))).thenReturn(responseDTO);
          ObjectMapper objectMapper = new ObjectMapper();
          String content = objectMapper.writeValueAsString(mediaDTO);

          ResultActions result = mockMvc.perform(
                    multipart("/media/post")
                              .content(content)
                              .contentType(MediaType.APPLICATION_JSON)
                              .accept(MediaType.APPLICATION_JSON));
          result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data[0].senderId").exists())
                    .andExpect(jsonPath("$.data[0].senderName").exists())
                    .andExpect(jsonPath("$.data[0].recipientId").exists())
                    .andExpect(jsonPath("$.data[0].recipientName").exists())
                    .andExpect(jsonPath("$.data[0].online").exists())
                    .andExpect(jsonPath("$.data[0].forwardMessage").exists())
                    .andExpect(jsonPath("$.data[0].type").exists())
                    .andExpect(jsonPath("$.data[0].message.id").exists())
                    .andExpect(jsonPath("$.data[0].message.type").exists())
                    .andExpect(jsonPath("$.data[0].message.content").exists())
                    .andExpect(jsonPath("$.data[0].message.caption").exists())
                    .andExpect(jsonPath("$.data[0].message.sendAt").exists())
                    .andExpect(jsonPath("$.data[0].message.deliveredAt").exists())
                    .andExpect(jsonPath("$.data[0].message.createdAt").exists())
                    .andExpect(jsonPath("$.data[0].message.updatedAt").exists());
     }

     private MediaDTO getMediaDTO() {
          List<MediaFile> images = new ArrayList<>();
          MediaFile mediaFile = MediaFile.builder()
                    .id("1111-2222-333")
                    .media(new MockMultipartFile(
                              "demo.png",
                              "demo.png",
                              "image/png",
                              new byte[10]))
                    .caption("DEMO image")
                    .build();
          images.add(mediaFile);
          return MediaDTO.builder()
                    .senderId("661f7e401a7cef5423668645")
                    .senderName("Shanmuga Sundaram A")
                    .senderProfilePicture("http://localhost:8080/profile.png")
                    .recipientId("661f7e401a7cef5423668645")
                    .recipientName("Jeeva T")
                    .sendAt(new Date())
                    .type(Type.IMAGE)
                    .build();
     }

     private Message getMessage() {
          return Message.builder()
                    .id("1715577263858-904491")
                    .content("sundar")
                    .caption("caption")
                    .type(Type.IMAGE)
                    .size(10L)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .deliveredAt(new Date())
                    .sendAt(new Date())
                    .build();
     }

     private MessageRequestBody getMessageBody() {
          return MessageRequestBody.builder()
                    .action("edit")
                    .senderId("661f7e401a7cef5423668645")
                    .senderName("Shanmuga Sundaram A")
                    .recipientId("661f7e401a7cef5423668645")
                    .recipientName("Jeeva")
                    .type(Type.IMAGE)
                    .messageId("1715577263858-904491")
                    .message(getMessage())
                    .isOnline(true)
                    .isForwardMessage(false)
                    .build();
     }
}
