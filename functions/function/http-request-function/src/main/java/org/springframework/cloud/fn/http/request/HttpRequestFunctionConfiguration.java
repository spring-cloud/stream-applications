/*
 * Copyright 2018-2022 the original author or authors.
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
import java.util.Map;
import java.util.function.Function;

import reactor.core.publisher.Flux;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
 * @author Sunny Hemdev
 *
 **/
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpRequestFunctionProperties.class)
public class HttpRequestFunctionConfiguration {

	@Bean
	public HttpRequestFunction httpRequestFunction(WebClient.Builder webClientBuilder, HttpRequestFunctionProperties properties) {
		return new HttpRequestFunction(webClientBuilder.build(), properties);
	}

	/**
	 * Function that accepts a {@code Flux<Message<?>>} containing body and headers and
	 * returns a {@code Flux<ResponseEntity<?>>}.
	 */
	public static class HttpRequestFunction implements Function<Flux<Message<?>>, Flux<?>> {

		private final WebClient webClient;

		private final UriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();

		private final HttpRequestFunctionProperties properties;

		public HttpRequestFunction(WebClient webClient, HttpRequestFunctionProperties properties) {
			this.webClient = webClient;
			this.properties = properties;
		}

		@Override
		public Flux<?> apply(Flux<Message<?>> messageFlux) {
			return messageFlux.flatMap(message -> this.webClient
					.method(resolveHttpMethod(message))
					.uri(uriBuilderFactory.uriString(resolveUrl(message)).build())
					.bodyValue(resolveBody(message))
					.headers(httpHeaders -> httpHeaders.addAll(resolveHeaders(message)))
					.retrieve()
					.toEntity(properties.getExpectedResponseType())
					.map(responseEntity -> properties.getReplyExpression().getValue(responseEntity))
					.timeout(Duration.ofMillis(properties.getTimeout())));
		}

		private String resolveUrl(Message<?> message) {
			return properties.getUrlExpression().getValue(message, String.class);
		}

		private HttpMethod resolveHttpMethod(Message<?> message) {
			return properties.getHttpMethodExpression().getValue(message, HttpMethod.class);
		}

		private Object resolveBody(Message<?> message) {
			return properties.getBodyExpression() != null ? properties.getBodyExpression().getValue(message)
					: message.getPayload();
		}

		private HttpHeaders resolveHeaders(Message<?> message) {
			HttpHeaders headers = new HttpHeaders();
			if (properties.getHeadersExpression() != null) {
				Map<?, ?> headersMap = properties.getHeadersExpression().getValue(message, Map.class);
				for (Map.Entry<?, ?> header : headersMap.entrySet()) {
					if (header.getKey() != null && header.getValue() != null) {
						headers.add(header.getKey().toString(),
								header.getValue().toString());
					}
				}
			}
			return headers;
		}

	}

}
