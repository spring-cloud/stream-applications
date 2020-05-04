/*
 * Copyright (c) 2011-2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.webflux.dsl.WebFlux;
import org.springframework.integration.webflux.inbound.WebFluxInboundEndpoint;
import org.springframework.messaging.Message;

import reactor.core.publisher.Flux;

/**
 * Configuration for the HTTP Supplier.
 *
 * @author Artem Bilan
 */
@EnableConfigurationProperties(HttpSourceProperties.class)
@Configuration
public class HttpSupplierConfiguration {

	@Bean
	public Publisher<Message<byte[]>> httpSupplierFlow(HttpSourceProperties httpSourceProperties) {
		return IntegrationFlows.from(
				WebFlux.inboundChannelAdapter(httpSourceProperties.getPathPattern())
						.requestPayloadType(byte[].class)
						.statusCodeExpression(new ValueExpression<>(HttpStatus.ACCEPTED))
						.mappedRequestHeaders(httpSourceProperties.getMappedRequestHeaders())
						.crossOrigin(crossOrigin ->
								crossOrigin.origin(httpSourceProperties.getCors().getAllowedOrigins())
										.allowedHeaders(httpSourceProperties.getCors().getAllowedHeaders())
										.allowCredentials(httpSourceProperties.getCors().getAllowCredentials()))
						.autoStartup(false))
				.toReactivePublisher();
	}

	@Bean
	public HeaderMapper<HttpHeaders> httpHeaderMapper() {
		return DefaultHttpHeaderMapper.inboundMapper();
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
