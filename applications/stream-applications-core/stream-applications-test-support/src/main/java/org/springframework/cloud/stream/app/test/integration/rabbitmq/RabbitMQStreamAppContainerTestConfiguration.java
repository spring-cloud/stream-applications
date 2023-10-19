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

package org.springframework.cloud.stream.app.test.integration.rabbitmq;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.BindingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.cloud.stream.app.test.integration.AbstractTestTopicListener;
import org.springframework.cloud.stream.app.test.integration.MessageMatcher;
import org.springframework.cloud.stream.app.test.integration.OutputMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils;
import org.springframework.cloud.stream.app.test.integration.TestTopicListener;
import org.springframework.cloud.stream.app.test.integration.TestTopicSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.CollectionUtils;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.test.integration.rabbitmq.RabbitMQConfig.rabbitmq;

/**
 * Spring configuration for testing {@link RabbitMQStreamAppContainer}s.
 * @author David Turanski
 */
@Configuration
@EnableRabbit
public abstract class RabbitMQStreamAppContainerTestConfiguration {

	private static final String STREAM_APPLICATION_TESTS_GROUP = "stream-application-tests";

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		RetryTemplate retryTemplate = new RetryTemplate();
		FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
		fixedBackOffPolicy.setBackOffPeriod(2000);
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts(5);
		retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
		retryTemplate.setRetryPolicy(retryPolicy);
		rabbitTemplate.setRetryTemplate(retryTemplate);
		// Need to be synchronous in case the app input exchange isn't ready to receive messages
		rabbitTemplate.setChannelTransacted(true);
		return rabbitTemplate;

	}

	@Bean
	public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	@Bean
	public OutputMatcher outputMatcher(TestTopicListener testTopicListener) {
		return new OutputMatcher(testTopicListener);
	}

	@Bean
	public TestTopicSender testTopicSender(RabbitTemplate rabbitTemplate, Client rabbitClient) {
		return new RabbitTemplateTopicSender(rabbitTemplate, rabbitClient);
	}

	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
			ConnectionFactory connectionFactory) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(new MessageConverter() {
			@Override
			public org.springframework.amqp.core.Message toMessage(Object o, MessageProperties messageProperties)
					throws MessageConversionException {
				throw new UnsupportedOperationException("toMessage not implemented.");
			}

			@Override
			public Object fromMessage(org.springframework.amqp.core.Message message)
					throws MessageConversionException {
				return new String(message.getBody());
			}
		});
		return factory;
	}

	@Bean
	public Client rabbitClient() {
		Client client;
		try {
			client = new Client("http://guest:guest@" + rabbitmq.getHost() + ":" + rabbitmq.getMappedPort(15672) + "/api");
		}
		catch (Exception e) {
			throw new BeanCreationException(e.getMessage(), e);
		}
		return client;
	}

	@Bean
	public ConnectionFactory connectionFactory() {
		return new CachingConnectionFactory(StreamAppContainerTestUtils.localHostAddress(),
				rabbitmq.getMappedPort(5672));
	}

	@Bean
	public RabbitMQTestListener rabbitMQTestListener(RabbitAdmin admin) {
		return new RabbitMQTestListener(admin);
	}

	static class RabbitMQTestListener extends AbstractTestTopicListener {

		public static final int CACHE_TTL_SEC = 120;

		private final Cache<String, Set<Message<?>>> cache = Caffeine.newBuilder()
				.expireAfterWrite(CACHE_TTL_SEC, TimeUnit.SECONDS)
				.build();

		static final String STREAM_APPLICATIONS_TEST_QUEUE = "stream-applications-test-queue";

		private final RabbitAdmin admin;

		private final Queue queue;

		private final TopicExchange exchange = new TopicExchange(STREAM_APPLICATIONS_TEST_TOPIC);

		RabbitMQTestListener(RabbitAdmin admin) {
			super();
			this.admin = admin;
			this.queue = new Queue(STREAM_APPLICATIONS_TEST_QUEUE);
			admin.declareQueue(queue);
			admin.declareExchange(exchange);
			admin.declareBinding(
					BindingBuilder.bind(queue).to(exchange).with("#"));
		}

		@Override
		public AtomicBoolean allMatch(String topic) {
			AtomicBoolean all = super.allMatch(topic);
			if (!all.get()) {
				if (messageMatchers.get(topic) == null || cache.getIfPresent(topic) == null) {
					return all;
				}
				List<MessageMatcher> matchers = messageMatchers.get(topic);
				all.set(true);
				cache.getIfPresent(topic).forEach(message -> matchers.stream().filter(mm -> !mm.isSatisfied())
						.forEach(mm -> all.compareAndSet(true, mm.test(message))));
			}
			return all;
		}

		@Override
		public AtomicBoolean matches(String topic, Predicate<?>... predicates) {
			AtomicBoolean matches = super.matches(topic, predicates);
			if (matches.get() || CollectionUtils.isEmpty(cache.getIfPresent(topic))) {
				return matches;
			}

			for (Predicate<?> predicate : predicates) {
				MessageMatcher matcher = messageMatcher(topic, predicate)
						.orElse(MessageMatcher.payloadMatcher(o -> false));
				if (messageMatcher(topic, predicate).isPresent()) {

					Set<Message<?>> messages = cache.getIfPresent(topic);

					matches.set(true);
					messages.forEach(message -> {
						if (matches.compareAndSet(true, matcher.test(message))) {
							logger.debug("Matched cached message {} for topic {}", message, topic);
							return;
						}
					});
				}
			}
			return matches;
		}

		private void updateCache(String topic, Set<Message<?>> messages) {
			if (CollectionUtils.isEmpty(messages)) {
				cache.invalidate(topic);
			}
			else {
				cache.put(topic, messages);
			}
		}

		private void cacheMessage(String topic, Message<?> message) {
			if (cache.getIfPresent(topic) == null) {
				cache.put(topic, new HashSet<>());
			}
			Set<Message<?>> messages = cache.getIfPresent(topic);
			if (messages.add(message)) {
				logger.debug("Caching message: {} for topic {}", message, topic);
			}

		}

		@Override
		protected Function<Message<?>, String> topicForMessage() {
			return message -> (String) message.getHeaders().get(AmqpHeaders.RECEIVED_EXCHANGE);
		}

		@RabbitListener(autoStartup = "true", group = STREAM_APPLICATION_TESTS_GROUP, queues = {
				STREAM_APPLICATIONS_TEST_QUEUE
		})
		@Override
		public void listen(Message<?> message) {
			String topic = topicForMessage().apply(message);
			logger.debug("Received message: {} on topic {}", message, topic);
			if (!messageMatchers.containsKey(topic)) {
				cacheMessage(topic, message);
				return;
			}

			logger.debug("Verifying message: {} on topic {}", message, topic);
			AtomicBoolean any = new AtomicBoolean(false);
			messageMatchers.get(topic).forEach(v -> {
				any.compareAndSet(false, v.test(message));
				v.setSatisfied(any.get());
			});
			if (!any.get()) {
				cacheMessage(topic, message);
			}
			else {
				logger.debug("Verified message: {} on topic {}", message, topic);
			}

			if (!allMatch(topic).get()) {
				cacheMessage(topic, message);
			}
		}
	}

	static class RabbitTemplateTopicSender implements TestTopicSender {

		private static Logger logger = LoggerFactory.getLogger(RabbitTemplateTopicSender.class);

		private final RabbitTemplate rabbitTemplate;

		private final Client rabbitClient;

		private String routingKey = "#";

		RabbitTemplateTopicSender(RabbitTemplate rabbitTemplate, Client rabbitClient) {
			this.rabbitTemplate = rabbitTemplate;
			this.rabbitClient = rabbitClient;
		}

		@Override
		public <P> void send(String topic, P payload) {
			await().atMost(Duration.ofSeconds(60))
					.pollDelay(Duration.ofSeconds(5))
					.pollInterval(Duration.ofSeconds(2))
					.until(topicExistsAndIsBound(topic));
			rabbitTemplate.convertAndSend(topic, routingKey, payload);
		}

		@Override
		public void send(String topic, Message<?> message) {
			throw new UnsupportedOperationException("send(String, Message<?>) is not implemented.");
		}

		private Callable<Boolean> topicExistsAndIsBound(String topic) {
			return () -> {
				List<BindingInfo> bindings = rabbitClient.getBindingsBySource("/", topic);
				if (bindings.isEmpty()) {
					logger.warn("Exchange {} does not exist or has no bindings", topic);
					return false;
				}
				bindings.forEach(
						b -> logger.debug("Exchange {} is bound to destination {} type {} routing key {}", topic,
								b.getSource(), b.getDestination(), b.getRoutingKey()));
				return true;
			};
		}

		public void setRoutingKey(@NonNull String routingKey) {
			this.routingKey = routingKey;
		}
	}
}
