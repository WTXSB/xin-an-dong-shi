package com.example.Ece.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@TableName("emotionrecords")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Emotion {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String emotionKind;
    private String txt;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    // 对于JSON数据，使用@JsonFormat指定格式和时区
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmotionKind() {
        return emotionKind;
    }

    public void setEmotionKind(String emotionKind) {
        this.emotionKind = emotionKind;
    }

    public String getTxt() {
        return txt;
    }

    public void setTxt(String txt) {
        this.txt = txt;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
}
