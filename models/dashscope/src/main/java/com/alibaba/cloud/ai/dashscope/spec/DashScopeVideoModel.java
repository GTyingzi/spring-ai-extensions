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

package com.alibaba.cloud.ai.dashscope.spec;

import org.jetbrains.annotations.NotNull;
import org.springframework.ai.model.ModelDescription;

/**
 * @author yingzi
 * @since 2026/1/18
 */
public enum DashScopeVideoModel implements ModelDescription {

    WANX21_I2V_TURBO("wanx2.1-i2v-turbo"),
    WANX21_I2V_PLUS("wanx2.1-i2v-plus"),
    WANX22_I2V_PLUS("wan2.2-i2v-plus"),
    WAN22_I2V_FLASH("wan2.2-i2v-flash"),
    WAN25_I2V_PREVIEW("wan2.5-i2v-preview"),
    WAN26_I2V_FLASH("wan2.6-i2v-flash"),
    WAN26_I2V("wan2.6-i2v"),
    WAN26_R2V("wan2.6-r2v"),
    WANX21_T2V_PLUS("wanx2.1-t2v-plus"),
    WANX21_T2V_TURBO("wanx2.1-t2v-turbo"),
    WANX22_T2V_PLUS("wan2.2-t2v-plus"),
    WANX25_T2V_PREVIEW("wan2.5-t2v-preview"),
    WANX26_T2V("wan2.6-t2v"),
    WANX21_VACE_PLUS("wanx2.1-vace-plus"),
    VIDEO_STYLE_TRANSFORM("video-style-transform"),

    WANX21_KF2V_PLUS("wanx2.1-kf2v-plus"),
    WAN22_KF2V_FLASH("wan2.2-kf2v-flash"),
    WAN22_ANIMATE_MOVE("wan2.2-animate-move"),
    WAN22_ANIMATE_MIX("wan2.2-animate-mix"),
    WAN22_S2V("wan2.2-s2v"),
    ANIMATE_ANYONE_GEN2("animate-anyone-gen2"),
    EMO_V1("emo-v1"),
    LIVEPORTRAIT("liveportrait"),
    VIDEORETALK("videoretalk"),
    EMOJI_V1("emoji-v1"),

    WAN22_S2V_DETECT("wan2.2-s2v-detect"),
    EMO_DETECT_V1("emo-detect-v1"),
    LIVEPORTRAIT_DETECT("liveportrait-detect"),
    EMOJI_DETECT_V1("emoji-detect-v1"),

    ANIMATE_ANYONE_DETECT_GEN2("animate-anyone-detect-gen2"),

    ANIMATE_ANYONE_TEMPLATE_GEN2("animate-anyone-template-gen2");

    public String value;

    DashScopeVideoModel(String value) {
        this.value = value;
    }

    @NotNull
    @Override
    public String getName() {
        return value;
    }
}
