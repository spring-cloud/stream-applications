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
 * A configuration for Apache Kafka Publisher (Consumer function).
 * Uses a {@link KafkaProducerMessageHandlerSpec} to publish a message to Kafka topic.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@EnableConfigurationProperties(KafkaPublisherProperties.class)
public class KafkaPublisherConfiguration {

	/**
	 * The function to produce messages to the Kafka topic.
	 * @param kafkaProducerMessageHandler the handler to publish messages to Kafka.
	 * @return the consumer for accepting message for producing to Kafka.
	 */
	@Bean
	public Consumer<Message<?>> kafkaPublisher(KafkaProducerMessageHandler<?, ?> kafkaProducerMessageHandler) {
		return kafkaProducerMessageHandler::handleMessage;
	}

	@Bean
	public KafkaProducerMessageHandler<?, ?> kafkaProducerMessageHandlerSpec(KafkaTemplate<?, ?> kafkaTemplate,
			KafkaPublisherProperties kafkaPublisherProperties,
			PublishSubscribeChannel kafkaPublisherSuccessChannel,
			PublishSubscribeChannel kafkaPublisherFailureChannel,
			PublishSubscribeChannel kafkaPublisherFuturesChannel,
			@Nullable ComponentCustomizer<KafkaProducerMessageHandlerSpec<?, ?, ?>> kafkaProducerSpecComponentCustomizer) {

		var kafkaProducerMessageHandlerSpec = Kafka.outboundChannelAdapter(kafkaTemplate);

		PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();

		mapper.from(kafkaPublisherProperties.getTopic()).to(kafkaProducerMessageHandlerSpec::topic);
		mapper.from(kafkaPublisherProperties.getTopicExpression()).to(kafkaProducerMessageHandlerSpec::topicExpression);
		mapper.from(kafkaPublisherProperties.getKey()).to(kafkaProducerMessageHandlerSpec::messageKey);
		mapper.from(kafkaPublisherProperties.getKeyExpression()).to(kafkaProducerMessageHandlerSpec::messageKeyExpression);
		mapper.from(kafkaPublisherProperties.getPartition()).to(kafkaProducerMessageHandlerSpec::partitionId);
		mapper.from(kafkaPublisherProperties.getPartitionExpression()).to(kafkaProducerMessageHandlerSpec::partitionIdExpression);
		mapper.from(kafkaPublisherProperties.getTimestamp()).as(ValueExpression::new).to(kafkaProducerMessageHandlerSpec::timestampExpression);
		mapper.from(kafkaPublisherProperties.getTimestampExpression()).to(kafkaProducerMessageHandlerSpec::timestampExpression);
		mapper.from(kafkaPublisherProperties.getSendTimeout()).as(Duration::toMillis).to(kafkaProducerMessageHandlerSpec::sendTimeout);
		mapper.from(kafkaPublisherProperties.isUseTemplateConverter()).to(kafkaProducerMessageHandlerSpec::useTemplateConverter);

		kafkaProducerMessageHandlerSpec.headerMapper(new DefaultKafkaHeaderMapper(kafkaPublisherProperties.getMappedHeaders()));

		kafkaProducerMessageHandlerSpec.sendSuccessChannel(kafkaPublisherSuccessChannel);
		kafkaProducerMessageHandlerSpec.sendFailureChannel(kafkaPublisherFailureChannel);
		kafkaProducerMessageHandlerSpec.futuresChannel(kafkaPublisherFuturesChannel);

		if (kafkaProducerSpecComponentCustomizer != null) {
			kafkaProducerSpecComponentCustomizer.customize(kafkaProducerMessageHandlerSpec);
		}

		return kafkaProducerMessageHandlerSpec.get();
	}

	/**
	 * @see KafkaProducerMessageHandler#setSendSuccessChannel(MessageChannel)
	 */
	@Bean
	public PublishSubscribeChannel kafkaPublisherSuccessChannel() {
		return new PublishSubscribeChannel();
	}

	/**
	 * @see KafkaProducerMessageHandler#setSendFailureChannel(MessageChannel)
	 */
	@Bean
	public PublishSubscribeChannel kafkaPublisherFailureChannel() {
		return new PublishSubscribeChannel();
	}

	/**
	 * @see KafkaProducerMessageHandler#setFuturesChannel(MessageChannel)
	 */
	@Bean
	public PublishSubscribeChannel kafkaPublisherFuturesChannel() {
		return new PublishSubscribeChannel();
	}

}
