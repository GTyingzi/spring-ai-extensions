package com.alibaba.cloud.ai.dashscope.audio;

import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechResponse;

/**
 * @author yingzi
 * @since 2026/1/29
 */
public class DashScopeSpeechResponse extends TextToSpeechResponse {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("output")
    private final DashScopeSpeechOutput output;

    @JsonProperty("usage")
    private final DashScopeSpeechUsage usage;

    @JsonCreator
    public DashScopeSpeechResponse(
            @JsonProperty("request_id") String requestId,
            @JsonProperty("output") DashScopeSpeechOutput output,
            @JsonProperty("usage") DashScopeSpeechUsage usage) {
        super(createSpeechList(output), null);
        this.requestId = requestId;
        this.output = output;
        this.usage = usage;
    }

    public String getRequestId() {
        return requestId;
    }

    public DashScopeSpeechOutput getOutput() {
        return output;
    }

    public DashScopeSpeechUsage getUsage() {
        return usage;
    }

    public record DashScopeSpeechOutput(
            @JsonProperty("finish_reason") String finishReason,
            @JsonProperty("audio") DashScopeSpeechAudio audio) {
    }

    public record DashScopeSpeechAudio(
            @JsonProperty("data") String data,
            @JsonProperty("url") String url,
            @JsonProperty("id") String id,
            @JsonProperty("expires_at") Integer expiresAt) {
    }

    public record DashScopeSpeechUsage(
            @JsonProperty("input_tokens") Integer inputTokens,
            @JsonProperty("output_tokens") Integer outputTokens,
            @JsonProperty("characters") Integer characters,
            @JsonProperty("input_tokens_details") InputTokensDetails inputTokensDetails,
            @JsonProperty("output_tokens_details") OutputTokensDetails outputTokensDetails,
            @JsonProperty("total_tokens") Integer totalTokens) {
    }

    public record InputTokensDetails(
            @JsonProperty("text_tokens") Integer textTokens) {
    }

    public record OutputTokensDetails(
            @JsonProperty("audio_tokens") Integer audioTokens,
            @JsonProperty("text_tokens") Integer textTokens) {
    }

    /**
     * Create Speech objects from the output.
     * If base64 audio data is available, decode it and create a Speech object.
     * Otherwise, create an empty Speech object (the URL can be accessed via getOutput()).
     */
    private static List<Speech> createSpeechList(DashScopeSpeechOutput output) {
        if (output == null || output.audio() == null) {
            return List.of(new Speech(new byte[0]));
        }

        DashScopeSpeechAudio audio = output.audio();
        // Prefer base64 data over URL
        if (audio.data() != null && !audio.data().isEmpty()) {
            try {
                byte[] audioData = Base64.getDecoder().decode(audio.data());
                return List.of(new Speech(audioData));
            }
            catch (IllegalArgumentException e) {
                // Invalid base64, return empty speech
                return List.of(new Speech(new byte[0]));
            }
        }

        // If only URL is available, create empty speech (URL can be accessed separately)
        return List.of(new Speech(new byte[0]));
    }

}

