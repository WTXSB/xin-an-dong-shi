package com.example.Ece.dto;

import com.example.Ece.entity.DetectionAnalysisRecord;
import com.example.Ece.entity.DetectionBfrbEvent;
import com.example.Ece.entity.DetectionEmotionResult;

import java.util.ArrayList;
import java.util.List;

public class DetectionAnalysisPayload extends DetectionAnalysisRecord {
    private List<DetectionEmotionResult> emotionResults = new ArrayList<>();
    private List<DetectionBfrbEvent> bfrbEvents = new ArrayList<>();

    public List<DetectionEmotionResult> getEmotionResults() {
        return emotionResults;
    }

    public void setEmotionResults(List<DetectionEmotionResult> emotionResults) {
        this.emotionResults = emotionResults == null ? new ArrayList<>() : emotionResults;
    }

    public List<DetectionBfrbEvent> getBfrbEvents() {
        return bfrbEvents;
    }

    public void setBfrbEvents(List<DetectionBfrbEvent> bfrbEvents) {
        this.bfrbEvents = bfrbEvents == null ? new ArrayList<>() : bfrbEvents;
    }
}
