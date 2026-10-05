package com.example.Ece.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("care_letter_summaries")
public class CareLetterSummary {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer awarenessRecordId;
    private Integer analysisRecordId;
    private String provider;
    private String modelName;
    private String promptVersion;
    private String letterText;
    private LocalDateTime generatedAt;
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getAwarenessRecordId() { return awarenessRecordId; }
    public void setAwarenessRecordId(Integer awarenessRecordId) { this.awarenessRecordId = awarenessRecordId; }
    public Integer getAnalysisRecordId() { return analysisRecordId; }
    public void setAnalysisRecordId(Integer analysisRecordId) { this.analysisRecordId = analysisRecordId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getLetterText() { return letterText; }
    public void setLetterText(String letterText) { this.letterText = letterText; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
