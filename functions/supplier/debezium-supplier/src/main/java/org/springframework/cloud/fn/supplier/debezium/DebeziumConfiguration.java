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
import java.util.Properties;
import java.util.function.Consumer;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.Header;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.function.context.FunctionProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;

/**
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties(DebeziumProperties.class)
public class DebeziumConfiguration implements BeanClassLoaderAware {

	private static final Log logger = LogFactory.getLog(DebeziumConfiguration.class);
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

	@Bean
	public Properties debeziumConfiguration(DebeziumProperties properties) {
		Properties outProps = new java.util.Properties();
		outProps.putAll(properties.getDebezium());
		return outProps;
	}

	@Bean
	public DebeziumEngine<?> debeziumEngine(Consumer<ChangeEvent<byte[], byte[]>> changeEventConsumer,
			DebeziumProperties properties) {

		Properties debeziumConfiguration = new java.util.Properties();
		debeziumConfiguration.putAll(properties.getDebezium());

		DebeziumEngine<ChangeEvent<byte[], byte[]>> debeziumEngine = DebeziumEngine
				.create(properties.getFormat().serializationFormat())
				.using(debeziumConfiguration)
				.notifying(changeEventConsumer)
				.build();

		return debeziumEngine;
	}

	@Bean
	public EmbeddedEngineExecutorService embeddedEngine(DebeziumEngine<?> debeziumEngine) {
		return new EmbeddedEngineExecutorService(debeziumEngine);
	}

	@Bean
	public BindingNameStrategy bindingNameStrategy(DebeziumProperties cdcProperties,
			FunctionProperties functionProperties) {
		return new BindingNameStrategy(cdcProperties, functionProperties);
	}

	@Bean
	@ConditionalOnProperty(name = "cdc.consumer.override", havingValue = "false", matchIfMissing = true)
	public Consumer<ChangeEvent<byte[], byte[]>> changeEventConsumer(StreamBridge streamBridge,
			BindingNameStrategy bindingNameStrategy, DebeziumProperties properties) {

		return new ChangeEventConsumer<byte[]>(streamBridge, bindingNameStrategy.bindingName(),
				properties.getFormat().contentType(), properties.isConvertHeaders());
	}

	/**
	 * Format-agnostic change event consumer.
	 */
	private final class ChangeEventConsumer<T> implements Consumer<ChangeEvent<T, T>> {

		private final StreamBridge streamBridge;
		private final String bindingName;
		private final String contentType;
		private final boolean convertHeaders;

		private ChangeEventConsumer(StreamBridge streamBridge, String bindingName, String contentType,
				boolean convertHeaders) {
			this.streamBridge = streamBridge;
			this.bindingName = bindingName;
			this.contentType = contentType;
			this.convertHeaders = convertHeaders;
		}

		@Override
		public void accept(ChangeEvent<T, T> changeEvent) {

			logger.debug("[Debezium Event]: " + changeEvent.key());

			Object key = changeEvent.key();
			Object payload = changeEvent.value();
			String destination = changeEvent.destination();

			// When the tombstone event is enabled, Debezium serializes the payload to null (e.g. empty payload)
			// while the metadata information is carried through the headers (cdc_key).
			// Note: Event for none flattened responses, when the cdc.debezium.tombstones.on.delete=true
			// (default), tombstones are generate by Debezium and handled by the code below.
			if (payload == null) {
				payload = DebeziumConfiguration.this.kafkaNull;
			}

			// If payload is still null ignore the message.
			if (payload == null) {
				logger.info("Dropped null payload message");
				return;
			}

			MessageBuilder<?> messageBuilder = MessageBuilder
					.withPayload(payload)
					.setHeader("cdc_key", key)
					.setHeader("cdc_destination", destination)
					.setHeader(MessageHeaders.CONTENT_TYPE,
							(payload.equals(DebeziumConfiguration.this.kafkaNull))
									? MimeTypeUtils.TEXT_PLAIN_VALUE
									: this.contentType);

			if (this.convertHeaders) {
				List<Header<T>> headers = changeEvent.headers();
				if (headers != null && !headers.isEmpty()) {
					Iterator<Header<T>> itr = headers.iterator();
					while (itr.hasNext()) {
						Header<T> header = itr.next();
						messageBuilder.setHeader(header.getKey(), header.getValue());
					}
				}
			}

			this.streamBridge.send(this.bindingName, messageBuilder.build());
		}
	}
}
