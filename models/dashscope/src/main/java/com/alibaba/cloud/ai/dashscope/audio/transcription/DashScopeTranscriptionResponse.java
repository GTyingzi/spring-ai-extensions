package com.alibaba.cloud.ai.dashscope.audio.transcription;

import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeTranscriptionResponse.Translation.Word;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;

import java.util.List;

/**
 * @author yingzi
 * @since 2026/2/1
 */

public class DashScopeTranscriptionResponse extends AudioTranscriptionResponse {

    private final List<Translation> translations;

    private final Transcription transcription;

    private final Sentence sentence;

    private final Usage usage;

    public DashScopeTranscriptionResponse(List<Translation> translations, Transcription transcription) {
        super(null);
        this.translations = translations;
        this.transcription = transcription;
        this.sentence = null;
        this.usage = null;
    }

    public DashScopeTranscriptionResponse(Sentence sentence, Usage usage) {
        super(null);
        this.sentence = sentence;
        this.usage = usage;
        this.translations = null;
        this.transcription = null;
    }

    public List<Translation> getTranslations() {
        return translations;
    }

    public Transcription getTranscription() {
        return transcription;
    }

    public Sentence getSentence() {
        return sentence;
    }

    public Usage getUsage() {
        return usage;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Translation(
            @JsonProperty("sentence_id") Integer sentenceId,
            @JsonProperty("begin_time") Integer beginTime,
            @JsonProperty("end_time") Integer endTime,
            @JsonProperty("text") String text,
            @JsonProperty("lang") String lang,
            @JsonProperty("words") List<Word> words,
            @JsonProperty("sentence_end") Boolean sentenceEnd,
            @JsonProperty("speaker_id") Integer speakerId
    ) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Word(
                @JsonProperty("begin_time") Integer beginTime,
                @JsonProperty("end_time") Integer endTime,
                @JsonProperty("text") String text,
                @JsonProperty("punctuation") String punctuation,
                @JsonProperty("fixed") Boolean fixed) {
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Transcription(
            @JsonProperty("sentence_id") Integer sentenceId,
            @JsonProperty("begin_time") Integer beginTime,
            @JsonProperty("end_time") Integer endTime,
            @JsonProperty("text") String text,
            @JsonProperty("words") List<Word> words,
            @JsonProperty("sentence_end") Boolean sentenceEnd,
            @JsonProperty("channel_id") Integer channelId,
            @JsonProperty("content_duration_in_milliseconds") Integer contentDurationInMilliseconds,
            @JsonProperty("sentences") List<Sentence> sentences
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Sentence(
            @JsonProperty("begin_time") Integer beginTime,
            @JsonProperty("end_time") Integer endTime,
            @JsonProperty("text") String text,
            @JsonProperty("heartbeat") Boolean heartbeat,
            @JsonProperty("sentence_end") Boolean sentenceEnd,
            @JsonProperty("emo_tag") String emoTag,
            @JsonProperty("emo_confidence") Double emoConfidence,
            @JsonProperty("words") List<Word> words,
            @JsonProperty("sentence_id") Integer sentenceId,
            @JsonProperty("speaker_id") Integer speakerId
            ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Usage(
            @JsonProperty("duration") Integer duration
    ) {}
}

