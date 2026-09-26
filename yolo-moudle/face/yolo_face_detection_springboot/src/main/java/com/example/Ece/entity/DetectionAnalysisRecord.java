package com.example.Ece.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("detection_analysis_records")
public class DetectionAnalysisRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String sessionId;
    private Integer awarenessRecordId;
    private String username;
    private String sourceType;
    private String analysisMode;
    private String modelConfiguration;
    private Integer emotionTypeCount;
    private Integer bfrbEventCount;
    private BigDecimal bfrbTotalDurationSeconds;
    private String evidenceTypes;
    private String complaint;
    private String additionalNotes;
    private String aggregationRulesJson;
    private String inputMedia;
    private String outputMedia;
    private Boolean keepRecord;
    private Boolean keepMedia;
    private LocalDateTime detectedAt;
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Integer getAwarenessRecordId() { return awarenessRecordId; }
    public void setAwarenessRecordId(Integer awarenessRecordId) { this.awarenessRecordId = awarenessRecordId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getAnalysisMode() { return analysisMode; }
    public void setAnalysisMode(String analysisMode) { this.analysisMode = analysisMode; }
    public String getModelConfiguration() { return modelConfiguration; }
    public void setModelConfiguration(String modelConfiguration) { this.modelConfiguration = modelConfiguration; }
    public Integer getEmotionTypeCount() { return emotionTypeCount; }
    public void setEmotionTypeCount(Integer emotionTypeCount) { this.emotionTypeCount = emotionTypeCount; }
    public Integer getBfrbEventCount() { return bfrbEventCount; }
    public void setBfrbEventCount(Integer bfrbEventCount) { this.bfrbEventCount = bfrbEventCount; }
    public BigDecimal getBfrbTotalDurationSeconds() { return bfrbTotalDurationSeconds; }
    public void setBfrbTotalDurationSeconds(BigDecimal bfrbTotalDurationSeconds) { this.bfrbTotalDurationSeconds = bfrbTotalDurationSeconds; }
    public String getEvidenceTypes() { return evidenceTypes; }
    public void setEvidenceTypes(String evidenceTypes) { this.evidenceTypes = evidenceTypes; }
    public String getComplaint() { return complaint; }
    public void setComplaint(String complaint) { this.complaint = complaint; }
    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
    public String getAggregationRulesJson() { return aggregationRulesJson; }
    public void setAggregationRulesJson(String aggregationRulesJson) { this.aggregationRulesJson = aggregationRulesJson; }
    public String getInputMedia() { return inputMedia; }
    public void setInputMedia(String inputMedia) { this.inputMedia = inputMedia; }
    public String getOutputMedia() { return outputMedia; }
    public void setOutputMedia(String outputMedia) { this.outputMedia = outputMedia; }
    public Boolean getKeepRecord() { return keepRecord; }
    public void setKeepRecord(Boolean keepRecord) { this.keepRecord = keepRecord; }
    public Boolean getKeepMedia() { return keepMedia; }
    public void setKeepMedia(Boolean keepMedia) { this.keepMedia = keepMedia; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
