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

package org.springframework.cloud.fn.consumer.wavefront;

import java.io.IOException;
import java.util.function.Consumer;

import javax.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.fn.consumer.wavefront.service.DirectConnectionWavefrontService;
import org.springframework.cloud.fn.consumer.wavefront.service.ProxyConnectionWavefrontService;
import org.springframework.cloud.fn.consumer.wavefront.service.WavefrontService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

/**
 * @author Timo Salm
 */
@Configuration
@EnableConfigurationProperties(WavefrontConsumerProperties.class)
@Import(RestTemplateAutoConfiguration.class)
public class WavefrontConsumerConfiguration {

	private final Logger log = LoggerFactory.getLogger(WavefrontConsumerConfiguration.class);

	@Bean
	Consumer<Message<String>> wavefrontConsumer(final WavefrontConsumerProperties properties,
												final WavefrontService service) {
		return message -> {
			log.info("Received event");
			final WavefrontFormat wavefrontFormat = new WavefrontFormat(properties, message.getPayload());
			String formattedString;
			try {
				formattedString = wavefrontFormat.getFormattedString();
			}
			catch (IOException e) {
				log.error("Unable to transform input into Wavefront format", e);
				return;
			}
			log.debug(formattedString);
			service.send(formattedString);
		};
	}

	@Bean
	WavefrontService wavefrontService(final WavefrontConsumerProperties properties,
									final RestTemplateBuilder restTemplateBuilder) {
		if (!StringUtils.isEmpty(properties.getWavefrontProxyUrl())) {
			return new ProxyConnectionWavefrontService(restTemplateBuilder, properties.getWavefrontProxyUrl());
		}
		else if (!StringUtils.isEmpty(properties.getWavefrontDomain())
				&& !StringUtils.isEmpty(properties.getWavefrontToken())) {
			return new DirectConnectionWavefrontService(restTemplateBuilder, properties.getWavefrontDomain(),
					properties.getWavefrontToken());
		}
		throw new ValidationException("Neither the Wavefront proxy url nor the Wavefront domain and token are set");
	}
}
