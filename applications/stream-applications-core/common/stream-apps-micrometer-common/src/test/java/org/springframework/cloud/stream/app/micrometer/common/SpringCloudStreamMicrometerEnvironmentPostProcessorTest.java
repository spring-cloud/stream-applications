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

package org.springframework.cloud.stream.app.micrometer.common;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.springframework.core.env.PropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@RunWith(Enclosed.class)
public class SpringCloudStreamMicrometerEnvironmentPostProcessorTest {

	public static class TestDefaultMetricsEnabledProperties extends AbstractMicrometerTagTest {

		@Test
		public void testDefaultProperties() {

			assertThat(context).isNotNull();

			PropertySource propertySource = context.getEnvironment().getPropertySources()
					.get(SpringCloudStreamMicrometerEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);

			assertThat(propertySource).isNotNull();

			assertThat(propertySource.getProperty("management.metrics.export.influx.enabled")).isEqualTo("false");
			assertThat(propertySource.getProperty("management.metrics.export.prometheus.enabled")).isEqualTo("false");
			assertThat(propertySource.getProperty("management.metrics.export.prometheus.rsocket.enabled")).isEqualTo("false");
			assertThat(propertySource.getProperty("management.metrics.export.datadog.enabled")).isEqualTo("false");
			assertThat(propertySource.getProperty("management.endpoints.web.exposure.include")).isEqualTo("prometheus");
			assertThat(propertySource.getProperty("management.metrics.export.influx.enabled")).isEqualTo("false");
			assertThat(propertySource.getProperty("management.metrics.export.prometheus.enabled")).isEqualTo("false");
			assertThat(propertySource.getProperty("management.metrics.export.datadog.enabled")).isEqualTo("false");
			assertThat(propertySource.getProperty("management.endpoints.web.exposure.include")).isEqualTo("prometheus");
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
			assertThat(context).isNotNull();

			PropertySource propertySource = context.getEnvironment().getPropertySources()
					.get(SpringCloudStreamMicrometerEnvironmentPostProcessor.PROPERTY_SOURCE_KEY_NAME);

			assertThat(propertySource).isNull();

			assertThat(context.getEnvironment().getProperty("management.metrics.export.influx.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("management.metrics.export.prometheus.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("management.metrics.export.prometheus.rsocket.enabled")).isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("management.metrics.export.datadog.enabled")).isEqualTo("true");

			assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include")).isEqualTo("info,health");
		}
	}
}
