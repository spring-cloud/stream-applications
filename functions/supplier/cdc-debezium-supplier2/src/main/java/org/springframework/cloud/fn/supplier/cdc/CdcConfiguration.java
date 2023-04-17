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

package org.springframework.cloud.fn.supplier.cdc;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
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
import org.springframework.util.StringUtils;

/**
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties(CdcProperties.class)
public class CdcConfiguration implements BeanClassLoaderAware {

	private static final Log logger = LogFactory.getLog(CdcConfiguration.class);

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
	public Properties cdcConfiguration(CdcProperties properties) {
		Properties outProps = new java.util.Properties();
		outProps.putAll(properties.getDebezium());
		return outProps;
	}

	@Bean
	@ConditionalOnProperty(name = "cdc.format", havingValue = "json", matchIfMissing = true)
	public DebeziumEngine<?> debeziumEngineJson(Consumer<ChangeEvent<String, String>> changeEventConsumer,
			java.util.Properties cdcConfiguration) {

		DebeziumEngine<ChangeEvent<String, String>> debeziumEngine = DebeziumEngine
				.create(io.debezium.engine.format.Json.class)
				.using(cdcConfiguration)
				.notifying(changeEventConsumer)
				.build();

		return debeziumEngine;
	}

	@Bean
	@ConditionalOnProperty(name = "cdc.format", havingValue = "avro")
	public DebeziumEngine<?> debeziumEngineAvro(Consumer<ChangeEvent<byte[], byte[]>> changeEventConsumer,
			java.util.Properties cdcConfiguration) {

		DebeziumEngine<ChangeEvent<byte[], byte[]>> debeziumEngine = DebeziumEngine
				.create(io.debezium.engine.format.Avro.class)
				.using(cdcConfiguration)
				.notifying(changeEventConsumer)
				.build();

		return debeziumEngine;
	}

	@Bean
	public EmbeddedEngineExecutorService embeddedEngine(DebeziumEngine<?> debeziumEngine) {

		return new EmbeddedEngineExecutorService(debeziumEngine) {
			@PostConstruct
			@Override
			public void start() {
				super.start();
			}

			@PreDestroy
			@Override
			public void close() {
				super.close();
			}
		};
	}

	@Bean
	public BindingNameStrategy bindingNameStrategy(CdcProperties cdcProperties, FunctionProperties functionProperties) {
		return new BindingNameStrategy(cdcProperties, functionProperties);
	}

	@Bean
	@ConditionalOnProperty(name = "cdc.disableDefaultConsumer", havingValue = "false", matchIfMissing = true)
	public Consumer<ChangeEvent<String, String>> stringSourceRecordConsumer(StreamBridge streamBridge,
			BindingNameStrategy bindingNameStrategy) {
		return new ChangeEventConsumer<String>(streamBridge, bindingNameStrategy.bindingName());
	}

	@Bean
	@ConditionalOnProperty(name = "cdc.disableDefaultConsumer", havingValue = "false", matchIfMissing = true)
	public Consumer<ChangeEvent<byte[], byte[]>> byteSourceRecordConsumer(StreamBridge streamBridge,
			BindingNameStrategy bindingNameStrategy) {
		return new ChangeEventConsumer<byte[]>(streamBridge, bindingNameStrategy.bindingName());
	}

	/**
	 * Format-agnostic change event consumer.
	 */
	private final class ChangeEventConsumer<T> implements Consumer<ChangeEvent<T, T>> {

		private final StreamBridge streamBridge;

		private final String bindingName;

		private ChangeEventConsumer(StreamBridge streamBridge, String bindingName) {
			this.streamBridge = streamBridge;
			this.bindingName = bindingName;
		}

		@Override
		public void accept(ChangeEvent<T, T> changeEvent) {

			logger.debug("[CDC Event]: " + changeEvent.key());

			Object key = changeEvent.key();
			Object cdcJsonPayload = changeEvent.value();
			String destination = changeEvent.destination();

			// When the tombstone event is enabled, Debezium serializes the payload to null (e.g. empty payload)
			// while the metadata information is carried through the headers (cdc_key).
			// Note: Event for none flattened responses, when the cdc.config.tombstones.on.delete=true
			// (default), tombstones are generate by Debezium and handled by the code below.
			if (cdcJsonPayload == null) {
				cdcJsonPayload = CdcConfiguration.this.kafkaNull;
			}

			// If payload is still null ignore the message.
			if (cdcJsonPayload == null) {
				logger.info("dropped null payload message");
				return;
			}

			MessageBuilder<?> messageBuilder = MessageBuilder
					.withPayload(cdcJsonPayload)
					.setHeader("cdc_key", key)
					.setHeader("cdc_destination", destination)
					.setHeader(MessageHeaders.CONTENT_TYPE,
							(cdcJsonPayload.equals(CdcConfiguration.this.kafkaNull))
									? MimeTypeUtils.TEXT_PLAIN_VALUE
									: MimeTypeUtils.APPLICATION_JSON_VALUE);

			streamBridge.send(bindingName, messageBuilder.build());
		}
	}

	/**
	 * Computes the binding name. If the 'overrideBindingName' property is not empty it is used as binding name.
	 * Otherwise the binding name is computed from the function definition name and the '-out-0' suffix. If the function
	 * definition name is empty, then the binding name defaults to 'cdcSupplier-out-0'.
	 */
	public static class BindingNameStrategy {

		private static final String DEFAULT_FUNCTION_DEFINITION_NAME = "cdcSupplier";
		private static final String DEFAULT_SUFFIX = "-out-0";

		private CdcProperties cdcProperties;
		private FunctionProperties functionProperties;

		public BindingNameStrategy(CdcProperties cdcProperties, FunctionProperties functionProperties) {
			this.cdcProperties = cdcProperties;
			this.functionProperties = functionProperties;
		}

		public String bindingName() {

			if (StringUtils.hasText(cdcProperties.getOverrideBindingName())) {
				return cdcProperties.getOverrideBindingName();
			}
			else if (StringUtils.hasText(functionProperties.getDefinition())) {
				return functionProperties.getDefinition() + DEFAULT_SUFFIX;
			}

			return DEFAULT_FUNCTION_DEFINITION_NAME + DEFAULT_SUFFIX;
		}
	}

}
