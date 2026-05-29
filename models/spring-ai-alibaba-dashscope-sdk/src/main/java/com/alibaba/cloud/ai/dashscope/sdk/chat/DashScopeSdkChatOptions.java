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

package com.alibaba.cloud.ai.dashscope.sdk.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Options for DashScope SDK chat model.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeSdkChatOptions implements ToolCallingChatOptions {

	@JsonProperty("model")
	private @Nullable String model;

	@JsonIgnore
	private @Nullable Boolean stream;

	@JsonProperty("temperature")
	private @Nullable Double temperature;

	@JsonProperty("seed")
	private @Nullable Integer seed;

	@JsonProperty("top_p")
	private @Nullable Double topP;

	@JsonProperty("top_k")
	private @Nullable Integer topK;

	@JsonProperty("stop")
	private @Nullable List<Object> stop;

	@JsonProperty("enable_search")
	private Boolean enableSearch = false;

	@JsonProperty("max_tokens")
	private @Nullable Integer maxTokens;

	@JsonProperty("incremental_output")
	private Boolean incrementalOutput = true;

	@JsonProperty("repetition_penalty")
	private @Nullable Double repetitionPenalty;

	@JsonProperty("tool_choice")
	private @Nullable Object toolChoice;

	@JsonIgnore
	private Map<String, String> httpHeaders = new HashMap<>();

	@JsonIgnore
	private List<ToolCallback> toolCallbacks = new ArrayList<>();

	@JsonIgnore
	private Set<String> toolNames = new HashSet<>();

	@JsonIgnore
	private @Nullable Boolean internalToolExecutionEnabled;

	@JsonIgnore
	private Map<String, Object> toolContext = new HashMap<>();

	@JsonProperty("extra_body")
	private @Nullable Map<String, Object> extraBody;

	public static DashScopeSdkChatOptionsBuilder builder() {
		return new DashScopeSdkChatOptionsBuilder();
	}

	public static @Nullable DashScopeSdkChatOptions fromOptions(@Nullable DashScopeSdkChatOptions options) {
		if (options == null) {
			return null;
		}
		DashScopeSdkChatOptions copy = new DashScopeSdkChatOptions();
		copy.setModel(options.getModel());
		copy.setStream(options.getStream());
		copy.setTemperature(options.getTemperature());
		copy.setSeed(options.getSeed());
		copy.setTopP(options.getTopP());
		copy.setTopK(options.getTopK());
		copy.setStop(options.getStop() == null ? null : new ArrayList<>(options.getStop()));
		copy.setEnableSearch(options.getEnableSearch());
		copy.setMaxTokens(options.getMaxTokens());
		copy.setIncrementalOutput(options.getIncrementalOutput());
		copy.setRepetitionPenalty(options.getRepetitionPenalty());
		copy.setToolChoice(options.getToolChoice());
		copy.setHttpHeaders(options.getHttpHeaders() == null ? new HashMap<>() : new HashMap<>(options.getHttpHeaders()));
		copy.setToolCallbacks(
				options.getToolCallbacks() == null ? new ArrayList<>() : new ArrayList<>(options.getToolCallbacks()));
		copy.setToolNames(options.getToolNames() == null ? new HashSet<>() : new HashSet<>(options.getToolNames()));
		copy.setInternalToolExecutionEnabled(options.getInternalToolExecutionEnabled());
		copy.setToolContext(options.getToolContext() == null ? new HashMap<>() : new HashMap<>(options.getToolContext()));
		copy.setExtraBody(options.getExtraBody() == null ? null : new HashMap<>(options.getExtraBody()));
		return copy;
	}

	@Override
	public @Nullable String getModel() {
		return this.model;
	}

	public void setModel(@Nullable String model) {
		this.model = model;
	}

	public @Nullable Boolean getStream() {
		return this.stream;
	}

	public void setStream(@Nullable Boolean stream) {
		this.stream = stream;
	}

	@Override
	public @Nullable Double getTemperature() {
		return this.temperature;
	}

	public void setTemperature(@Nullable Double temperature) {
		this.temperature = temperature;
	}

	public @Nullable Integer getSeed() {
		return this.seed;
	}

	public void setSeed(@Nullable Integer seed) {
		this.seed = seed;
	}

	@Override
	public @Nullable Double getTopP() {
		return this.topP;
	}

	public void setTopP(@Nullable Double topP) {
		this.topP = topP;
	}

	@Override
	public @Nullable Integer getTopK() {
		return this.topK;
	}

	public void setTopK(@Nullable Integer topK) {
		this.topK = topK;
	}

	public @Nullable List<Object> getStop() {
		return this.stop;
	}

	public void setStop(@Nullable List<Object> stop) {
		this.stop = stop;
	}

	public Boolean getEnableSearch() {
		return this.enableSearch;
	}

	public void setEnableSearch(Boolean enableSearch) {
		this.enableSearch = enableSearch;
	}

	@Override
	public @Nullable Integer getMaxTokens() {
		return this.maxTokens;
	}

	public void setMaxTokens(@Nullable Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	public Boolean getIncrementalOutput() {
		return this.incrementalOutput;
	}

	public void setIncrementalOutput(Boolean incrementalOutput) {
		this.incrementalOutput = incrementalOutput;
	}

	public @Nullable Double getRepetitionPenalty() {
		return this.repetitionPenalty;
	}

	public void setRepetitionPenalty(@Nullable Double repetitionPenalty) {
		this.repetitionPenalty = repetitionPenalty;
	}

	public @Nullable Object getToolChoice() {
		return this.toolChoice;
	}

	public void setToolChoice(@Nullable Object toolChoice) {
		this.toolChoice = toolChoice;
	}

	public Map<String, String> getHttpHeaders() {
		return this.httpHeaders;
	}

	public void setHttpHeaders(Map<String, String> httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

	public @Nullable Map<String, Object> getExtraBody() {
		return this.extraBody;
	}

	public void setExtraBody(@Nullable Map<String, Object> extraBody) {
		this.extraBody = extraBody;
	}

	@Override
	public @Nullable Double getFrequencyPenalty() {
		return null;
	}

	@Override
	public @Nullable Double getPresencePenalty() {
		return null;
	}

	@Override
	public @Nullable List<String> getStopSequences() {
		if (this.stop == null) {
			return null;
		}
		List<String> stopStrings = this.stop.stream().filter(String.class::isInstance).map(String.class::cast).toList();
		return stopStrings.isEmpty() ? null : stopStrings;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends ChatOptions> T copy() {
		return (T) Objects.requireNonNull(DashScopeSdkChatOptions.fromOptions(this));
	}

	@Override
	public ToolCallingChatOptions.Builder<?> mutate() {
		return new DashScopeSdkChatOptionsBuilder(Objects.requireNonNull(DashScopeSdkChatOptions.fromOptions(this)));
	}

	@Override
	@JsonIgnore
	public List<ToolCallback> getToolCallbacks() {
		return this.toolCallbacks;
	}

	@JsonIgnore
	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
		Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
		this.toolCallbacks = toolCallbacks;
	}

	@Override
	@JsonIgnore
	public Set<String> getToolNames() {
		return this.toolNames;
	}

	@JsonIgnore
	public void setToolNames(Set<String> toolNames) {
		Assert.notNull(toolNames, "toolNames cannot be null");
		Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
		toolNames.forEach(tool -> Assert.hasText(tool, "toolNames cannot contain empty elements"));
		this.toolNames = toolNames;
	}

	@Override
	@JsonIgnore
	public @Nullable Boolean getInternalToolExecutionEnabled() {
		return this.internalToolExecutionEnabled;
	}

	@JsonIgnore
	public void setInternalToolExecutionEnabled(@Nullable Boolean internalToolExecutionEnabled) {
		this.internalToolExecutionEnabled = internalToolExecutionEnabled;
	}

	@Override
	public Map<String, Object> getToolContext() {
		return this.toolContext;
	}

	public void setToolContext(Map<String, Object> toolContext) {
		this.toolContext = toolContext;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DashScopeSdkChatOptions that = (DashScopeSdkChatOptions) o;
		return Objects.equals(this.model, that.model) && Objects.equals(this.stream, that.stream)
				&& Objects.equals(this.temperature, that.temperature) && Objects.equals(this.seed, that.seed)
				&& Objects.equals(this.topP, that.topP) && Objects.equals(this.topK, that.topK)
				&& Objects.equals(this.stop, that.stop) && Objects.equals(this.enableSearch, that.enableSearch)
				&& Objects.equals(this.maxTokens, that.maxTokens)
				&& Objects.equals(this.incrementalOutput, that.incrementalOutput)
				&& Objects.equals(this.repetitionPenalty, that.repetitionPenalty)
				&& Objects.equals(this.toolChoice, that.toolChoice)
				&& Objects.equals(this.httpHeaders, that.httpHeaders)
				&& Objects.equals(this.toolCallbacks, that.toolCallbacks)
				&& Objects.equals(this.toolNames, that.toolNames)
				&& Objects.equals(this.internalToolExecutionEnabled, that.internalToolExecutionEnabled)
				&& Objects.equals(this.toolContext, that.toolContext)
				&& Objects.equals(this.extraBody, that.extraBody);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.model, this.stream, this.temperature, this.seed, this.topP, this.topK, this.stop,
				this.enableSearch, this.maxTokens, this.incrementalOutput, this.repetitionPenalty, this.toolChoice,
				this.httpHeaders, this.toolCallbacks, this.toolNames, this.internalToolExecutionEnabled, this.toolContext,
				this.extraBody);
	}

	@Override
	public String toString() {
		return "DashScopeSdkChatOptions{" + "model='" + this.model + '\'' + ", stream=" + this.stream
				+ ", temperature=" + this.temperature + ", seed=" + this.seed + ", topP=" + this.topP + ", topK="
				+ this.topK + ", stop=" + this.stop + ", enableSearch=" + this.enableSearch + ", maxTokens="
				+ this.maxTokens + ", incrementalOutput=" + this.incrementalOutput + ", repetitionPenalty="
				+ this.repetitionPenalty + ", toolChoice=" + this.toolChoice + ", httpHeaders=" + this.httpHeaders
				+ ", toolNames=" + this.toolNames + ", extraBody=" + this.extraBody + '}';
	}

	public static class DashScopeSdkChatOptionsBuilder
			implements ToolCallingChatOptions.Builder<DashScopeSdkChatOptionsBuilder> {

		private DashScopeSdkChatOptions options;

		public DashScopeSdkChatOptionsBuilder() {
			this.options = new DashScopeSdkChatOptions();
		}

		private DashScopeSdkChatOptionsBuilder(DashScopeSdkChatOptions options) {
			this.options = options;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder clone() {
			return new DashScopeSdkChatOptionsBuilder(Objects.requireNonNull(DashScopeSdkChatOptions.fromOptions(this.options)));
		}

		@Override
		public DashScopeSdkChatOptionsBuilder model(@Nullable String model) {
			this.options.model = model;
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder frequencyPenalty(@Nullable Double frequencyPenalty) {
			return this;
		}

		public DashScopeSdkChatOptionsBuilder stream(@Nullable Boolean stream) {
			this.options.stream = stream;
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder temperature(@Nullable Double temperature) {
			this.options.temperature = temperature;
			return this;
		}

		public DashScopeSdkChatOptionsBuilder seed(@Nullable Integer seed) {
			this.options.seed = seed;
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder topP(@Nullable Double topP) {
			this.options.topP = topP;
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder topK(@Nullable Integer topK) {
			this.options.topK = topK;
			return this;
		}

		public DashScopeSdkChatOptionsBuilder stop(@Nullable List<Object> stop) {
			this.options.stop = stop;
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder maxTokens(@Nullable Integer maxTokens) {
			this.options.maxTokens = maxTokens;
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder presencePenalty(@Nullable Double presencePenalty) {
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder stopSequences(@Nullable List<String> stopSequences) {
			this.options.stop = stopSequences == null ? null : new ArrayList<>(stopSequences);
			return this;
		}

		public DashScopeSdkChatOptionsBuilder enableSearch(Boolean enableSearch) {
			this.options.enableSearch = enableSearch;
			return this;
		}

		public DashScopeSdkChatOptionsBuilder incrementalOutput(Boolean incrementalOutput) {
			this.options.incrementalOutput = incrementalOutput;
			return this;
		}

		public DashScopeSdkChatOptionsBuilder repetitionPenalty(@Nullable Double repetitionPenalty) {
			this.options.repetitionPenalty = repetitionPenalty;
			return this;
		}

		public DashScopeSdkChatOptionsBuilder toolChoice(@Nullable Object toolChoice) {
			this.options.toolChoice = toolChoice;
			return this;
		}

		public DashScopeSdkChatOptionsBuilder httpHeaders(Map<String, String> httpHeaders) {
			this.options.httpHeaders = httpHeaders;
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder toolCallbacks(@Nullable List<ToolCallback> toolCallbacks) {
			if (toolCallbacks == null) {
				this.options.toolCallbacks = new ArrayList<>();
				return this;
			}
			Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
			this.options.toolCallbacks = toolCallbacks;
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder toolCallbacks(ToolCallback... toolCallbacks) {
			Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
			Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
			this.options.toolCallbacks.addAll(List.of(toolCallbacks));
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder toolNames(@Nullable Set<String> toolNames) {
			if (toolNames == null) {
				this.options.toolNames = new HashSet<>();
				return this;
			}
			Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
			toolNames.forEach(tool -> Assert.hasText(tool, "toolNames cannot contain empty elements"));
			this.options.toolNames = toolNames;
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder toolNames(String... toolNames) {
			Assert.notNull(toolNames, "toolNames cannot be null");
			this.options.toolNames.addAll(Set.of(toolNames));
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder internalToolExecutionEnabled(
				@Nullable Boolean internalToolExecutionEnabled) {
			this.options.internalToolExecutionEnabled = internalToolExecutionEnabled;
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder toolContext(@Nullable Map<String, Object> toolContext) {
			if (toolContext == null) {
				this.options.toolContext = new HashMap<>();
				return this;
			}
			this.options.toolContext = toolContext;
			return this;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder toolContext(String key, Object value) {
			Assert.hasText(key, "key cannot be empty");
			Assert.notNull(value, "value cannot be null");
			this.options.toolContext.put(key, value);
			return this;
		}

		public DashScopeSdkChatOptionsBuilder extraBody(@Nullable Map<String, Object> extraBody) {
			this.options.extraBody = extraBody;
			return this;
		}

		@Override
		public DashScopeSdkChatOptions build() {
			return this.options;
		}

		@Override
		public DashScopeSdkChatOptionsBuilder combineWith(ChatOptions.Builder<?> other) {
			ChatOptions otherOptions = other.build();
			if (otherOptions.getModel() != null) {
				this.options.model = otherOptions.getModel();
			}
			if (otherOptions.getMaxTokens() != null) {
				this.options.maxTokens = otherOptions.getMaxTokens();
			}
			if (otherOptions.getTemperature() != null) {
				this.options.temperature = otherOptions.getTemperature();
			}
			if (otherOptions.getTopK() != null) {
				this.options.topK = otherOptions.getTopK();
			}
			if (otherOptions.getTopP() != null) {
				this.options.topP = otherOptions.getTopP();
			}
			if (otherOptions.getStopSequences() != null) {
				this.options.stop = new ArrayList<>(otherOptions.getStopSequences());
			}
			if (otherOptions instanceof ToolCallingChatOptions toolCallingOptions) {
				this.options.toolCallbacks = new ArrayList<>(toolCallingOptions.getToolCallbacks());
				this.options.toolNames = new HashSet<>(toolCallingOptions.getToolNames());
				this.options.toolContext = new HashMap<>(toolCallingOptions.getToolContext());
				this.options.internalToolExecutionEnabled = toolCallingOptions.getInternalToolExecutionEnabled();
			}
			if (otherOptions instanceof DashScopeSdkChatOptions dashScopeOptions) {
				this.options = Objects.requireNonNull(DashScopeSdkChatOptions.fromOptions(dashScopeOptions));
			}
			return this;
		}

	}

}
