package com.alibaba.cloud.ai.dashscope.audio.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yingzi
 * @since 2026/1/26
 */

public class AudioCommonType {

    public enum TextType {

        // @formatter:off
        @JsonProperty("PlainText") PLAIN_TEXT("PlainText"),
        @JsonProperty("SSML") SSML("SSML");
        // @formatter:on

        private final String value;

        TextType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

    public enum Format {

        @JsonProperty("pcm") PCM("pcm"),
        @JsonProperty("wav") WAV("wav"),
        @JsonProperty("mp3") MP3("mp3");

        public final String formatType;

        Format(String value) {
            this.formatType = value;
        }

        public String getValue() {
            return this.formatType;
        }

    }
}
