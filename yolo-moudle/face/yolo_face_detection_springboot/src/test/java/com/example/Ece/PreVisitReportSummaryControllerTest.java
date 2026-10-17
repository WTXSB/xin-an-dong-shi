package com.example.Ece;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.Ece.controller.PreVisitReportSummaryController;
import com.example.Ece.entity.PreVisitReportSummary;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void cachedGeneratedCopyUsesSupportLanguageWithoutRewritingOriginals() {
        PreVisitReportSummary summary = new PreVisitReportSummary();
        summary.setPromptVersion("previsit-summary-v1");
        summary.setObjectiveSummary("预诊报告用于医患联动，患者可向医生说明主诉。");
        summary.setClinicianQuestionsJson("[\"患者视角与医生视角如何交流？\"]");
        summary.setVisitPreparationJson("[\"就诊时向医生提供预诊参考报告。\"]");
        summary.setSafetyNote("仅供参考，不构成医学诊断或治疗建议。");

        Map<String, Object> response = ReflectionTestUtils.invokeMethod(
                new PreVisitReportSummaryController(), "toResponse", summary, true, Collections.emptyList());

        assertEquals("心理感知报告用于协同支持，用户可向服务者说明自述。", response.get("objectiveSummary"));
        assertEquals(Collections.singletonList("用户视角与服务者视角如何交流？"), response.get("clinicianQuestions"));
        assertEquals(Collections.singletonList("交流时向服务者提供心理感知报告。"), response.get("visitPreparation"));
        assertEquals(summary.getSafetyNote(), response.get("safetyNote"));
        assertEquals("previsit-summary-v1", response.get("promptVersion"));
        assertEquals(true, response.get("cached"));
        assertEquals("预诊报告用于医患联动，患者可向医生说明主诉。", summary.getObjectiveSummary());
        assertEquals("[\"就诊时向医生提供预诊参考报告。\"]", summary.getVisitPreparationJson());
    }

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
                .andExpect(jsonPath("$.data.promptVersion").value("perception-summary-v2"))
                .andExpect(jsonPath("$.data.cached").value(false))
                .andExpect(jsonPath("$.data.clinicianQuestions.length()").value(4))
                .andExpect(jsonPath("$.data.visitPreparation.length()").value(4))
                .andExpect(jsonPath("$.data.visitPreparation[3]").value("交流时主动说明本报告来自算法辅助整理，可与服务者结合实际经历进一步核实。"));

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
