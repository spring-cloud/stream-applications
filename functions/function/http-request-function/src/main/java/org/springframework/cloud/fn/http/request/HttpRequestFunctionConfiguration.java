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

package org.springframework.cloud.fn.http.request;

import java.time.Duration;
import java.util.function.Function;

import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Configuration for a {@link Function} that makes HTTP requests to a resource and for
 * each request, returns a {@link ResponseEntity}.
 *
 * @author David Turanski
 *
 **/
@Configuration
@EnableConfigurationProperties(HttpRequestFunctionProperties.class)
public class HttpRequestFunctionConfiguration {

	@Bean
	@ConditionalOnMissingBean(WebClient.class)
	public WebClient webClient() {
		return WebClient.builder()
				.build();
	}

	@Bean
	public HttpRequestFunction httpRequestFunction(WebClient webClient, HttpRequestProperties properties) {
		return new HttpRequestFunction(webClient, properties);
	}

	/**
	 * Function that accepts a {@code Flux<Message<?>>} containing body and headers and
	 * returns a {@code Flux<ResponseEntity<?>>}.
	 */
	public static class HttpRequestFunction implements Function<Flux<Message<?>>, Flux<ResponseEntity<?>>> {
		private final WebClient webClient;

		private final UriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();

		private final HttpRequestProperties properties;

		public HttpRequestFunction(WebClient webClient, HttpRequestProperties properties) {
			this.webClient = webClient;
			this.properties = properties;
		}

		@Override
		public Flux<ResponseEntity<?>> apply(Flux<Message<?>> messageFlux) {
			return messageFlux.flatMap(message -> this.webClient
					.method(properties.getHttpMethod())
					.uri(uriBuilderFactory.uriString(properties.getUrl()).build())
					.bodyValue(properties.getBody() == null ? message.getPayload() : properties.getBody())
					.headers(httpHeaders -> httpHeaders.addAll(properties.getHeaders()))
					.retrieve()
					.toEntity(properties.getExpectedResponseType())
					.timeout(Duration.ofMillis(properties.getTimeout())));
		}
	}
}
