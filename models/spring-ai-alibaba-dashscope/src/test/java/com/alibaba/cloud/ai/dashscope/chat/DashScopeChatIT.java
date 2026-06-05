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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Integration tests for DashScope Chat functionality. These tests will only run if
 * DASHSCOPE_API_KEY or AI_DASHSCOPE_API_KEY environment variable is set.
 *
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
@Tag("integration")
class DashScopeChatIT {

	// Test constants
	private static final String TEST_MODEL = "qwen-plus";

	private static final String SYSTEM_PROMPT = "You are a helpful assistant.";

	private static final String USER_PROMPT = "你是谁？";

	private static final String API_KEY_ENV = "DASHSCOPE_API_KEY";

	private static final String LEGACY_API_KEY_ENV = "AI_DASHSCOPE_API_KEY";

	private String apiKey;

	@BeforeEach
	void setUp() {
		apiKey = System.getenv(API_KEY_ENV);
		if (!hasText(apiKey)) {
			apiKey = System.getenv(LEGACY_API_KEY_ENV);
		}
		Assumptions.assumeTrue(hasText(apiKey),
				"Skipping tests because neither " + API_KEY_ENV + " nor " + LEGACY_API_KEY_ENV + " is set");
	}

	/**
	 * Test basic chat functionality with simple text prompt.
	 */
	@Test
	void testBasicChat() {
		// Create real API client with API key from environment
		DashScopeApi realApi = DashScopeApi.builder().apiKey(apiKey).build();

		// Create chat model with default options
		DashScopeChatOptions options = DashScopeChatOptions.builder()
			.model(TEST_MODEL)
			.resultFormat("message")
			.build();
		DashScopeChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(realApi)
			.defaultOptions(options)
			.build();

		Prompt prompt = new Prompt(List.of(new SystemMessage(SYSTEM_PROMPT), new UserMessage(USER_PROMPT)), options);

		// Call the chat model
		ChatResponse chatResponse = chatModel.call(prompt);
		Generation response = chatResponse.getResult();

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.getOutput().getText()).isNotEmpty();
		String responseId = chatResponse.getMetadata().getId();
		if (responseId != null) {
			assertThat(responseId).isNotBlank();
		}
		System.out.println("Chat Response: " + response.getOutput().getText());
	}

	/**
	 * Test streaming chat functionality.
	 */
	@Test
	void testStreamChat() {
		// Create real API client with API key from environment
		DashScopeApi realApi = DashScopeApi.builder().apiKey(apiKey).build();

		// Create chat model with default options
		DashScopeChatOptions options = DashScopeChatOptions.builder()
			.model(TEST_MODEL)
			.resultFormat("message")
			.incrementalOutput(true)
			.build();
		DashScopeChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(realApi)
			.defaultOptions(options)
			.build();

		Prompt prompt = new Prompt(List.of(new SystemMessage(SYSTEM_PROMPT), new UserMessage(USER_PROMPT)), options);

		// Call the streaming API and collect responses
		StringBuilder responseBuilder = new StringBuilder();
		List<String> chunks = chatModel.stream(prompt)
			.map(response -> response.getResult().getOutput().getText())
			.filter(DashScopeChatIT::hasText)
			.doOnNext(content -> {
				System.out.println("Streaming chunk: " + content);
				responseBuilder.append(content);
			})
			.collectList()
			.block();

		// Verify final response
		String finalResponse = responseBuilder.toString();
		assertThat(chunks).isNotNull().isNotEmpty();
		assertThat(finalResponse).isNotBlank();
		System.out.println("Final streaming response: " + finalResponse);
	}

	private static boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

}
