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

package org.springframework.cloud.fn.consumer.rsocket;

import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.rsocket.RSocketRequester;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RsocketConsumerProperties.class)
public class RsocketConsumerConfiguration {

	@Bean
	public Function<Flux<Message<?>>, Mono<Void>> rsocketConsumer(RSocketRequester.Builder builder,
																RsocketConsumerProperties rsocketConsumerProperties) {
		final Mono<RSocketRequester> rSocketRequester =
				rsocketConsumerProperties.getUri() != null ? builder.connectWebSocket(rsocketConsumerProperties.getUri()).cache() :
						builder.connectTcp(rsocketConsumerProperties.getHost(),
								rsocketConsumerProperties.getPort()).cache();

		return input ->
				input.flatMap(message ->
						rSocketRequester
								.flatMap(requester -> requester.route(rsocketConsumerProperties.getRoute())
										.data(message.getPayload())
										.send()))
						.ignoreElements();
	}

}
