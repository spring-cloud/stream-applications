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

package org.springframework.cloud.stream.app.test.integration.pulsar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.internal.PulsarAdminBuilderImpl;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.app.test.integration.AbstractTestTopicListener;
import org.springframework.cloud.stream.app.test.integration.MessageMatcher;
import org.springframework.cloud.stream.app.test.integration.OutputMatcher;
import org.springframework.cloud.stream.app.test.integration.TestTopicListener;
import org.springframework.cloud.stream.app.test.integration.TestTopicSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.pulsar.annotation.EnablePulsar;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.config.ConcurrentPulsarListenerContainerFactory;
import org.springframework.pulsar.config.PulsarListenerEndpointRegistry;
import org.springframework.pulsar.core.ConsumerBuilderCustomizer;
import org.springframework.pulsar.core.DefaultPulsarConsumerFactory;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.support.PulsarHeaders;

import static org.springframework.cloud.stream.app.test.integration.AbstractTestTopicListener.STREAM_APPLICATIONS_TEST_TOPIC;

/**
 * Spring configuration for testing {@link PulsarStreamAppContainer}s.
 * @author David Turanski
 */
@Configuration
@EnablePulsar
public class PulsarStreamAppContainerTestConfiguration {

	private static final String SUFFIX = UUID.randomUUID().toString().substring(0, 8);

	private static final String STREAM_APPLICATION_TESTS_GROUP = "stream-application-tests_" + SUFFIX;

	@Bean
	public PulsarClient pulsarClient(@Value("${spring.pulsar.client.service-url}") String serviceUrl) throws Exception {
		return PulsarClient.builder()
				.serviceUrl(serviceUrl)
				.build();
	}

	@Bean
	public PulsarProducerFactory<Object> pulsarProducerFactory(PulsarClient pulsarClient) {
		return new DefaultPulsarProducerFactory<>(pulsarClient);
	}

	@Bean
	public PulsarTemplate<Object> pulsarTemplate(PulsarProducerFactory producerFactory) {
		return new PulsarTemplate<>(producerFactory);
	}

	@Bean
	public OutputMatcher outputMatcher(TestTopicListener testTopicListener) {
		return new OutputMatcher(testTopicListener);
	}

	@Bean
	public TestTopicSender testTopicSender(PulsarTemplate pulsarTemplate) {
		return new PulsarTemplateTopicSender(pulsarTemplate);
	}

	@Bean("pulsarConsumerFactory")
	public PulsarConsumerFactory<String> consumerFactory(PulsarClient pulsarClient) {
		Map<String, Object> configs = new HashMap<>();
		ConsumerBuilderCustomizer<String> consumerBuilderCustomizer = (builder) -> {
			builder.topic(STREAM_APPLICATIONS_TEST_TOPIC);
			builder.subscriptionName(STREAM_APPLICATION_TESTS_GROUP);
		};
		return new DefaultPulsarConsumerFactory<>(pulsarClient, List.of(consumerBuilderCustomizer));
	}

	@Bean
	public ConcurrentPulsarListenerContainerFactory<String> pulsarListenerContainerFactory(
			@Value("pulsarConsumerFactory") PulsarConsumerFactory consumerFactory) {
		return new ConcurrentPulsarListenerContainerFactory<>(consumerFactory, new PulsarContainerProperties());
	}

	@Bean
	public PulsarProducerFactory<String> producerFactory(PulsarClient pulsarClient) {
		return new DefaultPulsarProducerFactory<>(pulsarClient, STREAM_APPLICATIONS_TEST_TOPIC);

	}

	@Bean
	public PulsarAdmin admin(@Value("${spring.pulsar.admin.service-url}") String adminUrl) throws PulsarClientException {
		PulsarAdminBuilderImpl adminBuilder = new PulsarAdminBuilderImpl();
		adminBuilder.serviceHttpUrl(adminUrl);
		return adminBuilder.build();
	}

	@Bean
	public PulsarListenerEndpointRegistry pulsarListenerEndpointRegistry() {
		return new PulsarListenerEndpointRegistry();
	}

	@Bean
	public PulsarTestListener testListener(PulsarListenerEndpointRegistry endpointRegistry) {
		return new PulsarTestListener(endpointRegistry);
	}

	@PulsarListener(autoStartup = "true", topicPattern = STREAM_APPLICATIONS_TEST_TOPIC)
	static class PulsarTestListener extends AbstractTestTopicListener {

		PulsarTestListener(PulsarListenerEndpointRegistry endpointRegistry) {
			super();
		}

		@Override
		public boolean addMessageMatcher(String topic, MessageMatcher messageMatcher) {
			return super.addMessageMatcher(topic, messageMatcher);
		}

		@Override
		protected Function<Message<?>, String> topicForMessage() {
			return message -> (String) message.getHeaders().get(PulsarHeaders.TOPIC_NAME);
		}

		@PulsarListener
		public void listen(Message<?> message) {
			super.listen(message);
		}
	}

	static class PulsarTemplateTopicSender implements TestTopicSender {

		private final PulsarTemplate pulsarTemplate;

		PulsarTemplateTopicSender(PulsarTemplate pulsarTemplate) {
			this.pulsarTemplate = pulsarTemplate;
		}

		@Override
		public <P> void send(String topic, P payload) {
			doSend(topic, payload);
		}

		@Override
		public void send(String topic, Message<?> message) {
			doSend(topic, message);
		}

		private void doSend(String topic, Object payload) {
			pulsarTemplate.send(topic, payload);
		}
	}

}
