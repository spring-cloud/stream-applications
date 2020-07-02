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

package org.springframework.cloud.fn.consumer.s3;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Timo Salm
 */
class AwsS3ConsumerEndpointConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(UserConfigurations.of(AwsS3ConsumerEndpointConfiguration.class, TestConfiguration.class));

	@Test
	public void endpointConfigurationShouldBeAvailableIfEndpointUrlSet() {
		runner.withPropertyValues(
			"s3.consumer.bucket=bucket1",
			"s3.consumer.endpoint-url=https://object.ecstestdrive.com"
		).run(context -> assertThat(context).hasSingleBean(EndpointConfiguration.class));
	}

	@Test
	public void endpointConfigurationShouldNotBeAvailableIfEndpointUrlNotSet() {
		runner.withPropertyValues(
			"s3.consumer.bucket=bucket1"
		).run(context -> assertThat(context).doesNotHaveBean(EndpointConfiguration.class));
	}

	@EnableConfigurationProperties({AwsS3ConsumerProperties.class})
	private static class TestConfiguration {
		@Bean
		RegionProvider regionProvider() {
			return new StaticRegionProvider("eu-central-1");
		}
	}
}
