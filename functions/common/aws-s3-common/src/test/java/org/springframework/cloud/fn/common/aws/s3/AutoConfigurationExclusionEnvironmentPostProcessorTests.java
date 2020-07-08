/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.fn.common.aws.s3;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.springframework.cloud.fn.common.aws.s3.AutoConfigurationExclusionEnvironmentPostProcessor
		.SPRING_AUTOCONFIGURE_EXCLUDE_PROPERTY;

/**
 * @author Timo Salm
 */
class AutoConfigurationExclusionEnvironmentPostProcessorTests {

	@Test
	public void addsConfigurationPropertyToExcludeAmazonS3AutoConfiguration() {
		final ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(AutoConfigurationExclusionEnvironmentPostProcessor.class)
				.web(WebApplicationType.NONE)
				.build()
				.run();

		final ConfigurableEnvironment environment = context.getEnvironment();
		Assertions.assertEquals(
				"org.springframework.cloud.aws.autoconfigure.context.ContextResourceLoaderAutoConfiguration",
				environment.getProperty(SPRING_AUTOCONFIGURE_EXCLUDE_PROPERTY));
	}
}
