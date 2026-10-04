package com.example.Ece;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrivacyConsentControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void cameraConsentIsBoundToUserAndReturnsServerApprovedRetentionChoices() throws Exception {
        String payload = "{"
                + "\"username\":\"privacy-user\","
                + "\"scene\":\"camera\","
                + "\"consentType\":\"camera-recognition\","
                + "\"consentText\":\"camera privacy notice v2\","
                + "\"agreed\":true,\"keepRecord\":false,\"keepMedia\":true}";

        MvcResult saved = mockMvc.perform(post("/privacyConsents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.keepMedia").value(false))
                .andReturn();

        String body = saved.getResponse().getContentAsString();
        String id = body.replaceAll(".*\\\"id\\\":([0-9]+).*", "$1");
        mockMvc.perform(get("/privacyConsents/{id}/validate", id)
                        .param("username", "privacy-user")
                        .param("scene", "camera"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.keepRecord").value(false))
                .andExpect(jsonPath("$.data.keepMedia").value(false));

        mockMvc.perform(get("/privacyConsents/{id}/validate", id)
                        .param("username", "another-user")
                        .param("scene", "camera"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"));
    }

    @Test
    void declinedConsentCannotStartCameraInference() throws Exception {
        String payload = "{"
                + "\"username\":\"declined-user\","
                + "\"scene\":\"camera\","
                + "\"consentType\":\"camera-recognition\","
                + "\"agreed\":false}";
        MvcResult saved = mockMvc.perform(post("/privacyConsents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        String body = saved.getResponse().getContentAsString();
        String id = body.replaceAll(".*\\\"id\\\":([0-9]+).*", "$1");

        mockMvc.perform(get("/privacyConsents/{id}/validate", id)
                        .param("username", "declined-user")
                        .param("scene", "camera"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"));
    }
}
