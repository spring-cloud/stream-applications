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

package org.springframework.cloud.fn.consumer.kafka;

import java.time.Duration;
import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.cloud.fn.common.config.ComponentCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.dsl.KafkaProducerMessageHandlerSpec;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * A configuration for Apache Kafka Consumer function.
 * Uses a {@link KafkaProducerMessageHandlerSpec} to publish a message to Kafka topic.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@EnableConfigurationProperties(KafkaConsumerProperties.class)
public class KafkaConsumerConfiguration {

	/**
	 * The function to produce messages to the Kafka topic.
	 * Don't mix with the {@link org.apache.kafka.clients.consumer.KafkaConsumer}.
	 * @param kafkaProducerMessageHandler the handler to publish messages to Kafka.
	 * @return the consumer for accepting message for producing to Kafka.
	 */
	@Bean
	public Consumer<Message<?>> kafkaConsumer(KafkaProducerMessageHandler<?, ?> kafkaProducerMessageHandler) {
		return kafkaProducerMessageHandler::handleMessage;
	}

	@Bean
	public KafkaProducerMessageHandler<?, ?> kafkaProducerMessageHandlerSpec(KafkaTemplate<?, ?> kafkaTemplate,
			KafkaConsumerProperties kafkaConsumerProperties,
			PublishSubscribeChannel kafkaConsumerSuccessChannel,
			PublishSubscribeChannel kafkaConsumerFailureChannel,
			PublishSubscribeChannel kafkaConsumerFuturesChannel,
			@Nullable ComponentCustomizer<KafkaProducerMessageHandlerSpec<?, ?, ?>> kafkaProducerSpecComponentCustomizer) {

		var kafkaProducerMessageHandlerSpec = Kafka.outboundChannelAdapter(kafkaTemplate);

		PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();

		mapper.from(kafkaConsumerProperties.getTopic()).to(kafkaProducerMessageHandlerSpec::topic);
		mapper.from(kafkaConsumerProperties.getTopicExpression()).to(kafkaProducerMessageHandlerSpec::topicExpression);
		mapper.from(kafkaConsumerProperties.getKey()).to(kafkaProducerMessageHandlerSpec::messageKey);
		mapper.from(kafkaConsumerProperties.getKeyExpression()).to(kafkaProducerMessageHandlerSpec::messageKeyExpression);
		mapper.from(kafkaConsumerProperties.getPartition()).to(kafkaProducerMessageHandlerSpec::partitionId);
		mapper.from(kafkaConsumerProperties.getPartitionExpression()).to(kafkaProducerMessageHandlerSpec::partitionIdExpression);
		mapper.from(kafkaConsumerProperties.getTimestamp()).as(ValueExpression::new).to(kafkaProducerMessageHandlerSpec::timestampExpression);
		mapper.from(kafkaConsumerProperties.getTimestampExpression()).to(kafkaProducerMessageHandlerSpec::timestampExpression);
		mapper.from(kafkaConsumerProperties.getSendTimeout()).as(Duration::toMillis).to(kafkaProducerMessageHandlerSpec::sendTimeout);
		mapper.from(kafkaConsumerProperties.isUseTemplateConverter()).to(kafkaProducerMessageHandlerSpec::useTemplateConverter);

		kafkaProducerMessageHandlerSpec.headerMapper(new DefaultKafkaHeaderMapper(kafkaConsumerProperties.getMappedHeaders()));

		kafkaProducerMessageHandlerSpec.sendSuccessChannel(kafkaConsumerSuccessChannel);
		kafkaProducerMessageHandlerSpec.sendFailureChannel(kafkaConsumerFailureChannel);
		kafkaProducerMessageHandlerSpec.futuresChannel(kafkaConsumerFuturesChannel);

		if (kafkaProducerSpecComponentCustomizer != null) {
			kafkaProducerSpecComponentCustomizer.customize(kafkaProducerMessageHandlerSpec);
		}

		return kafkaProducerMessageHandlerSpec.get();
	}

	/**
	 * @see KafkaProducerMessageHandler#setSendSuccessChannel(MessageChannel)
	 */
	@Bean
	public PublishSubscribeChannel kafkaConsumerSuccessChannel() {
		return new PublishSubscribeChannel();
	}

	/**
	 * @see KafkaProducerMessageHandler#setSendFailureChannel(MessageChannel)
	 */
	@Bean
	public PublishSubscribeChannel kafkaConsumerFailureChannel() {
		return new PublishSubscribeChannel();
	}

	/**
	 * @see KafkaProducerMessageHandler#setFuturesChannel(MessageChannel)
	 */
	@Bean
	public PublishSubscribeChannel kafkaConsumerFuturesChannel() {
		return new PublishSubscribeChannel();
	}

}
