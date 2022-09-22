/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.postprocessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * An {@link EnvironmentPostProcessor} to set the {@code spring.cloud.stream.bindings.{input,output}.contentType}
 * channel properties to a default of {@code application/octet-stream} if it has not been set already.
 * <p>
 * Subclasses may extend this class to change the default content type and channel name(s).
 *
 * @author Chris Schaefer
 */
public class ContentTypeEnvironmentPostProcessor implements EnvironmentPostProcessor {
	protected static final String PROPERTY_SOURCE_KEY_NAME = ContentTypeEnvironmentPostProcessor.class.getName();
	protected static final String CONTENT_TYPE_PROPERTY_PREFIX = "spring.cloud.stream.bindings.";
	protected static final String CONTENT_TYPE_PROPERTY_SUFFIX = ".contentType";
	private Map<String, String> channelMap = createChannelMap();
	public ContentTypeEnvironmentPostProcessor() {
		super();
	}

	protected ContentTypeEnvironmentPostProcessor(Map<String, String> channelMap) {
		this.channelMap = channelMap;
	}

	protected ContentTypeEnvironmentPostProcessor(String contentType) {
		for (Map.Entry<String, String> channel : channelMap.entrySet()) {
			channel.setValue(contentType);
		}
	}

	protected ContentTypeEnvironmentPostProcessor(String channelName, String contentType) {
		channelMap.put(channelName, contentType);
	}

	private Map<String, String> createChannelMap() {
		Map<String, String> channelMap = new HashMap<>();
		channelMap.put("input", "application/octet-stream");
		channelMap.put("output", "application/octet-stream");
		return channelMap;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment configurableEnvironment, SpringApplication springApplication) {
		Properties properties = new Properties();

		for (Map.Entry<String, String> channel : channelMap.entrySet()) {
			String propertyKey = CONTENT_TYPE_PROPERTY_PREFIX + channel.getKey() + CONTENT_TYPE_PROPERTY_SUFFIX;

			if (!configurableEnvironment.containsProperty(propertyKey)) {
				properties.setProperty(propertyKey, channel.getValue());
			}
		}

		if (!properties.isEmpty()) {
			PropertiesPropertySource propertiesPropertySource =
					new PropertiesPropertySource(PROPERTY_SOURCE_KEY_NAME, properties);
			configurableEnvironment.getPropertySources().addLast(propertiesPropertySource);
		}
	}
}
