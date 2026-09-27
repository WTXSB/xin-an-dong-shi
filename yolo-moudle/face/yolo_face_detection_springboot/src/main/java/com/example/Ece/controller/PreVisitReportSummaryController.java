package com.example.Ece.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.Ece.common.Result;
import com.example.Ece.entity.AwarenessRecord;
import com.example.Ece.entity.DetectionAnalysisRecord;
import com.example.Ece.entity.DetectionBfrbEvent;
import com.example.Ece.entity.DetectionEmotionResult;
import com.example.Ece.entity.PreVisitReportSummary;
import com.example.Ece.mapper.AwarenessRecordMapper;
import com.example.Ece.mapper.DetectionAnalysisRecordMapper;
import com.example.Ece.mapper.DetectionBfrbEventMapper;
import com.example.Ece.mapper.DetectionEmotionResultMapper;
import com.example.Ece.mapper.PreVisitReportSummaryMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/preVisitReportSummaries")
public class PreVisitReportSummaryController {
    private static final String PROMPT_VERSION = "previsit-summary-v1";
    private static final String SAFETY_NOTE =
            "本内容仅对已保存的自述和算法线索进行辅助整理，不构成医学诊断、严重程度判断、疾病筛查结论、用药或治疗建议。";
    private static final String SYSTEM_PROMPT =
            "你是心理门诊预诊材料的文字整理助手。你只能整理输入中明确给出的自述和检测统计，不得补充不存在的事实。"
                    + "禁止诊断任何疾病，禁止判断轻中重程度，禁止建议药物、停药、加药、减药或治疗方案。"
                    + "不得把表情、动作或模型置信度解释为疾病证据，不得根据未识别到事件推断不存在问题。"
                    + "不得进行自伤、自杀或他伤风险分级；如输入没有相关自述，不得主动推断。"
                    + "使用客观、克制、非污名化的中文。只输出JSON对象，不要Markdown。"
                    + "JSON必须包含objectiveSummary字符串、clinicianQuestions字符串数组、visitPreparation字符串数组三个字段。";
    private static final List<String> PROHIBITED_OUTPUT = Arrays.asList(
            "确诊", "诊断为", "抑郁症", "焦虑症", "强迫症", "双相", "自闭症",
            "建议服用", "需要服药", "停药", "加药", "减药", "治疗方案", "轻度", "中度", "重度"
    );

    @Value("${deepseek.api-key:}")
    private String deepseekApiKey;
    @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String deepseekApiUrl;
    @Value("${deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Resource
    AwarenessRecordMapper awarenessRecordMapper;
    @Resource
    DetectionAnalysisRecordMapper analysisRecordMapper;
    @Resource
    DetectionEmotionResultMapper emotionResultMapper;
    @Resource
    DetectionBfrbEventMapper bfrbEventMapper;
    @Resource
    PreVisitReportSummaryMapper summaryMapper;

    @GetMapping("/by-awareness/{awarenessRecordId}")
    public Result<?> getByAwarenessRecordId(@PathVariable int awarenessRecordId) {
        PreVisitReportSummary summary = findByAwarenessId(awarenessRecordId);
        if (summary == null) {
            return Result.error("404", "该报告尚未生成AI辅助摘要");
        }
        return Result.success(toResponse(summary, true));
    }

    @PostMapping("/generate/{awarenessRecordId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> generate(@PathVariable int awarenessRecordId) {
        AwarenessRecord awareness = awarenessRecordMapper.selectById(awarenessRecordId);
        if (awareness == null) {
            return Result.error("404", "未找到对应的觉察记录");
        }
        DetectionAnalysisRecord analysis = analysisRecordMapper.selectOne(
                Wrappers.<DetectionAnalysisRecord>lambdaQuery()
                        .eq(DetectionAnalysisRecord::getAwarenessRecordId, awarenessRecordId)
                        .last("LIMIT 1")
        );
        if (analysis == null) {
            return Result.error("400", "该记录缺少结构化检测明细，不能生成可靠的AI辅助摘要");
        }

        PreVisitReportSummary existing = findByAwarenessId(awarenessRecordId);
        if (existing != null) {
            return Result.success(toResponse(existing, true));
        }

        List<DetectionEmotionResult> emotions = emotionResultMapper.selectList(
                Wrappers.<DetectionEmotionResult>lambdaQuery()
                        .eq(DetectionEmotionResult::getAnalysisRecordId, analysis.getId())
                        .orderByDesc(DetectionEmotionResult::getFrameCount)
        );
        List<DetectionBfrbEvent> events = bfrbEventMapper.selectList(
                Wrappers.<DetectionBfrbEvent>lambdaQuery()
                        .eq(DetectionBfrbEvent::getAnalysisRecordId, analysis.getId())
                        .orderByAsc(DetectionBfrbEvent::getStartSeconds)
        );

        GeneratedSummary generated = generateSummary(awareness, analysis, emotions, events);
        PreVisitReportSummary summary = new PreVisitReportSummary();
        summary.setAwarenessRecordId(awarenessRecordId);
        summary.setAnalysisRecordId(analysis.getId());
        summary.setProvider(generated.provider);
        summary.setModelName(generated.modelName);
        summary.setPromptVersion(PROMPT_VERSION);
        summary.setObjectiveSummary(limit(generated.objectiveSummary, 4000));
        summary.setClinicianQuestionsJson(JSONArray.toJSONString(generated.clinicianQuestions));
        summary.setVisitPreparationJson(JSONArray.toJSONString(generated.visitPreparation));
        summary.setSafetyNote(SAFETY_NOTE);
        summary.setGeneratedAt(LocalDateTime.now());
        summary.setUpdatedAt(LocalDateTime.now());
        summaryMapper.insert(summary);
        return Result.success(toResponse(summary, false));
    }

    private GeneratedSummary generateSummary(AwarenessRecord awareness,
                                             DetectionAnalysisRecord analysis,
                                             List<DetectionEmotionResult> emotions,
                                             List<DetectionBfrbEvent> events) {
        if (StrUtil.isBlank(deepseekApiKey)) {
            return localSummary(analysis, emotions, events);
        }
        try {
            JSONObject request = new JSONObject();
            request.put("model", deepseekModel);
            request.put("temperature", 0.2);
            request.put("max_tokens", 1000);
            request.put("stream", false);

            JSONArray messages = new JSONArray();
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", SYSTEM_PROMPT);
            messages.add(system);
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", buildDeidentifiedPrompt(awareness, analysis, emotions, events));
            messages.add(user);
            request.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepseekApiKey.trim());
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(15000);
            factory.setReadTimeout(30000);
            RestTemplate client = new RestTemplate(factory);
            ResponseEntity<String> response = client.exchange(
                    deepseekApiUrl, HttpMethod.POST, new HttpEntity<>(request.toJSONString(), headers), String.class
            );
            JSONObject responseJson = JSONObject.parseObject(response.getBody());
            String answer = responseJson.getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content");
            GeneratedSummary parsed = parseRemoteAnswer(answer);
            if (containsProhibitedOutput(parsed)) {
                return localSummary(analysis, emotions, events);
            }
            parsed.provider = "deepseek";
            parsed.modelName = deepseekModel;
            return parsed;
        } catch (Exception exception) {
            System.out.println("[PreVisitReport] AI summary failed, using local fallback: " + exception.getMessage());
            return localSummary(analysis, emotions, events);
        }
    }

    private String buildDeidentifiedPrompt(AwarenessRecord awareness,
                                           DetectionAnalysisRecord analysis,
                                           List<DetectionEmotionResult> emotions,
                                           List<DetectionBfrbEvent> events) {
        JSONObject data = new JSONObject(true);
        data.put("sourceType", analysis.getSourceType());
        data.put("analysisMode", analysis.getAnalysisMode());
        data.put("complaint", limit(StrUtil.blankToDefault(analysis.getComplaint(), "未填写"), 800));
        data.put("additionalNotes", limit(StrUtil.blankToDefault(analysis.getAdditionalNotes(), "未填写"), 1000));
        data.put("gentleSummary", limit(StrUtil.blankToDefault(awareness.getGentleSummary(), ""), 800));

        JSONArray emotionData = new JSONArray();
        for (DetectionEmotionResult emotion : emotions) {
            JSONObject item = new JSONObject(true);
            item.put("emotionType", emotion.getEmotionType());
            item.put("frameCount", emotion.getFrameCount());
            item.put("averageConfidence", emotion.getAverageConfidence());
            item.put("maxConfidence", emotion.getMaxConfidence());
            emotionData.add(item);
        }
        data.put("emotionResults", emotionData);

        JSONArray eventData = new JSONArray();
        for (int i = 0; i < Math.min(events.size(), 20); i++) {
            DetectionBfrbEvent event = events.get(i);
            JSONObject item = new JSONObject(true);
            item.put("cueType", event.getCueType());
            item.put("startSeconds", event.getStartSeconds());
            item.put("endSeconds", event.getEndSeconds());
            item.put("durationSeconds", event.getDurationSeconds());
            item.put("averageConfidence", event.getAverageConfidence());
            item.put("maxConfidence", event.getMaxConfidence());
            item.put("evidenceType", event.getEvidenceType());
            eventData.add(item);
        }
        data.put("bfrbEventCount", analysis.getBfrbEventCount());
        data.put("bfrbTotalDurationSeconds", analysis.getBfrbTotalDurationSeconds());
        data.put("bfrbEvents", eventData);
        return "请依据以下去标识化数据生成预诊材料辅助摘要。不得输出姓名、账号、素材路径或诊断结论：\n" + data.toJSONString();
    }

    private GeneratedSummary parseRemoteAnswer(String answer) {
        if (StrUtil.isBlank(answer)) {
            throw new IllegalArgumentException("AI返回为空");
        }
        String json = answer.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("AI未返回JSON对象");
        }
        JSONObject parsed = JSONObject.parseObject(json.substring(start, end + 1));
        String objective = parsed.getString("objectiveSummary");
        List<String> questions = readStringArray(parsed.getJSONArray("clinicianQuestions"), 6);
        List<String> preparation = readStringArray(parsed.getJSONArray("visitPreparation"), 6);
        if (StrUtil.isBlank(objective) || questions.isEmpty() || preparation.isEmpty()) {
            throw new IllegalArgumentException("AI返回字段不完整");
        }
        return new GeneratedSummary(limit(objective.trim(), 4000), questions, preparation, "deepseek", deepseekModel);
    }

    private GeneratedSummary localSummary(DetectionAnalysisRecord analysis,
                                          List<DetectionEmotionResult> emotions,
                                          List<DetectionBfrbEvent> events) {
        DetectionEmotionResult leadingEmotion = emotions.isEmpty() ? null : emotions.get(0);
        String emotionPart = leadingEmotion == null
                ? "本次未保存可展示的情绪分类明细。"
                : "本次记录中的主要情绪线索为“" + leadingEmotion.getEmotionType() + "”，共 "
                + value(leadingEmotion.getFrameCount()) + " 次有效采样，平均置信度 "
                + percent(leadingEmotion.getAverageConfidence()) + "。";
        String eventPart = events.isEmpty()
                ? "检测时段内没有形成满足连续性规则的BFRB行为事件；这不等同于排除相关行为。"
                : "检测时段内形成 " + events.size() + " 个满足连续性规则的BFRB行为事件，累计持续 "
                + decimal(analysis.getBfrbTotalDurationSeconds()) + " 秒。";
        String objective = emotionPart + eventPart + "以上内容仅描述本次记录，需结合实际情境由专业人员进一步核实。";

        String leadingCue = events.isEmpty() ? "重复性身体行为" : StrUtil.blankToDefault(events.get(0).getCueType(), "重复性身体行为");
        List<String> questions = Arrays.asList(
                "近期主要困扰从何时开始，通常在什么情境下出现？",
                "“" + leadingCue + "”出现前是否有压力、紧张、无聊或其他主观体验？",
                "相关情绪或动作是否影响学习、工作、睡眠、人际交往或身体皮肤状态？",
                "过去是否尝试过缓解方式，其效果和可持续性如何？"
        );
        List<String> preparation = Arrays.asList(
                "准备近期主要困扰、持续时间和典型触发情境的简短记录。",
                "如方便，记录一周内相关情绪或动作的大致频率及持续时间。",
                "整理近期睡眠、学习或工作压力、生活事件以及已经尝试的应对方式。",
                "就诊时主动说明本报告来自算法辅助整理，需要由医生进一步核实。"
        );
        return new GeneratedSummary(objective, questions, preparation, "local-fallback", "rule-summary-v1");
    }

    private boolean containsProhibitedOutput(GeneratedSummary summary) {
        String combined = summary.objectiveSummary + String.join("", summary.clinicianQuestions)
                + String.join("", summary.visitPreparation);
        for (String prohibited : PROHIBITED_OUTPUT) {
            if (combined.contains(prohibited)) return true;
        }
        return false;
    }

    private List<String> readStringArray(JSONArray array, int maxItems) {
        if (array == null) return Collections.emptyList();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < Math.min(array.size(), maxItems); i++) {
            String value = array.getString(i);
            if (StrUtil.isNotBlank(value)) values.add(limit(value.trim(), 500));
        }
        return values;
    }

    private PreVisitReportSummary findByAwarenessId(int awarenessRecordId) {
        return summaryMapper.selectOne(
                Wrappers.<PreVisitReportSummary>lambdaQuery()
                        .eq(PreVisitReportSummary::getAwarenessRecordId, awarenessRecordId)
                        .last("LIMIT 1")
        );
    }

    private Map<String, Object> toResponse(PreVisitReportSummary summary, boolean cached) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", summary.getId());
        data.put("awarenessRecordId", summary.getAwarenessRecordId());
        data.put("analysisRecordId", summary.getAnalysisRecordId());
        data.put("provider", summary.getProvider());
        data.put("modelName", summary.getModelName());
        data.put("promptVersion", summary.getPromptVersion());
        data.put("objectiveSummary", summary.getObjectiveSummary());
        data.put("clinicianQuestions", parseStoredArray(summary.getClinicianQuestionsJson()));
        data.put("visitPreparation", parseStoredArray(summary.getVisitPreparationJson()));
        data.put("safetyNote", summary.getSafetyNote());
        data.put("generatedAt", summary.getGeneratedAt());
        data.put("cached", cached);
        return data;
    }

    private List<String> parseStoredArray(String json) {
        if (StrUtil.isBlank(json)) return Collections.emptyList();
        try {
            return readStringArray(JSONArray.parseArray(json), 10);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String percent(BigDecimal value) {
        if (value == null) return "0%";
        return value.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private String decimal(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String limit(String text, int length) {
        if (text == null) return "";
        return text.length() <= length ? text : text.substring(0, length);
    }

    private static class GeneratedSummary {
        String objectiveSummary;
        List<String> clinicianQuestions;
        List<String> visitPreparation;
        String provider;
        String modelName;

        GeneratedSummary(String objectiveSummary, List<String> clinicianQuestions,
                         List<String> visitPreparation, String provider, String modelName) {
            this.objectiveSummary = objectiveSummary;
            this.clinicianQuestions = clinicianQuestions;
            this.visitPreparation = visitPreparation;
            this.provider = provider;
            this.modelName = modelName;
        }
    }
}
