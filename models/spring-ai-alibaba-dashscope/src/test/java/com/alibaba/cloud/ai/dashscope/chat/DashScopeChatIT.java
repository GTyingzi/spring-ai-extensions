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
package com.alibaba.cloud.ai.dashscope.chat;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.MESSAGE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

/**
 * Integration tests for DashScope Chat functionality. These tests will only run if
 * AI_DASHSCOPE_API_KEY environment variable is set.
 *
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class DashScopeChatIT {

	// Test constants
	private static final String TEST_MODEL = "qwen-plus";

	private static final String TEST_SYSTEM_PROMPT = "You are a helpful assistant.";

	private static final String TEST_USER_PROMPT = "你是谁？";

	private static final String API_KEY_ENV = "AI_DASHSCOPE_API_KEY";

	private String apiKey;

	@BeforeEach
	void setUp() {
		// Get API key from environment variable
		apiKey = System.getenv(API_KEY_ENV);
		// Skip tests if API key is not set
		Assumptions.assumeTrue(apiKey != null && !apiKey.trim().isEmpty(),
				"Skipping tests because " + API_KEY_ENV + " environment variable is not set");
	}

	private DashScopeChatModel chatModel(DashScopeChatOptions options) {
		DashScopeApi realApi = DashScopeApi.builder().apiKey(this.apiKey).build();
		return DashScopeChatModel.builder().dashScopeApi(realApi).defaultOptions(options).build();
	}

	private Prompt textPrompt(String userPrompt) {
		return new Prompt(List.of(new SystemMessage(TEST_SYSTEM_PROMPT), new UserMessage(userPrompt)));
	}

	private ChatResponse callOrSkipQuota(DashScopeChatModel chatModel, Prompt prompt) {
		try {
			return chatModel.call(prompt);
		}
		catch (NonTransientAiException ex) {
			if (ex.getMessage() != null && ex.getMessage().contains("Throttling.AllocationQuota")) {
				Assumptions.abort("Skipping real DashScope call because the account quota is exhausted: "
						+ ex.getMessage());
			}
			throw ex;
		}
	}

	/**
	 * Test basic chat functionality with simple text prompt.
	 */
	@Test
	void testBasicChat() {
		DashScopeChatOptions options = DashScopeChatOptions.builder().model(TEST_MODEL).build();

		Generation response = callOrSkipQuota(chatModel(options), textPrompt(TEST_USER_PROMPT)).getResult();

		assertThat(response).isNotNull();
		assertThat(response.getOutput().getText()).isNotEmpty();
		System.out.println("Chat Response: " + response.getOutput().getText());
	}

	/**
	 * Test streaming chat functionality.
	 */
	@Test
	void testStreamChat() {
		DashScopeChatOptions options = DashScopeChatOptions.builder()
			.model(TEST_MODEL)
			.incrementalOutput(true)
			.build();

		StringBuilder responseBuilder = new StringBuilder();
		Flux<Generation> responseFlux = chatModel(options).stream(textPrompt(TEST_USER_PROMPT)).map(ChatResponse::getResult);

		responseFlux.doOnNext(generation -> {
			String content = generation.getOutput().getText();
			System.out.println("Streaming chunk: " + content);
			responseBuilder.append(content);
		}).blockLast(Duration.ofSeconds(60));

		// Verify final response
		String finalResponse = responseBuilder.toString();
		assertThat(finalResponse).isNotEmpty();
		System.out.println("Final streaming response: " + finalResponse);
	}

	@Test
	void testImageInputWithUrls() throws Exception {
		DashScopeChatOptions options = DashScopeChatOptions.builder().model("qwen-vl-plus").multiModel(true).build();
		List<Media> media = List.of(
				new Media(MimeTypeUtils.IMAGE_JPEG,
						new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg")),
				new Media(MimeTypeUtils.IMAGE_PNG,
						new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/tiger.png")),
				new Media(MimeTypeUtils.IMAGE_PNG,
						new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/rabbit.png")));
		UserMessage message = UserMessage.builder().text("这些是什么?").media(media).build();
		message.getMetadata().put(MESSAGE_FORMAT, MessageFormat.IMAGE);

		ChatResponse response = callOrSkipQuota(chatModel(options), new Prompt(message));

		assertThat(response.getResult().getOutput().getText()).isNotEmpty();
		System.out.println("Image Response: " + response.getResult().getOutput().getText());
	}

	@Test
	void testVideoInputWithUrls() throws Exception {
		DashScopeChatOptions options = DashScopeChatOptions.builder().model("qwen-vl-max").multiModel(true).build();
		List<Media> media = List.of(
				new Media(MimeTypeUtils.IMAGE_JPEG,
						new URI("https://img.alicdn.com/imgextra/i3/O1CN01K3SgGo1eqmlUgeE9b_!!6000000003923-0-tps-3840-2160.jpg")),
				new Media(MimeTypeUtils.IMAGE_JPEG,
						new URI("https://img.alicdn.com/imgextra/i4/O1CN01BjZvwg1Y23CF5qIRB_!!6000000003000-0-tps-3840-2160.jpg")),
				new Media(MimeTypeUtils.IMAGE_JPEG,
						new URI("https://img.alicdn.com/imgextra/i4/O1CN01Ib0clU27vTgBdbVLQ_!!6000000007859-0-tps-3840-2160.jpg")),
				new Media(MimeTypeUtils.IMAGE_JPEG,
						new URI("https://img.alicdn.com/imgextra/i1/O1CN01aygPLW1s3EXCdSN4X_!!6000000005710-0-tps-3840-2160.jpg")));
		UserMessage message = UserMessage.builder().text("描述这个视频的具体过程").media(media).build();
		message.getMetadata().put(MESSAGE_FORMAT, MessageFormat.VIDEO);

		ChatResponse response = callOrSkipQuota(chatModel(options), new Prompt(message));

		assertThat(response.getResult().getOutput().getText()).isNotEmpty();
		System.out.println("Video Response: " + response.getResult().getOutput().getText());
	}

	@Test
	void testAudioInputWithUrl() throws Exception {
		DashScopeChatOptions options = DashScopeChatOptions.builder().model("qwen-audio-turbo").multiModel(true).build();
		UserMessage message = UserMessage.builder()
			.text("这段音频在说什么?")
			.media(new Media(MimeTypeUtils.parseMimeType("audio/mpeg"),
					new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3")))
			.build();
		message.getMetadata().put(MESSAGE_FORMAT, MessageFormat.AUDIO);

		ChatResponse response = callOrSkipQuota(chatModel(options),
				new Prompt(List.of(new SystemMessage(TEST_SYSTEM_PROMPT), message)));

		assertThat(response.getResult().getOutput().getText()).isNotEmpty();
		System.out.println("Audio Response: " + response.getResult().getOutput().getText());
	}

	@Test
	void testSearchEnabled() {
		DashScopeChatOptions options = DashScopeChatOptions.builder()
			.model(TEST_MODEL)
			.enableSearch(true)
			.build();

		ChatResponse response = callOrSkipQuota(chatModel(options), textPrompt("明天杭州天气如何？"));

		assertThat(response.getResult().getOutput().getText()).isNotEmpty();
		System.out.println("Search Response: " + response.getResult().getOutput().getText());
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_FILE_ID", matches = ".+")
	void testDocumentUnderstanding() {
		DashScopeChatOptions options = DashScopeChatOptions.builder().model("qwen-long").build();
		Prompt prompt = new Prompt(List.of(new SystemMessage(TEST_SYSTEM_PROMPT),
				new SystemMessage("fileid://" + System.getenv("AI_DASHSCOPE_FILE_ID")),
				new UserMessage("这篇文章讲了什么？")));

		ChatResponse response = callOrSkipQuota(chatModel(options), prompt);

		assertThat(response.getResult().getOutput().getText()).isNotEmpty();
		System.out.println("Document Response: " + response.getResult().getOutput().getText());
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_RUN_PPT_IT", matches = "true")
	void testPptGenerationStream() {
		DashScopeChatOptions options = DashScopeChatOptions.builder()
			.model("qwen-doc-turbo")
			.incrementalOutput(true)
			.extraBody(Map.of("skill", List.of(Map.of("type", "ppt", "mode", "general", "template_id", "news_01"))))
			.build();
		Prompt prompt = new Prompt(List.of(new SystemMessage(TEST_SYSTEM_PROMPT), new SystemMessage("您的文档内容"),
				new UserMessage("生成一个10到20页的ppt")));

		StringBuilder responseBuilder = new StringBuilder();
		chatModel(options).stream(prompt).map(ChatResponse::getResult).doOnNext(generation -> responseBuilder
			.append(generation.getOutput().getText())).blockLast(Duration.ofSeconds(120));

		assertThat(responseBuilder.toString()).isNotEmpty();
		System.out.println("PPT Response: " + responseBuilder);
	}

}
