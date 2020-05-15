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

package org.springframework.cloud.stream.app.processor.http.request;

import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.cloud.fn.http.request.HttpRequestFunctionConfiguration.HttpRequestFunction;

@Configuration
@EnableConfigurationProperties({ HttpRequestProcessorProperties.class })
public class HttpRequestProcessorConfiguration {

	private static Log log = LogFactory.getLog(HttpRequestProcessorConfiguration.class);

	@Bean
	@ConditionalOnMissingBean(WebClient.class)
	public WebClient webClient() {
		return WebClient.builder()
				.build();
	}

	@Bean
	HttpRequestFunctionFactory httpRequestFunctionFactory(WebClient webClient,
			HttpRequestProcessorProperties properties) {
		return new HttpRequestFunctionFactory(webClient, properties);
	}

	@Bean
	Function<Message<?>, ?> httpRequestProcessor(HttpRequestFunctionFactory httpRequestFunctionFactory,
			HttpRequestProcessorProperties properties) {

		return message -> Mono.from(httpRequestFunctionFactory.getHttpRequestFunction(message).apply(Flux.just(message))
				.map(responseEntity -> properties.getReplyExpression().getValue(responseEntity)))
				.doOnError(e -> log.error(e.getMessage(), e))
				.block();
	}

	static class HttpRequestFunctionFactory {

		private final WebClient webClient;

		private final HttpRequestProcessorProperties properties;

		private final HttpRequestFunction instance;

		HttpRequestFunctionFactory(WebClient webClient, HttpRequestProcessorProperties properties) {
			this.properties = properties;
			this.webClient = webClient;
			this.instance = properties.usesRequestExpressions() ? null
					: new HttpRequestFunction(webClient, properties);
		}

		HttpRequestFunction getHttpRequestFunction(Message<?> message) {
			if (instance != null) {
				return instance;
			}
			return new HttpRequestFunction(webClient, properties.evaluateFunctionProperties(message));
		}
	}
}
