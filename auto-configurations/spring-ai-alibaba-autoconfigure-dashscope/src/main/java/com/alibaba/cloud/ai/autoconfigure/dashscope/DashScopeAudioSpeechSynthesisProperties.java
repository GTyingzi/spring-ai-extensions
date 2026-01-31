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

package com.alibaba.cloud.ai.autoconfigure.dashscope;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @author kevinlin09
 */

@ConfigurationProperties(DashScopeAudioSpeechSynthesisProperties.CONFIG_PREFIX)
public class DashScopeAudioSpeechSynthesisProperties extends DashScopeParentProperties {

	/**
	 * Spring AI Alibaba configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.ai.dashscope.audio.synthesis";

	@NestedConfigurationProperty
	private DashScopeAudioSpeechOptions options = DashScopeAudioSpeechOptions.builder().build();

	public DashScopeAudioSpeechOptions getOptions() {
		return options;
	}

	public void setOptions(DashScopeAudioSpeechOptions options) {
		this.options = options;
	}

}
