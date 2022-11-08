/*
 * Copyright 2018-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@ContentTypeEnvironmentPostProcessor}.
 *
 * @author Chris Schaefer
 */
public class ContentTypeEnvironmentPostProcessorTests {

	private static final String SINK_INPUT = "input";

	private static final String SOURCE_OUTPUT = "output";

	private static String getContentTypeProperty(String channelName) {
		return ContentTypeEnvironmentPostProcessor.CONTENT_TYPE_PROPERTY_PREFIX + channelName
				+ ContentTypeEnvironmentPostProcessor.CONTENT_TYPE_PROPERTY_SUFFIX;
	}

	@Test
	public void testPostProcessorDefaults() {
		ConfigurableEnvironment configurableEnvironment = getEnvironment();

		PropertySource propertySource = configurableEnvironment.getPropertySources()
				.get(ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);

		assertThat(propertySource).isNotNull();
		assertThat(propertySource.getProperty(getContentTypeProperty(SINK_INPUT))).isEqualTo("application/octet-stream");
		assertThat(propertySource.getProperty(getContentTypeProperty(SOURCE_OUTPUT))).isEqualTo("application/octet-stream");
	}

	@Test
	public void testUserDefinedOutputContentType() {
		PropertiesPropertySource testProperties = buildTestProperties(SOURCE_OUTPUT, "text/plain");
		ConfigurableEnvironment configurableEnvironment = getEnvironment(testProperties);
		assertThat(configurableEnvironment.containsProperty(getContentTypeProperty(SOURCE_OUTPUT))).isTrue();
		assertThat(configurableEnvironment.getProperty(getContentTypeProperty(SOURCE_OUTPUT))).isEqualTo("text/plain");
	}

	@Test
	public void testUserDefinedInputContentType() {
		PropertiesPropertySource testProperties = buildTestProperties(SINK_INPUT, "text/html");
		ConfigurableEnvironment configurableEnvironment = getEnvironment(testProperties);

		assertThat(configurableEnvironment.containsProperty(getContentTypeProperty(SINK_INPUT))).isTrue();
		assertThat(configurableEnvironment.getProperty(getContentTypeProperty(SINK_INPUT))).isEqualTo("text/html");
	}

	@Test
	public void testConfigureCustomChannel() {
		ConfigurableEnvironment configurableEnvironment = getEnvironment(new CustomChannel());

		PropertySource propertySource = configurableEnvironment.getPropertySources()
				.get(ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);

		assertThat(propertySource).isNotNull();
		assertThat(propertySource.containsProperty(getContentTypeProperty("myChannelName"))).isTrue();
		assertThat(propertySource.getProperty(getContentTypeProperty("myChannelName"))).isEqualTo("application/octet-stream");
	}

	@Test
	public void testConfigureDefaultChannelsCustomContentType() {
		ConfigurableEnvironment configurableEnvironment = getEnvironment(new DefaultChannelsCustomContentTypes());

		PropertySource propertySource = configurableEnvironment.getPropertySources()
				.get(ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);

		assertThat(propertySource).isNotNull();
		assertThat(propertySource.containsProperty(getContentTypeProperty(SOURCE_OUTPUT))).isTrue();
		assertThat(propertySource.getProperty(getContentTypeProperty(SOURCE_OUTPUT))).isEqualTo("image/jpeg");
		assertThat(propertySource.containsProperty(getContentTypeProperty(SINK_INPUT))).isTrue();
		assertThat(propertySource.getProperty(getContentTypeProperty(SINK_INPUT))).isEqualTo("image/gif");
	}

	@Test
	public void testConfigureDefaultChannelsSameContentType() {
		ConfigurableEnvironment configurableEnvironment = getEnvironment(new ChannelSameContentType());

		PropertySource propertySource = configurableEnvironment.getPropertySources()
				.get(ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);


		assertThat(propertySource).isNotNull();
		assertThat(propertySource.containsProperty(getContentTypeProperty(SOURCE_OUTPUT))).isTrue();
		assertThat(propertySource.getProperty(getContentTypeProperty(SOURCE_OUTPUT))).isEqualTo("image/jpeg");
		assertThat(propertySource.containsProperty(getContentTypeProperty(SINK_INPUT))).isTrue();
		assertThat(propertySource.getProperty(getContentTypeProperty(SINK_INPUT))).isEqualTo("image/jpeg");
	}

	private PropertiesPropertySource buildTestProperties(String channelName, String contentType) {
		Properties testProperties = new Properties();
		testProperties.setProperty(getContentTypeProperty(channelName), contentType);

		return new PropertiesPropertySource("test-properties", testProperties);
	}

	private ConfigurableEnvironment getEnvironment() {
		return getEnvironment(null, null);
	}

	private ConfigurableEnvironment getEnvironment(PropertiesPropertySource propertiesPropertySource) {
		return getEnvironment(propertiesPropertySource, null);
	}

	private ConfigurableEnvironment getEnvironment(EnvironmentPostProcessor environmentPostProcessor) {
		return getEnvironment(null, environmentPostProcessor);
	}

	private ConfigurableEnvironment getEnvironment(PropertiesPropertySource propertiesPropertySource,
												EnvironmentPostProcessor environmentPostProcessor) {
		SpringApplication springApplication = new SpringApplicationBuilder()
				.sources(ContentTypeEnvironmentPostProcessorTests.class)
				.web(WebApplicationType.NONE).build();

		ConfigurableApplicationContext context = springApplication.run();

		if (propertiesPropertySource != null) {
			context.getEnvironment().getPropertySources().addFirst(propertiesPropertySource);
		}

		if (environmentPostProcessor == null) {
			environmentPostProcessor = new ContentTypeEnvironmentPostProcessor();
		}

		environmentPostProcessor.postProcessEnvironment(context.getEnvironment(), springApplication);

		ConfigurableEnvironment configurableEnvironment = context.getEnvironment();
		context.close();

		return configurableEnvironment;
	}

	public static class CustomChannel extends ContentTypeEnvironmentPostProcessor {
		private static final String CHANNEL_NAME = "myChannelName";
		private static final String CONTENT_TYPE = "application/octet-stream";

		public CustomChannel() {
			super(CHANNEL_NAME, CONTENT_TYPE);
		}
	}

	public static class ChannelSameContentType extends ContentTypeEnvironmentPostProcessor {
		private static final String CONTENT_TYPE = "image/jpeg";

		public ChannelSameContentType() {
			super(CONTENT_TYPE);
		}
	}

	public static class DefaultChannelsCustomContentTypes extends ContentTypeEnvironmentPostProcessor {
		private static Map<String, String> CHANNEL_MAP = createChannelMap();

		public DefaultChannelsCustomContentTypes() {
			super(CHANNEL_MAP);
		}

		private static Map<String, String> createChannelMap() {
			Map<String, String> channelMap = new HashMap<>();
			channelMap.put(SOURCE_OUTPUT, "image/jpeg");
			channelMap.put(SINK_INPUT, "image/gif");

			return channelMap;
		}
	}
}
