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

package org.springframework.cloud.fn.supplier.zeromq;

import java.util.function.Supplier;

import org.zeromq.ZContext;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.zeromq.inbound.ZeroMqMessageProducer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * A source module that receives data from ZeroMQ.
 *
 * @author Daniel Frey
 */
@Configuration
@EnableConfigurationProperties(ZeroMqSupplierProperties.class)
public class ZeroMqSupplierConfiguration {

	@Autowired
	private ZeroMqSupplierProperties properties;

	private FluxMessageChannel output = new FluxMessageChannel();

	@Bean
	public ZContext zContext() {
		return new ZContext();
	}

	@Bean
	public ZeroMqMessageProducer adapter(ZContext zContext) {

		ZeroMqMessageProducer zeroMqMessageProducer = new ZeroMqMessageProducer(zContext, properties.getReceiveSocketType());
		zeroMqMessageProducer.setConnectUrl(properties.getConnectUrl());
		zeroMqMessageProducer.setTopics(properties.getTopics());
		zeroMqMessageProducer.setMessageMapper((object, headers) -> new GenericMessage<>(object));
		zeroMqMessageProducer.setOutputChannel(output);
		zeroMqMessageProducer.setAutoStartup(false);

		return zeroMqMessageProducer;
	}

	@Bean
	public Supplier<Flux<Message<?>>> zeromqSupplier(ZeroMqMessageProducer adapter) {
		return () -> Flux.from(output).doOnSubscribe(subscription -> adapter.start());
	}

}
