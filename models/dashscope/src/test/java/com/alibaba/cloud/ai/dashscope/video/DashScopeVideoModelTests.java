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
package com.alibaba.cloud.ai.dashscope.video;

import java.util.ArrayList;

import com.alibaba.cloud.ai.dashscope.api.DashScopeVideoApi;
import com.alibaba.cloud.ai.dashscope.video.model.DashScopeVideoRequest;
import com.alibaba.cloud.ai.dashscope.video.model.DashScopeVideoResponse;
import com.alibaba.cloud.ai.dashscope.video.model.DashScopeVideoResponse.VideoOutput;
import com.alibaba.cloud.ai.dashscope.video.model.DashScopeVideoResponse.VideoUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeVideoModel. Tests cover basic video generation, custom options,
 * async task handling, error handling, and edge cases.
 *
 * @author yingzi
 * @since 1.1.0.0
 */
class DashScopeVideoModelTests {

    // Test constants
    private static final String TEST_MODEL = "wan2.2-t2v-plus";

    private static final String TEST_TASK_ID = "test-task-id-123456";

    private static final String TEST_REQUEST_ID = "test-request-id-789";

    private static final String TEST_VIDEO_URL = "https://example.com/generated-video.mp4";

    private static final String TEST_PROMPT = "低对比度，在一个复古的70年代风格地铁站里，街头音乐家在昏暗的色彩和粗糙的质感中演奏";

    private DashScopeVideoApi dashScopeVideoApi;

    private DashScopeVideoModel videoModel;

    private DashScopeVideoOptions defaultOptions;

    @BeforeEach
    void setUp() {
        // Initialize mock objects and test instances
        dashScopeVideoApi = Mockito.mock(DashScopeVideoApi.class);

        // Create default options with basic configuration
        DashScopeVideoOptions.VideoInputBuilder inputBuilder = new DashScopeVideoOptions.VideoInputBuilder();
        DashScopeVideoOptions.VideoParametersBuilder parametersBuilder = new DashScopeVideoOptions.VideoParametersBuilder();

        defaultOptions = DashScopeVideoOptions.builder()
                .model(TEST_MODEL)
                .input(inputBuilder.prompt(TEST_PROMPT).build())
                .parameters(parametersBuilder.size("832*480").promptExtend(true).build())
                .build();

        videoModel = new DashScopeVideoModel(dashScopeVideoApi, defaultOptions, RetryTemplate.builder().build());
    }

    @Test
    void testBasicVideoGeneration() {
        // Test basic video generation with successful response
        mockSuccessfulVideoGeneration();

        VideoPrompt prompt = VideoPrompt.builder().content(TEST_PROMPT).build();
        VideoResponse response = videoModel.call(prompt);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getOutput().videoUrl()).isEqualTo(TEST_VIDEO_URL);
        assertThat(response.getResult().getOutput().taskStatus()).isEqualTo("SUCCEEDED");
    }

    @Test
    void testVideoGenerationWithCustomOptions() {
        // Test video generation with custom options
        mockSuccessfulVideoGeneration();

        DashScopeVideoOptions.VideoInputBuilder inputBuilder = new DashScopeVideoOptions.VideoInputBuilder();
        DashScopeVideoOptions.VideoParametersBuilder parametersBuilder = new DashScopeVideoOptions.VideoParametersBuilder();

        DashScopeVideoOptions customOptions = DashScopeVideoOptions.builder()
                .model(TEST_MODEL)
                .input(inputBuilder.prompt(TEST_PROMPT).negativePrompt("低质量，模糊").build())
                .parameters(parametersBuilder.size("1280*720").promptExtend(false).duration(5).build())
                .build();

        VideoPrompt prompt = VideoPrompt.builder().options(customOptions).build();
        VideoResponse response = videoModel.call(prompt);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getOutput().videoUrl()).isEqualTo(TEST_VIDEO_URL);
    }

    @Test
    void testFailedVideoGeneration() {
        // Test handling of failed video generation
        mockFailedVideoGeneration();

        VideoPrompt prompt = VideoPrompt.builder().content(TEST_PROMPT).build();
        VideoResponse response = videoModel.call(prompt);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNull();
    }

    @Test
    void testNullResponse() {
        // Test handling of null API response
        when(dashScopeVideoApi.submitVideoGenTask(any(DashScopeVideoRequest.class))).thenReturn(null);

        VideoPrompt prompt = VideoPrompt.builder().content(TEST_PROMPT).build();
        VideoResponse response = videoModel.call(prompt);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNull();
    }

    @Test
    void testNullTaskIdResponse() {
        // Test handling of null task ID in submit response
        VideoOutput submitOutput = new VideoOutput(null, "PENDING", null, null, null, null, null, null, null, null, null, false, false, false, null, null);
        DashScopeVideoResponse submitResponse = new DashScopeVideoResponse(TEST_REQUEST_ID, submitOutput, null);
        when(dashScopeVideoApi.submitVideoGenTask(any(DashScopeVideoRequest.class))).thenReturn(ResponseEntity.ok(submitResponse));

        VideoPrompt prompt = VideoPrompt.builder().content(TEST_PROMPT).build();
        VideoResponse response = videoModel.call(prompt);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNull();
    }

    @Test
    void testNullPrompt() {
        // Test handling of null prompt
        assertThatThrownBy(() -> videoModel.call(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt");
    }

    @Test
    void testEmptyPrompt() {
        // Test handling of empty prompt
        assertThatThrownBy(() -> videoModel.call(VideoPrompt.builder()
                .messages(new ArrayList<>())
                .build())).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Prompt instructions");
    }

    @Test
    void testBuilderPattern() {
        // Test using builder pattern to create model
        DashScopeVideoModel builtModel = DashScopeVideoModel.builder()
                .videoApi(dashScopeVideoApi)
                .defaultOptions(defaultOptions)
                .retryTemplate(RetryTemplate.builder().build())
                .build();

        assertThat(builtModel).isNotNull();
    }

    @Test
    void testVideoOptionsWithSeed() {
        // Test video generation with seed parameter for reproducibility
        mockSuccessfulVideoGeneration();

        DashScopeVideoOptions.VideoInputBuilder inputBuilder = new DashScopeVideoOptions.VideoInputBuilder();
        DashScopeVideoOptions.VideoParametersBuilder parametersBuilder = new DashScopeVideoOptions.VideoParametersBuilder();

        DashScopeVideoOptions optionsWithSeed = DashScopeVideoOptions.builder()
                .model(TEST_MODEL)
                .input(inputBuilder.prompt(TEST_PROMPT).build())
                .parameters(parametersBuilder.size("832*480").seed(42L).build())
                .build();

        VideoPrompt prompt = VideoPrompt.builder().options(optionsWithSeed).build();
        VideoResponse response = videoModel.call(prompt);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
    }

    @Test
    void testVideoOptionsWithAllParameters() {
        // Test video generation with comprehensive parameter configuration
        mockSuccessfulVideoGeneration();

        DashScopeVideoOptions.VideoInputBuilder inputBuilder = new DashScopeVideoOptions.VideoInputBuilder();
        DashScopeVideoOptions.VideoParametersBuilder parametersBuilder = new DashScopeVideoOptions.VideoParametersBuilder();

        DashScopeVideoOptions comprehensiveOptions = DashScopeVideoOptions.builder()
                .model(TEST_MODEL)
                .input(inputBuilder.prompt(TEST_PROMPT)
                        .negativePrompt("低质量")
                        .firstFrameUrl("https://example.com/first-frame.jpg")
                        .build())
                .parameters(parametersBuilder.size("1280*720")
                        .promptExtend(true)
                        .duration(5)
                        .seed(123L)
                        .resolution("1080p")
                        .build())
                .build();

        VideoPrompt prompt = VideoPrompt.builder().options(comprehensiveOptions).build();
        VideoResponse response = videoModel.call(prompt);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getOutput().videoUrl()).isEqualTo(TEST_VIDEO_URL);
    }

    @Test
    void testVideoGenerationWithImageToVideo() {
        // Test image-to-video generation with image URL input
        mockSuccessfulVideoGeneration();

        DashScopeVideoOptions.VideoInputBuilder inputBuilder = new DashScopeVideoOptions.VideoInputBuilder();
        DashScopeVideoOptions.VideoParametersBuilder parametersBuilder = new DashScopeVideoOptions.VideoParametersBuilder();

        DashScopeVideoOptions imageToVideoOptions = DashScopeVideoOptions.builder()
                .model(TEST_MODEL)
                .input(inputBuilder.prompt(TEST_PROMPT).imageUrl("https://example.com/input.jpg").build())
                .parameters(parametersBuilder.size("832*480").duration(5).build())
                .build();

        VideoPrompt prompt = VideoPrompt.builder().options(imageToVideoOptions).build();
        VideoResponse response = videoModel.call(prompt);

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
    }

    private void mockSuccessfulVideoGeneration() {
        // Mock successful task submission
        VideoOutput submitOutput = new VideoOutput(TEST_TASK_ID, "PENDING", null, null, null, null, null, null, null, null, null, false, false, false, null, null);
        DashScopeVideoResponse submitResponse = new DashScopeVideoResponse(TEST_REQUEST_ID, submitOutput, null);
        when(dashScopeVideoApi.submitVideoGenTask(any(DashScopeVideoRequest.class))).thenReturn(ResponseEntity.ok(submitResponse));

        // Mock successful task completion
        VideoOutput completedOutput = new VideoOutput(TEST_TASK_ID, "SUCCEEDED", null, null, null, null, null, TEST_VIDEO_URL, null, null, null, true, false, true, null, null);
        VideoUsage usage = new VideoUsage(5, 0, 5, 1, 0, "832*480", "16:9", "5s", 0);
        DashScopeVideoResponse completedResponse = new DashScopeVideoResponse(TEST_REQUEST_ID, completedOutput, usage);
        when(dashScopeVideoApi.queryVideoGenTask(TEST_TASK_ID)).thenReturn(ResponseEntity.ok(completedResponse));
    }

    private void mockFailedVideoGeneration() {
        // Mock successful task submission but failed completion
        VideoOutput submitOutput = new VideoOutput(TEST_TASK_ID, "PENDING", null, null, null, null, null, null, null, null, null, false, false, false, null, null);
        DashScopeVideoResponse submitResponse = new DashScopeVideoResponse(TEST_REQUEST_ID, submitOutput, null);
        when(dashScopeVideoApi.submitVideoGenTask(any(DashScopeVideoRequest.class))).thenReturn(ResponseEntity.ok(submitResponse));

        // Mock failed task completion
        VideoOutput failedOutput = new VideoOutput(TEST_TASK_ID, "FAILED", null, null, null, null, null, null, "VIDEO_GEN_ERROR", "Video generation failed due to internal error", null, false, false, false, null, null);
        DashScopeVideoResponse failedResponse = new DashScopeVideoResponse(TEST_REQUEST_ID, failedOutput, null);
        when(dashScopeVideoApi.queryVideoGenTask(anyString())).thenReturn(ResponseEntity.ok(failedResponse));
    }

}
