package com.example.Ece;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PreVisitReportSummaryControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void generatesTraceableFallbackSummaryAndReusesIt() throws Exception {
        String awarenessResponse = mockMvc.perform(post("/awarenessRecords/fromPrediction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"summary-test\",\"sourceType\":\"video\","
                                + "\"emotionLabel\":\"sad\",\"keepRecord\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();
        int awarenessId = objectMapper.readTree(awarenessResponse).path("data").path("id").asInt();

        String sessionId = "summary-" + UUID.randomUUID();
        String analysisPayload = "{"
                + "\"sessionId\":\"" + sessionId + "\","
                + "\"awarenessRecordId\":" + awarenessId + ","
                + "\"username\":\"summary-test\","
                + "\"sourceType\":\"video\",\"analysisMode\":\"combined\","
                + "\"keepRecord\":true,\"keepMedia\":false,"
                + "\"complaint\":\"最近在考试前容易紧张\","
                + "\"emotionResults\":[{\"emotionType\":\"sad\",\"frameCount\":8,"
                + "\"averageConfidence\":0.72,\"maxConfidence\":0.88}],"
                + "\"bfrbEvents\":[{\"eventKey\":\"event-1\",\"behaviorCode\":\"nail_biting\","
                + "\"cueType\":\"咬指甲\",\"startSeconds\":1,\"endSeconds\":3,"
                + "\"durationSeconds\":2,\"averageConfidence\":0.75,\"maxConfidence\":0.91,"
                + "\"keyFrameSeconds\":2,\"evidenceType\":\"behavior-model+hand-face-geometry\"}]}";
        String analysisResponse = mockMvc.perform(post("/analysisRecords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(analysisPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();
        JsonNode analysisData = objectMapper.readTree(analysisResponse).path("data");
        int analysisId = analysisData.path("record").path("id").asInt();

        mockMvc.perform(post("/preVisitReportSummaries/generate/{id}", awarenessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.provider").value("local-fallback"))
                .andExpect(jsonPath("$.data.modelName").value("rule-summary-v1"))
                .andExpect(jsonPath("$.data.promptVersion").value("previsit-summary-v1"))
                .andExpect(jsonPath("$.data.cached").value(false))
                .andExpect(jsonPath("$.data.clinicianQuestions.length()").value(4))
                .andExpect(jsonPath("$.data.visitPreparation.length()").value(4));

        mockMvc.perform(post("/preVisitReportSummaries/generate/{id}", awarenessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached").value(true));

        mockMvc.perform(get("/preVisitReportSummaries/by-awareness/{id}", awarenessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objectiveSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.safetyNote").isNotEmpty());

        mockMvc.perform(delete("/analysisRecords/{id}", analysisId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/preVisitReportSummaries/by-awareness/{id}", awarenessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"));
    }
}
