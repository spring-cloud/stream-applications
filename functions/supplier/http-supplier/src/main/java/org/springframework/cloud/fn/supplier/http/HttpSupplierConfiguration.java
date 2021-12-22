/*
 * Copyright 2011-2021 the original author or authors.
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

package org.springframework.cloud.fn.supplier.http;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.webflux.dsl.WebFlux;
import org.springframework.integration.webflux.inbound.WebFluxInboundEndpoint;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Configuration for the HTTP Supplier.
 *
 * @author Artem Bilan
 */
@EnableConfigurationProperties(HttpSupplierProperties.class)
@Configuration
public class HttpSupplierConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public HeaderMapper<HttpHeaders> httpHeaderMapper(HttpSupplierProperties httpSupplierProperties) {
		DefaultHttpHeaderMapper defaultHttpHeaderMapper = DefaultHttpHeaderMapper.inboundMapper();
		defaultHttpHeaderMapper.setInboundHeaderNames(httpSupplierProperties.getMappedRequestHeaders());
		return defaultHttpHeaderMapper;
	}

	@Bean
	public Publisher<Message<byte[]>> httpSupplierFlow(HttpSupplierProperties httpSupplierProperties,
			HeaderMapper<HttpHeaders> httpHeaderMapper,
			ServerCodecConfigurer serverCodecConfigurer) {

		return IntegrationFlows.from(
						WebFlux.inboundChannelAdapter(httpSupplierProperties.getPathPattern())
								.requestPayloadType(byte[].class)
								.statusCodeExpression(new ValueExpression<>(HttpStatus.ACCEPTED))
								.headerMapper(httpHeaderMapper)
								.codecConfigurer(serverCodecConfigurer)
								.crossOrigin(crossOrigin ->
										crossOrigin.origin(httpSupplierProperties.getCors().getAllowedOrigins())
												.allowedHeaders(httpSupplierProperties.getCors().getAllowedHeaders())
												.allowCredentials(httpSupplierProperties.getCors().getAllowCredentials()))
								.autoStartup(false))
				.enrichHeaders((headers) ->
						headers.headerFunction(MessageHeaders.CONTENT_TYPE,
								(message) ->
										(MediaType.APPLICATION_FORM_URLENCODED.equals(
												message.getHeaders().get(MessageHeaders.CONTENT_TYPE, MediaType.class)))
												? MediaType.APPLICATION_JSON
												: null,
								true))
				.toReactivePublisher();
	}

	@Bean
	public Supplier<Flux<Message<byte[]>>> httpSupplier(
			Publisher<Message<byte[]>> httpRequestPublisher,
			WebFluxInboundEndpoint webFluxInboundEndpoint) {

		return () -> Flux.from(httpRequestPublisher)
				.doOnSubscribe((subscription) -> webFluxInboundEndpoint.start())
				.doOnTerminate(webFluxInboundEndpoint::stop);
	}

}
