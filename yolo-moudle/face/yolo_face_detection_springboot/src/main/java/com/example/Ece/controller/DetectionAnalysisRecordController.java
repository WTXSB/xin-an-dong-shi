package com.example.Ece.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Ece.common.Result;
import com.example.Ece.dto.DetectionAnalysisPayload;
import com.example.Ece.entity.DetectionAnalysisRecord;
import com.example.Ece.entity.DetectionBfrbEvent;
import com.example.Ece.entity.DetectionEmotionResult;
import com.example.Ece.mapper.AwarenessRecordMapper;
import com.example.Ece.mapper.DetectionAnalysisRecordMapper;
import com.example.Ece.mapper.DetectionBfrbEventMapper;
import com.example.Ece.mapper.DetectionEmotionResultMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Persists the complete output of one image/video/camera analysis session.
 *
 * This controller is deliberately separate from the legacy record endpoints so
 * that structured reporting can be added without changing their response shape.
 */
@RestController
@RequestMapping("/analysisRecords")
public class DetectionAnalysisRecordController {
    private static final Set<String> SOURCE_TYPES = new HashSet<>(Arrays.asList("image", "video", "camera"));
    private static final Set<String> ANALYSIS_MODES = new HashSet<>(Arrays.asList(
            "combined", "emotion", "bfrb_behavior", "bfrb_geometry"
    ));

    @Resource
    DetectionAnalysisRecordMapper analysisRecordMapper;
    @Resource
    DetectionEmotionResultMapper emotionResultMapper;
    @Resource
    DetectionBfrbEventMapper bfrbEventMapper;
    @Resource
    AwarenessRecordMapper awarenessRecordMapper;

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String username,
                              @RequestParam(defaultValue = "") String sourceType,
                              @RequestParam(defaultValue = "") String analysisMode) {
        LambdaQueryWrapper<DetectionAnalysisRecord> wrapper = Wrappers.<DetectionAnalysisRecord>lambdaQuery()
                .orderByDesc(DetectionAnalysisRecord::getDetectedAt)
                .orderByDesc(DetectionAnalysisRecord::getCreatedAt);
        if (StrUtil.isNotBlank(username)) {
            wrapper.eq(DetectionAnalysisRecord::getUsername, username);
        }
        if (StrUtil.isNotBlank(sourceType)) {
            wrapper.eq(DetectionAnalysisRecord::getSourceType, sourceType);
        }
        if (StrUtil.isNotBlank(analysisMode)) {
            wrapper.eq(DetectionAnalysisRecord::getAnalysisMode, analysisMode);
        }
        return Result.success(analysisRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable int id) {
        DetectionAnalysisRecord record = analysisRecordMapper.selectById(id);
        if (record == null) {
            return Result.error("404", "未找到检测分析记录");
        }
        return Result.success(buildDetail(record));
    }

    @GetMapping("/session/{sessionId}")
    public Result<?> getBySessionId(@PathVariable String sessionId) {
        DetectionAnalysisRecord record = findBySessionId(sessionId);
        if (record == null) {
            return Result.error("404", "未找到检测会话");
        }
        return Result.success(buildDetail(record));
    }

    @GetMapping("/by-awareness/{awarenessRecordId}")
    public Result<?> getByAwarenessRecordId(@PathVariable int awarenessRecordId) {
        DetectionAnalysisRecord record = analysisRecordMapper.selectOne(
                Wrappers.<DetectionAnalysisRecord>lambdaQuery()
                        .eq(DetectionAnalysisRecord::getAwarenessRecordId, awarenessRecordId)
                        .last("LIMIT 1")
        );
        if (record == null) {
            return Result.error("404", "该觉察记录尚未关联结构化检测结果");
        }
        return Result.success(buildDetail(record));
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<?> save(@RequestBody DetectionAnalysisPayload payload) {
        String validationMessage = normalizeAndValidate(payload);
        if (validationMessage != null) {
            return Result.error("400", validationMessage);
        }

        if (Boolean.FALSE.equals(payload.getKeepRecord())) {
            Map<String, Object> skipped = new LinkedHashMap<>();
            skipped.put("saved", false);
            skipped.put("sessionId", payload.getSessionId());
            skipped.put("message", "已尊重用户选择，本次结构化检测结果不会保存。");
            return Result.success(skipped);
        }

        DetectionAnalysisRecord existing = findBySessionId(payload.getSessionId());
        if (existing != null) {
            Map<String, Object> detail = buildDetail(existing);
            detail.put("saved", true);
            detail.put("duplicate", true);
            return Result.success(detail);
        }

        payload.setId(null);
        analysisRecordMapper.insert(payload);

        for (DetectionEmotionResult emotion : payload.getEmotionResults()) {
            emotion.setId(null);
            emotion.setAnalysisRecordId(payload.getId());
            normalizeEmotion(emotion);
            emotionResultMapper.insert(emotion);
        }
        for (DetectionBfrbEvent event : payload.getBfrbEvents()) {
            event.setId(null);
            event.setAnalysisRecordId(payload.getId());
            normalizeBfrbEvent(event);
            bfrbEventMapper.insert(event);
        }

        DetectionAnalysisRecord savedRecord = analysisRecordMapper.selectById(payload.getId());
        Map<String, Object> detail = buildDetail(savedRecord);
        detail.put("saved", true);
        detail.put("duplicate", false);
        return Result.success(detail);
    }

    @PutMapping("/{id}/awareness/{awarenessRecordId}")
    public Result<?> linkAwarenessRecord(@PathVariable int id, @PathVariable int awarenessRecordId) {
        DetectionAnalysisRecord record = analysisRecordMapper.selectById(id);
        if (record == null) {
            return Result.error("404", "未找到检测分析记录");
        }
        if (awarenessRecordMapper.selectById(awarenessRecordId) == null) {
            return Result.error("404", "未找到觉察记录");
        }
        record.setAwarenessRecordId(awarenessRecordId);
        analysisRecordMapper.updateById(record);
        return Result.success(buildDetail(record));
    }

    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(@PathVariable int id) {
        deleteAnalysisRecord(id);
        return Result.success();
    }

    @DeleteMapping("/by-awareness/{awarenessRecordId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteByAwarenessRecordId(@PathVariable int awarenessRecordId) {
        DetectionAnalysisRecord record = analysisRecordMapper.selectOne(
                Wrappers.<DetectionAnalysisRecord>lambdaQuery()
                        .eq(DetectionAnalysisRecord::getAwarenessRecordId, awarenessRecordId)
                        .last("LIMIT 1")
        );
        if (record == null) {
            return Result.error("404", "该觉察记录没有关联的结构化检测结果");
        }
        deleteAnalysisRecord(record.getId());
        return Result.success();
    }

    private void deleteAnalysisRecord(int id) {
        bfrbEventMapper.delete(Wrappers.<DetectionBfrbEvent>lambdaQuery()
                .eq(DetectionBfrbEvent::getAnalysisRecordId, id));
        emotionResultMapper.delete(Wrappers.<DetectionEmotionResult>lambdaQuery()
                .eq(DetectionEmotionResult::getAnalysisRecordId, id));
        analysisRecordMapper.deleteById(id);
    }

    private String normalizeAndValidate(DetectionAnalysisPayload payload) {
        if (StrUtil.isBlank(payload.getSessionId())) {
            payload.setSessionId(UUID.randomUUID().toString());
        }
        if (StrUtil.isBlank(payload.getSourceType())) {
            return "检测来源不能为空";
        }
        payload.setSourceType(payload.getSourceType().trim().toLowerCase(Locale.ROOT));
        if (!SOURCE_TYPES.contains(payload.getSourceType())) {
            return "检测来源仅支持 image、video 或 camera";
        }
        if (StrUtil.isBlank(payload.getAnalysisMode())) {
            payload.setAnalysisMode("emotion");
        }
        payload.setAnalysisMode(payload.getAnalysisMode().trim().toLowerCase(Locale.ROOT));
        if (!ANALYSIS_MODES.contains(payload.getAnalysisMode())) {
            return "不支持的分析模式";
        }
        if (payload.getKeepRecord() == null) {
            payload.setKeepRecord(true);
        }
        if (payload.getKeepMedia() == null) {
            payload.setKeepMedia(false);
        }
        if (!payload.getKeepMedia()) {
            payload.setInputMedia("");
            payload.setOutputMedia("");
        }
        if (payload.getDetectedAt() == null) {
            payload.setDetectedAt(LocalDateTime.now());
        }
        if (payload.getCreatedAt() == null) {
            payload.setCreatedAt(LocalDateTime.now());
        }

        payload.setEmotionTypeCount(payload.getEmotionResults().size());
        payload.setBfrbEventCount(payload.getBfrbEvents().size());
        BigDecimal totalDuration = BigDecimal.ZERO;
        LinkedHashSet<String> evidenceTypes = new LinkedHashSet<>();
        for (DetectionBfrbEvent event : payload.getBfrbEvents()) {
            if (event.getDurationSeconds() != null) {
                totalDuration = totalDuration.add(event.getDurationSeconds());
            }
            if (StrUtil.isNotBlank(event.getEvidenceType())) {
                evidenceTypes.add(event.getEvidenceType());
            }
        }
        payload.setBfrbTotalDurationSeconds(totalDuration);
        payload.setEvidenceTypes(String.join(",", evidenceTypes));
        return null;
    }

    private void normalizeEmotion(DetectionEmotionResult emotion) {
        if (StrUtil.isBlank(emotion.getEmotionType())) {
            emotion.setEmotionType("unknown");
        }
        if (emotion.getFrameCount() == null) {
            emotion.setFrameCount(0);
        }
        if (emotion.getAverageConfidence() == null) {
            emotion.setAverageConfidence(BigDecimal.ZERO);
        }
        if (emotion.getMaxConfidence() == null) {
            emotion.setMaxConfidence(BigDecimal.ZERO);
        }
    }

    private void normalizeBfrbEvent(DetectionBfrbEvent event) {
        if (StrUtil.isBlank(event.getEventKey())) {
            event.setEventKey(UUID.randomUUID().toString());
        }
        if (StrUtil.isBlank(event.getBehaviorCode())) {
            event.setBehaviorCode("unknown");
        }
        if (StrUtil.isBlank(event.getCueType())) {
            event.setCueType("未知线索");
        }
        if (event.getStartSeconds() == null) event.setStartSeconds(BigDecimal.ZERO);
        if (event.getEndSeconds() == null) event.setEndSeconds(BigDecimal.ZERO);
        if (event.getDurationSeconds() == null) event.setDurationSeconds(BigDecimal.ZERO);
        if (event.getSampleCount() == null) event.setSampleCount(0);
        if (event.getAverageConfidence() == null) event.setAverageConfidence(BigDecimal.ZERO);
        if (event.getMaxConfidence() == null) event.setMaxConfidence(BigDecimal.ZERO);
        if (event.getKeyFrameSeconds() == null) event.setKeyFrameSeconds(BigDecimal.ZERO);
        if (StrUtil.isBlank(event.getEvidenceType())) event.setEvidenceType("unknown");
        if (event.getKeyBboxJson() == null) event.setKeyBboxJson("[]");
        if (event.getGeometryJson() == null) event.setGeometryJson("{}");
    }

    private DetectionAnalysisRecord findBySessionId(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return null;
        }
        return analysisRecordMapper.selectOne(Wrappers.<DetectionAnalysisRecord>lambdaQuery()
                .eq(DetectionAnalysisRecord::getSessionId, sessionId));
    }

    private Map<String, Object> buildDetail(DetectionAnalysisRecord record) {
        List<DetectionEmotionResult> emotions = emotionResultMapper.selectList(
                Wrappers.<DetectionEmotionResult>lambdaQuery()
                        .eq(DetectionEmotionResult::getAnalysisRecordId, record.getId())
                        .orderByDesc(DetectionEmotionResult::getFrameCount)
        );
        List<DetectionBfrbEvent> events = bfrbEventMapper.selectList(
                Wrappers.<DetectionBfrbEvent>lambdaQuery()
                        .eq(DetectionBfrbEvent::getAnalysisRecordId, record.getId())
                        .orderByAsc(DetectionBfrbEvent::getStartSeconds)
                        .orderByAsc(DetectionBfrbEvent::getId)
        );
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("record", record);
        detail.put("emotionResults", emotions);
        detail.put("bfrbEvents", events);
        detail.put("behaviorStats", buildBehaviorStats(events));
        return detail;
    }

    private List<Map<String, Object>> buildBehaviorStats(List<DetectionBfrbEvent> events) {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (DetectionBfrbEvent event : events) {
            String cueType = StrUtil.blankToDefault(event.getCueType(), "未知线索");
            Map<String, Object> item = grouped.computeIfAbsent(cueType, key -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("cueType", key);
                value.put("count", 0);
                value.put("totalDurationSeconds", BigDecimal.ZERO);
                value.put("maxConfidence", BigDecimal.ZERO);
                return value;
            });
            item.put("count", ((Integer) item.get("count")) + 1);
            BigDecimal duration = (BigDecimal) item.get("totalDurationSeconds");
            item.put("totalDurationSeconds", duration.add(
                    event.getDurationSeconds() == null ? BigDecimal.ZERO : event.getDurationSeconds()
            ));
            BigDecimal currentMax = (BigDecimal) item.get("maxConfidence");
            BigDecimal eventMax = event.getMaxConfidence() == null ? BigDecimal.ZERO : event.getMaxConfidence();
            if (eventMax.compareTo(currentMax) > 0) {
                item.put("maxConfidence", eventMax);
            }
        }
        return new ArrayList<>(grouped.values());
    }
}
