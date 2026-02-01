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
package com.alibaba.cloud.ai.dashscope.audio.transcription;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioTranscriptionApi;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeTranscriptionResponse.Sentence;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeTranscriptionResponse.Transcription;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeTranscriptionResponse.Translation;
import com.alibaba.cloud.ai.dashscope.audio.transcription.TranscriptionReqRes.DashScopeAudioTranscriptionResponse;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel.AudioModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.model.SimpleApiKey;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DashScopeAudioTranscriptionModel using real API calls.
 * These tests require a valid DASHSCOPE_API_KEY environment variable.
 *
 * <p>Based on the official curl examples for 音视频翻译 - 通义千问 (qwen3-livetranslate-flash).</p>
 *
 * @author yingzi
 * @since 1.1
 */
@EnabledIf("isApiKeySet")
class DashScopeAudioTranscriptionIT {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeAudioTranscriptionIT.class);

	private static final String API_KEY_ENV = "DASHSCOPE_API_KEY";

	// Test audio URL from DashScope official documentation
	private static final String TEST_AUDIO_URL = "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250211/tixcef/cherry.wav";

	private String apiKey;

	private DashScopeAudioTranscriptionModel transcriptionModel;

	static boolean isApiKeySet() {
		return System.getenv(API_KEY_ENV) != null && !System.getenv(API_KEY_ENV).isEmpty();
	}

	@BeforeEach
	void setUp() {
		apiKey = System.getenv(API_KEY_ENV);
		if (apiKey == null || apiKey.isEmpty()) {
			logger.warn("{} is not set, skipping integration tests", API_KEY_ENV);
			return;
		}

		DashScopeAudioTranscriptionApi transcriptionApi =
				DashScopeAudioTranscriptionApi.builder()
						.apiKey(new SimpleApiKey(apiKey))
						.build();

		// Build model with default options
		DashScopeAudioTranscriptionOptions defaultOptions = DashScopeAudioTranscriptionOptions.builder()
				.build();

		transcriptionModel = DashScopeAudioTranscriptionModel.builder()
				.audioTranscriptionApi(transcriptionApi)
				.defaultOptions(defaultOptions)
				.build();
	}

	/**
	 * 对应 curl 示例 - 非流式调用 (stream: false)
	 *
	 * <p>curl 示例:</p>
	 * <pre>
	 * curl -X POST https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions \
	 * -H "Authorization: Bearer $DASHSCOPE_API_KEY" \
	 * -H "Content-Type: application/json" \
	 * -d '{
	 *     "model": "qwen3-livetranslate-flash",
	 *     "messages": [{
	 *         "role": "user",
	 *         "content": [{
	 *             "type": "input_audio",
	 *             "input_audio": {
	 *                 "data": "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250211/tixcef/cherry.wav",
	 *                 "format": "wav"
	 *             }
	 *         }]
	 *     }],
	 *     "modalities": ["text", "audio"],
	 *     "audio": {"voice": "Cherry", "format": "wav"},
	 *     "stream": false,
	 *     "translation_options": {"source_lang": "zh", "target_lang": "en"}
	 * }'
	 * </pre>
	 */
	@org.junit.jupiter.api.Test
	void testLiveTranslate_Call_RealApi() {
		// Arrange - 构造 Options
		DashScopeAudioTranscriptionOptions.Audio audio = new DashScopeAudioTranscriptionOptions.Audio();
		audio.setVoice("Cherry");
		audio.setFormat("wav");

		DashScopeAudioTranscriptionOptions.TranslationOptions translationOptions =
				new DashScopeAudioTranscriptionOptions.TranslationOptions();
		translationOptions.setSourceLang("zh");
		translationOptions.setTargetLang("en");

		DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder()
				.model(AudioModel.QWEN3_LIVETRANSLATE_FLASH.getValue())
				.modalities(List.of("text"))
				.audio(audio)
				.translationOptions(translationOptions)
				.build();

		// Arrange - 构造 Prompt
		DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.InputAudio inputAudio =
				new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.InputAudio(TEST_AUDIO_URL, "wav");
		DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.Content content =
				new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.Content("input_audio", inputAudio);
		DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage message =
				new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage(List.of(content));
		AudioTranscriptionPrompt prompt = new DashScopeAudioTranscriptionPrompt(options, message);

		// Act
		AudioTranscriptionResponse response = transcriptionModel.call(prompt);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response).isInstanceOf(DashScopeAudioTranscriptionResponse.class);

		DashScopeAudioTranscriptionResponse dashScopeResponse =
				(DashScopeAudioTranscriptionResponse) response;
		assertThat(dashScopeResponse.getId()).isNotEmpty();
		assertThat(dashScopeResponse.getModel()).isEqualTo(AudioModel.QWEN3_LIVETRANSLATE_FLASH.getValue());
		assertThat(dashScopeResponse.getObject()).isEqualTo("chat.completion");

		logger.info("LiveTranslate call test passed");
		logger.info("Response ID: {}", dashScopeResponse.getId());
		logger.info("Model: {}", dashScopeResponse.getModel());

		if (dashScopeResponse.getUsage() != null) {
			logger.info("Prompt Tokens: {}", dashScopeResponse.getUsage().promptTokens());
			logger.info("Completion Tokens: {}", dashScopeResponse.getUsage().completionTokens());
			logger.info("Total Tokens: {}", dashScopeResponse.getUsage().totalTokens());
		}

		if (response.getResult() != null) {
			logger.info("Transcription Result: {}", response.getResult().getOutput());
		}

        logger.info("content: {}", dashScopeResponse.getChoices().get(0).message().content());
	}

	/**
	 * 对应 curl 示例 - 流式调用 (stream: true)
	 *
	 * <p>curl 示例:</p>
	 * <pre>
	 * curl -X POST https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions \
	 * -H "Authorization: Bearer $DASHSCOPE_API_KEY" \
	 * -H "Content-Type: application/json" \
	 * -d '{
	 *     "model": "qwen3-livetranslate-flash",
	 *     "messages": [{
	 *         "role": "user",
	 *         "content": [{
	 *             "type": "input_audio",
	 *             "input_audio": {
	 *                 "data": "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250211/tixcef/cherry.wav",
	 *                 "format": "wav"
	 *             }
	 *         }]
	 *     }],
	 *     "modalities": ["text", "audio"],
	 *     "audio": {"voice": "Cherry", "format": "wav"},
	 *     "stream": true,
	 *     "stream_options": {"include_usage": true},
	 *     "translation_options": {"source_lang": "zh", "target_lang": "en"}
	 * }'
	 * </pre>
	 */
	@org.junit.jupiter.api.Test
	void testLiveTranslate_Stream_RealApi() {
		// Arrange - 构造 Options (stream: true)
		DashScopeAudioTranscriptionOptions.Audio audio = new DashScopeAudioTranscriptionOptions.Audio();
		audio.setVoice("Cherry");
		audio.setFormat("wav");

		DashScopeAudioTranscriptionOptions.StreamOptions streamOptions =
				new DashScopeAudioTranscriptionOptions.StreamOptions();
		streamOptions.setIncludeUsage(true);

		DashScopeAudioTranscriptionOptions.TranslationOptions translationOptions =
				new DashScopeAudioTranscriptionOptions.TranslationOptions();
		translationOptions.setSourceLang("zh");
		translationOptions.setTargetLang("en");

		DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder()
				.model(AudioModel.QWEN3_LIVETRANSLATE_FLASH.getValue())
				.modalities(List.of("text", "audio"))
				.audio(audio)
				.streamOptions(streamOptions)
				.translationOptions(translationOptions)
				.build();

		// Arrange - 构造 Prompt
		DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.InputAudio inputAudio =
				new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.InputAudio(TEST_AUDIO_URL, "wav");
		DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.Content content =
				new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage.Content("input_audio", inputAudio);
		DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage message =
				new DashScopeAudioTranscriptionPrompt.TranscriptionUserMessage(List.of(content));
		AudioTranscriptionPrompt prompt = new DashScopeAudioTranscriptionPrompt(options, message);

		// 用于收集所有响应
		List<AudioTranscriptionResponse> responses = new ArrayList<>();

		// Act
		Flux<AudioTranscriptionResponse> result = transcriptionModel.stream(prompt);

		// Assert - 使用 StepVerifier 处理流式响应
		StepVerifier.create(result)
				.thenConsumeWhile(response -> {
					assertThat(response).isNotNull();
					assertThat(response).isInstanceOf(DashScopeAudioTranscriptionResponse.class);

					DashScopeAudioTranscriptionResponse r =
							(DashScopeAudioTranscriptionResponse) response;
					assertThat(r.getId()).isNotEmpty();
					assertThat(r.getModel()).isEqualTo(AudioModel.QWEN3_LIVETRANSLATE_FLASH.getValue());

					responses.add(response);

					// 输出 choices 中的 delta 内容
					if (r.getChoices() != null && !r.getChoices().isEmpty()) {
						var choice = r.getChoices().get(0);
						if (choice.delta() != null) {
							if (choice.delta().content() != null) {
								logger.info("  - Delta content: {}", choice.delta().content());
							}
						}
						if (choice.message() != null && choice.message().content() != null) {
							logger.info("  - Message content: {}", choice.message().content());
						}
					}

					if (r.getUsage() != null) {
						logger.info("  - Usage - Prompt: {}, Completion: {}, Total: {}",
								r.getUsage().promptTokens(), r.getUsage().completionTokens(),
								r.getUsage().totalTokens());
					}

					if (response.getResult() != null) {
						logger.info("  - Result: {}", response.getResult().getOutput());
					}

					return true; // 继续消费更多元素
				})
				.verifyComplete();

		// 验证至少收到了一些响应
		assertThat(responses).isNotEmpty();
		logger.info("LiveTranslate stream test passed, total chunks: {}", responses.size());
	}

	/**
	 * WebSocket 实时短语音翻译
	 *
	 * 测试 gummy-chat-v1 模型的 WebSocket 实时语音识别功能
	 * 注意：由于OkHttp的maxFrameLength限制（256KB），使用较小的音频文件进行测试
	 */
	@org.junit.jupiter.api.Test
	void testWebSocket_short_RealApi() {
		// Arrange - 构造 Options
		DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder()
				.model(AudioModel.GUMMY_CHAT_V1.getValue())
				.sampleRate(16000)
				.format("wav")  // 使用wav格式
				.transcriptionEnabled(true)
				.translationEnabled(true)
				.translationTargetLanguages(List.of("en"))
				.build();

		// Arrange - 构造 Prompt（使用Resource传入音频文件）
		// 使用较小的测试音频文件（<256KB，避免OkHttp maxFrameLength限制）
		org.springframework.core.io.Resource audioResource =
				new org.springframework.core.io.ClassPathResource("audio/qwen-tts/stream-url-test.wav");
		AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);

		// Act - 调用stream方法
		Flux<AudioTranscriptionResponse> result = transcriptionModel.stream(prompt);

		// Assert - 验证响应
		List<DashScopeTranscriptionResponse> responses =
				new ArrayList<>();
		StepVerifier.create(result)
				.thenConsumeWhile(response -> {
					logger.info("WebSocket response received: {}", response);

					assertThat(response).isNotNull();
					assertThat(response).isInstanceOf(DashScopeTranscriptionResponse.class);

					DashScopeTranscriptionResponse r = (DashScopeTranscriptionResponse) response;
                    Transcription transcription = r.getTranscription();
                    logger.info("  - Transcription: {}", transcription.text());
                    List<Translation> translations = r.getTranslations();
                    for (Translation translation : translations) {
                        logger.info("  - Translation: {}", translation.text());
                    }

                    responses.add(r);

					logger.info("WebSocket response received:");

					return true;
				})
				.verifyComplete();

		// 验证至少收到了一些响应
		assertThat(responses).isNotEmpty();
		logger.info("WebSocket test passed, total responses: {}", responses.size());
	}

    /**
     * WebSocket 实时长语音翻译
     *
     * 测试 gummy-realtime-v1 模型的 WebSocket 实时语音识别功能
     * 注意：由于OkHttp的maxFrameLength限制（256KB），使用较小的音频文件进行测试
     */
    @org.junit.jupiter.api.Test
    void testWebSocket_long_RealApi() {
        // Arrange - 构造 Options
        DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder()
                .model(AudioModel.GUMMY_REALTIME_V1.getValue())
                .sampleRate(16000)
                .format("wav")  // 使用wav格式
                .transcriptionEnabled(true)
                .translationEnabled(true)
                .translationTargetLanguages(List.of("en"))
                .build();

        // Arrange - 构造 Prompt（使用Resource传入音频文件）
        // 使用较小的测试音频文件（<256KB，避免OkHttp maxFrameLength限制）
        org.springframework.core.io.Resource audioResource =
                new org.springframework.core.io.ClassPathResource("audio/qwen-tts/stream-url-test.wav");
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);

        // Act - 调用stream方法
        Flux<AudioTranscriptionResponse> result = transcriptionModel.stream(prompt);

        // Assert - 验证响应
        List<DashScopeTranscriptionResponse> responses =
                new ArrayList<>();
        StepVerifier.create(result)
                .thenConsumeWhile(response -> {
                    logger.info("WebSocket response received: {}", response);

                    assertThat(response).isNotNull();
                    assertThat(response).isInstanceOf(DashScopeTranscriptionResponse.class);

                    DashScopeTranscriptionResponse r = (DashScopeTranscriptionResponse) response;
                    Transcription transcription = r.getTranscription();
                    logger.info("  - Transcription: {}", transcription.text());
                    List<Translation> translations = r.getTranslations();
                    for (Translation translation : translations) {
                        logger.info("  - Translation: {}", translation.text());
                    }

                    responses.add(r);

                    logger.info("WebSocket response received:");

                    return true;
                })
                .verifyComplete();

        // 验证至少收到了一些响应
        assertThat(responses).isNotEmpty();
        logger.info("WebSocket test passed, total responses: {}", responses.size());
    }

    /**
     * WebSocket paraformer-realtime-v2 高精度实时语音识别
     *
     * 测试 paraformer-realtime-v2 模型的 WebSocket 实时语音识别功能
     */
    @org.junit.jupiter.api.Test
    void testWebSocket_Paraformer_RealApi() {
        // Arrange - 构造 Options
        DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder()
                .model(AudioModel.PARAFORMER_REALTIME_V2.getValue())
                .sampleRate(16000)
                .format("pcm")
                .disfluencyRemovalEnabled(false)
                .languageHints(List.of("zh")) // 可以改为en
                .vocabularyId(null) // 待传入
                .resources(null) // 待传入
                .build();

        // Arrange - 构造 Prompt（使用Resource传入音频文件）
        // 使用较小的测试音频文件（<256KB，避免OkHttp maxFrameLength限制）
        org.springframework.core.io.Resource audioResource =
                new org.springframework.core.io.ClassPathResource("audio/qwen-tts/stream-url-test.wav");
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);

        // Act - 调用stream方法
        Flux<AudioTranscriptionResponse> result = transcriptionModel.stream(prompt);

        // Assert - 验证响应
        List<DashScopeTranscriptionResponse> responses = new ArrayList<>();
        StepVerifier.create(result)
                .thenConsumeWhile(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response).isInstanceOf(DashScopeTranscriptionResponse.class);

                    DashScopeTranscriptionResponse r = (DashScopeTranscriptionResponse) response;
                    responses.add(r);

                    Sentence sentence = r.getSentence();
                    logger.info("WebSocket response received: {}", sentence.text());

                    return true;
                })
                .verifyComplete();

        assertThat(responses).isNotEmpty();
        logger.info("Paraformer WebSocket test passed, total responses: {}", responses.size());
    }

    /**
     * WebSocket fun-asr-realtime 高精度实时语音识别
     *
     * 测试 fun-asr-realtime 模型的 WebSocket 实时语音识别功能
     */
    @org.junit.jupiter.api.Test
    void testWebSocket_FUNASR_RealApi() {
        // Arrange - 构造 Options
        DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder()
                .model(AudioModel.FUN_ASR_REALTIME.getValue())
                .sampleRate(16000)
                .format("pcm")
                .vocabularyId(null) // 待传入
                .build();

        // Arrange - 构造 Prompt（使用Resource传入音频文件）
        // 使用较小的测试音频文件（<256KB，避免OkHttp maxFrameLength限制）
        org.springframework.core.io.Resource audioResource =
                new org.springframework.core.io.ClassPathResource("audio/qwen-tts/stream-url-test.wav");
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);

        // Act - 调用stream方法
        Flux<AudioTranscriptionResponse> result = transcriptionModel.stream(prompt);

        // Assert - 验证响应
        List<DashScopeTranscriptionResponse> responses = new ArrayList<>();
        StepVerifier.create(result)
                .thenConsumeWhile(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response).isInstanceOf(DashScopeTranscriptionResponse.class);

                    DashScopeTranscriptionResponse r = (DashScopeTranscriptionResponse) response;
                    responses.add(r);

                    Sentence sentence = r.getSentence();
                    logger.info("WebSocket response received: {}", sentence.text());
                    return true;
                })
                .verifyComplete();

        assertThat(responses).isNotEmpty();
        logger.info("Paraformer WebSocket test passed, total responses: {}", responses.size());
    }

}
