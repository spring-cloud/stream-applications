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

package org.springframework.cloud.fn.supplier.kafka;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.config.ComponentCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.dsl.KafkaMessageDrivenChannelAdapterSpec;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * An auto-configuration for Kafka Supplier.
 * Uses a {@link KafkaMessageDrivenChannelAdapterSpec} to consumer records from Kafka topic.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@EnableConfigurationProperties(KafkaSupplierProperties.class)
public class KafkaSupplierConfiguration {

	@Bean
	public Supplier<Flux<Message<?>>> kafkaSupplier(Publisher<Message<?>> kafkaSupplierPublisher) {
		return () -> Flux.from(kafkaSupplierPublisher);
	}

	@Bean
	public Publisher<Message<Object>> kafkaSupplierPublisher(
			KafkaMessageDrivenChannelAdapterSpec<?, ?, ?> kafkaMessageDrivenChannelAdapterSpec) {

		return IntegrationFlow.from(kafkaMessageDrivenChannelAdapterSpec)
				.toReactivePublisher(true);
	}

	@Bean
	public KafkaMessageDrivenChannelAdapterSpec<?, ?, ?> kafkaMessageDrivenChannelAdapterSpec(
			KafkaSupplierProperties kafkaSupplierProperties,
			ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory,
			ObjectProvider<RecordMessageConverter> recordMessageConverterProvider,
			ObjectProvider<RecordFilterStrategy<Object, Object>> recordFilterStrategyProvider,
			ObjectProvider<BatchMessageConverter> batchMessageConverterProvider,
			@Nullable ComponentCustomizer<KafkaMessageDrivenChannelAdapterSpec<?, ?, ?>> kafkaChannelAdapterComponentCustomizer) {

		ConcurrentMessageListenerContainer<Object, Object> container;

		Pattern topicPattern = kafkaSupplierProperties.getTopicPattern();
		if (topicPattern != null) {
			container = kafkaListenerContainerFactory.createContainer(topicPattern);
		}
		else {
			container = kafkaListenerContainerFactory.createContainer(kafkaSupplierProperties.getTopics());
		}

		KafkaMessageDrivenChannelAdapter.ListenerMode listenerMode =
				Boolean.TRUE.equals(kafkaListenerContainerFactory.isBatchListener())
						? KafkaMessageDrivenChannelAdapter.ListenerMode.batch
						: KafkaMessageDrivenChannelAdapter.ListenerMode.record;

		KafkaMessageDrivenChannelAdapterSpec<Object, Object, ?> kafkaMessageDrivenChannelAdapterSpec =
				Kafka.messageDrivenChannelAdapter(container, listenerMode)
						.ackDiscarded(kafkaSupplierProperties.isAckDiscarded())
						.autoStartup(false);

		RecordMessageConverter recordMessageConverter = recordMessageConverterProvider.getIfUnique();

		if (KafkaMessageDrivenChannelAdapter.ListenerMode.batch.equals(listenerMode)) {
			BatchMessageConverter batchMessageConverter =
					batchMessageConverterProvider.getIfUnique(
							() -> new BatchMessagingMessageConverter(recordMessageConverter));

			kafkaMessageDrivenChannelAdapterSpec.batchMessageConverter(batchMessageConverter);
		}
		else if (recordMessageConverter != null) {
			kafkaMessageDrivenChannelAdapterSpec.recordMessageConverter(recordMessageConverter);
		}

		recordFilterStrategyProvider.ifUnique(kafkaMessageDrivenChannelAdapterSpec::recordFilterStrategy);

		if (kafkaChannelAdapterComponentCustomizer != null) {
			kafkaChannelAdapterComponentCustomizer.customize(kafkaMessageDrivenChannelAdapterSpec);
		}

		return kafkaMessageDrivenChannelAdapterSpec;
	}

}
