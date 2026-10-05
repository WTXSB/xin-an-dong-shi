package com.example.Ece;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaConnectControllerTest {
    private static final String ADMIN = "demo";
    private static final String PRACTITIONER = "doctor.lin";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String applyPatient(String username, String realName) throws Exception {
        return mockMvc.perform(post("/spa/identity/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"identityType\":\"patient\","
                                + "\"realName\":\"" + realName + "\",\"idCard\":\"110101199003077713\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();
    }

    private void approve(String username) throws Exception {
        mockMvc.perform(post("/spa/identity/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adminUsername\":\"" + ADMIN + "\",\"username\":\"" + username
                                + "\",\"approve\":true,\"note\":\"资料清晰，欢迎加入\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.auditStatus").value("approved"));
    }

    private long createRequest(String patientUsername) throws Exception {
        String response = mockMvc.perform(post("/spa/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientUsername\":\"" + patientUsername + "\","
                                + "\"practitionerUsername\":\"" + PRACTITIONER + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    @Test
    void applyPatientIdentityThenMineIsPendingAndReapplyResetsToPending() throws Exception {
        String username = unique("spa-pat");

        applyPatient(username, "小安");

        mockMvc.perform(get("/spa/identity/mine").param("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.auditStatus").value("pending"))
                .andExpect(jsonPath("$.data.realName").value("小安"))
                // 身份证号只保存掩码，绝不明文返回
                .andExpect(jsonPath("$.data.idCardMasked").value("110***********7713"));

        // 重复提交会覆盖资料并重新进入待审核
        applyPatient(username, "安小然");
        mockMvc.perform(get("/spa/identity/mine").param("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditStatus").value("pending"))
                .andExpect(jsonPath("$.data.realName").value("安小然"));

        // 身份证号格式不正确时拒绝提交
        mockMvc.perform(post("/spa/identity/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + unique("spa-bad") + "\",\"identityType\":\"patient\","
                                + "\"realName\":\"小安\",\"idCard\":\"12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"));

        mockMvc.perform(get("/spa/identity/mine").param("username", unique("spa-none")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void nonAdminCannotReviewAndAdminApprovalMarksApproved() throws Exception {
        String username = unique("spa-pat");
        applyPatient(username, "小安");

        // 非管理员不能看待审核列表，也不能审核
        mockMvc.perform(get("/spa/identity/pending").param("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"));
        mockMvc.perform(post("/spa/identity/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adminUsername\":\"" + username + "\",\"username\":\"" + username
                                + "\",\"approve\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"));

        // 管理员可以看到待审核列表并完成审核
        mockMvc.perform(get("/spa/identity/pending").param("username", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
        approve(username);
        mockMvc.perform(get("/spa/identity/mine").param("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditStatus").value("approved"));

        // 心灵SPA师列表只展示已认证的公开信息，不含资格证号与身份证号
        mockMvc.perform(get("/spa/practitioners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data[0].username").value(PRACTITIONER))
                .andExpect(jsonPath("$.data[0].realName").isNotEmpty())
                .andExpect(jsonPath("$.data[0].licenseNo").doesNotExist())
                .andExpect(jsonPath("$.data[0].idCardMasked").doesNotExist());
    }

    @Test
    void requestRequiresApprovedPatientAndSucceedsAfterApproval() throws Exception {
        String username = unique("spa-pat");
        applyPatient(username, "小安");

        // 未完成认证时不能发起联动
        mockMvc.perform(post("/spa/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientUsername\":\"" + username + "\","
                                + "\"practitionerUsername\":\"" + PRACTITIONER + "\","
                                + "\"initialMessage\":\"最近心里有点闷，想找人聊聊\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"));

        approve(username);

        String response = mockMvc.perform(post("/spa/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientUsername\":\"" + username + "\","
                                + "\"practitionerUsername\":\"" + PRACTITIONER + "\","
                                + "\"initialMessage\":\"最近心里有点闷，想找人聊聊\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.counterpart.realName").value("林安宁"))
                .andExpect(jsonPath("$.data.counterpart.hospital").value("武汉心安医院"))
                .andExpect(jsonPath("$.data.latestMessage.content").value("最近心里有点闷，想找人聊聊"))
                .andReturn().getResponse().getContentAsString();
        long requestId = objectMapper.readTree(response).path("data").path("id").asLong();

        // 双方视角都能看到这张联动单
        mockMvc.perform(get("/spa/requests/mine").param("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get("/spa/requests/mine").param("username", PRACTITIONER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data[?(@.id == " + requestId + ")]").isNotEmpty());

        // 没有认证记录的用户看到的是空列表
        mockMvc.perform(get("/spa/requests/mine").param("username", unique("spa-none")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void messagesCanBeExchangedIncrementallyAndThirdPartyIsRejected() throws Exception {
        String username = unique("spa-pat");
        applyPatient(username, "小安");
        approve(username);
        long requestId = createRequest(username);

        String first = mockMvc.perform(post("/spa/requests/{id}/messages", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"content\":\"你好，我最近睡得不太好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.senderIdentity").value("patient"))
                .andReturn().getResponse().getContentAsString();
        long firstMessageId = objectMapper.readTree(first).path("data").path("id").asLong();

        mockMvc.perform(post("/spa/requests/{id}/messages", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + PRACTITIONER + "\",\"content\":\"我在，慢慢说，不着急\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.senderIdentity").value("practitioner"));

        // 全量读取两条留言，按 id 升序
        mockMvc.perform(get("/spa/requests/{id}/messages", requestId)
                        .param("username", username).param("afterId", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].content").value("你好，我最近睡得不太好"));

        // afterId 增量读取只剩新的一条
        mockMvc.perform(get("/spa/requests/{id}/messages", requestId)
                        .param("username", PRACTITIONER).param("afterId", String.valueOf(firstMessageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].content").value("我在，慢慢说，不着急"));

        // 第三方不能读也不能写
        String outsider = unique("spa-out");
        mockMvc.perform(get("/spa/requests/{id}/messages", requestId)
                        .param("username", outsider).param("afterId", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"));
        mockMvc.perform(post("/spa/requests/{id}/messages", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + outsider + "\",\"content\":\"打扰一下\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"));
    }

    @Test
    void practitionerCanViewPatientRecordsButOthersCannot() throws Exception {
        String username = unique("spa-pat");
        applyPatient(username, "小安");
        approve(username);
        long requestId = createRequest(username);

        // 为记录者留下一条觉察记录
        mockMvc.perform(post("/awarenessRecords/fromPrediction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"sourceType\":\"video\","
                                + "\"emotionLabel\":\"sad\",\"keepRecord\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        // 该单的心灵SPA师可以查看对方的觉察记录
        mockMvc.perform(get("/spa/requests/{id}/patient-records", requestId)
                        .param("username", PRACTITIONER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].emotionLabel").value("sad"));

        // 记录者本人与第三方都不能通过这个入口查看
        mockMvc.perform(get("/spa/requests/{id}/patient-records", requestId)
                        .param("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"));
        mockMvc.perform(get("/spa/requests/{id}/patient-records", requestId)
                        .param("username", unique("spa-out")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"));
    }
}
