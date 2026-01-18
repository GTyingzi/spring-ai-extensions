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

import java.util.List;

import com.alibaba.cloud.ai.dashscope.spec.DashScopeVideoModel;
import com.alibaba.cloud.ai.dashscope.video.model.DashScopeVideoRequest.VideoInput;
import com.alibaba.cloud.ai.dashscope.video.model.DashScopeVideoRequest.VideoParameters;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DashScope Video Generation Options.
 *
 * @author dashscope
 * @author yuluo，yingzi
 * @since 1.1.0.0
 */

public class DashScopeVideoOptions implements VideoOptions {

	/**
	 * Default video model.
	 */
    public static final String DEFAULT_MODEL = DashScopeVideoModel.WANX21_T2V_TURBO.getName();

	@JsonProperty("model")
	private String model;

    @JsonProperty("input")
    private VideoInput input;

    @JsonProperty("parameters")
    private VideoParameters parameters;

	@Override
	public String getModel() {
        return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

    public VideoInput getInput() {
        return input;
	}

    public void setInput(VideoInput input) {
        this.input = input;
	}

    public VideoParameters getParameters() {
        return parameters;
	}

    public void setParameters(VideoParameters parameters) {
        this.parameters = parameters;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

        private String model;

        private VideoInput input;

        private VideoParameters parameters;

        public Builder() {
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder input(VideoInput input) {
            this.input = input;
            return this;
        }

        public Builder parameters(VideoParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public DashScopeVideoOptions build() {
            DashScopeVideoOptions options = new DashScopeVideoOptions();
            options.setModel(model == null ? DEFAULT_MODEL : model);
            options.setInput(input);
            options.setParameters(parameters);
            return options;
        }

    }

    /**
     * Builder for VideoInput. Provides a fluent API to construct VideoInput objects.
     */
    public static class VideoInputBuilder {

		private String prompt;

        private String imgUrl;

		private String imageUrl;

        private String audioUrl;

        private String template;

		private String negativePrompt;

		private String firstFrameUrl;

		private String lastFrameUrl;

        private List<String> referenceVideoUrls;

        private String function;

        private String refImageUrl;

        private String refImagesUrl;

        private Integer maskFrameId;

        private String firstClipUrl;

        private String videoUrl;

        private String templateId;

        private List<Integer> faceBbox;

        private List<Integer> extBbox;

        private String drivenId;

        public VideoInputBuilder prompt(String prompt) {
			this.prompt = prompt;
			return this;
        }

        public VideoInputBuilder imgUrl(String imgUrl) {
            this.imgUrl = imgUrl;
            return this;
        }

        public VideoInputBuilder imageUrl(String imageUrl) {
			this.imageUrl = imageUrl;
			return this;
        }

        public VideoInputBuilder audioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
            return this;
        }

        public VideoInputBuilder template(String template) {
            this.template = template;
            return this;
        }

        public VideoInputBuilder negativePrompt(String negativePrompt) {
            this.negativePrompt = negativePrompt;
            return this;
        }

        public VideoInputBuilder firstFrameUrl(String firstFrameUrl) {
			this.firstFrameUrl = firstFrameUrl;
			return this;
        }

        public VideoInputBuilder lastFrameUrl(String lastFrameUrl) {
			this.lastFrameUrl = lastFrameUrl;
			return this;
        }

        public VideoInputBuilder referenceVideoUrls(List<String> referenceVideoUrls) {
            this.referenceVideoUrls = referenceVideoUrls;
            return this;
        }

        public VideoInputBuilder function(String function) {
            this.function = function;
            return this;
        }

        public VideoInputBuilder refImageUrl(String refImageUrl) {
            this.refImageUrl = refImageUrl;
            return this;
        }

        public VideoInputBuilder refImagesUrl(String refImagesUrl) {
            this.refImagesUrl = refImagesUrl;
            return this;
        }

        public VideoInputBuilder maskFrameId(Integer maskFrameId) {
            this.maskFrameId = maskFrameId;
            return this;
        }

        public VideoInputBuilder firstClipUrl(String firstClipUrl) {
            this.firstClipUrl = firstClipUrl;
            return this;
        }

        public VideoInputBuilder videoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
			return this;
        }

        public VideoInputBuilder templateId(String templateId) {
            this.templateId = templateId;
			return this;
        }

        public VideoInputBuilder faceBbox(List<Integer> faceBbox) {
            this.faceBbox = faceBbox;
            return this;
        }

        public VideoInputBuilder extBbox(List<Integer> extBbox) {
            this.extBbox = extBbox;
            return this;
        }

        public VideoInputBuilder drivenId(String drivenId) {
            this.drivenId = drivenId;
            return this;
        }

        public VideoInput build() {
            return VideoInput.builder()
                    .prompt(this.prompt)
                    .imgUrl(this.imgUrl)
                    .imageUrl(this.imageUrl)
                    .audioUrl(this.audioUrl)
                    .template(this.template)
                    .negativePrompt(this.negativePrompt)
                    .firstFrameUrl(this.firstFrameUrl)
                    .lastFrameUrl(this.lastFrameUrl)
                    .referenceVideoUrls(this.referenceVideoUrls)
                    .function(this.function)
                    .refImageUrl(this.refImageUrl)
                    .refImagesUrl(this.refImagesUrl)
                    .maskFrameId(this.maskFrameId)
                    .firstClipUrl(this.firstClipUrl)
                    .videoUrl(this.videoUrl)
                    .templateId(this.templateId)
                    .faceBbox(this.faceBbox)
                    .extBbox(this.extBbox)
                    .drivenId(this.drivenId)
                    .build();
        }

    }

    /**
     * Builder for VideoParameters. Provides a fluent API to construct VideoParameters
     * objects.
     */
    public static class VideoParametersBuilder {

        private String resolution;

        private String size;

        private Boolean promptExtend;

        private Integer duration;

        private String shotType;

        private List<String> objOrBg;

        private String maskType;

        private Double expandRatio;

        private Double topScale;

        private Double bottomScale;

        private Double leftScale;

        private Double rightScale;

        private String mode;

        private Boolean useRefImgBg;

        private String videoRatio;

        private String ratio;

        private String styleLevel;

        private String templateId;

        private Double eyeMoveFreq;

        private Integer videoFps;

        private Integer mouthMoveStrength;

        private Boolean pasteBack;

        private Double headMoveStrength;

        private Integer style;

        private Long seed;

        public VideoParametersBuilder resolution(String resolution) {
            this.resolution = resolution;
            return this;
        }

        public VideoParametersBuilder size(String size) {
			this.size = size;
			return this;
        }

        public VideoParametersBuilder promptExtend(Boolean promptExtend) {
            this.promptExtend = promptExtend;
            return this;
        }

        public VideoParametersBuilder duration(Integer duration) {
			this.duration = duration;
			return this;
        }

        public VideoParametersBuilder shotType(String shotType) {
            this.shotType = shotType;
            return this;
        }

        public VideoParametersBuilder objOrBg(List<String> objOrBg) {
            this.objOrBg = objOrBg;
            return this;
        }

        public VideoParametersBuilder maskType(String maskType) {
            this.maskType = maskType;
            return this;
        }

        public VideoParametersBuilder expandRatio(Double expandRatio) {
            this.expandRatio = expandRatio;
            return this;
        }

        public VideoParametersBuilder topScale(Double topScale) {
            this.topScale = topScale;
            return this;
        }

        public VideoParametersBuilder bottomScale(Double bottomScale) {
            this.bottomScale = bottomScale;
            return this;
        }

        public VideoParametersBuilder leftScale(Double leftScale) {
            this.leftScale = leftScale;
			return this;
        }

        public VideoParametersBuilder rightScale(Double rightScale) {
            this.rightScale = rightScale;
            return this;
        }

        public VideoParametersBuilder mode(String mode) {
            this.mode = mode;
			return this;
        }

        public VideoParametersBuilder useRefImgBg(Boolean useRefImgBg) {
            this.useRefImgBg = useRefImgBg;
			return this;
        }

        public VideoParametersBuilder videoRatio(String videoRatio) {
            this.videoRatio = videoRatio;
			return this;
        }

        public VideoParametersBuilder ratio(String ratio) {
            this.ratio = ratio;
            return this;
        }

        public VideoParametersBuilder styleLevel(String styleLevel) {
            this.styleLevel = styleLevel;
            return this;
        }

        public VideoParametersBuilder templateId(String templateId) {
            this.templateId = templateId;
            return this;
        }

        public VideoParametersBuilder eyeMoveFreq(Double eyeMoveFreq) {
            this.eyeMoveFreq = eyeMoveFreq;
            return this;
        }

        public VideoParametersBuilder videoFps(Integer videoFps) {
            this.videoFps = videoFps;
            return this;
        }

        public VideoParametersBuilder mouthMoveStrength(Integer mouthMoveStrength) {
            this.mouthMoveStrength = mouthMoveStrength;
            return this;
        }

        public VideoParametersBuilder pasteBack(Boolean pasteBack) {
            this.pasteBack = pasteBack;
            return this;
        }

        public VideoParametersBuilder headMoveStrength(Double headMoveStrength) {
            this.headMoveStrength = headMoveStrength;
            return this;
        }

        public VideoParametersBuilder style(Integer style) {
            this.style = style;
            return this;
        }

        public VideoParametersBuilder seed(Long seed) {
            this.seed = seed;
            return this;
        }

        public VideoParameters build() {
            return VideoParameters.builder()
                    .resolution(this.resolution)
                    .size(this.size)
                    .promptExtend(this.promptExtend)
                    .duration(this.duration)
                    .shotType(this.shotType)
                    .objOrBg(this.objOrBg)
                    .maskType(this.maskType)
                    .expandRatio(this.expandRatio)
                    .topScale(this.topScale)
                    .bottomScale(this.bottomScale)
                    .leftScale(this.leftScale)
                    .rightScale(this.rightScale)
                    .mode(this.mode)
                    .useRefImgBg(this.useRefImgBg)
                    .videoRatio(this.videoRatio)
                    .ratio(this.ratio)
                    .styleLevel(this.styleLevel)
                    .templateId(this.templateId)
                    .eyeMoveFreq(this.eyeMoveFreq)
                    .videoFps(this.videoFps)
                    .mouthMoveStrength(this.mouthMoveStrength)
                    .pasteBack(this.pasteBack)
                    .headMoveStrength(this.headMoveStrength)
                    .style(this.style)
                    .seed(this.seed)
				.build();
		}

	}

}
