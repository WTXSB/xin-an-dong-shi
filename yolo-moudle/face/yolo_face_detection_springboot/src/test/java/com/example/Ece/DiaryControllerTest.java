package com.example.Ece;

import com.alibaba.fastjson.JSONObject;
import com.example.Ece.service.DiaryStore;
import com.example.Ece.service.DiaryAnalysisService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiaryControllerTest {
    @Autowired MockMvc mvc;
    @Autowired DiaryStore store;
    @MockBean DiaryAnalysisService ai;
    MockHttpSession session;
    final String date="2026-10-06";
    @BeforeEach void setup(){session=new MockHttpSession();session.setAttribute("diaryUsername","diary-test-"+UUID.randomUUID());at("2026-10-06T01:00:00Z");when(ai.configured()).thenReturn(true);}
    @AfterEach void resetClock(){ReflectionTestUtils.setField(store,"clock",Clock.systemUTC());}
    void at(String instant){ReflectionTestUtils.setField(store,"clock",Clock.fixed(Instant.parse(instant),ZoneOffset.UTC));}
    JSONObject call(MockHttpServletRequestBuilder req,String body) throws Exception{
        MvcResult r=mvc.perform(req.session(session).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();return JSONObject.parseObject(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
    String entry(String date,String content,int revision){JSONObject p=new JSONObject();p.put("date",date);p.put("content",content);p.put("revision",revision);p.put("timezoneOffset",-480);return p.toJSONString();}
    JSONObject save(String day,String text,int revision)throws Exception{return call(put("/diary/entry"),entry(day,text,revision));}
    JSONObject mood(String period,int score)throws Exception{return call(post("/diary/mood"),"{\"date\":\""+date+"\",\"period\":\""+period+"\",\"score\":"+score+",\"timezoneOffset\":-480}");}
    JSONObject prompt()throws Exception{return call(post("/diary/prompt"),"{\"timezoneOffset\":-480}");}
    String analyze(int revision,boolean consent){return "{\"date\":\""+date+"\",\"revision\":"+revision+",\"timezoneOffset\":-480,\"consent\":"+consent+"}";}
    JSONObject validAnalysis(){JSONObject a=new JSONObject();a.put("summary","会议前的不安与散步后的轻松可以同时存在。");a.put("emotions",Arrays.asList(new JSONObject(new LinkedHashMap<String,Object>(){{put("label","不安");put("evidence","会议前有点紧张");put("reflection","对重要时刻的在意可能带来不安。");}})));a.put("suggestion","明天开会前先把要说的第一句写在纸上。");a.put("blessing","愿明天会议前的你，也能想起散步时那段不需要赶路的时间。");a.put("provider","deepseek");a.put("model","test-model");a.put("safetyNote","");return a;}
    @Test void promptsOncePerPeriodAndPerDayEvenAfterDismissal() throws Exception {
        assertTrue(prompt().getJSONObject("data").getBooleanValue("show"));assertFalse(prompt().getJSONObject("data").getBooleanValue("show"));
        at("2026-10-06T05:00:00Z");assertEquals("afternoon",prompt().getJSONObject("data").getString("period"));assertFalse(prompt().getJSONObject("data").getBooleanValue("show"));
        at("2026-10-06T11:00:00Z");assertTrue(prompt().getJSONObject("data").getBooleanValue("show"));assertFalse(prompt().getJSONObject("data").getBooleanValue("show"));
        at("2026-10-07T01:00:00Z");assertTrue(prompt().getJSONObject("data").getBooleanValue("show"));
    }
    @Test void threeRatingsPersistIndependentlyAndEditingDoesNotDuplicate() throws Exception {
        assertEquals("0",mood("morning",3).getString("code"));at("2026-10-06T05:00:00Z");mood("afternoon",6);at("2026-10-06T11:00:00Z");mood("evening",8);mood("morning",4);
        JSONObject response=call(get("/diary/month").param("month","2026-10"),"");assertEquals(3,response.getJSONArray("data").size());
        assertTrue(response.getJSONArray("data").stream().anyMatch(o->((JSONObject)o).getString("period").equals("morning")&&((JSONObject)o).getIntValue("score")==4));
    }
    @Test void keepsPastDiariesAcrossNewDaysAndSupportsEditingPastDate() throws Exception {
        save("2026-10-05","昨天的小事",0);save(date,"今天的记录",0);save("2026-10-05","昨天的小事与补充",1);
        JSONObject old=call(get("/diary/entry").param("date","2026-10-05").param("timezoneOffset","-480"),"");assertEquals("昨天的小事与补充",old.getJSONObject("data").getString("content"));
        assertEquals("今天的记录",call(get("/diary/entry").param("date",date).param("timezoneOffset","-480"),"").getJSONObject("data").getString("content"));
    }
    @Test void rejectsFutureSlotsDatesAndInvalidScores() throws Exception {
        assertEquals("400",mood("evening",8).getString("code"));assertEquals("400",mood("morning",11).getString("code"));assertEquals("400",save("2026-10-07","future",0).getString("code"));
        assertEquals("400",call(post("/diary/prompt"),"{\"timezoneOffset\":9999}").getString("code"));
    }
    @Test void sessionOwnershipIgnoresSpoofedUsernameAndDeniesAnonymousAccess() throws Exception {
        save(date,"私密记录",0);MockHttpSession owner=session;session=new MockHttpSession();session.setAttribute("diaryUsername","another-"+UUID.randomUUID());
        JSONObject empty=call(get("/diary/entry").param("date",date).param("timezoneOffset","-480").param("username",String.valueOf(owner.getAttribute("diaryUsername"))),"");assertEquals("",empty.getJSONObject("data").getString("content"));
        mvc.perform(get("/diary/month").param("month","2026-10")).andExpect(jsonPath("$.code").value("403"));
    }
    @Test void rejectsStaleSaveWithoutLosingOriginalText() throws Exception {
        save(date,"原文",0);assertEquals("409",save(date,"过期覆盖",0).getString("code"));assertEquals("原文",call(get("/diary/entry").param("date",date).param("timezoneOffset","-480"),"").getJSONObject("data").getString("content"));
    }
    @Test void analysisRequiresExplicitConsentAndNeverRunsOnSave() throws Exception {
        save(date,"会议前有点紧张",0);assertEquals("400",call(post("/diary/analyze"),analyze(1,false)).getString("code"));verify(ai,never()).analyze(anyMap());
    }
    @Test void completeAnalysisFlowsToInsightsAndIsCachedUntilContentOrMoodChanges() throws Exception {
        save(date,"会议前有点紧张，散步后轻松一些。",0);when(ai.analyze(anyMap())).thenReturn(validAnalysis());
        assertEquals("0",call(post("/diary/analyze"),analyze(1,true)).getString("code"));call(post("/diary/analyze"),analyze(1,true));verify(ai,times(1)).analyze(anyMap());
        assertEquals(1,call(get("/diary/insights"),"").getJSONArray("data").size());mood("morning",4);
        assertEquals(0,call(get("/diary/insights"),"").getJSONArray("data").size());assertEquals("409",call(post("/diary/analyze"),analyze(1,true)).getString("code"));
    }
    @Test void aiFailurePreservesDiaryAndDoesNotCreateFakeInsights() throws Exception {
        save(date,"会议前有点紧张",0);when(ai.analyze(anyMap())).thenThrow(new DiaryAnalysisService.AnalysisUnavailable("AI 尚未连接"));
        assertEquals("503",call(post("/diary/analyze"),analyze(1,true)).getString("code"));assertEquals(0,call(get("/diary/insights"),"").getJSONArray("data").size());
        assertEquals("会议前有点紧张",call(get("/diary/entry").param("date",date).param("timezoneOffset","-480"),"").getJSONObject("data").getString("content"));
    }
    @Test void textEditedDuringAnalysisCannotReceiveStaleAnalysis() throws Exception {
        save(date,"会议前有点紧张",0);String username=String.valueOf(session.getAttribute("diaryUsername"));
        when(ai.analyze(anyMap())).thenAnswer(invocation->{store.save(username,date,"会议前有点紧张，但结束后松了一口气。",1,-480);return validAnalysis();});
        assertEquals("409",call(post("/diary/analyze"),analyze(1,true)).getString("code"));assertEquals(0,call(get("/diary/insights"),"").getJSONArray("data").size());
    }
    @Test void validatesProviderOutputAndRejectsInventedQuotes() {
        JSONObject good=validAnalysis();assertNotNull(DiaryAnalysisService.validate(good,"会议前有点紧张，散步后轻松一些。"));
        good.getJSONArray("emotions").getJSONObject(0).put("evidence","根本没有发生的事情");assertThrows(IllegalArgumentException.class,()->DiaryAnalysisService.validate(good,"会议前有点紧张"));
    }
    @Test void successfulLoginCreatesPrivateSessionAndLogoutInvalidatesIt() throws Exception {
        MvcResult logged=mvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"demo\",\"password\":\"123456\"}")).andExpect(jsonPath("$.code").value("0")).andReturn();
        MockHttpSession signed=(MockHttpSession)logged.getRequest().getSession(false);assertEquals("demo",signed.getAttribute("diaryUsername"));
        mvc.perform(post("/user/logout").session(signed)).andExpect(jsonPath("$.code").value("0"));assertTrue(signed.isInvalid());
    }
}
