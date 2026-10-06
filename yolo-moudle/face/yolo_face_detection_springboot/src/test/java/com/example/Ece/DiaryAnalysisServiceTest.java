package com.example.Ece;

import com.alibaba.fastjson.JSONObject;
import com.example.Ece.service.DiaryAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DiaryAnalysisServiceTest {
    @Test void remoteRequestUsesServerKeyAndReturnsValidatedSpecificAnalysis() {
        DiaryAnalysisService service=new DiaryAnalysisService();ReflectionTestUtils.setField(service,"key","test-only-not-a-real-key");ReflectionTestUtils.setField(service,"url","http://127.0.0.1:1/test");ReflectionTestUtils.setField(service,"model","test-model");
        RestTemplate client=(RestTemplate)ReflectionTestUtils.getField(service,"client");MockRestServiceServer server=MockRestServiceServer.bindTo(client).build();
        String text="开会前紧张，午后和朋友散步轻松了一些。";
        JSONObject analysis=JSONObject.parseObject("{\"summary\":\"紧张与轻松出现在同一天。\",\"emotions\":[{\"label\":\"紧张\",\"evidence\":\"开会前紧张\",\"reflection\":\"也许重要的会议让你有所在意。\"}],\"suggestion\":\"下次开会前先记下第一句话。\",\"blessing\":\"愿午后那段和朋友散步的时间，在你下次开会前也留一点空间。\",\"safetyNote\":\"\"}");
        JSONObject response=new JSONObject();response.put("choices",Arrays.asList(Collections.singletonMap("message",Collections.singletonMap("content",analysis.toJSONString()))));
        server.expect(requestTo("http://127.0.0.1:1/test")).andExpect(header("Authorization","Bearer test-only-not-a-real-key")).andExpect(content().string(org.hamcrest.Matchers.containsString(text))).andRespond(withSuccess(response.toJSONString(),MediaType.APPLICATION_JSON));
        Map<String,Object> snapshot=new LinkedHashMap<>();snapshot.put("date","2026-10-06");snapshot.put("content",text);snapshot.put("moods",Collections.emptyList());
        JSONObject result=service.analyze(snapshot);assertEquals("deepseek",result.getString("provider"));assertEquals("test-model",result.getString("model"));assertFalse(result.toJSONString().contains("test-only-not-a-real-key"));server.verify();
    }
    @Test void missingKeyFailsExplicitlyWithoutAnyNetworkRequest() {
        DiaryAnalysisService service=new DiaryAnalysisService();ReflectionTestUtils.setField(service,"key","");assertFalse(service.configured());assertThrows(DiaryAnalysisService.AnalysisUnavailable.class,()->service.analyze(Collections.emptyMap()));
    }
    @Test void malformedProviderReplyIsNotStoredAsAnAnalysis() {
        DiaryAnalysisService service=new DiaryAnalysisService();ReflectionTestUtils.setField(service,"key","test-only");ReflectionTestUtils.setField(service,"url","http://127.0.0.1:1/test");
        MockRestServiceServer server=MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(service,"client")).build();server.expect(requestTo("http://127.0.0.1:1/test")).andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"not json\"}}]}",MediaType.APPLICATION_JSON));
        assertThrows(DiaryAnalysisService.AnalysisUnavailable.class,()->service.analyze(Collections.singletonMap("content","日记内容")));server.verify();
    }
}
