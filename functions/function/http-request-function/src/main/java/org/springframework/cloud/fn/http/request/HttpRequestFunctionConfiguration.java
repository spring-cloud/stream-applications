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

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Configuration for a {@link Function} that makes HTTP requests to a resource and for
 * each request, returns a {@link ResponseEntity}.
 *
 * @author David Turanski
 * @author Corneil du Plessis
 *
 **/
@Configuration
@EnableConfigurationProperties(HttpRequestFunctionProperties.class)
public class HttpRequestFunctionConfiguration {

	@Bean
	@ConditionalOnMissingBean(RestTemplate.class)
	public RestTemplate webClient(HttpRequestFunctionProperties properties) {
		RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
		return restTemplateBuilder.setReadTimeout(Duration.ofMillis(properties.getTimeout())).build();
	}

	@Bean
	public HttpRequestFunction httpRequestFunction(RestTemplate restTemplate, HttpRequestFunctionProperties properties) {
		return new HttpRequestFunction(restTemplate, properties);
	}

	/**
	 * Function that accepts a {@code Flux<Message<?>>} containing body and headers and
	 * returns a {@code Flux<ResponseEntity<?>>}.
	 */
	public static class HttpRequestFunction implements Function<Message<?>, Object> {
		private final RestTemplate restTemplate;

		private final UriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();

		private final HttpRequestFunctionProperties properties;

		public HttpRequestFunction(RestTemplate restTemplate, HttpRequestFunctionProperties properties) {
			this.restTemplate = restTemplate;
			this.properties = properties;
		}

		@Override
		public Object apply(Message<?> message) {
			HttpEntity<?> httpEntity = new HttpEntity<>(resolveBody(message), resolveHeaders(message));
			URI uri = uriBuilderFactory.uriString(resolveUrl(message)).build();
			ResponseEntity<?> responseEntity = restTemplate.exchange(uri,
				resolveHttpMethod(message),
				httpEntity,
				properties.getExpectedResponseType()
			);
			return properties.getReplyExpression().getValue(responseEntity);
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
