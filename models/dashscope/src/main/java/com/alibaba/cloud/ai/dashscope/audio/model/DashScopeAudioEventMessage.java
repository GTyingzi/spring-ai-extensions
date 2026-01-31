package com.alibaba.cloud.ai.dashscope.audio.model;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeWebSocketClient.EventType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author yingzi
 * @since 2026/1/25
 */

public record DashScopeAudioEventMessage(
        @JsonProperty("header") EventMessageHeader header,
        @JsonProperty("payload") EventMessagePayload payload
) {
    public record EventMessageHeader (
            @JsonProperty("task_id") String taskId,
            @JsonProperty("event") EventType event,
            @JsonProperty("error_code") String code,
            @JsonProperty("error_message") String message
    ){}
    public record EventMessagePayload(
            @JsonProperty("output") JsonNode output,
            @JsonProperty("usage")  JsonNode usage
    ){}
}
