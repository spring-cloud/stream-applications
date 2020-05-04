/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.stream.app.micrometer.common;

import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.springframework.core.env.PropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Tzolov
 */
@RunWith(Enclosed.class)
public class SpringCloudStreamMicrometerEnvironmentPostProcessorTest {

	public static class TestDefaultMetricsEnabledProperties extends AbstractMicrometerTagTest {

		@Test
		public void testDefaultProperties() {
			assertNotNull(context);

			PropertySource propertySource = context.getEnvironment().getPropertySources()
					.get(SpringCloudStreamMicrometerEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);

			assertNotNull("Property source "
							+ SpringCloudStreamMicrometerEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME + " is null",
					propertySource);

			assertThat(propertySource.getProperty("management.metrics.export.influx.enabled"), Is.is("false"));
			assertThat(propertySource.getProperty("management.metrics.export.prometheus.enabled"), Is.is("false"));
			assertThat(propertySource.getProperty("management.metrics.export.prometheus.rsocket.enabled"), Is.is("false"));
			assertThat(propertySource.getProperty("management.metrics.export.datadog.enabled"), Is.is("false"));

			assertThat(propertySource.getProperty("management.endpoints.web.exposure.include"), Is.is("prometheus"));

			assertThat(context.getEnvironment().getProperty("management.metrics.export.influx.enabled"), Is.is("false"));
			assertThat(context.getEnvironment().getProperty("management.metrics.export.prometheus.enabled"), Is.is("false"));
			assertThat(context.getEnvironment().getProperty("management.metrics.export.datadog.enabled"), Is.is("false"));

			assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include"), Is.is("prometheus"));

		}
	}

	@TestPropertySource(properties = {
			"management.metrics.export.simple.enabled=true",
			"management.metrics.export.influx.enabled=true",
			"management.metrics.export.prometheus.enabled=true",
			"management.metrics.export.prometheus.rsocket.enabled=true",
			"management.metrics.export.datadog.enabled=true",
			"management.endpoints.web.exposure.include=info,health"})
	public static class TestOverrideMetricsEnabledProperties extends AbstractMicrometerTagTest {

		@Test
		public void testOverrideProperties() {
			assertNotNull(context);

			PropertySource propertySource = context.getEnvironment().getPropertySources()
					.get(SpringCloudStreamMicrometerEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);

			assertNull("Property source "
							+ SpringCloudStreamMicrometerEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME + " is not null",
					propertySource);

			assertThat(context.getEnvironment().getProperty("management.metrics.export.influx.enabled"), Is.is("true"));
			assertThat(context.getEnvironment().getProperty("management.metrics.export.prometheus.enabled"), Is.is("true"));
			assertThat(context.getEnvironment().getProperty("management.metrics.export.prometheus.rsocket.enabled"), Is.is("true"));
			assertThat(context.getEnvironment().getProperty("management.metrics.export.datadog.enabled"), Is.is("true"));

			assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include"), Is.is("info,health"));
		}
	}
}
