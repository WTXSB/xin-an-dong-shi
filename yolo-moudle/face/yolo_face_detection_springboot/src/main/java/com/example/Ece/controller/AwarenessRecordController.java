package com.example.Ece.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Ece.common.Result;
import com.example.Ece.entity.AwarenessRecord;
import com.example.Ece.mapper.AwarenessRecordMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/awarenessRecords")
public class AwarenessRecordController {
    @Resource
    AwarenessRecordMapper awarenessRecordMapper;

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String username,
                              @RequestParam(defaultValue = "") String sourceType,
                              @RequestParam(defaultValue = "") String emotionLabel) {
        LambdaQueryWrapper<AwarenessRecord> wrapper = Wrappers.<AwarenessRecord>lambdaQuery();
        wrapper.orderByDesc(AwarenessRecord::getCreatedAt);
        if (StrUtil.isNotBlank(username)) {
            wrapper.like(AwarenessRecord::getUsername, username);
        }
        if (StrUtil.isNotBlank(sourceType)) {
            wrapper.eq(AwarenessRecord::getSourceType, sourceType);
        }
        if (StrUtil.isNotBlank(emotionLabel)) {
            wrapper.like(AwarenessRecord::getEmotionLabel, emotionLabel);
        }
        return Result.success(awarenessRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable int id) {
        return Result.success(awarenessRecordMapper.selectById(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody AwarenessRecord record) {
        normalizeRecord(record);
        if (Boolean.FALSE.equals(record.getKeepRecord())) {
            Map<String, Object> data = new HashMap<>();
            data.put("saved", false);
            data.put("message", "已尊重你的选择，这次觉察不会保存为记录。");
            return Result.success(data);
        }

        awarenessRecordMapper.insert(record);
        Map<String, Object> data = new HashMap<>();
        data.put("saved", true);
        data.put("id", record.getId());
        data.put("gentleSummary", record.getGentleSummary());
        data.put("suggestedPractice", record.getSuggestedPractice());
        return Result.success(data);
    }

    @PostMapping("/fromPrediction")
    public Result<?> saveFromPrediction(@RequestBody AwarenessRecord record) {
        normalizeRecord(record);
        fillGentleText(record);
        return save(record);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable int id) {
        awarenessRecordMapper.deleteById(id);
        return Result.success();
    }

    private void normalizeRecord(AwarenessRecord record) {
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(LocalDateTime.now());
        }
        if (record.getKeepRecord() == null) {
            record.setKeepRecord(true);
        }
        if (record.getKeepMedia() == null) {
            record.setKeepMedia(false);
        }
        if (StrUtil.isBlank(record.getSourceType())) {
            record.setSourceType("image");
        }
        if (StrUtil.isBlank(record.getPrivacyNote())) {
            record.setPrivacyNote("仅在你选择保存时留下觉察记录；素材是否保留由你单独决定。");
        }
    }

    private void fillGentleText(AwarenessRecord record) {
        String emotion = StrUtil.blankToDefault(record.getEmotionLabel(), "暂未形成清晰倾向");
        String source = friendlySource(record.getSourceType());

        if (StrUtil.isBlank(record.getGentleSummary())) {
            record.setGentleSummary("这次" + source + "里，系统捕捉到的主要情绪倾向是“" + emotion
                    + "”。这只是一个帮助你觉察当下状态的参考，不代表对你的定义。");
        }
        if (StrUtil.isBlank(record.getBodySignal())) {
            record.setBodySignal("可以轻轻留意呼吸、肩颈、胃部和手心的紧绷程度，看看身体正在提醒你什么。");
        }
        if (StrUtil.isBlank(record.getSuggestedPractice())) {
            record.setSuggestedPractice("先做三轮慢呼吸：吸气 4 秒，停留 2 秒，呼气 6 秒。然后问问自己：我现在最需要的是休息、支持，还是把任务拆小一点？");
        }
    }

    private String friendlySource(String sourceType) {
        if ("video".equalsIgnoreCase(sourceType)) {
            return "视频觉察";
        }
        if ("camera".equalsIgnoreCase(sourceType)) {
            return "摄像头实时觉察";
        }
        return "图片觉察";
    }
}
