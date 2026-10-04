package com.example.Ece.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Ece.common.Result;
import com.example.Ece.entity.PrivacyConsent;
import com.example.Ece.mapper.PrivacyConsentMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/privacyConsents")
public class PrivacyConsentController {
    private static final int CAMERA_CONSENT_VALID_MINUTES = 10;

    @Resource
    PrivacyConsentMapper privacyConsentMapper;

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String username,
                              @RequestParam(defaultValue = "") String scene) {
        LambdaQueryWrapper<PrivacyConsent> wrapper = Wrappers.<PrivacyConsent>lambdaQuery();
        wrapper.orderByDesc(PrivacyConsent::getCreatedAt);
        if (username != null && username.trim().length() > 0) {
            wrapper.like(PrivacyConsent::getUsername, username);
        }
        if (scene != null && scene.trim().length() > 0) {
            wrapper.eq(PrivacyConsent::getScene, scene);
        }
        return Result.success(privacyConsentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @PostMapping
    public Result<?> save(@RequestBody PrivacyConsent consent) {
        if (consent.getUsername() == null || consent.getUsername().trim().isEmpty()) {
            return Result.error("400", "授权用户不能为空");
        }
        if (consent.getScene() == null || consent.getScene().trim().isEmpty()) {
            return Result.error("400", "授权场景不能为空");
        }
        if (consent.getConsentType() == null || consent.getConsentType().trim().isEmpty()) {
            return Result.error("400", "授权类型不能为空");
        }
        consent.setUsername(consent.getUsername().trim());
        consent.setScene(consent.getScene().trim().toLowerCase());
        consent.setConsentType(consent.getConsentType().trim());
        // The server owns the timestamp so a caller cannot forge a future date
        // and keep a short-lived camera consent valid indefinitely.
        consent.setCreatedAt(LocalDateTime.now());
        if (consent.getAgreed() == null) {
            consent.setAgreed(false);
        }
        if (consent.getKeepRecord() == null) {
            consent.setKeepRecord(false);
        }
        if (consent.getKeepMedia() == null) {
            consent.setKeepMedia(false);
        }
        if (!Boolean.TRUE.equals(consent.getKeepRecord())) {
            consent.setKeepMedia(false);
        }
        privacyConsentMapper.insert(consent);
        return Result.success(consent);
    }

    /**
     * Server-side gate for camera inference. A consent row is deliberately short lived
     * and bound to one user and one scene, so the inference service cannot be started
     * by merely forging query parameters in the browser.
     */
    @GetMapping("/{id}/validate")
    public Result<?> validate(@PathVariable Integer id,
                              @RequestParam String username,
                              @RequestParam(defaultValue = "camera") String scene) {
        PrivacyConsent consent = privacyConsentMapper.selectById(id);
        if (consent == null) {
            return Result.error("403", "未找到有效的摄像头授权");
        }
        boolean identityMatches = consent.getUsername() != null
                && consent.getUsername().equals(username == null ? "" : username.trim());
        boolean sceneMatches = consent.getScene() != null
                && consent.getScene().equalsIgnoreCase(scene == null ? "" : scene.trim());
        boolean typeMatches = "camera-recognition".equals(consent.getConsentType());
        boolean notExpired = consent.getCreatedAt() != null
                && !consent.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(CAMERA_CONSENT_VALID_MINUTES));
        if (!Boolean.TRUE.equals(consent.getAgreed()) || !identityMatches || !sceneMatches || !typeMatches || !notExpired) {
            return Result.error("403", "摄像头授权无效、已过期或与当前用户不匹配");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", true);
        result.put("consentId", consent.getId());
        result.put("keepRecord", Boolean.TRUE.equals(consent.getKeepRecord()));
        result.put("keepMedia", Boolean.TRUE.equals(consent.getKeepMedia()));
        result.put("expiresAt", consent.getCreatedAt().plusMinutes(CAMERA_CONSENT_VALID_MINUTES));
        return Result.success(result);
    }
}
