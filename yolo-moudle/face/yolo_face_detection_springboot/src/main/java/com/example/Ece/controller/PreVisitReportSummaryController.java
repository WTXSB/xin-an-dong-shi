package com.example.Ece.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.Ece.common.Result;
import com.example.Ece.entity.AwarenessRecord;
import com.example.Ece.entity.CareLetterSummary;
import com.example.Ece.entity.DetectionAnalysisRecord;
import com.example.Ece.entity.DetectionBfrbEvent;
import com.example.Ece.entity.DetectionEmotionResult;
import com.example.Ece.entity.PreVisitReportSummary;
import com.example.Ece.mapper.AwarenessRecordMapper;
import com.example.Ece.mapper.CareLetterSummaryMapper;
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
    private static final String CARE_LETTER_PROMPT_VERSION = "care-letter-v1";
    private static final String CARE_LETTER_SYSTEM_PROMPT =
            "你是“心安动识”平台的陪伴书信撰写助手，为写下记录的人写一封温柔的中文书信。"
                    + "以书信形式、第二人称“你”写作，语气温暖、克制，不贴标签、不下结论。"
                    + "结构依次为：问候；接住对方的感受；委婉提及记录中观察到的身体小信号（生活化描述，不评判）；"
                    + "给出2到3个当下就能完成的小建议；给一个温柔的下一步，把“找专业人员聊聊”表达为低压力的选择，不强迫；"
                    + "最后以“心安动识”落款。"
                    + "严禁出现医学用词（如诊断、症状、疾病、障碍、治疗、用药、处方、临床、患者、病人、轻中重程度等）。"
                    + "只输出书信正文纯文本，不要JSON，不要Markdown，不要解释。";
    private static final List<String> LETTER_PROHIBITED_OUTPUT = Arrays.asList(
            "检测结果", "诊断", "症状", "证明", "确诊", "疾病", "障碍", "治疗", "用药", "处方",
            "病理", "临床", "患者", "病人", "轻度", "中度", "重度"
    );
    private static final String LETTER_DISCLAIMER =
            "这封信基于你主动分享的记录写成，只是陪伴与参考，不是任何结论。";
    private static final String SAFETY_FLAGS_NOTE =
            "以上标注仅为算法线索汇总，仅供具备资质的专业人员参考，不构成任何诊断依据。";
    private static final List<String> SAFETY_KEYWORDS = Arrays.asList(
            "自伤", "自杀", "伤害", "暴力", "失控", "想死", "躁"
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
    @Resource
    CareLetterSummaryMapper careLetterMapper;

    @GetMapping("/by-awareness/{awarenessRecordId}")
    public Result<?> getByAwarenessRecordId(@PathVariable int awarenessRecordId) {
        PreVisitReportSummary summary = findByAwarenessId(awarenessRecordId);
        if (summary == null) {
            return Result.error("404", "该报告尚未生成AI辅助摘要");
        }
        return Result.success(toResponse(summary, true, loadSafetyFlags(summary.getAnalysisRecordId())));
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
            return Result.success(toResponse(existing, true, loadSafetyFlags(existing.getAnalysisRecordId())));
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
        return Result.success(toResponse(summary, false, computeSafetyFlags(analysis, emotions, events)));
    }

    @GetMapping("/care-letter/{awarenessRecordId}")
    public Result<?> getCareLetter(@PathVariable int awarenessRecordId) {
        CareLetterSummary letter = findLetterByAwarenessId(awarenessRecordId);
        if (letter == null) {
            return Result.error("404", "该记录的关怀书信尚未生成");
        }
        return Result.success(toLetterResponse(letter, true));
    }

    @PostMapping("/care-letter/generate/{awarenessRecordId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> generateCareLetter(@PathVariable int awarenessRecordId) {
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
            return Result.error("400", "该记录缺少结构化检测明细，不能生成可靠的关怀书信");
        }

        CareLetterSummary existing = findLetterByAwarenessId(awarenessRecordId);
        if (existing != null) {
            return Result.success(toLetterResponse(existing, true));
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

        GeneratedLetter generated = generateLetterText(awareness, analysis, emotions, events);
        CareLetterSummary letter = new CareLetterSummary();
        letter.setAwarenessRecordId(awarenessRecordId);
        letter.setAnalysisRecordId(analysis.getId());
        letter.setProvider(generated.provider);
        letter.setModelName(generated.modelName);
        letter.setPromptVersion(CARE_LETTER_PROMPT_VERSION);
        letter.setLetterText(limit(generated.letterText, 8000));
        letter.setGeneratedAt(LocalDateTime.now());
        letter.setUpdatedAt(LocalDateTime.now());
        careLetterMapper.insert(letter);
        return Result.success(toLetterResponse(letter, false));
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
        return "请依据以下去标识化数据生成预诊材料辅助摘要。不得输出姓名、账号、素材路径或诊断结论：\n"
                + buildDeidentifiedData(awareness, analysis, emotions, events).toJSONString();
    }

    private JSONObject buildDeidentifiedData(AwarenessRecord awareness,
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
        return data;
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

    private GeneratedLetter generateLetterText(AwarenessRecord awareness,
                                               DetectionAnalysisRecord analysis,
                                               List<DetectionEmotionResult> emotions,
                                               List<DetectionBfrbEvent> events) {
        if (StrUtil.isBlank(deepseekApiKey)) {
            return localCareLetter(awareness, analysis, emotions, events);
        }
        try {
            JSONObject request = new JSONObject();
            request.put("model", deepseekModel);
            request.put("temperature", 0.7);
            request.put("max_tokens", 1200);
            request.put("stream", false);

            JSONArray messages = new JSONArray();
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", CARE_LETTER_SYSTEM_PROMPT);
            messages.add(system);
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", "请依据以下去标识化数据，给写下这份记录的人写一封温柔的书信。"
                    + "不得输出姓名、账号、素材路径，只输出书信正文纯文本：\n"
                    + buildDeidentifiedData(awareness, analysis, emotions, events).toJSONString());
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
            String letterText = cleanRemoteLetter(answer);
            if (StrUtil.isBlank(letterText)) {
                throw new IllegalArgumentException("AI书信返回为空");
            }
            if (containsProhibitedLetterOutput(letterText)) {
                return localCareLetter(awareness, analysis, emotions, events);
            }
            return new GeneratedLetter(limit(letterText, 8000), "deepseek", deepseekModel);
        } catch (Exception exception) {
            System.out.println("[CareLetter] AI letter failed, using local fallback: " + exception.getMessage());
            return localCareLetter(awareness, analysis, emotions, events);
        }
    }

    private String cleanRemoteLetter(String answer) {
        if (StrUtil.isBlank(answer)) return "";
        String text = answer.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:text|plaintext)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        return text.trim();
    }

    private boolean containsProhibitedLetterOutput(String letterText) {
        for (String prohibited : LETTER_PROHIBITED_OUTPUT) {
            if (letterText.contains(prohibited)) return true;
        }
        return false;
    }

    private GeneratedLetter localCareLetter(AwarenessRecord awareness,
                                            DetectionAnalysisRecord analysis,
                                            List<DetectionEmotionResult> emotions,
                                            List<DetectionBfrbEvent> events) {
        StringBuilder letter = new StringBuilder();
        letter.append("亲爱的你：\n\n");
        letter.append("见字如面。谢谢你愿意把这段时间的自己记录下来，也愿意让这些记录被看见。\n\n");

        // 接住感受：优先使用觉察记录里的情绪标注，其次使用主要情绪线索
        String emotionCue = StrUtil.blankToDefault(awareness.getEmotionLabel(),
                emotions.isEmpty() ? "" : emotions.get(0).getEmotionType());
        letter.append("从这些记录里，我们隐约感觉到，").append(gentleEmotionPhrase(emotionCue))
                .append("。无论它是什么，都值得被温柔地接住，而不是被评判。\n\n");

        // 委婉提及观察到的身体小信号，使用生活化描述
        if (!events.isEmpty()) {
            letter.append("我们也留意到，记录里有一些").append(gentleCuePhrase(events.get(0).getCueType()))
                    .append("。这些时刻往往发生在不经意间，它们不是错，只是身体在用自己的方式说话。\n\n");
        }

        // 自述
        String complaint = StrUtil.blankToDefault(analysis.getComplaint(), "");
        if (StrUtil.isNotBlank(complaint)) {
            letter.append("你还写下了“").append(limit(complaint, 100))
                    .append("”。谢谢你愿意说出来，把心里的事情写下来，本身就已经很不容易。\n\n");
        }

        letter.append("如果你愿意，可以试试这几件小事：\n")
                .append("1. 找个安静的地方，慢慢做几次深呼吸，吸气四拍、呼气六拍。\n")
                .append("2. 当那些不经意的时刻出现时，先轻轻把手放在桌面上，感受手心的温度，不急着责怪自己。\n")
                .append("3. 睡前花几分钟，把今天最惦记的一件事写在纸上，写完就把它放下。\n\n");
        letter.append("如果有一天，你想找专业的人聊聊，那是一个随时都可以做的选择，")
                .append("不必着急，也不用勉强自己；在此之前，我们会一直在这里陪着你。\n\n");
        letter.append(LETTER_DISCLAIMER).append("\n\n");
        letter.append("—— 心安动识");
        return new GeneratedLetter(letter.toString(), "local-fallback", "rule-letter-v1");
    }

    private String gentleEmotionPhrase(String emotionCue) {
        String cue = StrUtil.blankToDefault(emotionCue, "").toLowerCase(Locale.ROOT);
        if (cue.contains("sad") || cue.contains("难过") || cue.contains("低落")) {
            return "最近的日子似乎有些沉，心里压着一些东西";
        }
        if (cue.contains("angry") || cue.contains("生气") || cue.contains("烦")) {
            return "心里好像攒着一些火气，闷着不太舒服";
        }
        if (cue.contains("fear") || cue.contains("害怕") || cue.contains("紧张")) {
            return "心里似乎有些紧绷，像一直提着一口气";
        }
        if (cue.contains("happy") || cue.contains("开心")) {
            return "这段日子里透着一些轻松的时刻";
        }
        return "这段时间的情绪有些起伏";
    }

    private String gentleCuePhrase(String cueType) {
        String cue = StrUtil.blankToDefault(cueType, "");
        if (cue.contains("咬") || cue.contains("嘴") || cue.contains("进食") || cue.contains("指甲")) {
            return "手不自觉地靠近嘴边的时刻";
        }
        if (cue.contains("抠") || cue.contains("抓") || cue.contains("皮肤")) {
            return "手指不自觉地触碰皮肤的时刻";
        }
        if (cue.contains("拔") || cue.contains("头发")) {
            return "手指不自觉地绕向头发的时刻";
        }
        return "一些小动作反复出现的时刻";
    }

    private List<Map<String, Object>> loadSafetyFlags(Integer analysisRecordId) {
        if (analysisRecordId == null) {
            return Collections.emptyList();
        }
        DetectionAnalysisRecord analysis = analysisRecordMapper.selectById(analysisRecordId);
        if (analysis == null) {
            return Collections.emptyList();
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
        return computeSafetyFlags(analysis, emotions, events);
    }

    private List<Map<String, Object>> computeSafetyFlags(DetectionAnalysisRecord analysis,
                                                         List<DetectionEmotionResult> emotions,
                                                         List<DetectionBfrbEvent> events) {
        List<Map<String, Object>> flags = new ArrayList<>();

        // 愤怒类情绪线索：帧占比达到一半且平均置信度足够高时提示注意
        int totalFrames = 0;
        DetectionEmotionResult angryEmotion = null;
        for (DetectionEmotionResult emotion : emotions) {
            totalFrames += value(emotion.getFrameCount());
            if ("angry".equalsIgnoreCase(StrUtil.blankToDefault(emotion.getEmotionType(), ""))) {
                angryEmotion = emotion;
            }
        }
        if (angryEmotion != null && totalFrames > 0 && angryEmotion.getAverageConfidence() != null) {
            double ratio = value(angryEmotion.getFrameCount()) * 1.0 / totalFrames;
            if (ratio >= 0.5 && angryEmotion.getAverageConfidence().doubleValue() >= 0.6) {
                flags.add(safetyFlag("attention", "持续愤怒类情绪线索占比较高：帧占比 "
                        + percent(BigDecimal.valueOf(ratio)) + "，该类型平均置信度 "
                        + angryEmotion.getAverageConfidence().stripTrailingZeros().toPlainString()));
            }
        }

        // 重复性身体行为线索：次数或累计时长偏高时提示注意
        int eventCount = value(analysis.getBfrbEventCount());
        double totalDuration = analysis.getBfrbTotalDurationSeconds() == null
                ? 0 : analysis.getBfrbTotalDurationSeconds().doubleValue();
        if (eventCount >= 10 || totalDuration >= 120) {
            flags.add(safetyFlag("attention", "重复性身体行为线索较频繁：共 " + eventCount
                    + " 次，累计持续 " + decimal(analysis.getBfrbTotalDurationSeconds()) + " 秒"));
        }

        // 单次持续时间较长的行为线索
        BigDecimal maxDuration = null;
        for (DetectionBfrbEvent event : events) {
            if (event.getDurationSeconds() != null
                    && (maxDuration == null || event.getDurationSeconds().compareTo(maxDuration) > 0)) {
                maxDuration = event.getDurationSeconds();
            }
        }
        if (maxDuration != null && maxDuration.doubleValue() >= 60) {
            flags.add(safetyFlag("prompt", "存在单次持续时间较长的行为线索：最长约 "
                    + decimal(maxDuration) + " 秒"));
        }

        // 自述中的优先关注表达
        String selfReport = StrUtil.blankToDefault(analysis.getComplaint(), "")
                + StrUtil.blankToDefault(analysis.getAdditionalNotes(), "");
        for (String keyword : SAFETY_KEYWORDS) {
            if (selfReport.contains(keyword)) {
                flags.add(safetyFlag("priority", "自述中包含需要优先关注的表达"));
                break;
            }
        }
        return flags;
    }

    private Map<String, Object> safetyFlag(String level, String reason) {
        Map<String, Object> flag = new LinkedHashMap<>();
        flag.put("level", level);
        flag.put("reason", reason);
        return flag;
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

    private CareLetterSummary findLetterByAwarenessId(int awarenessRecordId) {
        return careLetterMapper.selectOne(
                Wrappers.<CareLetterSummary>lambdaQuery()
                        .eq(CareLetterSummary::getAwarenessRecordId, awarenessRecordId)
                        .last("LIMIT 1")
        );
    }

    private Map<String, Object> toResponse(PreVisitReportSummary summary, boolean cached,
                                           List<Map<String, Object>> safetyFlags) {
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
        data.put("safetyFlags", safetyFlags);
        data.put("safetyFlagsNote", SAFETY_FLAGS_NOTE);
        data.put("generatedAt", summary.getGeneratedAt());
        data.put("cached", cached);
        return data;
    }

    private Map<String, Object> toLetterResponse(CareLetterSummary letter, boolean cached) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", letter.getId());
        data.put("awarenessRecordId", letter.getAwarenessRecordId());
        data.put("analysisRecordId", letter.getAnalysisRecordId());
        data.put("provider", letter.getProvider());
        data.put("modelName", letter.getModelName());
        data.put("promptVersion", letter.getPromptVersion());
        data.put("letterText", letter.getLetterText());
        data.put("generatedAt", letter.getGeneratedAt());
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

    private static class GeneratedLetter {
        String letterText;
        String provider;
        String modelName;

        GeneratedLetter(String letterText, String provider, String modelName) {
            this.letterText = letterText;
            this.provider = provider;
            this.modelName = modelName;
        }
    }
}
