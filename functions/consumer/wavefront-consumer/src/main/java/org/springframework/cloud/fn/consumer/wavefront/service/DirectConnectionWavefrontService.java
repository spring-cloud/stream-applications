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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * @author Timo Salm
 */
public class DirectConnectionWavefrontService implements WavefrontService {

	private static final Log logger = LogFactory.getLog(DirectConnectionWavefrontService.class);

	private final RestTemplate restTemplate;
	private final String wavefrontDomain;
	private final String wavefrontToken;

	public DirectConnectionWavefrontService(final RestTemplateBuilder restTemplateBuilder,
			final String wavefrontServerUri, final String wavefrontApiToken) {
		this.restTemplate = restTemplateBuilder.build();
		this.wavefrontDomain = wavefrontServerUri;
		this.wavefrontToken = wavefrontApiToken;
	}

	@Override
	public void send(String metricInWavefrontFormat) {
		if (logger.isDebugEnabled()) {
			logger.debug("Send metric directly to Wavefront");
		}
		final HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(wavefrontToken);
		final HttpEntity<String> httpEntity = new HttpEntity<>(metricInWavefrontFormat, headers);
		restTemplate.exchange(wavefrontDomain + "/report", HttpMethod.POST, httpEntity, Void.class);
	}
}
