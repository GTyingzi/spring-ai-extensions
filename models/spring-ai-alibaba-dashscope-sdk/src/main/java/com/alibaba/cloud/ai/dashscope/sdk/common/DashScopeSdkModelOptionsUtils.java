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
package com.alibaba.cloud.ai.dashscope.sdk.common;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Compatibility helpers for Spring AI model options APIs removed in Spring AI 2.0.0-M8.
 */
public final class DashScopeSdkModelOptionsUtils {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
		.build()
		.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

	private static final List<String> BEAN_MERGE_FIELD_EXCISIONS = List.of("class");

	private static final ConcurrentHashMap<Class<?>, List<String>> REQUEST_FIELD_NAMES_PER_CLASS = new ConcurrentHashMap<>();

	private DashScopeSdkModelOptionsUtils() {
	}

	public static <T> @Nullable T mergeOption(@Nullable T runtimeValue, @Nullable T defaultValue) {
		return ObjectUtils.isEmpty(runtimeValue) ? defaultValue : runtimeValue;
	}

	public static <T> T merge(@Nullable Object source, Object target, Class<T> clazz) {
		Object sourceToMerge = source == null ? Map.of() : source;
		List<String> requestFieldNames = REQUEST_FIELD_NAMES_PER_CLASS.computeIfAbsent(clazz,
				DashScopeSdkModelOptionsUtils::getJsonPropertyValues);

		if (CollectionUtils.isEmpty(requestFieldNames)) {
			throw new IllegalArgumentException("No @JsonProperty fields found in " + clazz.getName());
		}

		Map<String, Object> sourceMap = objectToMap(sourceToMerge);
		Map<String, Object> targetMap = objectToMap(target);

		targetMap.putAll(sourceMap.entrySet()
			.stream()
			.filter(entry -> entry.getValue() != null)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

		targetMap = targetMap.entrySet()
			.stream()
			.filter(entry -> requestFieldNames.contains(entry.getKey()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		return mapToClass(targetMap, clazz);
	}

	public static Map<String, Object> objectToMap(@Nullable Object source) {
		if (source == null) {
			return new HashMap<>();
		}
		try {
			String json = OBJECT_MAPPER.writeValueAsString(source);
			return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
			})
				.entrySet()
				.stream()
				.filter(entry -> entry.getValue() != null)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Failed to convert options to map", e);
		}
	}

	public static <T> T mapToClass(Map<String, Object> source, Class<T> clazz) {
		try {
			String json = OBJECT_MAPPER.writeValueAsString(source);
			return OBJECT_MAPPER.readValue(json, clazz);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Failed to convert map to " + clazz.getName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <I, S extends I, T extends S> @Nullable T copyToTarget(@Nullable S sourceBean,
			Class<I> sourceInterfaceClazz, Class<T> targetBeanClazz) {

		Assert.notNull(sourceInterfaceClazz, "Source options class must not be null");
		Assert.notNull(targetBeanClazz, "Target options class must not be null");

		if (sourceBean == null) {
			return null;
		}

		if (sourceBean.getClass().isAssignableFrom(targetBeanClazz)) {
			return (T) sourceBean;
		}

		try {
			T targetOptions = targetBeanClazz.getConstructor().newInstance();
			mergeBeans(sourceBean, targetOptions, sourceInterfaceClazz, true);
			return targetOptions;
		}
		catch (Exception e) {
			throw new IllegalArgumentException(
					"Failed to convert " + sourceInterfaceClazz.getName() + " into " + targetBeanClazz.getName(), e);
		}
	}

	public static <I, S extends I, T extends S> T mergeBeans(S source, T target, Class<I> sourceInterfaceClazz,
			boolean overrideNonNullTargetValues) {
		Assert.notNull(source, "Source object must not be null");
		Assert.notNull(target, "Target object must not be null");

		BeanWrapper sourceBean = new BeanWrapperImpl(source);
		BeanWrapper targetBean = new BeanWrapperImpl(target);
		List<String> interfaceNames = Arrays.stream(sourceInterfaceClazz.getMethods()).map(method -> method.getName())
			.toList();

		for (PropertyDescriptor descriptor : sourceBean.getPropertyDescriptors()) {
			if (BEAN_MERGE_FIELD_EXCISIONS.contains(descriptor.getName())
					|| !interfaceNames.contains(toGetName(descriptor.getName()))) {
				continue;
			}

			String propertyName = descriptor.getName();
			Object value = sourceBean.getPropertyValue(propertyName);
			if (value != null) {
				Object targetValue = targetBean.getPropertyValue(propertyName);
				if (targetValue == null || overrideNonNullTargetValues) {
					targetBean.setPropertyValue(propertyName, value);
				}
			}
		}

		return target;
	}

	private static List<String> getJsonPropertyValues(Class<?> clazz) {
		return Arrays.stream(clazz.getDeclaredFields())
			.map(field -> field.getAnnotation(JsonProperty.class))
			.filter(annotation -> annotation != null && !annotation.value().isBlank())
			.map(JsonProperty::value)
			.toList();
	}

	private static String toGetName(String name) {
		return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
	}

}
