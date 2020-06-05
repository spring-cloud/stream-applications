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

package org.springframework.cloud.fn.consumer.wavefront.service;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * @author Timo Salm
 */
public class DirectConnectionWavefrontServiceTest {

	@Test
	void testSendMetricInWavefrontFormat() {
		final RestTemplateBuilder restTemplateBuilderMock = mock(RestTemplateBuilder.class);
		final RestTemplate restTemplateMock = mock(RestTemplate.class);
		when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);

		final String metricInWavefrontFormat = "testMetric";
		final String wavefrontServerUri = "testWavefrontDomain";
		final String wavefrontApiToken = "testWavefrontToken";

		final WavefrontService service = new DirectConnectionWavefrontService(restTemplateBuilderMock,
				wavefrontServerUri, wavefrontApiToken);
		service.send(metricInWavefrontFormat);

		final ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);
		verify(restTemplateMock, Mockito.times(1))
				.exchange(eq(wavefrontServerUri + "/report"), eq(HttpMethod.POST), argument.capture(),
						eq(Void.class));
		assertThat(Objects.requireNonNull(argument.getValue().getHeaders().get("Authorization")).get(0))
				.isEqualTo("Bearer " + wavefrontApiToken);
		assertThat(Objects.requireNonNull(argument.getValue().getBody())).isEqualTo(metricInWavefrontFormat);
	}
}
