package com.alibaba.cloud.ai.dashscope.audio.transcription;

import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeTranscriptionResponse.Translation.Word;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;

import java.util.List;

/**
 * @author yingzi
 * @since 2026/2/1
 */

public class DashScopeTranscriptionResponse extends AudioTranscriptionResponse {

    private final List<Translation> translations;

    private final List<Transcription> transcription;

    public DashScopeTranscriptionResponse(List<Translation> translations, List<Transcription> transcription) {
        super(null);
        this.translations = translations;
        this.transcription = transcription;
    }

    public List<Translation> getTranslations() {
        return translations;
    }

    public List<Transcription> getTranscription() {
        return transcription;
    }

    public record Translation(
            @JsonProperty("sentence_id") Integer sentenceId,
            @JsonProperty("begin_time") Integer beginTime,
            @JsonProperty("end_time") Integer endTime,
            @JsonProperty("text") String text,
            @JsonProperty("lang") String lang,
            @JsonProperty("words") List<Word> words,
            @JsonProperty("sentence_end") Boolean sentenceEnd
    ) {
        public record Word(
                @JsonProperty("begin_time") Integer beginTime,
                @JsonProperty("end_time") Integer endTime,
                @JsonProperty("text") String text,
                @JsonProperty("punctuation") String punctuation,
                @JsonProperty("fixed") Boolean fixed) {
        }
    }

    public record Transcription(
            @JsonProperty("sentence_id") Integer sentenceId,
            @JsonProperty("begin_time") Integer beginTime,
            @JsonProperty("end_time") Integer endTime,
            @JsonProperty("text") String text,
            @JsonProperty("words") List<Word> words,
            @JsonProperty("sentence_end") Boolean sentenceEnd
    ) {}
}

