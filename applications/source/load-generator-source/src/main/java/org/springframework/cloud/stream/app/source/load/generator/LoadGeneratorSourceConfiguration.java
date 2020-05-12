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

package org.springframework.cloud.stream.app.source.load.generator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * A source that sends a set amount of empty byte array messages to verify the speed
 * of the infrastructure.
 *
 * @author Glenn Renfro
 * @author Gary Russell
 * @author Soby Chacko
 */
@Configuration
@EnableConfigurationProperties({LoadGeneratorSourceProperties.class})
public class LoadGeneratorSourceConfiguration {

	private static final Log logger = LogFactory.getLog(LoadGeneratorSourceConfiguration.class);

	private final EmitterProcessor<Message<?>> processor = EmitterProcessor.create();

	@Autowired
	private LoadGeneratorSourceProperties properties;

	@Bean
	public Supplier<Flux<Message<?>>> loadGeneratorSupplier() {
		return () -> processor;
	}

	@Bean
	public LoadGeneratorEndpoint loadGeneratorEndpoint() {
		return new LoadGeneratorEndpoint(properties, processor);
	}

	static class LoadGeneratorEndpoint extends AbstractEndpoint {

		private final LoadGeneratorSourceProperties properties;
		private final EmitterProcessor<Message<?>> processor;
		private final AtomicBoolean running = new AtomicBoolean(false);
		private volatile ExecutorService executorService;

		LoadGeneratorEndpoint(LoadGeneratorSourceProperties properties, EmitterProcessor<Message<?>> processor) {
			this.properties = properties;
			this.processor = processor;
		}

		@Override
		protected void doStart() {
			if (running.compareAndSet(false, true)) {
				this.executorService = Executors.newFixedThreadPool(this.properties.getProducers());
				for (int i = 0; i < properties.getProducers(); i++) {
					this.executorService.execute(new Producer(i, this.processor,
							this.properties.getMessageCount(), this.properties.getMessageSize()));
				}
			}
		}

		@Override
		protected void doStop() {
			if (running.compareAndSet(true, false)) {
				executorService.shutdown();
			}
		}

		private static class Producer implements Runnable {
			private final int producerId;

			private final EmitterProcessor<Message<?>> processor;

			private final int messageCount;

			private final int messageSize;

			Producer(int producerId, EmitterProcessor<Message<?>> processor, int messageCount, int messageSize) {
				this.producerId = producerId;
				this.processor = processor;
				this.messageCount = messageCount;
				this.messageSize = messageSize;
			}

			@Override
			public void run() {
				LoadGeneratorSourceConfiguration.logger.info(String.format("Producer %d sending %d messages", this.producerId, this.messageCount));
				Message<byte[]> message = new GenericMessage<>(new byte[this.messageSize]);
				for (int i = 0; i < this.messageCount; i++) {
					this.processor.onNext(message);
				}
				LoadGeneratorSourceConfiguration.logger.info("All Messages Dispatched");
			}
		}
	}
}
