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
package com.alibaba.cloud.ai.dashscope.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.cloud.ai.dashscope.api.ApiUtils;
import com.alibaba.cloud.ai.dashscope.audio.model.AudioDataWithMetadata;
import com.alibaba.cloud.ai.dashscope.audio.model.DashScopeAudioEventMessage;
import com.alibaba.cloud.ai.dashscope.protocol.DashScopeWebSocketClientOptions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.util.JacksonUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * @author kevinlin09
 * @author xuguan
 */
public class DashScopeWebSocketClient extends WebSocketListener {

	private final Logger logger = LoggerFactory.getLogger(DashScopeWebSocketClient.class);

	private final DashScopeWebSocketClientOptions options;

	private final AtomicBoolean isOpen;

	private final ObjectMapper objectMapper;

	private WebSocket webSocketClient;

	FluxSink<ByteBuffer> binaryEmitter;

	FluxSink<String> textEmitter;

	FluxSink<AudioDataWithMetadata> audioWithMetadataEmitter;

	// Event context for correlating binary audio data with events
	private volatile Integer currentSentenceIndex;

	private volatile String currentTaskId;

	private volatile String currentEventType;

	public DashScopeWebSocketClient(DashScopeWebSocketClientOptions options) {
		this.options = options;
		this.isOpen = new AtomicBoolean(false);
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

	public Flux<ByteBuffer> streamBinaryOut(String text) {
		Flux<ByteBuffer> flux = Flux.<ByteBuffer>create(emitter -> {
			this.binaryEmitter = emitter;
		}, FluxSink.OverflowStrategy.BUFFER);

		sendText(text);

		return flux;
	}

	/**
	 * Stream binary output using event-driven duplex flow for CosyVoice.
	 * This implements the official protocol specification:
	 * 1. Send run-task
	 * 2. Wait for task-started event
	 * 3. Send continue-task
	 * 4. Send finish-task
	 * 5. Wait for task-finished event
	 *
	 * @param runTaskMessage the run-task JSON message
	 * @param continueTaskMessage the continue-task JSON message
	 * @param finishTaskMessage the finish-task JSON message
	 * @return the binary data flux
	 */
	public Flux<ByteBuffer> streamDuplexWithEvents(String runTaskMessage, String continueTaskMessage,
			String finishTaskMessage) {
		// Prepare the messages to be sent
		this.pendingRunTaskMessage = runTaskMessage;
		this.pendingContinueTaskMessage = continueTaskMessage;
		this.pendingFinishTaskMessage = finishTaskMessage;
		this.duplexState = DuplexState.RUN_TASK_SENT;

		return Flux.<ByteBuffer>create(emitter -> {
			this.binaryEmitter = emitter;

			// Send run-task first
			logger.info("Event-driven duplex: Sending run-task message");
			sendText(runTaskMessage);

		}, FluxSink.OverflowStrategy.BUFFER);
	}

	/**
	 * Stream binary output with metadata using event-driven duplex flow for CosyVoice.
	 * This method returns {@link AudioDataWithMetadata} which includes both the audio data
	 * and event context (sentence index, task ID, etc.).
	 *
	 * This implements the official protocol specification:
	 * 1. Send run-task
	 * 2. Wait for task-started event
	 * 3. Send continue-task
	 * 4. Send finish-task
	 * 5. Wait for task-finished event
	 *
	 * @param runTaskMessage the run-task JSON message
	 * @param continueTaskMessage the continue-task JSON message
	 * @param finishTaskMessage the finish-task JSON message
	 * @return the audio data with metadata flux
	 */
	public Flux<AudioDataWithMetadata> streamDuplexWithMetadata(String runTaskMessage, String continueTaskMessage,
			String finishTaskMessage) {
		// Prepare the messages to be sent
		this.pendingRunTaskMessage = runTaskMessage;
		this.pendingContinueTaskMessage = continueTaskMessage;
		this.pendingFinishTaskMessage = finishTaskMessage;
		this.duplexState = DuplexState.RUN_TASK_SENT;

		// Reset event context
		this.currentSentenceIndex = null;
		this.currentTaskId = null;
		this.currentEventType = null;

		return Flux.<AudioDataWithMetadata>create(emitter -> {
			this.audioWithMetadataEmitter = emitter;

			// Send run-task first
			logger.info("Event-driven duplex with metadata: Sending run-task message");
			sendText(runTaskMessage);

		}, FluxSink.OverflowStrategy.BUFFER);
	}

	// Duplex flow state tracking
	private enum DuplexState {

		RUN_TASK_SENT, WAITING_TASK_STARTED, CONTINUE_TASK_SENT, FINISH_TASK_SENT, COMPLETED

	}

	private DuplexState duplexState = DuplexState.COMPLETED;

	private String pendingRunTaskMessage;

	private String pendingContinueTaskMessage;

	private String pendingFinishTaskMessage;

	public Flux<String> streamTextOut(Flux<ByteBuffer> binary) {
		Flux<String> flux = Flux.<String>create(emitter -> {
			this.textEmitter = emitter;
		}, FluxSink.OverflowStrategy.BUFFER);

		binary.subscribe(this::sendBinary);

		return flux;
	}

	public void sendText(String text) {
        if (!isOpen.get()) {
            establishWebSocketClient();
            try {
                TimeUnit.SECONDS.sleep(Constants.DEFAULT_READY_TIMEOUT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 恢复中断状态
                throw new RuntimeException("Interrupted while waiting for WebSocket connection", e);
            }
        }

		boolean success = webSocketClient.send(text);

		if (!success) {
			logger.error("send text failed");
		}
	}

	public void sendBinary(ByteBuffer binary) {
		if (!isOpen.get()) {
			establishWebSocketClient();
		}

		if (binary == null) {
			logger.error("binary data is null");
			return;
		}

		boolean success = webSocketClient.send(ByteString.of(binary));

		if (!success) {
			logger.error("send binary failed");
		}
	}

	private void establishWebSocketClient() {
		HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
		logging.setLevel(HttpLoggingInterceptor.Level.valueOf(Constants.DEFAULT_HTTP_LOGGING_LEVEL));
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.setMaxRequests(Constants.DEFAULT_MAXIMUM_ASYNC_REQUESTS);
		dispatcher.setMaxRequestsPerHost(Constants.DEFAULT_MAXIMUM_ASYNC_REQUESTS_PER_HOST);

		OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
		clientBuilder.connectTimeout(Constants.DEFAULT_CONNECT_TIMEOUT)
			.readTimeout(Constants.DEFAULT_READ_TIMEOUT)
			.writeTimeout(Constants.DEFAULT_WRITE_TIMEOUT)
			.addInterceptor(logging)
			.dispatcher(dispatcher)
			.protocols(Collections.singletonList(Protocol.HTTP_1_1))
			.connectionPool(new ConnectionPool(Constants.DEFAULT_CONNECTION_POOL_SIZE,
					Constants.DEFAULT_CONNECTION_IDLE_TIMEOUT.getSeconds(), TimeUnit.SECONDS));
		OkHttpClient httpClient = clientBuilder.build();

		try {
			this.webSocketClient = httpClient.newWebSocket(buildConnectionRequest(), this);
		}
		catch (Throwable ex) {
			logger.error("create websocket failed: msg={}", ex.getMessage());
		}
	}

	private Request buildConnectionRequest() {
		Builder bd = new Request.Builder();
		bd.headers(Headers.of(ApiUtils.getMapContentHeaders(options.getApiKey(), false,
			options.getWorkSpaceId(), null)));
		return bd.url(options.getUrl()).build();
	}

	private String getRequestBody(Response response) {
		String responseBody = "";
		if (response != null && response.body() != null) {
			try {
				responseBody = response.body().string();
			}
			catch (IOException ex) {
				logger.error("get response body failed: {}", ex.getMessage());
			}
		}
		return responseBody;
	}

	@Override
	public void onOpen(WebSocket webSocket, Response response) {
		logger.info("receive ws event onOpen: handle={}, body={}", webSocket, getRequestBody(response));
		isOpen.set(true);
	}

	@Override
	public void onClosed(WebSocket webSocket, int code, String reason) {
		logger.info("receive ws event onClosed: handle={}, code={}, reason={}", webSocket, code, reason);
		isOpen.set(false);
		emittersComplete("closed");
	}

	@Override
	public void onClosing(WebSocket webSocket, int code, String reason) {
		logger.info("receive ws event onClosing: handle={}, code={}, reason={}", webSocket.toString(), code, reason);
		emittersComplete("closing");
		webSocket.close(code, reason);
	}

	@Override
	public void onFailure(WebSocket webSocket, Throwable t, Response response) {
		String failureMessage = String.format("msg=%s, cause=%s, body=%s", t.getMessage(), t.getCause(),
				getRequestBody(response));
		logger.error("receive ws event onFailure: handle={}, {}", webSocket, failureMessage);
		isOpen.set(false);
		emittersError("failure", new Exception(failureMessage, t));
	}

	@Override
	public void onMessage(WebSocket webSocket, String text) {
		logger.debug("receive ws event onMessage(text): handle={}, text={}", webSocket, text);

		try {
            DashScopeAudioEventMessage message = this.objectMapper.readValue(text, DashScopeAudioEventMessage.class);
			switch (message.header().event()) {
				case TASK_STARTED:
					logger.info("task started: text={}", text);
					// Handle event-driven duplex flow
					handleTaskStartedEvent();
					break;
				case TASK_FINISHED:
					logger.info("task finished: text={}", text);
					// Mark duplex flow as completed
					if (this.duplexState != DuplexState.COMPLETED) {
						this.duplexState = DuplexState.COMPLETED;
					}
					emittersComplete("finished");
					break;
				case TASK_FAILED:
                    String errorCode = message.header().code() != null ? message.header().code() : "UNKNOWN";
                    String errorMessage =
                            message.header().message() != null ? message.header().message() : "No error message provided";
                    String errorDetail = String.format("Task failed with error_code='%s', error_message='%s'", errorCode, errorMessage);
                    logger.error("task failed: text={}, error_code={}, error_message={}", text, errorCode, errorMessage);
                    // Reset duplex state on error
                    this.duplexState = DuplexState.COMPLETED;
                    emittersError("task failed", new Exception(errorDetail));
					break;
				case RESULT_GENERATED:
                    logger.info("result generated: text={}", text);
                    // Track event context for binary audio correlation
                    trackEventContext(message);
					if (this.textEmitter != null) {
						textEmitter.next(text);
					}
					break;
				default:
                    String eventName = message.header().event().getValue();
                    String unsupportedError = String.format("Unsupported event type: %s", eventName);
                    logger.error("task error: text={}, event={}", text, eventName);
                    emittersError("unsupported event", new Exception(unsupportedError));
			}
		}
		catch (Exception e) {
			logger.error("parse message failed: text={}, msg={}", text, e.getMessage());
		}
	}

	/**
	 * Handle task-started event for event-driven duplex flow.
	 */
	private void handleTaskStartedEvent() {
		if (this.duplexState == DuplexState.RUN_TASK_SENT
				|| this.duplexState == DuplexState.WAITING_TASK_STARTED) {
			logger.info("Event-driven duplex: Received task-started, sending continue-task");
			this.duplexState = DuplexState.CONTINUE_TASK_SENT;
			sendText(this.pendingContinueTaskMessage);

			// After a short delay, send finish-task
			try {
				TimeUnit.MILLISECONDS.sleep(500);
				logger.info("Event-driven duplex: Sending finish-task");
				this.duplexState = DuplexState.FINISH_TASK_SENT;
				sendText(this.pendingFinishTaskMessage);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.error("Interrupted while sending finish-task", e);
			}
		}
	}

	/**
	 * Track event context for correlating binary audio data with events.
	 * When a sentence-synthesis event is received, store the sentence index and task ID
	 * so that the subsequent binary audio data can be associated with this event.
	 *
	 * @param message the event message
	 */
	private void trackEventContext(DashScopeAudioEventMessage message) {
		if (message.payload() == null || message.payload().output() == null) {
			return;
		}

		var output = message.payload().output();
		var typeNode = output.get("type");
		var sentenceNode = output.get("sentence");

		// Check if this is a sentence-synthesis event
		if (typeNode != null && "sentence-synthesis".equals(typeNode.asText())) {
			this.currentEventType = "sentence-synthesis";

			// Extract task ID
			if (message.header() != null && message.header().taskId() != null) {
				this.currentTaskId = message.header().taskId();
			}

			// Extract sentence index
			if (sentenceNode != null) {
				var indexNode = sentenceNode.get("index");
				if (indexNode != null && indexNode.isInt()) {
					this.currentSentenceIndex = indexNode.asInt();
					logger.debug("Tracking sentence-synthesis event: taskId={}, sentenceIndex={}",
							this.currentTaskId, this.currentSentenceIndex);
				}
			}
		}
		// Clear context for other event types
		else if (typeNode != null && "sentence-end".equals(typeNode.asText())) {
			this.currentSentenceIndex = null;
			this.currentEventType = null;
		}
	}

	@Override
	public void onMessage(WebSocket webSocket, ByteString bytes) {
		logger.debug("receive ws event onMessage(bytes): handle={}, size={}", webSocket, bytes.size());
		ByteBuffer audioData = bytes.asByteBuffer();

		// Emit to binary emitter (legacy behavior)
		if (this.binaryEmitter != null) {
			binaryEmitter.next(audioData);
		}

		// Emit to metadata emitter if available
		if (this.audioWithMetadataEmitter != null) {
			AudioDataWithMetadata audioWithMetadata;
			if (this.currentSentenceIndex != null && this.currentTaskId != null) {
				// Create with event context
				audioWithMetadata = AudioDataWithMetadata.fromSentence(
						audioData, this.currentSentenceIndex, this.currentTaskId);
				logger.debug("Emitting audio with metadata: sentenceIndex={}, size={}",
						this.currentSentenceIndex, bytes.size());
			}
			else {
				// Create without event context (fallback)
				audioWithMetadata = AudioDataWithMetadata.of(audioData);
			}
			this.audioWithMetadataEmitter.next(audioWithMetadata);
		}
	}

	private void emittersComplete(String event) {
		if (this.binaryEmitter != null && !this.binaryEmitter.isCancelled()) {
			logger.info("binary emitter handling: complete on {}", event);
			this.binaryEmitter.complete();
		}
		if (this.textEmitter != null && !this.textEmitter.isCancelled()) {
			logger.info("text emitter handling: complete on {}", event);
			this.textEmitter.complete();
			logger.info("done");
		}
		if (this.audioWithMetadataEmitter != null && !this.audioWithMetadataEmitter.isCancelled()) {
			logger.info("audio with metadata emitter handling: complete on {}", event);
			this.audioWithMetadataEmitter.complete();
		}
	}

	private void emittersError(String event, Throwable t) {
		if (this.binaryEmitter != null && !this.binaryEmitter.isCancelled()) {
			logger.info("binary emitter handling: error on {}", event);
			this.binaryEmitter.error(t);
		}
		if (this.textEmitter != null && !this.textEmitter.isCancelled()) {
			logger.info("text emitter handling: error on {}", event);
			this.textEmitter.error(t);
		}
		if (this.audioWithMetadataEmitter != null && !this.audioWithMetadataEmitter.isCancelled()) {
			logger.info("audio with metadata emitter handling: error on {}", event);
			this.audioWithMetadataEmitter.error(t);
		}
	}

	public static class Constants {

		private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(120);

		private static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(60);

		private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(300);

		private static final Duration DEFAULT_CONNECTION_IDLE_TIMEOUT = Duration.ofSeconds(300);

        private static final Integer DEFAULT_READY_TIMEOUT = 1;

		private static final Integer DEFAULT_CONNECTION_POOL_SIZE = 32;

		private static final Integer DEFAULT_MAXIMUM_ASYNC_REQUESTS = 32;

		private static final Integer DEFAULT_MAXIMUM_ASYNC_REQUESTS_PER_HOST = 32;

		private static final String DEFAULT_HTTP_LOGGING_LEVEL = "NONE";

	}

	// @formatter:off
	public enum EventType {

		// receive
		@JsonProperty("task-started")
		TASK_STARTED("task-started"),

		@JsonProperty("result-generated")
		RESULT_GENERATED("result-generated"),

		@JsonProperty("task-finished")
		TASK_FINISHED("task-finished"),

		@JsonProperty("task-failed")
		TASK_FAILED("task-failed"),

		// send
		@JsonProperty("run-task")
		RUN_TASK("run-task"),

		@JsonProperty("continue-task")
		CONTINUE_TASK("continue-task"),

		@JsonProperty("finish-task")
		FINISH_TASK("finish-task");

		private final String value;

		private EventType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

}
