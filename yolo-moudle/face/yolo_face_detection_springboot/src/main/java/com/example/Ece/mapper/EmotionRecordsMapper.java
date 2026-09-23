package com.example.Ece.mapper;

import com.example.Ece.entity.Emotion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface EmotionRecordsMapper extends BaseMapper<Emotion> {

    /**
     * 获取近N天的情绪识别趋势数据
     * @param cutoff 起始时间（由 Java 计算，兼容 H2 与 MySQL）
     * @param days 天数（同时作为最大返回行数）
     * @return 包含日期、总识别次数、积极情绪次数的Map列表
     */
    @Select({
            "SELECT ",
            "    DATE(start_time) as date, ",
            "    COUNT(*) as total_count, ",
            "    SUM(CASE WHEN emotion_kind = '高兴' THEN 1 ELSE 0 END) as positive_count ",
            "FROM emotionrecords ",
            "WHERE start_time >= #{cutoff} ",
            "GROUP BY DATE(start_time) ",
            "ORDER BY date DESC ",
            "LIMIT #{days}"
    })
    List<Map<String, Object>> getRecentEmotionTrend(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("days") int days);

    /**
     * 获取情绪类型占比分析
     * @return 情绪类型和对应数量
     */
    @Select({
            "SELECT ",
            "    emotion_kind as emotion, ",
            "    COUNT(*) as count ",
            "FROM emotionrecords ",
            "GROUP BY emotion_kind ",
            "ORDER BY count DESC"
    })
    List<Map<String, Object>> getEmotionCategoryAnalysis();

    /**
     * 获取今日识别统计数据
     * @return 包含今日识别总量、积极情绪、消极情绪、中性情绪的Map
     */
    @Select({
            "SELECT ",
            "    COUNT(*) as total_recognitions, ",
            "    SUM(CASE WHEN emotion_kind = '高兴' THEN 1 ELSE 0 END) as positive_emotions, ",
            "    SUM(CASE WHEN emotion_kind IN ('悲伤', '生气') THEN 1 ELSE 0 END) as negative_emotions, ",
            "    SUM(CASE WHEN emotion_kind = '中性' THEN 1 ELSE 0 END) as neutral_emotions ",
            "FROM emotionrecords ",
            "WHERE DATE(start_time) = CURDATE()"
    })
    Map<String, Object> getTodayStatistics();
}
