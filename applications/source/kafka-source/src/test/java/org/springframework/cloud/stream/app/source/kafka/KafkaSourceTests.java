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

package org.springframework.cloud.stream.app.source.kafka;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
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
		"kafka.supplier.topics=" + KafkaSourceTests.TEST_TOPIC,
		"spring.kafka.consumer.group-id=test-group",
		"spring.kafka.consumer.auto-offset-reset=earliest"
})
@EmbeddedKafka(bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
public class KafkaSourceTests {

	static final String TEST_TOPIC = "TEST_TOPIC";

	@Test
	void fromKafkaToTestBinder(
			@Autowired KafkaTemplate<Object, Object> kafkaTemplate,
			@Autowired OutputDestination outputDestination) {

		String testData = "some test data";
		kafkaTemplate.send(TEST_TOPIC, testData);
		kafkaTemplate.flush();

		Message<byte[]> receive = outputDestination.receive(30_000, "kafkaSupplier-out-0");
		assertThat(receive).extracting(Message::getPayload).isEqualTo(testData.getBytes());
	}

	@SpringBootApplication
	@Import(TestChannelBinderConfiguration.class)
	public static class KafkaSourceTestApplication {

	}

}
