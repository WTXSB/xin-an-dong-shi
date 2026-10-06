package com.example.Ece.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.Ece.common.Result;
import com.example.Ece.service.DiaryStore;
import com.example.Ece.service.DiaryAnalysisService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.function.Supplier;

@RestController
@RequestMapping("/diary")
public class DiaryController {
    @Resource private DiaryStore store;
    @Resource private DiaryAnalysisService ai;
    private String user(HttpServletRequest request){HttpSession s=request.getSession(false);Object u=s==null?null:s.getAttribute("diaryUsername");if(!(u instanceof String))throw new LoginRequired();return (String)u;}
    private Result<?> run(Supplier<Object> action){try{return Result.success(action.get());}catch(LoginRequired e){return Result.error("403","请重新登录，以安全访问你的私人日记。");}catch(DiaryStore.DiaryConflict e){return Result.error("409",e.getMessage());}catch(DiaryAnalysisService.AnalysisUnavailable e){return Result.error("503",e.getMessage());}catch(IllegalArgumentException e){return Result.error("400",e.getMessage());}}
    private int integer(JSONObject p,String key){try{Object n=p.get(key);if(!(n instanceof Number)||((Number)n).doubleValue()!=((Number)n).intValue())throw new IllegalArgumentException();return ((Number)n).intValue();}catch(Exception e){throw new IllegalArgumentException("缺少或无效的 "+key);}}
    @GetMapping("/status") public Result<?> status(HttpServletRequest r){return run(()->{user(r);return Collections.singletonMap("aiConfigured",ai.configured());});}
    @PostMapping("/prompt") public Result<?> prompt(HttpServletRequest r,@RequestBody JSONObject p){return run(()->store.claim(user(r),integer(p,"timezoneOffset")));}
    @GetMapping("/month") public Result<?> month(HttpServletRequest r,@RequestParam String month){return run(()->store.month(user(r),month));}
    @GetMapping("/entry") public Result<?> entry(HttpServletRequest r,@RequestParam String date,@RequestParam int timezoneOffset){return run(()->{String u=user(r);store.date(date,timezoneOffset);return store.read(u,date);});}
    @PostMapping("/mood") public Result<?> mood(HttpServletRequest r,@RequestBody JSONObject p){return run(()->store.saveMood(user(r),p.getString("date"),p.getString("period"),integer(p,"score"),integer(p,"timezoneOffset")));}
    @PutMapping("/entry") public Result<?> save(HttpServletRequest r,@RequestBody JSONObject p){return run(()->{String u=user(r);Object content=p.get("content");if(!(content instanceof String))throw new IllegalArgumentException("日记内容格式不正确");return store.save(u,p.getString("date"),(String)content,integer(p,"revision"),integer(p,"timezoneOffset"));});}
    @PostMapping("/analyze") public Result<?> analyze(HttpServletRequest r,@RequestBody JSONObject p){return run(()->{
        String u=user(r);if(!Boolean.TRUE.equals(p.getBoolean("consent")))throw new IllegalArgumentException("请先确认将这一天的日记与心情评分发送给 AI");
        int revision=integer(p,"revision"),offset=integer(p,"timezoneOffset");String date=p.getString("date");Map<String,Object> snapshot=store.snapshot(u,date,revision,offset);
        if(snapshot.get("analysis")!=null)return store.read(u,date);
        JSONObject analysis=ai.analyze(snapshot);return store.complete(u,date,revision,String.valueOf(snapshot.get("fingerprint")),analysis,offset);
    });}
    @GetMapping("/insights") public Result<?> insights(HttpServletRequest r,@RequestParam(defaultValue="1") int page){return run(()->store.insights(user(r),page));}
    private static class LoginRequired extends RuntimeException {}
}
