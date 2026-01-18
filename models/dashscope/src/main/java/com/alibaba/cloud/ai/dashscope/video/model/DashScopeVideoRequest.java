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

package com.alibaba.cloud.ai.dashscope.video.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DashScope Video Generation Request.
 *
 * @author yingzi
 * @since 2026/1/18
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeVideoRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("input")
    private VideoInput input;

    @JsonProperty("parameters")
    private VideoParameters parameters;

    public DashScopeVideoRequest(String model, VideoInput input, VideoParameters parameters) {
        this.model = model;
        this.input = input;
        this.parameters = parameters;
    }

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

        public DashScopeVideoRequest build() {
            return new DashScopeVideoRequest(this.model, this.input, this.parameters);
        }

    }

    /**
     * Video input parameters.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VideoInput {

        @JsonProperty("prompt")
        private String prompt;

        @JsonProperty("img_url")
        private String imgUrl;

        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("audio_url")
        private String audioUrl;

        @JsonProperty("template")
        private String template;

        @JsonProperty("negative_prompt")
        private String negativePrompt;

        @JsonProperty("first_frame_url")
        private String firstFrameUrl;

        @JsonProperty("last_frame_url")
        private String lastFrameUrl;

        @JsonProperty("reference_video_urls")
        private List<String> referenceVideoUrls;

        @JsonProperty("function")
        private String function;

        @JsonProperty("ref_image_url")
        private String refImageUrl;

        @JsonProperty("ref_images_url")
        private String refImagesUrl;

        @JsonProperty("mask_frame_id")
        private Integer maskFrameId;

        @JsonProperty("first_clip_url")
        private String firstClipUrl;

        @JsonProperty("video_url")
        private String videoUrl;

        @JsonProperty("template_id")
        private String templateId;

        @JsonProperty("face_bbox")
        private List<Integer> faceBbox;

        @JsonProperty("ext_bbox")
        private List<Integer> extBbox;

        @JsonProperty("driven_id")
        private String drivenId;

        public VideoInput() {
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String getImgUrl() {
            return imgUrl;
        }

        public void setImgUrl(String imgUrl) {
            this.imgUrl = imgUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getAudioUrl() {
            return audioUrl;
        }

        public void setAudioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        public String getNegativePrompt() {
            return negativePrompt;
        }

        public void setNegativePrompt(String negativePrompt) {
            this.negativePrompt = negativePrompt;
        }

        public String getFirstFrameUrl() {
            return firstFrameUrl;
        }

        public void setFirstFrameUrl(String firstFrameUrl) {
            this.firstFrameUrl = firstFrameUrl;
        }

        public String getLastFrameUrl() {
            return lastFrameUrl;
        }

        public void setLastFrameUrl(String lastFrameUrl) {
            this.lastFrameUrl = lastFrameUrl;
        }

        public List<String> getReferenceVideoUrls() {
            return referenceVideoUrls;
        }

        public void setReferenceVideoUrls(List<String> referenceVideoUrls) {
            this.referenceVideoUrls = referenceVideoUrls;
        }

        public String getFunction() {
            return function;
        }

        public void setFunction(String function) {
            this.function = function;
        }

        public String getRefImageUrl() {
            return refImageUrl;
        }

        public void setRefImageUrl(String refImageUrl) {
            this.refImageUrl = refImageUrl;
        }

        public String getRefImagesUrl() {
            return refImagesUrl;
        }

        public void setRefImagesUrl(String refImagesUrl) {
            this.refImagesUrl = refImagesUrl;
        }

        public Integer getMaskFrameId() {
            return maskFrameId;
        }

        public void setMaskFrameId(Integer maskFrameId) {
            this.maskFrameId = maskFrameId;
        }

        public String getFirstClipUrl() {
            return firstClipUrl;
        }

        public void setFirstClipUrl(String firstClipUrl) {
            this.firstClipUrl = firstClipUrl;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }

        public List<Integer> getFaceBbox() {
            return faceBbox;
        }

        public void setFaceBbox(List<Integer> faceBbox) {
            this.faceBbox = faceBbox;
        }

        public List<Integer> getExtBbox() {
            return extBbox;
        }

        public void setExtBbox(List<Integer> extBbox) {
            this.extBbox = extBbox;
        }

        public String getDrivenId() {
            return drivenId;
        }

        public void setDrivenId(String drivenId) {
            this.drivenId = drivenId;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

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

            public Builder prompt(String prompt) {
                this.prompt = prompt;
                return this;
            }

            public Builder imgUrl(String imgUrl) {
                this.imgUrl = imgUrl;
                return this;
            }

            public Builder imageUrl(String imageUrl) {
                this.imageUrl = imageUrl;
                return this;
            }

            public Builder audioUrl(String audioUrl) {
                this.audioUrl = audioUrl;
                return this;
            }

            public Builder template(String template) {
                this.template = template;
                return this;
            }

            public Builder negativePrompt(String negativePrompt) {
                this.negativePrompt = negativePrompt;
                return this;
            }

            public Builder firstFrameUrl(String firstFrameUrl) {
                this.firstFrameUrl = firstFrameUrl;
                return this;
            }

            public Builder lastFrameUrl(String lastFrameUrl) {
                this.lastFrameUrl = lastFrameUrl;
                return this;
            }

            public Builder referenceVideoUrls(List<String> referenceVideoUrls) {
                this.referenceVideoUrls = referenceVideoUrls;
                return this;
            }

            public Builder function(String function) {
                this.function = function;
                return this;
            }

            public Builder refImageUrl(String refImageUrl) {
                this.refImageUrl = refImageUrl;
                return this;
            }

            public Builder refImagesUrl(String refImagesUrl) {
                this.refImagesUrl = refImagesUrl;
                return this;
            }

            public Builder maskFrameId(Integer maskFrameId) {
                this.maskFrameId = maskFrameId;
                return this;
            }

            public Builder firstClipUrl(String firstClipUrl) {
                this.firstClipUrl = firstClipUrl;
                return this;
            }

            public Builder videoUrl(String videoUrl) {
                this.videoUrl = videoUrl;
                return this;
            }

            public Builder templateId(String templateId) {
                this.templateId = templateId;
                return this;
            }

            public Builder faceBbox(List<Integer> faceBbox) {
                this.faceBbox = faceBbox;
                return this;
            }

            public Builder extBbox(List<Integer> extBbox) {
                this.extBbox = extBbox;
                return this;
            }

            public Builder drivenId(String drivenId) {
                this.drivenId = drivenId;
                return this;
            }

            public VideoInput build() {
                VideoInput input = new VideoInput();
                input.setPrompt(this.prompt);
                input.setImgUrl(this.imgUrl);
                input.setImageUrl(this.imageUrl);
                input.setAudioUrl(this.audioUrl);
                input.setTemplate(this.template);
                input.setNegativePrompt(this.negativePrompt);
                input.setFirstFrameUrl(this.firstFrameUrl);
                input.setLastFrameUrl(this.lastFrameUrl);
                input.setReferenceVideoUrls(this.referenceVideoUrls);
                input.setFunction(this.function);
                input.setRefImageUrl(this.refImageUrl);
                input.setRefImagesUrl(this.refImagesUrl);
                input.setMaskFrameId(this.maskFrameId);
                input.setFirstClipUrl(this.firstClipUrl);
                input.setVideoUrl(this.videoUrl);
                input.setTemplateId(this.templateId);
                input.setFaceBbox(this.faceBbox);
                input.setExtBbox(this.extBbox);
                input.setDrivenId(this.drivenId);
                return input;
            }

        }

    }

    /**
     * Video generation parameters.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VideoParameters {

        @JsonProperty("resolution")
        private String resolution;

        @JsonProperty("size")
        private String size;

        @JsonProperty("prompt_extend")
        private Boolean promptExtend;

        @JsonProperty("duration")
        private Integer duration;

        @JsonProperty("shot_type")
        private String shotType;

        @JsonProperty("obj_or_bg")
        private List<String> objOrBg;

        @JsonProperty("mask_type")
        private String maskType;

        @JsonProperty("expand_ratio")
        private Double expandRatio;

        @JsonProperty("top_scale")
        private Double topScale;

        @JsonProperty("bottom_scale")
        private Double bottomScale;

        @JsonProperty("left_scale")
        private Double leftScale;

        @JsonProperty("right_scale")
        private Double rightScale;

        @JsonProperty("mode")
        private String mode;

        @JsonProperty("use_ref_img_bg")
        private Boolean useRefImgBg;

        @JsonProperty("video_ratio")
        private String videoRatio;

        @JsonProperty("ratio")
        private String ratio;

        @JsonProperty("style_level")
        private String styleLevel;

        @JsonProperty("template_id")
        private String templateId;

        @JsonProperty("eye_move_freq")
        private Double eyeMoveFreq;

        @JsonProperty("video_fps")
        private Integer videoFps;

        @JsonProperty("mouth_move_strength")
        private Integer mouthMoveStrength;

        @JsonProperty("paste_back")
        private Boolean pasteBack;

        @JsonProperty("head_move_strength")
        private Double headMoveStrength;

        @JsonProperty("style")
        private Integer style;

        @JsonProperty("seed")
        private Long seed;

        public VideoParameters() {
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public Boolean getPromptExtend() {
            return promptExtend;
        }

        public void setPromptExtend(Boolean promptExtend) {
            this.promptExtend = promptExtend;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }

        public String getShotType() {
            return shotType;
        }

        public void setShotType(String shotType) {
            this.shotType = shotType;
        }

        public List<String> getObjOrBg() {
            return objOrBg;
        }

        public void setObjOrBg(List<String> objOrBg) {
            this.objOrBg = objOrBg;
        }

        public String getMaskType() {
            return maskType;
        }

        public void setMaskType(String maskType) {
            this.maskType = maskType;
        }

        public Double getExpandRatio() {
            return expandRatio;
        }

        public void setExpandRatio(Double expandRatio) {
            this.expandRatio = expandRatio;
        }

        public Double getTopScale() {
            return topScale;
        }

        public void setTopScale(Double topScale) {
            this.topScale = topScale;
        }

        public Double getBottomScale() {
            return bottomScale;
        }

        public void setBottomScale(Double bottomScale) {
            this.bottomScale = bottomScale;
        }

        public Double getLeftScale() {
            return leftScale;
        }

        public void setLeftScale(Double leftScale) {
            this.leftScale = leftScale;
        }

        public Double getRightScale() {
            return rightScale;
        }

        public void setRightScale(Double rightScale) {
            this.rightScale = rightScale;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public Boolean getUseRefImgBg() {
            return useRefImgBg;
        }

        public void setUseRefImgBg(Boolean useRefImgBg) {
            this.useRefImgBg = useRefImgBg;
        }

        public String getVideoRatio() {
            return videoRatio;
        }

        public void setVideoRatio(String videoRatio) {
            this.videoRatio = videoRatio;
        }

        public String getRatio() {
            return ratio;
        }

        public void setRatio(String ratio) {
            this.ratio = ratio;
        }

        public String getStyleLevel() {
            return styleLevel;
        }

        public void setStyleLevel(String styleLevel) {
            this.styleLevel = styleLevel;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }

        public Double getEyeMoveFreq() {
            return eyeMoveFreq;
        }

        public void setEyeMoveFreq(Double eyeMoveFreq) {
            this.eyeMoveFreq = eyeMoveFreq;
        }

        public Integer getVideoFps() {
            return videoFps;
        }

        public void setVideoFps(Integer videoFps) {
            this.videoFps = videoFps;
        }

        public Integer getMouthMoveStrength() {
            return mouthMoveStrength;
        }

        public void setMouthMoveStrength(Integer mouthMoveStrength) {
            this.mouthMoveStrength = mouthMoveStrength;
        }

        public Boolean getPasteBack() {
            return pasteBack;
        }

        public void setPasteBack(Boolean pasteBack) {
            this.pasteBack = pasteBack;
        }

        public Double getHeadMoveStrength() {
            return headMoveStrength;
        }

        public void setHeadMoveStrength(Double headMoveStrength) {
            this.headMoveStrength = headMoveStrength;
        }

        public Integer getStyle() {
            return style;
        }

        public void setStyle(Integer style) {
            this.style = style;
        }

        public Long getSeed() {
            return seed;
        }

        public void setSeed(Long seed) {
            this.seed = seed;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

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

            public Builder resolution(String resolution) {
                this.resolution = resolution;
                return this;
            }

            public Builder size(String size) {
                this.size = size;
                return this;
            }

            public Builder promptExtend(Boolean promptExtend) {
                this.promptExtend = promptExtend;
                return this;
            }

            public Builder duration(Integer duration) {
                this.duration = duration;
                return this;
            }

            public Builder shotType(String shotType) {
                this.shotType = shotType;
                return this;
            }

            public Builder objOrBg(List<String> objOrBg) {
                this.objOrBg = objOrBg;
                return this;
            }

            public Builder maskType(String maskType) {
                this.maskType = maskType;
                return this;
            }

            public Builder expandRatio(Double expandRatio) {
                this.expandRatio = expandRatio;
                return this;
            }

            public Builder topScale(Double topScale) {
                this.topScale = topScale;
                return this;
            }

            public Builder bottomScale(Double bottomScale) {
                this.bottomScale = bottomScale;
                return this;
            }

            public Builder leftScale(Double leftScale) {
                this.leftScale = leftScale;
                return this;
            }

            public Builder rightScale(Double rightScale) {
                this.rightScale = rightScale;
                return this;
            }

            public Builder mode(String mode) {
                this.mode = mode;
                return this;
            }

            public Builder useRefImgBg(Boolean useRefImgBg) {
                this.useRefImgBg = useRefImgBg;
                return this;
            }

            public Builder videoRatio(String videoRatio) {
                this.videoRatio = videoRatio;
                return this;
            }

            public Builder ratio(String ratio) {
                this.ratio = ratio;
                return this;
            }

            public Builder styleLevel(String styleLevel) {
                this.styleLevel = styleLevel;
                return this;
            }

            public Builder templateId(String templateId) {
                this.templateId = templateId;
                return this;
            }

            public Builder eyeMoveFreq(Double eyeMoveFreq) {
                this.eyeMoveFreq = eyeMoveFreq;
                return this;
            }

            public Builder videoFps(Integer videoFps) {
                this.videoFps = videoFps;
                return this;
            }

            public Builder mouthMoveStrength(Integer mouthMoveStrength) {
                this.mouthMoveStrength = mouthMoveStrength;
                return this;
            }

            public Builder pasteBack(Boolean pasteBack) {
                this.pasteBack = pasteBack;
                return this;
            }

            public Builder headMoveStrength(Double headMoveStrength) {
                this.headMoveStrength = headMoveStrength;
                return this;
            }

            public Builder style(Integer style) {
                this.style = style;
                return this;
            }

            public Builder seed(Long seed) {
                this.seed = seed;
                return this;
            }

            public VideoParameters build() {
                VideoParameters parameters = new VideoParameters();
                parameters.setResolution(this.resolution);
                parameters.setSize(this.size);
                parameters.setPromptExtend(this.promptExtend);
                parameters.setDuration(this.duration);
                parameters.setShotType(this.shotType);
                parameters.setObjOrBg(this.objOrBg);
                parameters.setMaskType(this.maskType);
                parameters.setExpandRatio(this.expandRatio);
                parameters.setTopScale(this.topScale);
                parameters.setBottomScale(this.bottomScale);
                parameters.setLeftScale(this.leftScale);
                parameters.setRightScale(this.rightScale);
                parameters.setMode(this.mode);
                parameters.setUseRefImgBg(this.useRefImgBg);
                parameters.setVideoRatio(this.videoRatio);
                parameters.setRatio(this.ratio);
                parameters.setStyleLevel(this.styleLevel);
                parameters.setTemplateId(this.templateId);
                parameters.setEyeMoveFreq(this.eyeMoveFreq);
                parameters.setVideoFps(this.videoFps);
                parameters.setMouthMoveStrength(this.mouthMoveStrength);
                parameters.setPasteBack(this.pasteBack);
                parameters.setHeadMoveStrength(this.headMoveStrength);
                parameters.setStyle(this.style);
                parameters.setSeed(this.seed);
                return parameters;
            }

        }

    }

}
