/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.zeromq;

import java.util.function.Consumer;
import java.util.function.Function;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.zeromq.outbound.ZeroMqMessageHandler;
import org.springframework.messaging.Message;

/**
 *
 * @author Daniel Frey
 * @since 3.1.0
 */
@Configuration
@EnableConfigurationProperties(ZeroMqConsumerProperties.class)
public class ZeroMqConsumerConfiguration {

	@Bean
	public ZContext zContext() {
		return new ZContext();
	}

	@Bean
	public ZeroMqMessageHandler zeromqMessageHandler(ZeroMqConsumerProperties properties, ZContext zContext,
			@Autowired(required = false) Consumer<ZMQ.Socket> socketConfigurer,
			@Autowired(required = false) OutboundMessageMapper<byte[]> messageMapper) {
		ZeroMqMessageHandler zeroMqMessageHandler = new ZeroMqMessageHandler(zContext, properties.getConnectUrl(),
				properties.getSocketType());

		if (properties.getTopic() != null) {
			zeroMqMessageHandler.setTopicExpression(properties.getTopic());
		}

		if (socketConfigurer != null) {
			zeroMqMessageHandler.setSocketConfigurer(socketConfigurer);
		}

		if (messageMapper != null) {
			zeroMqMessageHandler.setMessageMapper(messageMapper);
		}

		return zeroMqMessageHandler;
	}

	@Bean
	public Function<Flux<Message<?>>, Mono<Void>> zeromqConsumer(ZeroMqMessageHandler zeromqMessageHandler) {
		return input -> input.flatMap(zeromqMessageHandler::handleMessage)
				.ignoreElements();
	}

}
