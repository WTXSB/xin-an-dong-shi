package com.example.Ece.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("detection_emotion_results")
public class DetectionEmotionResult {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer analysisRecordId;
    private String emotionType;
    private Integer frameCount;
    private BigDecimal averageConfidence;
    private BigDecimal maxConfidence;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getAnalysisRecordId() { return analysisRecordId; }
    public void setAnalysisRecordId(Integer analysisRecordId) { this.analysisRecordId = analysisRecordId; }
    public String getEmotionType() { return emotionType; }
    public void setEmotionType(String emotionType) { this.emotionType = emotionType; }
    public Integer getFrameCount() { return frameCount; }
    public void setFrameCount(Integer frameCount) { this.frameCount = frameCount; }
    public BigDecimal getAverageConfidence() { return averageConfidence; }
    public void setAverageConfidence(BigDecimal averageConfidence) { this.averageConfidence = averageConfidence; }
    public BigDecimal getMaxConfidence() { return maxConfidence; }
    public void setMaxConfidence(BigDecimal maxConfidence) { this.maxConfidence = maxConfidence; }
}
