package com.example.Ece.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("awareness_records")
public class AwarenessRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String sourceType;
    private String emotionLabel;
    private String confidence;
    private String bodySignal;
    private String gentleSummary;
    private String suggestedPractice;
    private String inputMedia;
    private String outputMedia;
    private Boolean keepRecord;
    private Boolean keepMedia;
    private String privacyNote;
    private LocalDateTime createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getEmotionLabel() {
        return emotionLabel;
    }

    public void setEmotionLabel(String emotionLabel) {
        this.emotionLabel = emotionLabel;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getBodySignal() {
        return bodySignal;
    }

    public void setBodySignal(String bodySignal) {
        this.bodySignal = bodySignal;
    }

    public String getGentleSummary() {
        return gentleSummary;
    }

    public void setGentleSummary(String gentleSummary) {
        this.gentleSummary = gentleSummary;
    }

    public String getSuggestedPractice() {
        return suggestedPractice;
    }

    public void setSuggestedPractice(String suggestedPractice) {
        this.suggestedPractice = suggestedPractice;
    }

    public String getInputMedia() {
        return inputMedia;
    }

    public void setInputMedia(String inputMedia) {
        this.inputMedia = inputMedia;
    }

    public String getOutputMedia() {
        return outputMedia;
    }

    public void setOutputMedia(String outputMedia) {
        this.outputMedia = outputMedia;
    }

    public Boolean getKeepRecord() {
        return keepRecord;
    }

    public void setKeepRecord(Boolean keepRecord) {
        this.keepRecord = keepRecord;
    }

    public Boolean getKeepMedia() {
        return keepMedia;
    }

    public void setKeepMedia(Boolean keepMedia) {
        this.keepMedia = keepMedia;
    }

    public String getPrivacyNote() {
        return privacyNote;
    }

    public void setPrivacyNote(String privacyNote) {
        this.privacyNote = privacyNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
