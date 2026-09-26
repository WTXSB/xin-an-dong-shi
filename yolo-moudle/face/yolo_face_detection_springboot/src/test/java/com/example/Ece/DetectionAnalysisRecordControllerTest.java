package com.example.Ece;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DetectionAnalysisRecordControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void savesCompleteResultAndTreatsSessionIdAsIdempotencyKey() throws Exception {
        String sessionId = "test-" + UUID.randomUUID();
        String payload = "{"
                + "\"sessionId\":\"" + sessionId + "\","
                + "\"username\":\"demo\","
                + "\"sourceType\":\"video\","
                + "\"analysisMode\":\"combined\","
                + "\"keepRecord\":true,"
                + "\"keepMedia\":false,"
                + "\"inputMedia\":\"must-be-cleared.mp4\","
                + "\"complaint\":\"最近容易紧绷\","
                + "\"emotionResults\":[{"
                + "\"emotionType\":\"sad\",\"frameCount\":12,"
                + "\"averageConfidence\":0.62,\"maxConfidence\":0.88}],"
                + "\"bfrbEvents\":[{"
                + "\"eventKey\":\"bfrb-1\",\"behaviorCode\":\"nail_biting\","
                + "\"cueType\":\"咬指甲\",\"startSeconds\":1.2,\"endSeconds\":2.6,"
                + "\"durationSeconds\":1.5,\"sampleCount\":5,"
                + "\"averageConfidence\":0.71,\"maxConfidence\":0.91,"
                + "\"keyFrameSeconds\":1.8,"
                + "\"evidenceType\":\"behavior-model+hand-face-geometry\"}]}";

        mockMvc.perform(post("/analysisRecords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.saved").value(true))
                .andExpect(jsonPath("$.data.duplicate").value(false))
                .andExpect(jsonPath("$.data.record.inputMedia").value(""))
                .andExpect(jsonPath("$.data.record.bfrbEventCount").value(1))
                .andExpect(jsonPath("$.data.emotionResults.length()").value(1))
                .andExpect(jsonPath("$.data.bfrbEvents.length()").value(1));

        mockMvc.perform(post("/analysisRecords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(true))
                .andExpect(jsonPath("$.data.duplicate").value(true));

        mockMvc.perform(get("/analysisRecords/session/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.record.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.record.complaint").value("最近容易紧绷"))
                .andExpect(jsonPath("$.data.behaviorStats[0].count").value(1));
    }

    @Test
    void doesNotPersistWhenUserOptsOut() throws Exception {
        String sessionId = "privacy-" + UUID.randomUUID();
        String payload = "{"
                + "\"sessionId\":\"" + sessionId + "\","
                + "\"sourceType\":\"camera\","
                + "\"analysisMode\":\"combined\","
                + "\"keepRecord\":false}";

        mockMvc.perform(post("/analysisRecords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(false));

        mockMvc.perform(get("/analysisRecords/session/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"));
    }
}
