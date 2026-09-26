package com.example.Ece.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("detection_bfrb_events")
public class DetectionBfrbEvent {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer analysisRecordId;
    private String eventKey;
    private String behaviorCode;
    private String cueType;
    private BigDecimal startSeconds;
    private BigDecimal endSeconds;
    private BigDecimal durationSeconds;
    private Integer sampleCount;
    private BigDecimal averageConfidence;
    private BigDecimal maxConfidence;
    private BigDecimal keyFrameSeconds;
    private String evidenceType;
    private String keyBboxJson;
    private String geometryJson;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getAnalysisRecordId() { return analysisRecordId; }
    public void setAnalysisRecordId(Integer analysisRecordId) { this.analysisRecordId = analysisRecordId; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public String getBehaviorCode() { return behaviorCode; }
    public void setBehaviorCode(String behaviorCode) { this.behaviorCode = behaviorCode; }
    public String getCueType() { return cueType; }
    public void setCueType(String cueType) { this.cueType = cueType; }
    public BigDecimal getStartSeconds() { return startSeconds; }
    public void setStartSeconds(BigDecimal startSeconds) { this.startSeconds = startSeconds; }
    public BigDecimal getEndSeconds() { return endSeconds; }
    public void setEndSeconds(BigDecimal endSeconds) { this.endSeconds = endSeconds; }
    public BigDecimal getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(BigDecimal durationSeconds) { this.durationSeconds = durationSeconds; }
    public Integer getSampleCount() { return sampleCount; }
    public void setSampleCount(Integer sampleCount) { this.sampleCount = sampleCount; }
    public BigDecimal getAverageConfidence() { return averageConfidence; }
    public void setAverageConfidence(BigDecimal averageConfidence) { this.averageConfidence = averageConfidence; }
    public BigDecimal getMaxConfidence() { return maxConfidence; }
    public void setMaxConfidence(BigDecimal maxConfidence) { this.maxConfidence = maxConfidence; }
    public BigDecimal getKeyFrameSeconds() { return keyFrameSeconds; }
    public void setKeyFrameSeconds(BigDecimal keyFrameSeconds) { this.keyFrameSeconds = keyFrameSeconds; }
    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }
    public String getKeyBboxJson() { return keyBboxJson; }
    public void setKeyBboxJson(String keyBboxJson) { this.keyBboxJson = keyBboxJson; }
    public String getGeometryJson() { return geometryJson; }
    public void setGeometryJson(String geometryJson) { this.geometryJson = geometryJson; }
}
