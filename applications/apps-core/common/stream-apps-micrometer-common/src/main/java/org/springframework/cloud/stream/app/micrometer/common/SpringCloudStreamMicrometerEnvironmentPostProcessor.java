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

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * Disables all Micrometer Repositories added as App Starters dependencies by default.
 * That means disabling Datadog, Influx and Prometheus.
 *
 * @author Christian Tzolov
 */
public class SpringCloudStreamMicrometerEnvironmentPostProcessor implements EnvironmentPostProcessor {

	protected static final String PROPERTY_SOURCE_KEY_NAME = SpringCloudStreamMicrometerEnvironmentPostProcessor.class.getName();

	private final static String METRICS_PROPERTY_NAME_TEMPLATE = "management.metrics.export.%s.enabled";

	private final static String[] METRICS_REPOSITORY_NAMES =
			new String[] { "datadog", "influx", "prometheus", "prometheus.rsocket" };

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Properties properties = new Properties();

		if (!environment.containsProperty("management.endpoints.web.exposure.include")) {
			properties.setProperty("management.endpoints.web.exposure.include", "prometheus");
		}

		for (String metricsRepositoryName : METRICS_REPOSITORY_NAMES) {
			String propertyKey = String.format(METRICS_PROPERTY_NAME_TEMPLATE, metricsRepositoryName);

			// Back off if the property is already set.
			if (!environment.containsProperty(propertyKey)) {
				properties.setProperty(propertyKey, "false");
			}
		}

		// This post-processor is called multiple times but sets the properties only once.
		if (!properties.isEmpty()) {
			PropertiesPropertySource propertiesPropertySource =
					new PropertiesPropertySource(PROPERTY_SOURCE_KEY_NAME, properties);
			environment.getPropertySources().addLast(propertiesPropertySource);
		}
	}
}
