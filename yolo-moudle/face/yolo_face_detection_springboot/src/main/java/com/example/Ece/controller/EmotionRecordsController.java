package com.example.Ece.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Ece.common.Result;
import com.example.Ece.entity.Emotion;
import com.example.Ece.mapper.EmotionRecordsMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/emotion")
public class EmotionRecordsController {
    @Resource
    EmotionRecordsMapper emotionRecordsMapper;

    /**
     * 获取近N天的情绪识别趋势
     */
    @GetMapping("/recent")
    public Result<?> getRecentTrend(@RequestParam(defaultValue = "10") int days) {
        try {
            // 1. 从数据库获取原始数据
            LocalDateTime cutoff = LocalDate.now().minusDays(days - 1L).atStartOfDay();
            List<Map<String, Object>> trendData = emotionRecordsMapper.getRecentEmotionTrend(cutoff, days);

            // 2. 将数据库数据转换为 Map，方便根据日期快速查找
            // Key: 日期字符串 (MM-dd), Value: 数据对象
            Map<String, Map<String, Object>> dataMap = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

            for (Map<String, Object> item : trendData) {
                Object dateObj = item.get("date");
                if (dateObj != null) {
                    // 注意：这里要确保数据库出来的日期格式解析正确
                    // 假设数据库出来的是 java.sql.Date 或 String (yyyy-MM-dd)
                    String dateStr = dateObj.toString();
                    // 统一转为 MM-dd 格式作为 Key
                    LocalDate date = LocalDate.parse(dateStr);
                    dataMap.put(date.format(formatter), item);
                }
            }

            // 3. 初始化返回结果容器
            List<String> dates = new ArrayList<>();
            List<Integer> totalData = new ArrayList<>();
            List<Integer> positiveData = new ArrayList<>();
            List<Integer> negativeData = new ArrayList<>();

            // 4. 生成完整的日期序列（从 days-1 天前 到 今天）
            // 例如 days=10, 循环从 9 递减到 0
            LocalDate today = LocalDate.now();

            for (int i = days - 1; i >= 0; i--) {
                LocalDate calcDate = today.minusDays(i);
                String dateKey = calcDate.format(formatter);

                dates.add(dateKey); // 添加日期轴标签

                // 检查该日期是否有数据
                if (dataMap.containsKey(dateKey)) {
                    Map<String, Object> data = dataMap.get(dateKey);

                    // 安全处理数值转换 (防止 BigDecimal/Long 类型转换错误)
                    Object totalObj = data.get("total_count");
                    Object positiveObj = data.get("positive_count");

                    int total = totalObj == null ? 0 : Integer.parseInt(totalObj.toString());
                    int positive = positiveObj == null ? 0 : Integer.parseInt(positiveObj.toString());

                    totalData.add(total);
                    positiveData.add(positive);

                    // 计算消极情绪 (需要额外查询或在SQL中一并查出，这里沿用你之前的逻辑单独查)
                    // 优化建议：最好在 SQL getRecentEmotionTrend 里直接把悲伤+愤怒算出来，避免循环查库
                    // 但为了保持兼容，这里还是调用方法：
                    Long negativeCount = Long.valueOf(getNegativeEmotionCount(calcDate));
                    negativeData.add(negativeCount.intValue());
                } else {
                    // 没有数据，补 0
                    totalData.add(0);
                    positiveData.add(0);
                    negativeData.add(0);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("dates", dates);
            result.put("totalData", totalData);
            result.put("positiveData", positiveData);
            result.put("negativeData", negativeData);

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "获取趋势数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取情绪类型占比分析
     */
    @GetMapping("/categories")
    public Result<?> getEmotionCategories() {
        try {
            List<Map<String, Object>> categoryData = emotionRecordsMapper.getEmotionCategoryAnalysis();

            // 处理返回数据格式
            List<Map<String, Object>> emotionData = categoryData.stream()
                    .map(data -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("emotion", data.get("emotion"));

                        // 修复：通过 toString() 再解析，兼容 Long 和 BigDecimal
                        Object countObj = data.get("count");
                        int count = countObj == null ? 0 : Integer.parseInt(countObj.toString());

                        item.put("count", count);

                        return item;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("emotionData", emotionData);

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "获取情绪分类数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日统计数据
     */
    @GetMapping("/today")
    public Result<?> getTodayStatistics() {
        try {
            Map<String, Object> statistics = emotionRecordsMapper.getTodayStatistics();

            // 修复：处理所有数值类型，防止类型转换错误
            Map<String, Object> result = new HashMap<>();

            if (statistics != null) {
                for (Map.Entry<String, Object> entry : statistics.entrySet()) {
                    Object val = entry.getValue();
                    if (val instanceof Number) {
                        // 统一转为 int 或 long 并在前端展示，这里使用 intValue()
                        result.put(entry.getKey(), ((Number) val).intValue());
                    } else {
                        // 如果是 null 或者其他类型，尝试转换或保留
                        result.put(entry.getKey(), val == null ? 0 : val);
                    }
                }
            }

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "获取今日统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定日期的消极情绪数量
     */
    private Integer getNegativeEmotionCount(LocalDate date) {
        LambdaQueryWrapper<Emotion> wrapper = Wrappers.<Emotion>lambdaQuery();
        wrapper.between(Emotion::getStartTime,
                        date.atStartOfDay(),
                        date.atTime(23, 59, 59))
                .and(w -> w.eq(Emotion::getEmotionKind, "悲伤")
                        .or()
                        .eq(Emotion::getEmotionKind, "生气"));
        return emotionRecordsMapper.selectCount(wrapper);
    }

    /**
     * 分页查询区域信息
     */
    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String emotionKind,
                              @RequestParam(defaultValue = "") String txt,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime) {
        LambdaQueryWrapper<Emotion> wrapper = Wrappers.<Emotion>lambdaQuery();
        wrapper.orderByDesc(Emotion::getStartTime);
        if (startTime != null) {
            wrapper.ge(Emotion::getStartTime, startTime);
        }
        if (StrUtil.isNotBlank(emotionKind)) {
            wrapper.like(Emotion::getEmotionKind, emotionKind);
        }
        if (StrUtil.isNotBlank(txt)) {
            wrapper.like(Emotion::getTxt, txt);
        }
        Page<Emotion> areaPage = emotionRecordsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(areaPage);
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody Emotion emotion) {
        emotionRecordsMapper.updateById(emotion);
        return Result.success();
    }

    /**
     * 删除信息
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Integer id) {
        emotionRecordsMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping
    public Result<?> save(@RequestBody Emotion emotion) {
        emotionRecordsMapper.insert(emotion);
        return Result.success();
    }
}
