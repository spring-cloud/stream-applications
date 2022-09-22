/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.jms;

import java.util.function.Supplier;

import jakarta.jms.Session;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
		"jms.supplier.sessionTransacted = true", "jms.supplier.destination = jmssource.test.queue",
		"jms.supplier.messageSelector = JMSCorrelationId=foo",
		"jms.supplier.subscriptionDurable = false", "jms.supplier.subscriptionShared = false",
		"spring.jms.listener.acknowledgeMode = AUTO",
		"spring.jms.listener.concurrency = 3",
		"spring.jms.listener.maxConcurrency = 4",
		"spring.jms.pubSubDomain = false" })
public class PropertiesPopulated3Tests extends AbstractJmsSupplierTests {

	@Autowired
	private JmsTemplate template;

	@Autowired
	private Supplier<Flux<Message<?>>> jmsSupplier;

	@Test
	public void test() throws Exception {
		AbstractMessageListenerContainer container = TestUtils.getPropertyValue(this.endpoint, "listenerContainer",
				AbstractMessageListenerContainer.class);
		assertThat(container).isInstanceOf(DefaultMessageListenerContainer.class);
		assertThat(TestUtils.getPropertyValue(container, "sessionAcknowledgeMode")).isEqualTo(Session.AUTO_ACKNOWLEDGE);
		assertThat(TestUtils.getPropertyValue(container, "sessionTransacted", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(container, "destination")).isEqualTo("jmssource.test.queue");
		assertThat(TestUtils.getPropertyValue(container, "messageSelector")).isEqualTo("JMSCorrelationId=foo");
		assertThat(TestUtils.getPropertyValue(container, "subscriptionDurable", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(container, "subscriptionShared", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(container, "concurrentConsumers")).isEqualTo(3);
		assertThat(TestUtils.getPropertyValue(container, "maxConcurrentConsumers")).isEqualTo(4);
		assertThat(TestUtils.getPropertyValue(container, "pubSubDomain", Boolean.class)).isFalse();

		final Flux<Message<?>> messageFlux = jmsSupplier.get();

		final StepVerifier stepVerifier = StepVerifier.create(messageFlux)
				.assertNext((message) -> {
							assertThat(message.getPayload())
									.isEqualTo("Hello, world!");
						}
				)
				.thenCancel()
				.verifyLater();

		template.convertAndSend("jmssource.test.queue", "Hello, world!");

		stepVerifier.verify();
	}
}
