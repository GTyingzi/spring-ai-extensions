package com.alibaba.cloud.ai.dashscope.metadata.audio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.audio.transcription.AudioTranscriptionMetadata;

import java.util.List;

/**
 * @author yingzi
 * @since 2026/2/7
 */

public record DashScopeAudioTranscriptionMetadata(
        @JsonProperty("sentence_id") Integer sentenceId,
        @JsonProperty("begin_time") Integer beginTime,
        @JsonProperty("end_time") Integer endTime,
        @JsonProperty("words") List<DashScopeAudioTranscriptionResponseMetadata.Translation.Word> words,
        @JsonProperty("sentence_end") Boolean sentenceEnd,
        @JsonProperty("channel_id") Integer channelId,
        @JsonProperty("content_duration_in_milliseconds") Integer contentDurationInMilliseconds,
        @JsonProperty("sentences") List<DashScopeAudioTranscriptionResponseMetadata.Sentence> sentences
) implements AudioTranscriptionMetadata {}
