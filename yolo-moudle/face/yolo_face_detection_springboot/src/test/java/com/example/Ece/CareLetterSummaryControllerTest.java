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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CareLetterSummaryControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void generatesFallbackCareLetterAndReusesIt() throws Exception {
        int awarenessId = createAwareness("letter-test", "sad");
        createAnalysis(awarenessId, "最近在考试前容易紧张",
                "\"emotionResults\":[{\"emotionType\":\"sad\",\"frameCount\":8,"
                        + "\"averageConfidence\":0.72,\"maxConfidence\":0.88}],"
                        + "\"bfrbEvents\":[{\"eventKey\":\"event-1\",\"behaviorCode\":\"nail_biting\","
                        + "\"cueType\":\"咬指甲\",\"startSeconds\":1,\"endSeconds\":3,"
                        + "\"durationSeconds\":2,\"averageConfidence\":0.75,\"maxConfidence\":0.91,"
                        + "\"keyFrameSeconds\":2,\"evidenceType\":\"behavior-model+hand-face-geometry\"}]");

        String letterResponse = mockMvc.perform(post("/preVisitReportSummaries/care-letter/generate/{id}", awarenessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.provider").value("local-fallback"))
                .andExpect(jsonPath("$.data.modelName").value("rule-letter-v1"))
                .andExpect(jsonPath("$.data.promptVersion").value("care-letter-v1"))
                .andExpect(jsonPath("$.data.cached").value(false))
                .andExpect(jsonPath("$.data.letterText").isNotEmpty())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String letterText = objectMapper.readTree(letterResponse).path("data").path("letterText").asText();
        assertTrue(letterText.contains("这封信基于你主动分享的记录写成，只是陪伴与参考，不是任何结论。"));
        assertTrue(letterText.contains("心安动识"));
        // 本地书信模板不得包含任何医学用词
        List<String> prohibited = Arrays.asList(
                "检测结果", "诊断", "症状", "证明", "确诊", "疾病", "障碍", "治疗", "用药", "处方",
                "病理", "临床", "患者", "病人", "轻度", "中度", "重度"
        );
        for (String word : prohibited) {
            assertFalse(letterText.contains(word), "书信不应包含禁词：" + word);
        }

        // 重复生成走缓存
        mockMvc.perform(post("/preVisitReportSummaries/care-letter/generate/{id}", awarenessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached").value(true));

        // 已生成后可以直接读取
        mockMvc.perform(get("/preVisitReportSummaries/care-letter/{id}", awarenessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.letterText").isNotEmpty())
                .andExpect(jsonPath("$.data.cached").value(true));
    }

    @Test
    void careLetterReturns404BeforeGeneration() throws Exception {
        int awarenessId = createAwareness("letter-404-test", "happy");
        mockMvc.perform(get("/preVisitReportSummaries/care-letter/{id}", awarenessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void safetyFlagsCoverAllRulesAndStayEmptyForNormalData() throws Exception {
        // 规则一：愤怒类帧占比≥50%且平均置信度≥0.6
        int angryAwareness = createAwareness("flag-angry-test", "angry");
        createAnalysis(angryAwareness, "最近容易发火",
                "\"emotionResults\":[{\"emotionType\":\"angry\",\"frameCount\":6,"
                        + "\"averageConfidence\":0.72,\"maxConfidence\":0.9},"
                        + "{\"emotionType\":\"sad\",\"frameCount\":3,"
                        + "\"averageConfidence\":0.65,\"maxConfidence\":0.8}],"
                        + "\"bfrbEvents\":[]");
        mockMvc.perform(post("/preVisitReportSummaries/generate/{id}", angryAwareness))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.safetyFlags.length()").value(1))
                .andExpect(jsonPath("$.data.safetyFlags[0].level").value("attention"))
                .andExpect(jsonPath("$.data.safetyFlags[0].reason").value(
                        org.hamcrest.Matchers.containsString("愤怒类情绪线索占比较高")))
                .andExpect(jsonPath("$.data.safetyFlagsNote").isNotEmpty());

        // 规则二+三：累计时长≥120秒且存在单次≥60秒的事件
        int longAwareness = createAwareness("flag-long-test", "sad");
        createAnalysis(longAwareness, "",
                "\"emotionResults\":[{\"emotionType\":\"sad\",\"frameCount\":5,"
                        + "\"averageConfidence\":0.6,\"maxConfidence\":0.7}],"
                        + "\"bfrbEvents\":[{\"eventKey\":\"event-long\",\"behaviorCode\":\"nail_biting\","
                        + "\"cueType\":\"咬指甲\",\"startSeconds\":0,\"endSeconds\":130,"
                        + "\"durationSeconds\":130,\"averageConfidence\":0.8,\"maxConfidence\":0.9,"
                        + "\"keyFrameSeconds\":60,\"evidenceType\":\"behavior-model\"}]");
        String longResponse = mockMvc.perform(post("/preVisitReportSummaries/generate/{id}", longAwareness))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.safetyFlags.length()").value(2))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode longFlags = objectMapper.readTree(longResponse).path("data").path("safetyFlags");
        assertTrue(longFlags.toString().contains("重复性身体行为线索较频繁"));
        assertTrue(longFlags.toString().contains("单次持续时间较长"));

        // 规则四：自述命中优先关注关键词
        int priorityAwareness = createAwareness("flag-priority-test", "sad");
        createAnalysis(priorityAwareness, "压力大的时候会有想死的念头",
                "\"emotionResults\":[{\"emotionType\":\"sad\",\"frameCount\":5,"
                        + "\"averageConfidence\":0.6,\"maxConfidence\":0.7}],"
                        + "\"bfrbEvents\":[]");
        mockMvc.perform(post("/preVisitReportSummaries/generate/{id}", priorityAwareness))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.safetyFlags.length()").value(1))
                .andExpect(jsonPath("$.data.safetyFlags[0].level").value("priority"))
                .andExpect(jsonPath("$.data.safetyFlags[0].reason").value("自述中包含需要优先关注的表达"));

        // 正常数据：不命中任何规则，返回空数组
        int normalAwareness = createAwareness("flag-normal-test", "happy");
        createAnalysis(normalAwareness, "最近状态还不错",
                "\"emotionResults\":[{\"emotionType\":\"happy\",\"frameCount\":9,"
                        + "\"averageConfidence\":0.8,\"maxConfidence\":0.9}],"
                        + "\"bfrbEvents\":[{\"eventKey\":\"event-ok\",\"behaviorCode\":\"nail_biting\","
                        + "\"cueType\":\"咬指甲\",\"startSeconds\":1,\"endSeconds\":3,"
                        + "\"durationSeconds\":2,\"averageConfidence\":0.7,\"maxConfidence\":0.8,"
                        + "\"keyFrameSeconds\":2,\"evidenceType\":\"behavior-model\"}]");
        mockMvc.perform(post("/preVisitReportSummaries/generate/{id}", normalAwareness))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.safetyFlags.length()").value(0))
                .andExpect(jsonPath("$.data.safetyFlagsNote").isNotEmpty());

        // GET 读取时同样附带标注
        mockMvc.perform(get("/preVisitReportSummaries/by-awareness/{id}", normalAwareness))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.safetyFlags.length()").value(0));
    }

    private int createAwareness(String username, String emotionLabel) throws Exception {
        String response = mockMvc.perform(post("/awarenessRecords/fromPrediction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"sourceType\":\"video\","
                                + "\"emotionLabel\":\"" + emotionLabel + "\",\"keepRecord\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private void createAnalysis(int awarenessId, String complaint, String detailJson) throws Exception {
        String payload = "{"
                + "\"sessionId\":\"letter-" + UUID.randomUUID() + "\","
                + "\"awarenessRecordId\":" + awarenessId + ","
                + "\"username\":\"letter-test\","
                + "\"sourceType\":\"video\",\"analysisMode\":\"combined\","
                + "\"keepRecord\":true,\"keepMedia\":false,"
                + "\"complaint\":\"" + complaint + "\","
                + detailJson + "}";
        mockMvc.perform(post("/analysisRecords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }
}
