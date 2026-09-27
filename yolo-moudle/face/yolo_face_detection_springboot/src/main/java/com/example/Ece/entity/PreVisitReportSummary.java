package com.example.Ece.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("previsit_report_summaries")
public class PreVisitReportSummary {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer awarenessRecordId;
    private Integer analysisRecordId;
    private String provider;
    private String modelName;
    private String promptVersion;
    private String objectiveSummary;
    private String clinicianQuestionsJson;
    private String visitPreparationJson;
    private String safetyNote;
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
    public String getObjectiveSummary() { return objectiveSummary; }
    public void setObjectiveSummary(String objectiveSummary) { this.objectiveSummary = objectiveSummary; }
    public String getClinicianQuestionsJson() { return clinicianQuestionsJson; }
    public void setClinicianQuestionsJson(String clinicianQuestionsJson) { this.clinicianQuestionsJson = clinicianQuestionsJson; }
    public String getVisitPreparationJson() { return visitPreparationJson; }
    public void setVisitPreparationJson(String visitPreparationJson) { this.visitPreparationJson = visitPreparationJson; }
    public String getSafetyNote() { return safetyNote; }
    public void setSafetyNote(String safetyNote) { this.safetyNote = safetyNote; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
