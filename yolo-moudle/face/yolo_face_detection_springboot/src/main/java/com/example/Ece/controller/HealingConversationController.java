package com.example.Ece.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Ece.common.Result;
import com.example.Ece.entity.HealingConversation;
import com.example.Ece.mapper.HealingConversationMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/healingConversations")
public class HealingConversationController {
    @Resource
    HealingConversationMapper healingConversationMapper;

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String username) {
        LambdaQueryWrapper<HealingConversation> wrapper = Wrappers.<HealingConversation>lambdaQuery();
        wrapper.orderByDesc(HealingConversation::getCreatedAt);
        if (username != null && username.trim().length() > 0) {
            wrapper.like(HealingConversation::getUsername, username);
        }
        return Result.success(healingConversationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @PostMapping
    public Result<?> save(@RequestBody HealingConversation conversation) {
        if (conversation.getCreatedAt() == null) {
            conversation.setCreatedAt(LocalDateTime.now());
        }
        healingConversationMapper.insert(conversation);
        return Result.success(conversation);
    }
}
