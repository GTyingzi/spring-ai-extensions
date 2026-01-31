package com.alibaba.cloud.ai.dashscope.audio.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yingzi
 * @since 2026/1/29
 */

public class DashScopeAudioTTSModel {

    public static class DashScopeAudioTTSRequest {
        @JsonProperty("model")
        private String model;

        @JsonProperty("input")
        private TTSInput input;

        public DashScopeAudioTTSRequest(String model, String text, String voice, String languageType) {
            this.model = model;
            this.input = new TTSInput();
            this.input.text = text;
            this.input.voice = voice;
            this.input.languageType = languageType;
        }

        private class TTSInput {
            @JsonProperty("text")
            private String text;

            @JsonProperty("voice")
            private String voice;

            @JsonProperty("language_type")
            private String languageType;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String model;
            private String text;
            private String voice;
            private String languageType;

            public Builder model(String model) {
                this.model = model;
                return this;
            }

            public Builder text(String text) {
                this.text = text;
                return this;
            }

            public Builder voice(String voice) {
                this.voice = voice;
                return this;
            }

            public Builder languageType(String languageType) {
                this.languageType = languageType;
                return this;
            }

            public DashScopeAudioTTSRequest build() {
                return new DashScopeAudioTTSRequest(model, text, voice, languageType);
            }
        }

    }


    public record DashScopeAudioTTSResponse(
            @JsonProperty("request_id") String requestId,
            @JsonProperty("output") TTSOutput output,
            @JsonProperty("result") TTSResult result
            ) {

        public record TTSOutput(
                @JsonProperty("finish_reason") String finishReason,
                @JsonProperty("audio") TTSAudio audio
            ) {
        }

        public record TTSResult (
           @JsonProperty("input_tokens") Integer inputTokens,
           @JsonProperty("output_tokens") Integer outputTokens,
           @JsonProperty("total_tokens") Integer totalTokens,
           @JsonProperty("characters") Integer characters,
           @JsonProperty("input_tokens_details") TTSInputTokensDetails inputTokensDetails,
           @JsonProperty("output_tokens_details") TTSTokenDetails outputTokensDetails
        ) {}

        public record TTSAudio(
                @JsonProperty("data") String data,
                @JsonProperty("url") String url,
                @JsonProperty("id") String id,
                @JsonProperty("expires_at") Integer expiresAt
             ) {
        }

        public record TTSInputTokensDetails(
                @JsonProperty("text_tokens") Integer textTokens
        ) {}

        public record TTSTokenDetails(
                @JsonProperty("text_tokens") Integer textTokens,
                @JsonProperty("audio_tokens") Integer audioTokens
        ) {
        }

    }

}
