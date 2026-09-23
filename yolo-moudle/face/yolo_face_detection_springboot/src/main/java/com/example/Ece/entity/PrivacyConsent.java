package com.example.Ece.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("privacy_consents")
public class PrivacyConsent {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String scene;
    private String consentType;
    private String consentText;
    private Boolean agreed;
    private Boolean keepRecord;
    private Boolean keepMedia;
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

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getConsentType() {
        return consentType;
    }

    public void setConsentType(String consentType) {
        this.consentType = consentType;
    }

    public String getConsentText() {
        return consentText;
    }

    public void setConsentText(String consentText) {
        this.consentText = consentText;
    }

    public Boolean getAgreed() {
        return agreed;
    }

    public void setAgreed(Boolean agreed) {
        this.agreed = agreed;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
