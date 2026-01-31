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

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioSpeechApi;
import com.alibaba.cloud.ai.dashscope.audio.model.AudioCommonType;
import com.alibaba.cloud.ai.dashscope.audio.model.DashScopeAudioRequest;
import com.alibaba.cloud.ai.dashscope.common.DashScopeAudioApiConstants;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechOptions;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

/**
 * Audio Speech: input text, output audio.
 *
 * @author kevinlin09, xuguan, yingzi
 */
public class DashScopeAudioSpeechModel implements TextToSpeechModel {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeAudioSpeechModel.class);

	private final DashScopeAudioSpeechApi audioSpeechApi;

	private final DashScopeAudioSpeechOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	public DashScopeAudioSpeechModel(DashScopeAudioSpeechApi audioSpeechApi) {
		this(audioSpeechApi, DashScopeAudioSpeechOptions.builder()
			.build());
	}

	public DashScopeAudioSpeechModel(DashScopeAudioSpeechApi audioSpeechApi, DashScopeAudioSpeechOptions defaultOptions) {
		this(audioSpeechApi, defaultOptions, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public DashScopeAudioSpeechModel(DashScopeAudioSpeechApi audioSpeechApi, DashScopeAudioSpeechOptions defaultOptions,
		RetryTemplate retryTemplate) {
		this.audioSpeechApi = audioSpeechApi;
		this.defaultOptions = defaultOptions;
		this.retryTemplate = retryTemplate;
	}

	@NotNull
    @Override
	public TextToSpeechResponse call(TextToSpeechPrompt prompt) {
        DashScopeAudioSpeechOptions options = this.mergeOptions(prompt);
        if (DashScopeAudioApiConstants.isQwenTTSModel(options.getModel())) {
            return this.audioSpeechApi.callQwenTTS(prompt.getInstructions().getText(), options);
        }

        String taskId = UUID.randomUUID().toString();
        String text = prompt.getInstructions().getText();
        String model = options.getModel();

        logger.info("Starting TTS call for model: {}", model);

        // For CosyVoice models, use full duplex flow
        if (DashScopeAudioApiConstants.COSY_VOICE_MODEL_LIST.contains(model)) {
            logger.info("Using CosyVoice duplex flow: run-task -> continue-task -> finish-task");
            return this.retryTemplate.execute(ctx -> this.audioSpeechApi.streamDuplexOut(taskId, text, options)
                    .collectList()
                    .map(byteBuffers -> {
                        // combine all byte buffers
                        ByteBuffer combined = ByteBuffer.allocate(byteBuffers.stream()
                                .mapToInt(ByteBuffer::remaining)
                                .sum());

                        for (ByteBuffer byteBuffer : byteBuffers) {
                            combined.put(byteBuffer);
                        }

                        combined.flip();

                        byte[] data = new byte[combined.remaining()];
                        combined.get(data);
                        return new TextToSpeechResponse(List.of(new Speech(data)));
                    })
                    .block());
        }

        // For Sambert models, use simple run-task flow
        DashScopeAudioRequest runTaskRequest = this.createRequest(prompt, taskId);
        logger.info("Using Sambert simple flow: run-task");
        return this.retryTemplate.execute(ctx -> this.audioSpeechApi.streamBinaryOut(runTaskRequest)
                .collectList()
                .map(byteBuffers -> {
                    // combine all byte buffers
                    ByteBuffer combined = ByteBuffer.allocate(byteBuffers.stream()
                            .mapToInt(ByteBuffer::remaining)
                            .sum());

                    for (ByteBuffer byteBuffer : byteBuffers) {
                        combined.put(byteBuffer);
                    }

                    combined.flip();

                    byte[] data = new byte[combined.remaining()];
                    combined.get(data);
                    return new TextToSpeechResponse(List.of(new Speech(data)));
                })
                .block());
	}

    @Override
	public Flux<TextToSpeechResponse> stream(TextToSpeechPrompt prompt) {
        DashScopeAudioSpeechOptions options = this.mergeOptions(prompt);
        if (DashScopeAudioApiConstants.isQwenTTSModel(options.getModel())) {
            return this.audioSpeechApi.streamQwenTTS(prompt.getInstructions().getText(), options)
                    .map(response -> (TextToSpeechResponse) response);
        }

        String taskId = UUID.randomUUID().toString();
        String text = prompt.getInstructions().getText();
        String model = options.getModel();

        logger.info("Starting TTS stream for model: {}", model);

        // For CosyVoice models, use full duplex flow
        if (DashScopeAudioApiConstants.COSY_VOICE_MODEL_LIST.contains(model)) {
            logger.info("Using CosyVoice duplex flow: run-task -> continue-task -> finish-task");
            return this.retryTemplate.execute(ctx -> this.audioSpeechApi.streamDuplexOut(taskId, text, options)
                .map(byteBuffer -> {
                    byte[] data = new byte[byteBuffer.remaining()];
                    byteBuffer.get(data);
                    return new TextToSpeechResponse(List.of(new Speech(data)));
                }));
        }

        // For Sambert models, use simple run-task flow
        DashScopeAudioRequest runTaskRequest = this.createRequest(prompt, taskId);
        logger.info("Using Sambert simple flow: run-task");
        return this.retryTemplate.execute(ctx -> this.audioSpeechApi.streamBinaryOut(runTaskRequest)
            .map(byteBuffer -> {
                byte[] data = new byte[byteBuffer.remaining()];
                byteBuffer.get(data);
                return new TextToSpeechResponse(List.of(new Speech(data)));
            }));
	}

	private DashScopeAudioRequest createRequest(TextToSpeechPrompt prompt,
		String taskId) {
		DashScopeAudioSpeechOptions options = this.mergeOptions(prompt);
        String model = options.getModel();
        boolean isSambert = false;
        if (DashScopeAudioApiConstants.COSY_VOICE_MODEL_LIST.contains(model)) {
            isSambert = false;
        } else if (DashScopeAudioApiConstants.SAMBERT_MODEL_LIST.contains(model)) {
            isSambert = true;
        } else {
            throw new IllegalArgumentException("Audio Unsupported model: " + model);
        }

        return DashScopeAudioRequest.builder()
			.header(DashScopeAudioRequest.RequestHeader.builder()
				.action(DashScopeWebSocketClient.EventType.RUN_TASK)
				.taskId(taskId)
				.streaming(isSambert ? "out" : "duplex") // "out" for sambert, "duplex" for cosy voice
				.build())
			.payload(DashScopeAudioRequest.RequestPayload.builder()
				.model(options.getModel())
				.taskGroup("audio")
				.task("tts")
				.function("SpeechSynthesizer")
				.input(DashScopeAudioRequest.RequestPayloadInput.builder()
					.text(prompt.getInstructions().getText())
					.build())
				.parameters(DashScopeAudioRequest.RequestPayloadParameters
                        .optionsConvertReq(options))
			    .build())
                .build();
	}

	private DashScopeAudioSpeechOptions mergeOptions(TextToSpeechPrompt prompt) {
		DashScopeAudioSpeechOptions options = DashScopeAudioSpeechOptions.builder().build();
		if (prompt.getOptions() != null) {
			DashScopeAudioSpeechOptions runtimeOptions = ModelOptionsUtils.copyToTarget(prompt.getOptions(),
				TextToSpeechOptions.class, DashScopeAudioSpeechOptions.class);

			options = ModelOptionsUtils.merge(runtimeOptions, options, DashScopeAudioSpeechOptions.class);
		}

		return ModelOptionsUtils.merge(options, this.defaultOptions, DashScopeAudioSpeechOptions.class);
	}

    /**
     * Returns a builder pre-populated with the current configuration for mutation.
     */
    public Builder mutate() {
        return new Builder(this);
    }

    @Override
    public DashScopeAudioSpeechModel clone() {
        return this.mutate().build();
    }

    /**
     * Returns the underlying {@link DashScopeAudioSpeechApi} for advanced usage.
     * This can be used to access methods like {@code streamDuplexOutWithMetadata()}
     * that return audio data with event metadata.
     *
     * @return the audio speech API
     */
    public DashScopeAudioSpeechApi audioSpeechApi() {
        return this.audioSpeechApi;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private DashScopeAudioSpeechApi audioSpeechApi;

        private DashScopeAudioSpeechOptions defaultOptions = DashScopeAudioSpeechOptions.builder()
                .model(DashScopeModel.AudioModel.COSYVOICE_V1.getValue())
                .voice("longhua")
                .speed(1.0)
                .format(AudioCommonType.Format.MP3.getValue())
                .build();

        private RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

        private Builder() {
        }

        private Builder(DashScopeAudioSpeechModel audioSpeechModel) {
            this.audioSpeechApi = audioSpeechModel.audioSpeechApi;
            this.defaultOptions = audioSpeechModel.defaultOptions;
            this.retryTemplate = audioSpeechModel.retryTemplate;
        }

        public Builder audioSpeechApi(DashScopeAudioSpeechApi audioSpeechApi) {
            this.audioSpeechApi = audioSpeechApi;
            return this;
        }

        public Builder defaultOptions(DashScopeAudioSpeechOptions defaultOptions) {
            this.defaultOptions = defaultOptions;
            return this;
        }

        public Builder retryTemplate(RetryTemplate retryTemplate) {
            this.retryTemplate = retryTemplate;
            return this;
        }

        public DashScopeAudioSpeechModel build() {
            return new DashScopeAudioSpeechModel(this.audioSpeechApi, this.defaultOptions, this.retryTemplate);
        }
    }

}
