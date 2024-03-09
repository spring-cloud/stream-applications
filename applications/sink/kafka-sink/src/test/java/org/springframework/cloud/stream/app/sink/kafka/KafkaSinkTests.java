/*
 * Copyright 2023-2024 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.kafka;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringBootTest(properties = {
		"kafka.publisher.topic=" + KafkaSinkTests.TEST_TOPIC,
		"kafka.publisher.mappedHeaders=mapped"
})
@EmbeddedKafka(kraft = false)
@DirtiesContext
public class KafkaSinkTests {

	static final String TEST_TOPIC = "TEST_TOPIC";

	@Test
	void fromTestBinderToKafka(
			@Autowired InputDestination inputDestination,
			@Autowired KafkaSinkTestApplication kafkaSinkTestApplication) throws InterruptedException {

		String testData = "some other data";
		Message<String> testMessage =
				MessageBuilder.withPayload(testData)
						.setHeader("mapped", "mapped value")
						.setHeader("not mapped", "not mapped value")
						.build();

		inputDestination.send(testMessage);

		Message<?> receive = kafkaSinkTestApplication.messageFromKafka.poll(10, TimeUnit.SECONDS);

		assertThat(receive).extracting(Message::getPayload).isEqualTo(testData);
		assertThat(receive.getHeaders())
				.containsEntry("mapped", "mapped value")
				.doesNotContainKey("not mapped")
				.containsKeys(KafkaHeaders.RECEIVED_TOPIC, KafkaHeaders.RECEIVED_PARTITION, KafkaHeaders.GROUP_ID);
	}

	@SpringBootApplication
	@Import(TestChannelBinderConfiguration.class)
	public static class KafkaSinkTestApplication {

		BlockingQueue<Message<String>> messageFromKafka = new LinkedBlockingQueue<>();

		@KafkaListener(topics = TEST_TOPIC, id = "testListener")
		void testKafkaListener(Message<String> message) {
			this.messageFromKafka.offer(message);
		}

	}

}
