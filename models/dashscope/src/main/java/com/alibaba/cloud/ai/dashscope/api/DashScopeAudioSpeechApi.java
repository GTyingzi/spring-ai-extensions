/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dashscope.api;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechOptions;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechResponse;
import com.alibaba.cloud.ai.dashscope.audio.model.AudioDataWithMetadata;
import com.alibaba.cloud.ai.dashscope.audio.model.DashScopeAudioRequest;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeWebSocketClient;
import com.alibaba.cloud.ai.dashscope.audio.model.DashScopeAudioTTSModel.DashScopeAudioTTSRequest;
import com.alibaba.cloud.ai.dashscope.common.DashScopeAudioApiConstants;
import com.alibaba.cloud.ai.dashscope.protocol.DashScopeWebSocketClientOptions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.function.Predicate;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.ENABLED;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.HEADER_SSE;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeAudioApiConstants.MULTIMODAL_GENERATION;

/**
 * @author xuguan
 */
public class DashScopeAudioSpeechApi {

    // 日志
    private static final Logger log = LoggerFactory.getLogger(DashScopeAudioSpeechApi.class);

	private final DashScopeWebSocketClient webSocketClient;

	private final ObjectMapper objectMapper;

    private final RestClient restClient;

    private final WebClient webClient;

    private final String apiKey;

	public DashScopeAudioSpeechApi(String baseUrl, String apiKey, String workSpaceId, RestClient.Builder restClientBuilder,
                                   WebClient.Builder webClientBuilder) {
		this.apiKey = apiKey;
		this.webSocketClient = new DashScopeWebSocketClient(DashScopeWebSocketClientOptions.builder()
			.apiKey(apiKey)
			.workSpaceId(workSpaceId)
			.url(DashScopeAudioApiConstants.DEFAULT_WEBSOCKET_URL)
			.build());

		this.objectMapper =
			JsonMapper.builder()
				// Deserialization configuration
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				// Serialization configuration
				.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.serializationInclusion(JsonInclude.Include.NON_NULL)
				// Register standard Jackson modules (Jdk8, JavaTime, ParameterNames, Kotlin)
				.addModules(JacksonUtils.instantiateAvailableModules())
				.build();

        this.restClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .build();

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
	}

	public Flux<ByteBuffer> streamBinaryOut(DashScopeAudioRequest request) {
		try {
			String message = this.objectMapper.writeValueAsString(request);
			return this.webSocketClient.streamBinaryOut(message);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Send a continue-task request for CosyVoice duplex mode.
	 *
	 * @param taskId the task ID
	 * @param text the text to send
	 */
	public void sendContinueTask(String taskId, String text) {
		try {
			DashScopeAudioRequest request = DashScopeAudioRequest.createContinueTaskRequest(taskId, text);
			String message = this.objectMapper.writeValueAsString(request);
			this.webSocketClient.sendText(message);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Send a finish-task request for CosyVoice duplex mode.
	 *
	 * @param taskId the task ID
	 */
	public void sendFinishTask(String taskId) {
		try {
			DashScopeAudioRequest request = DashScopeAudioRequest.createFinishTaskRequest(taskId);
			String message = this.objectMapper.writeValueAsString(request);
			this.webSocketClient.sendText(message);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Stream audio output with full duplex flow for CosyVoice models.
	 * This method implements event-driven flow:
	 * 1. Send run-task (without text in input)
	 * 2. Wait for task-started event
	 * 3. Send continue-task (with text)
	 * 4. Send finish-task (empty input)
	 * 5. Receive audio data
	 * 6. Wait for task-finished event
	 *
	 * @param taskId the task ID
	 * @param text the text to synthesize
	 * @param options the speech options
	 * @return the audio data flux
	 */
	public Flux<ByteBuffer> streamDuplexOut(String taskId, String text,
			DashScopeAudioSpeechOptions options) {
		try {
			// Create run-task request (without text in input for CosyVoice)
			DashScopeAudioRequest runTaskRequest = DashScopeAudioRequest.builder()
					.header(DashScopeAudioRequest.RequestHeader.builder()
							.action(DashScopeWebSocketClient.EventType.RUN_TASK)
							.taskId(taskId)
							.streaming("duplex")
							.build())
					.payload(DashScopeAudioRequest.RequestPayload.builder()
							.model(options.getModel())
							.taskGroup("audio")
							.task("tts")
							.function("SpeechSynthesizer")
							.input(DashScopeAudioRequest.RequestPayloadInput.builder()
									.build())
							.parameters(DashScopeAudioRequest.RequestPayloadParameters
									.optionsConvertReq(options))
							.build())
					.build();

			String runTaskMessage = this.objectMapper.writeValueAsString(runTaskRequest);
			String continueTaskMessage = this.objectMapper.writeValueAsString(
					DashScopeAudioRequest.createContinueTaskRequest(taskId, text));
			String finishTaskMessage = this.objectMapper.writeValueAsString(
					DashScopeAudioRequest.createFinishTaskRequest(taskId));

			// Use event-driven duplex flow
			return this.webSocketClient.streamDuplexWithEvents(runTaskMessage, continueTaskMessage,
					finishTaskMessage);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Stream audio output with full duplex flow for CosyVoice models, including metadata.
	 * This method returns {@link AudioDataWithMetadata} which includes both the audio data
	 * and event context (sentence index, task ID, etc.) for proper correlation.
	 *
	 * This method implements event-driven flow:
	 * 1. Send run-task (without text in input)
	 * 2. Wait for task-started event
	 * 3. Send continue-task (with text)
	 * 4. Send finish-task (empty input)
	 * 5. Receive audio data with metadata
	 * 6. Wait for task-finished event
	 *
	 * @param taskId the task ID
	 * @param text the text to synthesize
	 * @param options the speech options
	 * @return the audio data with metadata flux
	 */
	public Flux<AudioDataWithMetadata> streamDuplexOutWithMetadata(String taskId, String text,
			DashScopeAudioSpeechOptions options) {
		try {
			// Create run-task request (without text in input for CosyVoice)
			DashScopeAudioRequest runTaskRequest = DashScopeAudioRequest.builder()
					.header(DashScopeAudioRequest.RequestHeader.builder()
							.action(DashScopeWebSocketClient.EventType.RUN_TASK)
							.taskId(taskId)
							.streaming("duplex")
							.build())
					.payload(DashScopeAudioRequest.RequestPayload.builder()
							.model(options.getModel())
							.taskGroup("audio")
							.task("tts")
							.function("SpeechSynthesizer")
							.input(DashScopeAudioRequest.RequestPayloadInput.builder()
									.build())
							.parameters(DashScopeAudioRequest.RequestPayloadParameters
									.optionsConvertReq(options))
							.build())
					.build();

			String runTaskMessage = this.objectMapper.writeValueAsString(runTaskRequest);
			String continueTaskMessage = this.objectMapper.writeValueAsString(
					DashScopeAudioRequest.createContinueTaskRequest(taskId, text));
			String finishTaskMessage = this.objectMapper.writeValueAsString(
					DashScopeAudioRequest.createFinishTaskRequest(taskId));

			// Use event-driven duplex flow with metadata
			return this.webSocketClient.streamDuplexWithMetadata(runTaskMessage, continueTaskMessage,
					finishTaskMessage);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

    public DashScopeSpeechResponse callQwenTTS(String text, DashScopeAudioSpeechOptions options) {
        DashScopeAudioTTSRequest request = DashScopeAudioTTSRequest.builder()
                .model(options.getModel())
                .text(text)
                .voice(options.getVoice())
                .languageType(options.getLanguageType())
                .build();

        // 构建REST API请求，添加 Authorization 头
        ResponseEntity<DashScopeSpeechResponse> response = restClient.post()
                .uri(MULTIMODAL_GENERATION)
                .headers(headers -> headers.setBearerAuth(this.apiKey))
                .body(request)
                .retrieve()
                .toEntity(DashScopeSpeechResponse.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }

        log.error("Failed to call Qwen TTS API: " + response.getStatusCode());
        throw new RuntimeException("Failed to call Qwen TTS API: " + response.getStatusCode());
    }

    public Flux<DashScopeSpeechResponse> streamQwenTTS(String text, DashScopeAudioSpeechOptions options) {
        DashScopeAudioTTSRequest request = DashScopeAudioTTSRequest.builder()
                .model(options.getModel())
                .text(text)
                .voice(options.getVoice())
                .languageType(options.getLanguageType())
                .build();

        // SSE 流结束标志
        Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

        return this.webClient.post()
                .uri(MULTIMODAL_GENERATION)
                .headers(headers -> {
                    headers.setBearerAuth(this.apiKey);
                    headers.add(HEADER_SSE, ENABLED);  // X-DashScope-SSE: enable
                })
                .body(Mono.just(request), DashScopeAudioTTSRequest.class)
                .retrieve()
                .bodyToFlux(String.class)  // 接收 SSE 流数据
                .takeUntil(SSE_DONE_PREDICATE)  // 遇到 [DONE] 停止
                .filter(SSE_DONE_PREDICATE.negate())  // 过滤掉 [DONE]
                .map(content -> {
                    // 解析 JSON 响应
                    try {
                        return this.objectMapper.readValue(content, DashScopeSpeechResponse.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to parse TTS response: " + content, e);
                    }
                });
    }
}
