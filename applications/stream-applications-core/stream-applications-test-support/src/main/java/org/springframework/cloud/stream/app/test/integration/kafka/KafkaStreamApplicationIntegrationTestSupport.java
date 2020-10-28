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

package org.springframework.cloud.stream.app.test.integration.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cloud.stream.app.test.integration.AbstractTestTopicListener;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.StreamApplicationIntegrationTestSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.test.integration.AbstractTestTopicListener.STREAM_APPLICATIONS_TEST_TOPIC;

/**
 * Base class for stream application integration testing with Test Containers and Kafka
 * binder.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = KafkaStreamApplicationIntegrationTestSupport.KafkaTestConfiguration.class)
public abstract class KafkaStreamApplicationIntegrationTestSupport extends StreamApplicationIntegrationTestSupport {

	final static String BINDER = "kafka";

	final static Network network = Network.SHARED;

	protected final static KafkaContainer kafka = new KafkaContainer(
			DockerImageName.parse("confluentinc/cp-kafka:5.5.1"))
					.withExposedPorts(9092, 9093)
					.withNetwork(network);

	static {
		kafka.start();
	}

	protected static StreamAppContainer prepackagedKafkaContainerFor(String appName, String version) {
		return new KafkaStreamAppContainer(prePackagedStreamAppImageName(appName, BINDER, version),
				kafka);
	}

	@Configuration
	@EnableKafka
	static class KafkaTestConfiguration {
		private static final String SUFFIX = UUID.randomUUID().toString().substring(0, 8);

		private static final String STREAM_APPLICATION_TESTS_GROUP = "stream-application-tests_" + SUFFIX;

		@Bean
		KafkaTemplate<String, String> kafkaTemplate(ProducerFactory producerFactory) {
			return new KafkaTemplate(producerFactory);
		}

		@Bean
		public ConsumerFactory<String, String> consumerFactory() {
			Map<String, Object> configs = new HashMap<>();
			configs.put(ConsumerConfig.GROUP_ID_CONFIG, STREAM_APPLICATION_TESTS_GROUP);
			configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
			configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
			configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
			DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(configs);
			cf.setBootstrapServersSupplier(() -> kafka.getBootstrapServers());
			return cf;
		}

		@Bean
		public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
				ConsumerFactory consumerFactory) {

			ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
			factory.setConsumerFactory(consumerFactory);
			return factory;
		}

		@Bean
		public ProducerFactory<String, String> producerFactory() {
			Map<String, Object> configs = new HashMap<>();
			configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
			configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
			DefaultKafkaProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(configs);
			pf.setBootstrapServersSupplier(() -> kafka.getBootstrapServers());
			return pf;
		}

		@Bean
		public AdminClient admin() {
			Map<String, Object> configs = new HashMap<>();
			configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
			return KafkaAdminClient.create(configs);
		}

		@Bean
		KafkaTestListener testListener(AdminClient admin, KafkaListenerEndpointRegistry endpointRegistry) {
			return new KafkaTestListener(admin, endpointRegistry);
		}

		@KafkaListener(autoStartup = "true", topicPattern = STREAM_APPLICATIONS_TEST_TOPIC)
		static class KafkaTestListener extends AbstractTestTopicListener {

			private final AdminClient admin;

			private final KafkaListenerEndpointRegistry endpointRegistry;

			private final Object lock = new Object();

			KafkaTestListener(AdminClient admin, KafkaListenerEndpointRegistry endpointRegistry) {
				super();
				this.admin = admin;
				this.endpointRegistry = endpointRegistry;
				this.admin.createTopics(
						Collections.singletonList(
								new NewTopic(STREAM_APPLICATIONS_TEST_TOPIC, Optional.empty(), Optional.empty())));
				await().atMost(Duration.ofSeconds(30))
						.until(() -> {
							Set<String> topics = admin.listTopics().names().get();
							return topics.contains(STREAM_APPLICATIONS_TEST_TOPIC);
						});
			}

			@Override
			public boolean addOutputMessageVerifier(String topic, Predicate<Message<?>> verifier) {
				boolean added = super.addOutputMessageVerifier(topic, verifier);
				if (added) {
					synchronized (lock) {
						stop();
						// rewind to consume messages that may have arrived before a verifier is registered.
						admin.alterConsumerGroupOffsets(STREAM_APPLICATION_TESTS_GROUP,
								Collections.singletonMap(new TopicPartition(topic, 0), new OffsetAndMetadata(0)));
						start();
					}
				}
				return added;
			}

			private void stop() {
				this.endpointRegistry.getAllListenerContainers().forEach(container -> container.stop());
			}

			private void start() {
				this.endpointRegistry.getAllListenerContainers().forEach(container -> container.start());
			}

			@Override
			protected Function<Message<?>, String> topicForMessage() {
				return message -> (String) message.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC);
			}

			@KafkaHandler(isDefault = true)
			public void listen(Message<?> message) {
				super.listen(message);
			}
		}
	}
}
