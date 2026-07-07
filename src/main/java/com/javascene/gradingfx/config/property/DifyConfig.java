package com.javascene.gradingfx.config.property;

import lombok.Data;

@Data
public class DifyConfig {
    private ApiConfig api = new ApiConfig();

    @Data
    public static class ApiConfig {
        private String apiKey;
        private String baseUrl;

        public String getWorkflowUrl() {
            return baseUrl + "/workflows/run";
        }

        public String getChatUrl() {
            return baseUrl + "/chat-messages";
        }
    }
}