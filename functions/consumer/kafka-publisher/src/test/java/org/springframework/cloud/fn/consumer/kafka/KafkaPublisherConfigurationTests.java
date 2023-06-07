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
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.common.config.SpelExpressionConverterConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class KafkaPublisherConfigurationTests {

	static final EmbeddedKafkaBroker EMBEDDED_KAFKA =
			new EmbeddedKafkaBroker(1, true, 1)
					.brokerListProperty("spring.kafka.bootstrap-servers");

	final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					KafkaAutoConfiguration.class,
					KafkaPublisherConfiguration.class,
					SpelExpressionConverterConfiguration.class));

	@BeforeAll
	static void initializeEmbeddedKafka() {
		EMBEDDED_KAFKA.afterPropertiesSet();
	}

	@Test
	void defaultTopicReceivesTheRecord() {
		String defaultTopic = "DEFAULT_TOPIC";
		this.contextRunner.withPropertyValues("spring.kafka.template.defaultTopic=" + defaultTopic)
				.run((context) -> {
					KafkaTemplate<?, ?> kafkaTemplate = obtainKafkaTemplate(context);
					Consumer<Message<?>> kafkaPublisher = getKafkaPublisher(context);
					String testData = "test data";
					kafkaPublisher.accept(new GenericMessage<>(testData));
					ConsumerRecord<?, ?> receive = kafkaTemplate.receive(defaultTopic, 0, 0, Duration.ofSeconds(10));
					assertThat(receive).extracting(ConsumerRecord::value).isEqualTo(testData);
				});
	}

	@Test
	void wrongPartitionViaProperties() {
		this.contextRunner.withPropertyValues(
						"spring.kafka.producer.properties[max.block.ms]=1000",
						"kafka.publisher.topic=topic1",
						"kafka.publisher.partition=1", // Our broker allows only one partition for auto-created topic
						"kafka.publisher.sync=true")
				.run((context) -> {
					Consumer<Message<?>> kafkaConsumer = getKafkaPublisher(context);
					assertThatExceptionOfType(MessageHandlingException.class)
							.isThrownBy(() -> kafkaConsumer.accept(new GenericMessage<>("test data")))
							.withCauseInstanceOf(KafkaException.class)
							.withStackTraceContaining("Topic topic1 not present in metadata after 1000 ms.");
				});
	}

	@Test
	void successChannelInteractionAndMappedHeaders() {
		this.contextRunner.withPropertyValues("kafka.publisher.topicExpression=headers.topic",
						"kafka.publisher.mappedHeaders=mapped")
				.run((context) -> {
					KafkaTemplate<?, ?> kafkaTemplate = obtainKafkaTemplate(context);
					Consumer<Message<?>> kafkaConsumer = getKafkaPublisher(context);

					PublishSubscribeChannel kafkaConsumerSuccessChannel =
							context.getBean("kafkaPublisherSuccessChannel", PublishSubscribeChannel.class);

					Sinks.One<Message<?>> successSend = Sinks.one();

					kafkaConsumerSuccessChannel.subscribe(successSend::tryEmitValue);

					String testTopic = "topic2";
					String testData = "some other data";
					Message<String> testMessage =
							MessageBuilder.withPayload(testData)
									.setHeader("topic", testTopic)
									.setHeader("mapped", "mapped value")
									.setHeader("not mapped", "not mapped")
									.build();

					kafkaConsumer.accept(testMessage);

					ConsumerRecord<?, ?> receive = kafkaTemplate.receive(testTopic, 0, 0, Duration.ofSeconds(10));
					assertThat(receive).extracting(ConsumerRecord::value).isEqualTo(testData);
					Map<String, String> headers =
							Arrays.stream(receive.headers().toArray())
									.collect(Collectors.toMap(Header::key, (header) -> new String(header.value())));
					assertThat(headers)
							.containsEntry("mapped", "mapped value")
							.doesNotContainKeys("topic", "not mapped");

					Message<?> successMessage = successSend.asMono().block(Duration.ofSeconds(10));

					assertThat(successMessage)
							.satisfies(message -> {
								assertThat(message.getPayload()).isEqualTo(testData);
								MessageHeaders messageHeaders = message.getHeaders();
								assertThat(messageHeaders)
										.containsKeys("topic", "mapped", "not mapped", KafkaHeaders.RECORD_METADATA);
								assertThat(messageHeaders.get(KafkaHeaders.RECORD_METADATA))
										.isInstanceOf(RecordMetadata.class)
										.extracting("topicPartition")
										.isEqualTo(new TopicPartition(testTopic, 0));
							});
				});
	}

	@SuppressWarnings("unchecked")
	private static KafkaTemplate<?, ?> obtainKafkaTemplate(ApplicationContext applicationContext) {
		KafkaTemplate<?, ?> kafkaTemplate = applicationContext.getBean(KafkaTemplate.class);
		kafkaTemplate.setConsumerFactory(applicationContext.getBean(ConsumerFactory.class));
		return kafkaTemplate;
	}

	@SuppressWarnings("unchecked")
	private static Consumer<Message<?>> getKafkaPublisher(ApplicationContext applicationContext) {
		return (Consumer<Message<?>>) applicationContext.getBean("kafkaPublisher");
	}

}
