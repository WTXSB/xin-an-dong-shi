package com.example.Ece.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.Ece.entity.VideoRecords;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VideoRecordsMapper extends BaseMapper<VideoRecords> {
    @Select("SELECT DATE(start_time) AS date, COUNT(*) AS count " +
            "FROM videorecords " +
            "WHERE start_time >= #{startDate} " +
            "GROUP BY DATE(start_time) " +
            "ORDER BY date DESC")
    List<Map<String, Object>> findDailyCountsSince(@Param("startDate") LocalDateTime startDate);
}
