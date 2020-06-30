/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.wavefront.service;

import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.common.config.SpelExpressionConverterConfiguration;
import org.springframework.cloud.fn.consumer.wavefront.WavefrontConsumerConfiguration;
import org.springframework.core.NestedExceptionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Timo Salm
 */
class WavefrontServiceConditionTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(UserConfigurations.of(WavefrontConsumerConfiguration.class,
					SpelExpressionConverterConfiguration.class));

	@Test
	public void proxyConnectionShouldBeUsedIfWavefrontProxyAddressSet() {
		runner.withPropertyValues(
				"wavefront.metric-name=vehicle-location",
				"wavefront.source=vehicle-api",
				"wavefront.metric-expression=#jsonPath(payload,'$.mileage')",
				"wavefront.wavefront-proxy-url=http://wavefront-proxy.internal:2878",
				"wavefront.wavefront-domain=",
				"wavefront.wavefront-token="
		).run(context -> {
			assertThat(context).hasSingleBean(ProxyConnectionWavefrontService.class);
			assertThat(context).doesNotHaveBean(DirectConnectionWavefrontService.class);
		});
	}

	@Test
	public void proxyConnectionShouldBeUsedIfWavefrontProxyAddressAndDomainAndTokenSet() {
		runner.withPropertyValues(
				"wavefront.metric-name=vehicle-location",
				"wavefront.source=vehicle-api",
				"wavefront.metric-expression=#jsonPath(payload,'$.mileage')",
				"wavefront.wavefront-proxy-url=http://wavefront-proxy.internal:2878",
				"wavefront.wavefront-domain=https://my.wavefront.com",
				"wavefront.wavefront-token=" + UUID.randomUUID()
		).run(context -> {
			assertThat(context).hasSingleBean(ProxyConnectionWavefrontService.class);
			assertThat(context).doesNotHaveBean(DirectConnectionWavefrontService.class);
		});
	}

	@Test
	public void directConnectionShouldBeUsedIfWavefrontDomainAndTokenSet() {
		runner.withPropertyValues(
				"wavefront.metric-name=vehicle-location",
				"wavefront.source=vehicle-api",
				"wavefront.metric-expression=#jsonPath(payload,'$.mileage')",
				"wavefront.wavefront-proxy-url=",
				"wavefront.wavefront-domain=https://my.wavefront.com",
				"wavefront.wavefront-token=" + UUID.randomUUID()
		).run(context -> {
			assertThat(context).hasSingleBean(DirectConnectionWavefrontService.class);
			assertThat(context).doesNotHaveBean(ProxyConnectionWavefrontService.class);
		});
	}

	@Test
	public void applicationStartupShouldFailWithMeaningfulErrorMessageIfWavefrontProxyAddressOrDomainAndTokenNotSet() {
		runner.withPropertyValues(
				"wavefront.metric-name=vehicle-location",
				"wavefront.source=vehicle-api",
				"wavefront.metric-expression=#jsonPath(payload,'$.mileage')",
				"wavefront.wavefront-proxy-url=",
				"wavefront.wavefront-domain=",
				"wavefront.wavefront-token="
		).run(context -> {
			assertThat(context).hasFailed();
			final Throwable rootCause = NestedExceptionUtils.getRootCause(context.getStartupFailure());
			assertThat(Objects.requireNonNull(rootCause).getLocalizedMessage())
					.isEqualTo("Neither the Wavefront proxy url nor the Wavefront domain and token are set");
		});
	}
}
