package com.example.Ece;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiChatControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void rejectsEmptyMessageWhenAttachmentWasNotApproved() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\",\"saveConversation\":false,\"attachments\":[{"
                                + "\"name\":\"private.txt\",\"mimeType\":\"text/plain\"," 
                                + "\"textContent\":\"private content\",\"approved\":false}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("-1"));
    }

    @Test
    void acceptsOnlyExplicitlyApprovedTextAttachment() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"请帮我梳理资料\",\"saveConversation\":false,\"attachments\":["
                                + "{\"name\":\"approved.txt\",\"mimeType\":\"text/plain\"," 
                                + "\"textContent\":\"最近容易紧张\",\"approved\":true},"
                                + "{\"name\":\"private.txt\",\"mimeType\":\"text/plain\"," 
                                + "\"textContent\":\"不要发送\",\"approved\":false}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.provider").value("local-fallback"))
                .andExpect(jsonPath("$.data.attachmentCount").value(1))
                .andExpect(jsonPath("$.data.privacy").isNotEmpty());
    }

    @Test
    void acceptsExplicitlyApprovedImageAttachment() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"请描述图片\",\"saveConversation\":false,\"attachments\":[{"
                                + "\"name\":\"mood.png\",\"mimeType\":\"image/png\","
                                + "\"textContent\":\"\",\"images\":[\"data:image/png;base64,iVBORw0KGgo=\"],"
                                + "\"approved\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.provider").value("local-fallback"))
                .andExpect(jsonPath("$.data.attachmentCount").value(1));
    }

    @Test
    void rejectsUnsupportedImagePayloadWhenThereIsNoMessage() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\",\"saveConversation\":false,\"attachments\":[{"
                                + "\"name\":\"unsafe.svg\",\"mimeType\":\"image/svg+xml\","
                                + "\"images\":[\"data:image/svg+xml;base64,PHN2Zz4=\"],\"approved\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("-1"));
    }

    @Test
    void rejectsApprovedAttachmentWithoutReadableContent() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"普通消息\",\"saveConversation\":false,\"attachments\":[{"
                                + "\"name\":\"empty.txt\",\"mimeType\":\"text/plain\","
                                + "\"textContent\":\"\",\"images\":[],\"approved\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("-1"))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("没有可发送")));
    }
}
