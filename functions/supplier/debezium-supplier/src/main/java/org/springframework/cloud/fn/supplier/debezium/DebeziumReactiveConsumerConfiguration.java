/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.cloud.fn.supplier.debezium;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.DebeziumEngine.Builder;
import io.debezium.engine.Header;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.debezium.DebeziumEngineBuilderAutoConfiguration;
import org.springframework.cloud.fn.common.debezium.DebeziumProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;

/**
 *
 * @author Christian Tzolov
 * @author Artem Bilan
 */
@AutoConfiguration(after = DebeziumEngineBuilderAutoConfiguration.class)
@EnableConfigurationProperties({ DebeziumSupplierProperties.class })
public class DebeziumReactiveConsumerConfiguration implements BeanClassLoaderAware {

	private static final Log logger = LogFactory.getLog(DebeziumReactiveConsumerConfiguration.class);
	/**
	 * ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL.
	 */
	public static final String ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL = "org.springframework.kafka.support.KafkaNull";

	private Object kafkaNull = null;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		try {
			Class<?> clazz = ClassUtils.forName(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL, classLoader);
			Field field = clazz.getDeclaredField("INSTANCE");
			this.kafkaNull = field.get(null);
		}
		catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
		}
	}

	/**
	 * Reactive Streams, single subscriber, sink used to push down the change event signals received from the Debezium
	 * Engine.
	 */
	private final Sinks.Many<Message<?>> eventSink = Sinks.many().unicast().onBackpressureError();

	/**
	 * Debezium Engine is designed to be submitted to an {@link ExecutorService} for execution by a single thread, and a
	 * running connector can be stopped either by calling {@link #stop()} from another thread or by interrupting the
	 * running thread (e.g., as is the case with {@link ExecutorService#shutdownNow()}).
	 */
	private final ExecutorService debeziumExecutor = Executors.newSingleThreadExecutor();

	@Bean
	public DebeziumEngine<ChangeEvent<byte[], byte[]>> debeziumEngine(
			Consumer<ChangeEvent<byte[], byte[]>> changeEventConsumer,
			Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder) {

		return debeziumEngineBuilder.notifying(changeEventConsumer).build();
	}

	@Bean
	public Supplier<Flux<Message<?>>> debeziumSupplier(DebeziumEngine<ChangeEvent<byte[], byte[]>> debeziumEngine) {

		return () -> this.eventSink.asFlux()
				.doOnRequest(r -> debeziumExecutor.execute(debeziumEngine))
				.doOnTerminate(debeziumExecutor::shutdownNow);
	}

	@Bean
	@ConditionalOnMissingBean
	public Consumer<ChangeEvent<byte[], byte[]>> changeEventConsumer(DebeziumProperties engineProperties,
			DebeziumSupplierProperties supplierProperties) {

		return new ChangeEventConsumer<byte[]>(engineProperties.getPayloadFormat().contentType(),
				supplierProperties.isCopyHeaders(),
				this.eventSink);
	}

	/**
	 * Format-agnostic change event consumer.
	 */
	private final class ChangeEventConsumer<T> implements Consumer<ChangeEvent<T, T>> {

		private final String contentType;
		private final boolean copyHeaders;
		private final Sinks.Many<Message<?>> eventSink;

		private ChangeEventConsumer(String contentType, boolean copyHeaders, Sinks.Many<Message<?>> eventSink) {
			this.contentType = contentType;
			this.copyHeaders = copyHeaders;
			this.eventSink = eventSink;
		}

		@Override
		public void accept(ChangeEvent<T, T> changeEvent) {
			if (logger.isDebugEnabled()) {
				logger.debug("[Debezium Event]: " + changeEvent.key());
			}

			Object key = changeEvent.key();
			Object payload = changeEvent.value();
			String destination = changeEvent.destination();

			// When the tombstone event is enabled, Debezium serializes the payload to null (e.g. empty payload)
			// while the metadata information is carried through the headers (debezium_key).
			// Note: Event for none flattened responses, when the debezium.properties.tombstones.on.delete=true
			// (default), tombstones are generate by Debezium and handled by the code below.
			if (payload == null) {
				payload = DebeziumReactiveConsumerConfiguration.this.kafkaNull;
			}

			// If payload is still null ignore the message.
			if (payload == null) {
				logger.info("Dropped null payload message");
				return;
			}

			MessageBuilder<?> messageBuilder = MessageBuilder
					.withPayload(payload)
					.setHeader("debezium_key", key)
					.setHeader("debezium_destination", destination)
					.setHeader(MessageHeaders.CONTENT_TYPE,
							(payload.equals(DebeziumReactiveConsumerConfiguration.this.kafkaNull))
									? MimeTypeUtils.TEXT_PLAIN_VALUE
									: this.contentType);

			if (this.copyHeaders) {
				List<Header<T>> headers = changeEvent.headers();
				if (headers != null && !headers.isEmpty()) {
					Iterator<Header<T>> itr = headers.iterator();
					while (itr.hasNext()) {
						Header<T> header = itr.next();
						messageBuilder.setHeader(header.getKey(), header.getValue());
					}
				}
			}

			this.eventSink.tryEmitNext(messageBuilder.build());
		}
	}

}
