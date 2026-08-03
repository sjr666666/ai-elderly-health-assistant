package com.example.backend.model.dto;

import java.util.List;
import java.util.Map;

/**
 * 药品追问请求
 */
public class FollowUpQuestionRequest {

    private DrugDetailResponse drugDetail;
    private String question;
    private List<Map<String, String>> conversationHistory;

    public DrugDetailResponse getDrugDetail() {
        return drugDetail;
    }

    public void setDrugDetail(DrugDetailResponse drugDetail) {
        this.drugDetail = drugDetail;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<Map<String, String>> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<Map<String, String>> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }
}
