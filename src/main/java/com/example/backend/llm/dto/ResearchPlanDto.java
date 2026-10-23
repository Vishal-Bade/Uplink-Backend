package com.example.backend.llm.dto;

import java.util.ArrayList;
import java.util.List;

public class ResearchPlanDto {
    private String topic;
    private String hypothesis;
    private List<SubQuestion> subquestions = new ArrayList<>();

    public ResearchPlanDto() {}

    public ResearchPlanDto(String topic, String hypothesis, List<SubQuestion> subquestions) {
        this.topic = topic;
        this.hypothesis = hypothesis;
        this.subquestions = subquestions;
    }

    public static class SubQuestion {
        private String id;
        private String question;
        private List<String> searchQueries = new ArrayList<>();
        private String focusArea;

        public SubQuestion() {}

        public SubQuestion(String id, String question, List<String> searchQueries, String focusArea) {
            this.id = id;
            this.question = question;
            this.searchQueries = searchQueries;
            this.focusArea = focusArea;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }

        public List<String> getSearchQueries() { return searchQueries; }
        public void setSearchQueries(List<String> searchQueries) { this.searchQueries = searchQueries; }

        public String getFocusArea() { return focusArea; }
        public void setFocusArea(String focusArea) { this.focusArea = focusArea; }
    }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getHypothesis() { return hypothesis; }
    public void setHypothesis(String hypothesis) { this.hypothesis = hypothesis; }

    public List<SubQuestion> getSubquestions() { return subquestions; }
    public void setSubquestions(List<SubQuestion> subquestions) { this.subquestions = subquestions; }
}
