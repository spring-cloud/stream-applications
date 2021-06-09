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

package org.springframework.cloud.fn.supplier.cdc;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.source.SourceRecord;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.cdc.CdcCommonConfiguration;
import org.springframework.cloud.fn.common.cdc.EmbeddedEngine;
import org.springframework.cloud.fn.common.cdc.EmbeddedEngineExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;

/**
 * CDC source that uses the Debezium Connectors to monitor and record all of the row-level
 * changes in the databases. https://debezium.io/docs/connectors
 *
 * @author Christian Tzolov
 */
@EnableConfigurationProperties(CdcSupplierProperties.class)
@Import(CdcCommonConfiguration.class)
public class CdcSupplierConfiguration implements BeanClassLoaderAware {

	private static final Log logger = LogFactory.getLog(CdcSupplierConfiguration.class);

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
	public Supplier<Flux<Message<?>>> cdcSupplier(EmbeddedEngineExecutorService embeddedEngineExecutorService) {

		return () -> emitterProcessor
				.doOnSubscribe(subscription -> {
					embeddedEngineExecutorService.start();
				})
				.doAfterTerminate(() -> {
					logger.info("Proactive shutdown");
					embeddedEngineExecutorService.close();
				})
				.doOnError(throwable -> logger.error(throwable.getMessage(), throwable));
	}

	private EmitterProcessor<Message<?>> emitterProcessor = EmitterProcessor.create(256, false);

	@Bean
	public EmbeddedEngineExecutorService embeddedEngineExecutorService(
			EmbeddedEngine.Builder embeddedEngineBuilder,
			Function<SourceRecord, byte[]> valueSerializer, Function<SourceRecord, byte[]> keySerializer,
			Function<SourceRecord, SourceRecord> recordFlattening,
			ObjectMapper mapper, CdcSupplierProperties cdcStreamingEngineProperties) {

		FluxSink<Message<?>> sink = emitterProcessor.sink(FluxSink.OverflowStrategy.BUFFER);
		Consumer<SourceRecord> messageConsumer = sourceRecord -> {

			// When cdc.flattening.deleteHandlingMode=none and cdc.flattening.dropTombstones=false
			// then on deletion event an additional sourceRecord is sent with value Null.
			// Here we filter out such condition.
			if (sourceRecord == null) {
				logger.debug("Ignore disabled tombstone events");
				return;
			}

			Object cdcJsonPayload = valueSerializer.apply(sourceRecord);

			// When the tombstone event is enabled, Debezium serializes the payload to null (e.g. empty payload)
			// while the metadata information is carried through the headers (cdc_key).
			// Note: Event for none flattened responses, when the cdc.config.tombstones.on.delete=true
			// (default), tombstones are generate by Debezium and handled by the code below.
			if (cdcJsonPayload == null) {
				cdcJsonPayload = this.kafkaNull;
			}

			// If payload is still null ignore the message.
			if (cdcJsonPayload == null) {
				logger.info("dropped null payload message");
				return;
			}

			byte[] key = keySerializer.apply(sourceRecord);
			if (key == null) {
				logger.warn("Null serialised key for sourceRecord: " + sourceRecord);
				key = new byte[0];
			}

			MessageBuilder<?> messageBuilder = MessageBuilder
					.withPayload(cdcJsonPayload)
//					.setHeader("cdc_key", new String(key))
					.setHeader("cdc_key", key)
					.setHeader("cdc_topic", sourceRecord.topic())
					.setHeader(MessageHeaders.CONTENT_TYPE,
							(cdcJsonPayload.equals(this.kafkaNull)) ? MimeTypeUtils.TEXT_PLAIN_VALUE
									: MimeTypeUtils.APPLICATION_JSON_VALUE);

			if (cdcStreamingEngineProperties.getHeader().isConvertConnectHeaders()) {
				// Convert the Connect Headers into Message Headers.
				if (sourceRecord.headers() != null && !sourceRecord.headers().isEmpty()) {
					Iterator<Header> itr = sourceRecord.headers().iterator();
					while (itr.hasNext()) {
						Header header = itr.next();
						messageBuilder.setHeader(header.key(), header.value());
					}
				}
			}

			if (cdcStreamingEngineProperties.getHeader().isOffset()) {
				try {
					messageBuilder.setHeader("cdc_offset",
							mapper.writeValueAsString(sourceRecord.sourceOffset()));
				}
				catch (JsonProcessingException e) {
					logger.warn("Failed to record cdc_offset header", e);
				}
			}

			sink.next(messageBuilder.build());
		};

		EmbeddedEngine engine = embeddedEngineBuilder
				.notifying(record -> messageConsumer.accept(recordFlattening.apply(record)))
				.build();

		return new EmbeddedEngineExecutorService(engine);
	}
}
