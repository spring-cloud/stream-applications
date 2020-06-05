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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * @author Timo Salm
 */
public class DirectConnectionWavefrontService implements WavefrontService {

	private final Logger log = LoggerFactory.getLogger(DirectConnectionWavefrontService.class);

	private final RestTemplate restTemplate;
	private final String wavefrontDomain;
	private final String wavefrontToken;

	public DirectConnectionWavefrontService(final RestTemplateBuilder restTemplateBuilder, final String wavefrontDomain,
											final String wavefrontToken) {
		this.restTemplate = restTemplateBuilder.build();
		this.wavefrontDomain = wavefrontDomain;
		this.wavefrontToken = wavefrontToken;
	}

	@Override
	public void send(String metricInWavefrontFormat) {
		log.info("Send metric directly to Wavefront");
		final HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(wavefrontToken);
		final HttpEntity<String> httpEntity = new HttpEntity<>(metricInWavefrontFormat, headers);
		restTemplate.exchange(wavefrontDomain + "/report", HttpMethod.POST, httpEntity, Void.class);
	}
}
