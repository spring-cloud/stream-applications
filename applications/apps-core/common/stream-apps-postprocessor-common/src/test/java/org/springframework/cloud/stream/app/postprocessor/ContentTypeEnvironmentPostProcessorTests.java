/*
 * Copyright 2018 the original author or authors.
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

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@ContentTypeEnvironmentPostProcessor}.
 *
 * @author Chris Schaefer
 */
public class ContentTypeEnvironmentPostProcessorTests {
	@Test
	public void testPostProcessorDefaults() {
		ConfigurableEnvironment configurableEnvironment = getEnvironment();

		PropertySource propertySource = configurableEnvironment.getPropertySources()
				.get(ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);

		assertNotNull("Property source " + ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME + " is null",
				propertySource);

		assertTrue("Unexpected input content type", propertySource.getProperty(getContentTypeProperty(Sink.INPUT))
				.equals("application/octet-stream"));

		assertTrue("Unexpected output content type", propertySource.getProperty(getContentTypeProperty(Source.OUTPUT))
				.equals("application/octet-stream"));
	}

	@Test
	public void testUserDefinedOutputContentType() {
		PropertiesPropertySource testProperties = buildTestProperties(Source.OUTPUT, "text/plain");
		ConfigurableEnvironment configurableEnvironment = getEnvironment(testProperties);

		assertTrue("Output contentType property key not found",
				configurableEnvironment.containsProperty(getContentTypeProperty(Source.OUTPUT)));

		assertTrue("Unexpected output content type", configurableEnvironment.getProperty(getContentTypeProperty(Source.OUTPUT))
				.equals("text/plain"));
	}

	@Test
	public void testUserDefinedInputContentType() {
		PropertiesPropertySource testProperties = buildTestProperties(Sink.INPUT, "text/html");
		ConfigurableEnvironment configurableEnvironment = getEnvironment(testProperties);

		assertTrue("Input contentType property key not found",
				configurableEnvironment.containsProperty(getContentTypeProperty(Sink.INPUT)));

		assertTrue("Unexpected input content type", configurableEnvironment.getProperty(getContentTypeProperty(Sink.INPUT))
				.equals("text/html"));
	}

	@Test
	public void testConfigureCustomChannel() {
		ConfigurableEnvironment configurableEnvironment = getEnvironment(new CustomChannel());

		PropertySource propertySource = configurableEnvironment.getPropertySources()
				.get(ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);

		assertNotNull("Property source " + ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME + " is null",
				propertySource);

		assertTrue("myChannelName contentType property key not found",
				propertySource.containsProperty(getContentTypeProperty("myChannelName")));

		assertTrue("Unexpected myChannelName content type", propertySource.getProperty(getContentTypeProperty("myChannelName"))
				.equals("application/octet-stream"));
	}

	public static class CustomChannel extends ContentTypeEnvironmentPostProcessor {
		private static final String CHANNEL_NAME = "myChannelName";
		private static final String CONTENT_TYPE = "application/octet-stream";

		public CustomChannel() {
			super(CHANNEL_NAME, CONTENT_TYPE);
		}
	}

	@Test
	public void testConfigureDefaultChannelsCustomContentType() {
		ConfigurableEnvironment configurableEnvironment = getEnvironment(new DefaultChannelsCustomContentTypes());

		PropertySource propertySource = configurableEnvironment.getPropertySources()
				.get(ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);

		assertNotNull("Property source " + ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME + " is null",
				propertySource);

		assertTrue("Output contentType property key not found",
				propertySource.containsProperty(getContentTypeProperty(Source.OUTPUT)));

		assertTrue("Unexpected output content type", propertySource.getProperty(getContentTypeProperty(Source.OUTPUT))
				.equals("image/jpeg"));

		assertTrue("Input contentType property key not found",
				propertySource.containsProperty(getContentTypeProperty(Sink.INPUT)));

		assertTrue("Unexpected input content type", propertySource.getProperty(getContentTypeProperty(Sink.INPUT))
				.equals("image/gif"));
	}

	@Test
	public void testConfigureDefaultChannelsSameContentType() {
		ConfigurableEnvironment configurableEnvironment = getEnvironment(new ChannelSameContentType());

		PropertySource propertySource = configurableEnvironment.getPropertySources()
				.get(ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);

		assertNotNull("Property source " + ContentTypeEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME + " is null",
				propertySource);

		assertTrue("Output contentType property key not found",
				propertySource.containsProperty(getContentTypeProperty(Source.OUTPUT)));

		assertTrue("Unexpected output content type", propertySource.getProperty(getContentTypeProperty(Source.OUTPUT))
				.equals("image/jpeg"));

		assertTrue("Input contentType property key not found",
				propertySource.containsProperty(getContentTypeProperty(Sink.INPUT)));

		assertTrue("Unexpected input content type", propertySource.getProperty(getContentTypeProperty(Sink.INPUT))
				.equals("image/jpeg"));
	}

	public static class ChannelSameContentType extends ContentTypeEnvironmentPostProcessor {
		private static final String CONTENT_TYPE = "image/jpeg";

		public ChannelSameContentType() {
			super(CONTENT_TYPE);
		}
	}

	public static class DefaultChannelsCustomContentTypes extends ContentTypeEnvironmentPostProcessor {
		private static Map<String, String> CHANNEL_MAP = createChannelMap();

		private static Map<String, String> createChannelMap() {
			Map<String, String> channelMap = new HashMap<>();
			channelMap.put(Source.OUTPUT, "image/jpeg");
			channelMap.put(Sink.INPUT, "image/gif");

			return channelMap;
		}

		public DefaultChannelsCustomContentTypes() {
			super(CHANNEL_MAP);
		}
	}

	private static String getContentTypeProperty(String channelName) {
		return ContentTypeEnvironmentPostProcessor.CONTENT_TYPE_PROPERTY_PREFIX + channelName
				+ ContentTypeEnvironmentPostProcessor.CONTENT_TYPE_PROPERTY_SUFFIX;
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
}
