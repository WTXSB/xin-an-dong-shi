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

@RestController
@RequestMapping("/privacyConsents")
public class PrivacyConsentController {
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
        if (consent.getCreatedAt() == null) {
            consent.setCreatedAt(LocalDateTime.now());
        }
        if (consent.getAgreed() == null) {
            consent.setAgreed(false);
        }
        if (consent.getKeepRecord() == null) {
            consent.setKeepRecord(false);
        }
        if (consent.getKeepMedia() == null) {
            consent.setKeepMedia(false);
        }
        privacyConsentMapper.insert(consent);
        return Result.success(consent);
    }
}
