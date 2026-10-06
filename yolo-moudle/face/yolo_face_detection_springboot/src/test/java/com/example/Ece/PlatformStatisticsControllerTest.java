package com.example.Ece;

import com.alibaba.fastjson.JSONObject;
import com.example.Ece.entity.AwarenessRecord;
import com.example.Ece.entity.User;
import com.example.Ece.mapper.AwarenessRecordMapper;
import com.example.Ece.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlatformStatisticsControllerTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    UserMapper userMapper;
    @Autowired
    AwarenessRecordMapper awarenessRecordMapper;

    @Test
    void returnsPersistentAggregateCountsWithoutPersonalDetails() throws Exception {
        JSONObject before = overview();

        User participant = new User();
        participant.setUsername("statistics-user-" + UUID.randomUUID());
        participant.setPassword("test-only");
        participant.setRole("common");
        userMapper.insert(participant);

        User administrator = new User();
        administrator.setUsername("statistics-admin-" + UUID.randomUUID());
        administrator.setPassword("test-only");
        administrator.setRole("admin");
        userMapper.insert(administrator);

        insertAwareness(participant.getUsername(), "image");
        insertAwareness(participant.getUsername(), "video");
        insertAwareness(participant.getUsername(), "camera");

        JSONObject after = overview();
        assertEquals(before.getLongValue("experienceUsers") + 1, after.getLongValue("experienceUsers"));
        assertEquals(before.getLongValue("totalAwareness") + 3, after.getLongValue("totalAwareness"));
        assertEquals(before.getLongValue("imageCount") + 1, after.getLongValue("imageCount"));
        assertEquals(before.getLongValue("videoCount") + 1, after.getLongValue("videoCount"));
        assertEquals(before.getLongValue("cameraCount") + 1, after.getLongValue("cameraCount"));
        assertNotNull(after.getString("updatedAt"));
        assertFalse(after.containsKey("username"));
    }

    private JSONObject overview() throws Exception {
        MvcResult result = mvc.perform(get("/statistics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();
        return JSONObject.parseObject(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .getJSONObject("data");
    }

    private void insertAwareness(String username, String sourceType) {
        AwarenessRecord record = new AwarenessRecord();
        record.setUsername(username);
        record.setSourceType(sourceType);
        record.setEmotionLabel("neutral");
        record.setKeepRecord(true);
        record.setKeepMedia(false);
        record.setCreatedAt(LocalDateTime.now());
        awarenessRecordMapper.insert(record);
    }
}
