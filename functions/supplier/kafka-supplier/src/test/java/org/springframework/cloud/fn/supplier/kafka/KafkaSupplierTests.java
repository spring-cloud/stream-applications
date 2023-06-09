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

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class KafkaSupplierTests {

	static final EmbeddedKafkaBroker EMBEDDED_KAFKA =
			new EmbeddedKafkaBroker(1, true, 1)
					.brokerListProperty("spring.kafka.bootstrap-servers");

	final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					KafkaAutoConfiguration.class,
					KafkaSupplierConfiguration.class));

	@BeforeAll
	static void initializeEmbeddedKafka() {
		EMBEDDED_KAFKA.afterPropertiesSet();
	}

	@Test
	void recordModeAndTopicPattern() {
		this.contextRunner.withPropertyValues(
						"spring.kafka.template.defaultTopic=defaultTopic",
						"spring.kafka.consumer.group-id=test-group1",
						"spring.kafka.consumer.auto-offset-reset=earliest",
						"kafka.supplier.topicPattern=default.+")
				.run((context) -> {
					String testPayload1 = "test data #1";
					String testPayload2 = "test data #2";

					KafkaTemplate<Object, Object> kafkaTemplate = getKafkaTemplate(context);
					kafkaTemplate.sendDefault(testPayload1);
					kafkaTemplate.sendDefault(testPayload2);
					kafkaTemplate.flush();

					Supplier<Flux<Message<?>>> kafkaSupplier = getKafkaSupplier(context);
					StepVerifier.create(
									kafkaSupplier.get()
											.map(Message::getPayload)
											.cast(String.class))
							.expectNext(testPayload1, testPayload2)
							.thenCancel()
							.verify(Duration.ofSeconds(30));
				});
	}

	@Test
	void batchMode() {
		this.contextRunner.withPropertyValues(
						"spring.kafka.consumer.group-id=test-group2",
						"spring.kafka.consumer.auto-offset-reset=earliest",
						"spring.kafka.listener.type=BATCH",
						"kafka.supplier.topics=testTopic1,testTopic2")
				.run((context) -> {
					String testPayload1 = "test data #1";
					String testPayload2 = "test data #2";

					Supplier<Flux<Message<?>>> kafkaSupplier = getKafkaSupplier(context);
					StepVerifier stepVerifier = StepVerifier.create(
									kafkaSupplier.get()
											.map(Message::getPayload)
											.cast(List.class))
							.expectNext(List.of(testPayload1, testPayload2))
							.thenCancel()
							.verifyLater();


					KafkaTemplate<Object, Object> kafkaTemplate = getKafkaTemplate(context);
					kafkaTemplate.send("testTopic1", testPayload1);
					kafkaTemplate.send("testTopic2", testPayload2);
					kafkaTemplate.flush();

					stepVerifier.verify(Duration.ofSeconds(30));
				});
	}

	@SuppressWarnings("unchecked")
	private static KafkaTemplate<Object, Object> getKafkaTemplate(ApplicationContext applicationContext) {
		return applicationContext.getBean(KafkaTemplate.class);
	}

	@SuppressWarnings("unchecked")
	private static Supplier<Flux<Message<?>>> getKafkaSupplier(ApplicationContext applicationContext) {
		return (Supplier<Flux<Message<?>>>) applicationContext.getBean("kafkaSupplier");
	}

}
