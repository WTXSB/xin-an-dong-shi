package com.example.Ece.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Ece.entity.AwarenessRecord;
import com.example.Ece.entity.User;
import com.example.Ece.mapper.AwarenessRecordMapper;
import com.example.Ece.mapper.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Produces the public, privacy-safe platform totals shown on the home page.
 * The values are derived from persisted rows instead of session memory so a
 * refresh, logout or backend restart cannot reset them.
 */
@Service
public class PlatformStatisticsService {
    private static final String[] NON_EXPERIENCE_ROLES = {"admin", "system"};

    @Resource
    UserMapper userMapper;
    @Resource
    AwarenessRecordMapper awarenessRecordMapper;

    public Map<String, Object> overview() {
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.and(wrapper -> wrapper.isNull("role")
                .or()
                .notIn("role", Arrays.asList(NON_EXPERIENCE_ROLES)));

        long experienceUsers = countUsers(userQuery);
        long imageCount = countAwareness("image");
        long videoCount = countAwareness("video");
        long cameraCount = countAwareness("camera");
        long totalAwareness = countAwareness(null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("experienceUsers", experienceUsers);
        data.put("totalAwareness", totalAwareness);
        data.put("imageCount", imageCount);
        data.put("videoCount", videoCount);
        data.put("cameraCount", cameraCount);
        data.put("updatedAt", LocalDateTime.now());
        return data;
    }

    private long countUsers(QueryWrapper<User> query) {
        Integer count = userMapper.selectCount(query);
        return count == null ? 0L : count.longValue();
    }

    private long countAwareness(String sourceType) {
        QueryWrapper<AwarenessRecord> query = new QueryWrapper<>();
        if (sourceType != null) {
            query.eq("source_type", sourceType);
        }
        Integer count = awarenessRecordMapper.selectCount(query);
        return count == null ? 0L : count.longValue();
    }
}
