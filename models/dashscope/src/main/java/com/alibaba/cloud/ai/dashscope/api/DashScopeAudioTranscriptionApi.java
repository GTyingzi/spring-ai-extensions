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

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeWebSocketClient.EventType;
import com.alibaba.cloud.ai.dashscope.audio.WebSocketRequest;
import com.alibaba.cloud.ai.dashscope.audio.WebSocketRequest.RequestHeader;
import com.alibaba.cloud.ai.dashscope.audio.WebSocketRequest.RequestPayload;
import com.alibaba.cloud.ai.dashscope.audio.WebSocketRequest.RequestPayloadInput;
import com.alibaba.cloud.ai.dashscope.audio.WebSocketRequest.RequestPayloadParameters;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeAudioTranscriptionOptions;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeAudioTranscriptionPrompt;
import com.alibaba.cloud.ai.dashscope.audio.transcription.TranscriptionReqRes.DashScopeAudioTranscriptionRequest;
import com.alibaba.cloud.ai.dashscope.audio.transcription.TranscriptionReqRes.DashScopeAudioTranscriptionResponse;
import com.alibaba.cloud.ai.dashscope.audio.tts.TTSReqRes.DashScopeAudioTTSRequest;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.alibaba.cloud.ai.dashscope.common.DashScopeAudioApiConstants;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeWebSocketClient;
import com.alibaba.cloud.ai.dashscope.protocol.DashScopeWebSocketClientOptions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Turn audio into text or text into audio. Based on <a href=
 * "https://help.aliyun.com/zh/model-studio/user-guide/speech-recognition-and-synthesis">DashScope
 * Audio Model</a>
 *
 * @author Kevin Lin
 * @author yuluo-yx
 * @author xuguan
 * @author yingzi
 */
public class DashScopeAudioTranscriptionApi {

    private static final Logger log = LoggerFactory.getLogger(DashScopeAudioTranscriptionApi.class);

    private final String baseUrl;

    private final String websocketUrl;

    private final ApiKey apiKey;

    private final String workSpaceId;

    private final MultiValueMap<String, String> headers;

	private final DashScopeWebSocketClient webSocketClient;

	private final RestClient restClient;

    private final WebClient webClient;

	private final ObjectMapper objectMapper;

	public DashScopeAudioTranscriptionApi(
        String baseUrl,
        String  websocketUrl,
        ApiKey apiKey,
		String workSpaceId,
		MultiValueMap<String, String> headers,
		RestClient.Builder restClientBuilder,
        WebClient.Builder webClientBuilder,
		ResponseErrorHandler responseErrorHandler) {
        this.baseUrl = baseUrl;
        this.websocketUrl = websocketUrl;
        this.apiKey = apiKey;
        this.workSpaceId = workSpaceId;
        this.headers = headers;

		Consumer<HttpHeaders> authHeaders = h -> {
			h.addAll(headers);
			if (!(apiKey instanceof NoopApiKey)) {
				h.setBearerAuth(apiKey.getValue());
			}
		};

		this.restClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeaders(authHeaders)
                .defaultStatusHandler(responseErrorHandler)
                .build();

        this.webClient = webClientBuilder.clone()
			    .baseUrl(baseUrl)
			    .defaultHeaders(authHeaders)
                .build();

		this.webSocketClient = new DashScopeWebSocketClient(
			DashScopeWebSocketClientOptions.builder()
				.apiKey(apiKey.getValue())
				.workSpaceId(workSpaceId)
                    .url(websocketUrl)
				.build());

		this.objectMapper = JsonMapper.builder()
			// Deserialization configuration
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			// Serialization configuration
			.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.serializationInclusion(JsonInclude.Include.NON_NULL)
			// Register standard Jackson modules (Jdk8, JavaTime, ParameterNames, Kotlin)
			.addModules(JacksonUtils.instantiateAvailableModules())
			.build();
	}

    public AudioTranscriptionResponse callLiveTranslate(
            AudioTranscriptionPrompt prompt,
            DashScopeAudioTranscriptionOptions options) {
        DashScopeAudioTranscriptionPrompt dashScopeAudioTranscriptionPrompt = (DashScopeAudioTranscriptionPrompt) prompt;

        DashScopeAudioTranscriptionRequest request = DashScopeAudioTranscriptionRequest
                .builder()
                .model(options.getModel())
                .messages(dashScopeAudioTranscriptionPrompt.getMessages())
                .modalities(options.getModalities())
                .stream(false)
                .audio(options.getAudio())
                .maxTokens(options.getMaxTokens())
                .seed(options.getSeed())
                .temperature(options.getTemperature())
                .topP(options.getTopP())
                .presencePenalty(options.getPresencePenalty())
                .topK(options.getTopK())
                .repetitionPenalty(options.getRepetitionPenalty())
                .translationOptions(options.getTranslationOptions())
                .build();

        ResponseEntity<DashScopeAudioTranscriptionResponse> response = restClient.post()
                .uri(DashScopeAudioApiConstants.CHAT_COMPLETIONS)
                .body(request)
                .retrieve()
                .toEntity(DashScopeAudioTranscriptionResponse.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }

        log.error("Failed to call Live Translate API: " + response.getStatusCode());
        throw new RuntimeException("Failed to call Live Translate API: " + response.getStatusCode());
    }

    public Flux<AudioTranscriptionResponse> streamLiveTranslate(
            AudioTranscriptionPrompt prompt,
            DashScopeAudioTranscriptionOptions options) {
        DashScopeAudioTranscriptionPrompt dashScopeAudioTranscriptionPrompt = (DashScopeAudioTranscriptionPrompt) prompt;

        DashScopeAudioTranscriptionRequest request = DashScopeAudioTranscriptionRequest
                .builder()
                .model(options.getModel())
                .messages(dashScopeAudioTranscriptionPrompt.getMessages())
                .modalities(options.getModalities())
                .stream(true)
                .streamOptions(options.getStreamOptions())
                .audio(options.getAudio())
                .maxTokens(options.getMaxTokens())
                .seed(options.getSeed())
                .temperature(options.getTemperature())
                .topP(options.getTopP())
                .presencePenalty(options.getPresencePenalty())
                .topK(options.getTopK())
                .repetitionPenalty(options.getRepetitionPenalty())
                .translationOptions(options.getTranslationOptions())
                .build();

        // SSE 流结束标志
        Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

        return this.webClient.post()
                .uri(DashScopeAudioApiConstants.CHAT_COMPLETIONS)
                .body(Mono.just(request), DashScopeAudioTTSRequest.class)
                .retrieve()
                .bodyToFlux(String.class)  // 接收 SSE 流数据
                .takeUntil(SSE_DONE_PREDICATE)  // 遇到 [DONE] 停止
                .filter(SSE_DONE_PREDICATE.negate())  // 过滤掉 [DONE]
                .map(content -> {
                    // 解析 JSON 响应
                    try {
                        return this.objectMapper.readValue(content, DashScopeAudioTranscriptionResponse.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to parse TTS response: " + content, e);
                    }
                });
    }

    public Flux<String> createWebSocketTask(ByteBuffer binaryData, DashScopeAudioTranscriptionOptions options) {
        String taskId = UUID.randomUUID().toString();
        // run-task
        WebSocketRequest runTaskRequest = WebSocketRequest.builder()
                .header(RequestHeader.builder()
                        .action(EventType.RUN_TASK)
                        .taskId(taskId)
                        .streaming("duplex")
                        .build())
                .payload(RequestPayload.builder()
                        .model(options.getModel())
                        .task("asr")
                        .function("recognition")
                        .taskGroup("audio")
                        .input(RequestPayloadInput.builder().build())
                        .parameters(RequestPayloadParameters.builder()
                                .sampleRate(options.getSampleRate())
                                .format(options.getFormat())
                                .vocabularyId(options.getVocabularyId())
                                .sourceLanguage(options.getSourceLanguage())
                                .transcriptionEnabled(options.getTranscriptionEnabled())
                                .translationEnabled(options.getTranslationEnabled())
                                .translationTargetLanguages(options.getTranslationTargetLanguages())
                                .maxEndSilence(options.getMaxEndSilence())
                                .multiThresholdModeEnabled(options.getMultiThresholdModeEnabled())
                                .punctuationPredictionEnabled(options.getPunctuationPredictionEnabled())
                                .heartbeat(options.getHeartbeat())
                                .inverseTextNormalizationEnabled(options.getInverseTextNormalizationEnabled())
                                .disfluencyRemovalEnabled(options.getDisfluencyRemovalEnabled())
                                .languageHints(options.getLanguageHints())
                                .build())
                        .resources(options.getResources())
                        .build())
                .build();
        // finish-task
        WebSocketRequest finishTaskRequest = WebSocketRequest.builder()
                .header(RequestHeader.builder()
                        .action(EventType.FINISH_TASK)
                        .taskId(taskId)
                        .streaming("duplex")
                        .build())
                .payload(RequestPayload.builder()
                        .input(RequestPayloadInput.builder()
                                .build())
                        .build())
                .build();
        try{
            String runTaskMessage = this.objectMapper.writeValueAsString(runTaskRequest);
            String finishTaskMessage = this.objectMapper.writeValueAsString(finishTaskRequest);
            return this.webSocketClient.command(runTaskMessage, binaryData, finishTaskMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create WebSocket task: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a builder pre-populated with the current configuration for mutation.
     */
    public Builder mutate() {
        return new Builder(this);
    }

    public String getBaseUrl() {
		return this.baseUrl;
	}

    public String getWebsocketUrl() {
		return this.websocketUrl;
	}

    public ApiKey getApiKey() {
		return this.apiKey;
	}

    public String getWorkSpaceId() {
		return this.workSpaceId;
	}

    public MultiValueMap<String, String> getHeaders() {
		return this.headers;
	}

    public static Builder builder() {
        return new Builder();
    }

	public static class Builder {

		private String baseUrl = DashScopeApiConstants.DEFAULT_BASE_URL;

        private String websocketUrl = DashScopeAudioApiConstants.DEFAULT_WEBSOCKET_URL;

        private ApiKey apiKey;

        private String workSpaceId;

		private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

		private RestClient.Builder restClientBuilder = RestClient.builder();

        private WebClient.Builder webClientBuilder = WebClient.builder();

		private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

		public Builder() {
		}

        public Builder(DashScopeAudioTranscriptionApi api) {
            this.baseUrl = api.getBaseUrl();
            this.websocketUrl = api.getWebsocketUrl();
            this.apiKey = api.getApiKey();
            this.workSpaceId = api.getWorkSpaceId();
            this.headers = api.getHeaders();
		}

		public Builder baseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}

        public Builder websocketUrl(String websocketUrl) {
			this.websocketUrl = websocketUrl;
			return this;
		}

		public Builder workSpaceId(String workSpaceId) {
			this.workSpaceId = workSpaceId;
			return this;
		}

		public Builder apiKey(ApiKey apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public Builder headers(MultiValueMap<String, String> headers) {
			this.headers = headers;
			return this;
		}

		public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
			this.restClientBuilder = restClientBuilder;
			return this;
		}

        public Builder webClientBuilder(WebClient.Builder webClientBuilder) {
			this.webClientBuilder = webClientBuilder;
			return this;
		}

		public Builder responseErrorHandler(ResponseErrorHandler responseErrorHandler) {
			this.responseErrorHandler = responseErrorHandler;
			return this;
		}

		public DashScopeAudioTranscriptionApi build() {
			Assert.hasText(this.baseUrl, "baseUrl cannot be null or empty");
            Assert.hasText(this.websocketUrl, "websocketUrl cannot be null or empty");
			Assert.notNull(this.apiKey, "apiKey must be set");
			Assert.notNull(this.headers, "headers cannot be null");
			Assert.notNull(this.restClientBuilder, "restClientBuilder cannot be null");
            Assert.notNull(this.webClientBuilder, "webClientBuilder cannot be null");
			Assert.notNull(this.responseErrorHandler, "responseErrorHandler cannot be null");

			return new DashScopeAudioTranscriptionApi(
				this.baseUrl,
                this.websocketUrl,
				this.apiKey,
				this.workSpaceId,
				this.headers,
				this.restClientBuilder,
                this.webClientBuilder,
				this.responseErrorHandler);
		}
	}
}
