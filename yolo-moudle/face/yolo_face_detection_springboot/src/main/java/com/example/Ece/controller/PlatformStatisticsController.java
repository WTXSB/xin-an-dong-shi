package com.example.Ece.controller;

import com.example.Ece.common.Result;
import com.example.Ece.service.PlatformStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/statistics")
public class PlatformStatisticsController {
    @Resource
    PlatformStatisticsService statisticsService;

    @GetMapping("/overview")
    public Result<?> overview() {
        return Result.success(statisticsService.overview());
    }
}
