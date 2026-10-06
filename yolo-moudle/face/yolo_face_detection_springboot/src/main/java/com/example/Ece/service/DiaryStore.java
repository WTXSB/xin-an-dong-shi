package com.example.Ece.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Account-scoped storage. Single-node synchronization plus optimistic revisions prevent stale overwrites. */
@Service
public class DiaryStore {
    @Resource private JdbcTemplate jdbc;
    public static final List<String> PERIODS = Arrays.asList("morning", "afternoon", "evening");
    private Clock clock = Clock.systemUTC();
    public LocalDate today(int offset) {
        if (offset < -840 || offset > 720) throw new IllegalArgumentException("时区格式不正确");
        return LocalDate.now(clock.withZone(ZoneOffset.ofTotalSeconds(-offset * 60)));
    }
    public String period(int offset) {
        today(offset);
        int hour = LocalTime.now(clock.withZone(ZoneOffset.ofTotalSeconds(-offset * 60))).getHour();
        return hour < 12 ? "morning" : hour < 18 ? "afternoon" : "evening";
    }
    public LocalDate date(String value, int offset) {
        LocalDate day;
        try { day = LocalDate.parse(value); } catch (Exception e) { throw new IllegalArgumentException("日期格式不正确"); }
        if (day.getYear() < 1900 || day.isAfter(today(offset))) throw new IllegalArgumentException("不能提前记录未来的心情或日记");
        return day;
    }
    public Map<String,Object> claim(String username, int offset) {
        String date = today(offset).toString(), period = period(offset);
        int changed;
        try { changed = jdbc.update("INSERT INTO diary_moods (username,entry_date,period,prompt_shown) VALUES (?,?,?,TRUE)", username,date,period); }
        catch (DuplicateKeyException e) { changed = jdbc.update("UPDATE diary_moods SET prompt_shown=TRUE WHERE username=? AND entry_date=? AND period=? AND prompt_shown=FALSE", username,date,period); }
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("show",changed == 1); result.put("date",date); result.put("period",period);return result;
    }
    public List<Map<String,Object>> month(String username, String value) {
        YearMonth month;
        try { month = YearMonth.parse(value); } catch (Exception e) { throw new IllegalArgumentException("月份格式不正确"); }
        return jdbc.query("SELECT entry_date,period,score FROM diary_moods WHERE username=? AND entry_date BETWEEN ? AND ? AND score IS NOT NULL ORDER BY entry_date,period",
            (rs,n) -> { Map<String,Object> m=new LinkedHashMap<>();m.put("date",rs.getString(1));m.put("period",rs.getString(2));m.put("score",rs.getInt(3));return m; }, username,month.atDay(1).toString(),month.atEndOfMonth().toString());
    }
    private void ensure(String username,String date) {
        try { jdbc.update("INSERT INTO emotion_diaries (username,entry_date) VALUES (?,?)",username,date); }
        catch (DuplicateKeyException ignored) { /* Never reset an existing diary. */ }
    }
    public Map<String,Object> read(String username,String date) {
        List<Map<String,Object>> rows=jdbc.query("SELECT entry_date,content,revision,analysis_json,updated_at FROM emotion_diaries WHERE username=? AND entry_date=?",
            (rs,n) -> { Map<String,Object> m=new LinkedHashMap<>();m.put("date",rs.getString(1));m.put("content",rs.getString(2));m.put("revision",rs.getInt(3));m.put("analysis",rs.getString(4)==null?null:JSONObject.parseObject(rs.getString(4)));m.put("updatedAt",rs.getTimestamp(5).toLocalDateTime().toString());return m; }, username,date);
        if (!rows.isEmpty()) return rows.get(0);
        Map<String,Object> blank=new LinkedHashMap<>();blank.put("date",date);blank.put("content","");blank.put("revision",0);blank.put("analysis",null);return blank;
    }
    public synchronized Map<String,Object> saveMood(String username,String date,String period,int score,int offset) {
        LocalDate day=date(date,offset);
        if (!PERIODS.contains(period) || score<1 || score>10) throw new IllegalArgumentException("请选择 1–10 分的心情和正确时段");
        if (day.equals(today(offset)) && PERIODS.indexOf(period)>PERIODS.indexOf(period(offset))) throw new IllegalArgumentException("这个时段还没有到，可以稍后再记录");
        ensure(username,date);
        List<Integer> old=jdbc.query("SELECT score FROM diary_moods WHERE username=? AND entry_date=? AND period=?",(rs,n)->(Integer)rs.getObject(1),username,date,period);
        if (!old.isEmpty() && Objects.equals(old.get(0),score)) return read(username,date);
        int updated=jdbc.update("UPDATE diary_moods SET score=?,updated_at=CURRENT_TIMESTAMP WHERE username=? AND entry_date=? AND period=?",score,username,date,period);
        if(updated==0) {
            try { jdbc.update("INSERT INTO diary_moods (username,entry_date,period,score,prompt_shown) VALUES (?,?,?,?,FALSE)",username,date,period,score); }
            catch (DuplicateKeyException e) {
                // A simultaneous prompt claim may have created the row; preserve its shown flag.
                jdbc.update("UPDATE diary_moods SET score=?,updated_at=CURRENT_TIMESTAMP WHERE username=? AND entry_date=? AND period=?",score,username,date,period);
            }
        }
        jdbc.update("UPDATE emotion_diaries SET analysis_json=NULL,analysis_fingerprint=NULL,revision=revision+1,updated_at=CURRENT_TIMESTAMP WHERE username=? AND entry_date=?",username,date);
        return read(username,date);
    }
    public synchronized Map<String,Object> save(String username,String date,String content,int revision,int offset) {
        date(date,offset);
        if(content.length()>10000) throw new IllegalArgumentException("日记最多保存 10000 字");
        ensure(username,date);Map<String,Object> old=read(username,date);
        if(((Number)old.get("revision")).intValue()!=revision) throw new DiaryConflict();
        if(!Objects.equals(old.get("content"),content)) jdbc.update("UPDATE emotion_diaries SET content=?,revision=revision+1,analysis_json=NULL,analysis_fingerprint=NULL,updated_at=CURRENT_TIMESTAMP WHERE username=? AND entry_date=?",content,username,date);
        return read(username,date);
    }
    public synchronized Map<String,Object> snapshot(String username,String date,int revision,int offset) {
        date(date,offset);Map<String,Object> entry=read(username,date);
        if(((Number)entry.get("revision")).intValue()!=revision) throw new DiaryConflict();
        if(String.valueOf(entry.get("content")).trim().isEmpty()) throw new IllegalArgumentException("请先写下一些今天的事情，再分析日记");
        List<Map<String,Object>> moods=month(username,date.substring(0,7));moods.removeIf(m -> !date.equals(m.get("date")));
        entry.put("moods",moods);entry.put("fingerprint",fingerprint(String.valueOf(entry.get("content"))+JSONObject.toJSONString(moods)));return entry;
    }
    public synchronized Map<String,Object> complete(String username,String date,int revision,String fingerprint,JSONObject analysis,int offset) {
        Map<String,Object> current=snapshot(username,date,revision,offset);
        if(!fingerprint.equals(current.get("fingerprint"))) throw new DiaryConflict();
        int changed=jdbc.update("UPDATE emotion_diaries SET analysis_json=?,analysis_fingerprint=? WHERE username=? AND entry_date=? AND revision=?",analysis.toJSONString(),fingerprint,username,date,revision);
        if(changed!=1) throw new DiaryConflict();return read(username,date);
    }
    public List<Map<String,Object>> insights(String username,int page) {
        if(page<1 || page>10000) throw new IllegalArgumentException("页码不正确");
        List<String> dates=jdbc.query("SELECT entry_date FROM emotion_diaries WHERE username=? AND analysis_json IS NOT NULL ORDER BY entry_date DESC LIMIT 6 OFFSET ?",(rs,n)->rs.getString(1),username,(page-1)*6);
        List<Map<String,Object>> result=new ArrayList<>();for(String date:dates) result.add(read(username,date));return result;
    }
    private String fingerprint(String text) {
        try { byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format("%02x",b));return out.toString(); }
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public static class DiaryConflict extends RuntimeException { public DiaryConflict(){super("这一天的记录已更新。请重新加载后再保存，避免覆盖另一窗口的内容。");} }
}
