package com.example.Ece.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Ece.common.Result;
import com.example.Ece.entity.ImgRecords;
import com.example.Ece.mapper.ImgRecordsMapper;
import com.example.Ece.mapper.VideoRecordsMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/imgRecords")
public class ImgRecordsController {
    @Resource
    ImgRecordsMapper imgRecordsMapper;
    @Resource
    VideoRecordsMapper videoRecordsMapper;

    @GetMapping("/all")
    public Result<?> GetAll() {
        return Result.success(imgRecordsMapper.selectList(null));
    }
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable int id) {
        System.out.println(id);
        return Result.success(imgRecordsMapper.selectById(id));
    }

    @GetMapping
    public Result<?> findPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String search1,
            @RequestParam(defaultValue = "") String search3,
            @RequestParam(defaultValue = "") String search2) {

        LambdaQueryWrapper<ImgRecords> wrapper = Wrappers.<ImgRecords>lambdaQuery();
        wrapper.orderByDesc(ImgRecords::getStartTime);

        // 1. 调试输出原始搜索词
        System.out.println("Original search2: " + search2);

        // 2. 处理中文搜索词
        if (StrUtil.isNotBlank(search2)) {
            // 将中文转换为Unicode转义序列格式
            String unicodeSearch = convertToUnicodeFormat(search2);
            System.out.println("Converted search: " + unicodeSearch);


            // 使用JSON格式进行查询
            wrapper.like(ImgRecords::getLabel, unicodeSearch);
        }

        if (StrUtil.isNotBlank(search)) {
            wrapper.like(ImgRecords::getUsername, search);
        }

        if (StrUtil.isNotBlank(search1)) {
            wrapper.like(ImgRecords::getKind, search1);
        }

        if (StrUtil.isNotBlank(search3)) {
            wrapper.like(ImgRecords::getConf, search3);
        }

        System.out.println("Final SQL: " + wrapper.getSqlSegment());

        Page<ImgRecords> page = imgRecordsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(page);
    }

    // 将中文字符转换为Unicode转义序列格式
    private String convertToUnicodeFormat(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c > 127) { // 非ASCII字符
                sb.append("\\u").append(Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable int id) {
        imgRecordsMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<?> updates(@RequestBody ImgRecords imgrecords) {
        imgRecordsMapper.updateById(imgrecords);
        return Result.success();
    }


    @PostMapping
    public Result<?> save(@RequestBody ImgRecords imgrecords) {
        System.out.println(imgrecords);
        imgRecordsMapper.insert(imgrecords);
        return Result.success();
    }

    @GetMapping("/recent")
    public Result<Map<String, Object>> getRecentData(@RequestParam(name = "days", defaultValue = "10") int days) {
// 计算起始日期（包含今天共days天）
        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        // 获取两个表的数据
        List<Map<String, Object>> imgRecordsResults = imgRecordsMapper.findDailyCountsSince(startDateTime);
        List<Map<String, Object>> newTableResults = videoRecordsMapper.findDailyCountsSince(startDateTime);

        // 合并两个表的数据
        Map<LocalDate, Long> combinedCountMap = new HashMap<>();
        addResultsToMap(imgRecordsResults, combinedCountMap);
        addResultsToMap(newTableResults, combinedCountMap);

        // 填充完整日期范围（即使某天没有数据也设为0）
        Map<LocalDate, Long> filledData = new LinkedHashMap<>();
        LocalDate currentDate = startDate;
        LocalDate endDate = LocalDate.now();

        while (!currentDate.isAfter(endDate)) {
            filledData.put(currentDate, combinedCountMap.getOrDefault(currentDate, 0L));
            currentDate = currentDate.plusDays(1);
        }

        // 转换为前端需要的格式
        List<Map<String, Object>> formattedData = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : filledData.entrySet()) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", entry.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE));
            dayData.put("count", entry.getValue());
            formattedData.add(dayData);
        }

        // 构建返回结果
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("days", days);
        resultMap.put("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        resultMap.put("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        resultMap.put("dailyData", formattedData);

        return Result.success(resultMap);
    }
    private void addResultsToMap(List<Map<String, Object>> results, Map<LocalDate, Long> targetMap) {
        for (Map<String, Object> result : results) {
            try {
                Object dateObj = result.get("date");
                LocalDate date = null;

                // 处理不同类型的日期返回格式
                if (dateObj instanceof java.sql.Date) {
                    date = ((java.sql.Date) dateObj).toLocalDate();
                } else if (dateObj instanceof String) {
                    date = LocalDate.parse((String) dateObj, DateTimeFormatter.ISO_LOCAL_DATE);
                } else if (dateObj instanceof java.util.Date) {
                    date = new java.sql.Date(((java.util.Date) dateObj).getTime()).toLocalDate();
                }

                if (date != null) {
                    // 处理count字段类型不同的问题（Long/Integer）
                    Object countObj = result.get("count");
                    Long count = countObj != null ?
                            (countObj instanceof Long ? (Long) countObj :
                                    countObj instanceof Integer ? ((Integer) countObj).longValue() : 0L)
                            : 0L;

                    // 如果已有该日期的数据，则累加
                    Long existingCount = targetMap.getOrDefault(date, 0L);
                    targetMap.put(date, existingCount + count);
                }
            } catch (Exception e) {
                e.printStackTrace(); // 添加异常日志
            }
        }
    }

}
