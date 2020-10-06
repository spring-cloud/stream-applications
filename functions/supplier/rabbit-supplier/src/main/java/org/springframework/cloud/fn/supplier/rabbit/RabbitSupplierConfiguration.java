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

package org.springframework.cloud.fn.supplier.rabbit;

import java.util.function.Supplier;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;
import reactor.core.publisher.Flux;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.messaging.Message;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.util.Assert;

/**
 * A source module that receives data from RabbitMQ.
 *
 * @author Gary Russell
 * @author Chris Schaefer
 * @author Roger Perez
 */
@EnableConfigurationProperties(RabbitSupplierProperties.class)
public class RabbitSupplierConfiguration implements DisposableBean {

	private static final MessagePropertiesConverter inboundMessagePropertiesConverter =
			new DefaultMessagePropertiesConverter() {

				@Override
				public MessageProperties toMessageProperties(AMQP.BasicProperties source,
															Envelope envelope,
															String charset) {
					MessageProperties properties = super.toMessageProperties(source, envelope, charset);
					properties.setDeliveryMode(null);
					return properties;
				}
			};

	@Autowired
	private RabbitProperties rabbitProperties;

	@Autowired
	private ObjectProvider<ConnectionNameStrategy> connectionNameStrategy;

	@Autowired
	private RabbitSupplierProperties properties;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private ObjectProvider<CredentialsProvider> credentialsProvider;

	@Autowired
	private ObjectProvider<CredentialsRefreshService> credentialsRefreshService;

	@Autowired
	private ConnectionFactory rabbitConnectionFactory;

	private CachingConnectionFactory ownConnectionFactory;

	@Bean
	public SimpleMessageListenerContainer container() {
		ConnectionFactory connectionFactory = this.properties.isOwnConnection()
				? buildLocalConnectionFactory()
				: this.rabbitConnectionFactory;
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
		container.setAutoStartup(false);
		RabbitProperties.SimpleContainer simpleContainer = this.rabbitProperties.getListener().getSimple();

		AcknowledgeMode acknowledgeMode = simpleContainer.getAcknowledgeMode();
		if (acknowledgeMode != null) {
			container.setAcknowledgeMode(acknowledgeMode);
		}
		Integer concurrency = simpleContainer.getConcurrency();
		if (concurrency != null) {
			container.setConcurrentConsumers(concurrency);
		}
		Integer maxConcurrency = simpleContainer.getMaxConcurrency();
		if (maxConcurrency != null) {
			container.setMaxConcurrentConsumers(maxConcurrency);
		}
		Integer prefetch = simpleContainer.getPrefetch();
		if (prefetch != null) {
			container.setPrefetchCount(prefetch);
		}
		Integer transactionSize = simpleContainer.getBatchSize();
		if (transactionSize != null) {
			container.setBatchSize(transactionSize);
		}

		container.setDefaultRequeueRejected(this.properties.getRequeue());
		container.setChannelTransacted(this.properties.getTransacted());
		String[] queues = this.properties.getQueues();
		Assert.state(queues.length > 0, "At least one queue is required");
		Assert.noNullElements(queues, "queues cannot have null elements");
		container.setQueueNames(queues);
		if (this.properties.isEnableRetry()) {
			container.setAdviceChain(rabbitSourceRetryInterceptor());
		}
		container.setMessagePropertiesConverter(inboundMessagePropertiesConverter);
		return container;
	}

	@Bean
	public AmqpInboundChannelAdapter adapter(SimpleMessageListenerContainer container,
											FluxMessageChannel channel) {
		return Amqp.inboundAdapter(container)
				.autoStartup(false)
				.outputChannel(channel)
				.mappedRequestHeaders(properties.getMappedRequestHeaders())
				.get();
	}

	@Bean
	public Supplier<Flux<Message<?>>> rabbitSupplier(AmqpInboundChannelAdapter adapter,
													FluxMessageChannel channel) {
		return () -> Flux.from(channel).doOnSubscribe(subscription -> adapter.start());
	}

	@Bean
	public FluxMessageChannel output() {
		return new FluxMessageChannel();
	}

	@Bean
	public RetryOperationsInterceptor rabbitSourceRetryInterceptor() {
		return RetryInterceptorBuilder.stateless()
				.maxAttempts(this.properties.getMaxAttempts())
				.backOffOptions(this.properties.getInitialRetryInterval(), this.properties.getRetryMultiplier(),
						this.properties.getMaxRetryInterval())
				.recoverer(new RejectAndDontRequeueRecoverer())
				.build();
	}

	@Override
	public void destroy() throws Exception {
		if (this.ownConnectionFactory != null) {
			this.ownConnectionFactory.destroy();
		}
	}

	private ConnectionFactory buildLocalConnectionFactory() {
		try {
			this.ownConnectionFactory = new AutoConfig.Creator().rabbitConnectionFactory(
					this.rabbitProperties, this.resourceLoader, this.credentialsProvider,
					this.credentialsRefreshService, this.connectionNameStrategy);

		}
		catch (Exception exception) {
			throw new IllegalStateException("Error building connection factory", exception);
		}

		return this.ownConnectionFactory;
	}
}

class AutoConfig extends RabbitAutoConfiguration {

	static class Creator extends RabbitConnectionFactoryCreator {

		public CachingConnectionFactory rabbitConnectionFactory(
				RabbitProperties config,
				ResourceLoader resourceLoader,
				ObjectProvider<CredentialsProvider> credentialsProvider,
				ObjectProvider<CredentialsRefreshService> credentialsRefreshService,
				ObjectProvider<ConnectionNameStrategy> connectionNameStrategy
		) throws Exception {
			CachingConnectionFactory cf = super.rabbitConnectionFactory(config, resourceLoader, credentialsProvider,
					credentialsRefreshService, connectionNameStrategy);
			cf.setConnectionNameStrategy(new ConnectionNameStrategy() {

				@Override
				public String obtainNewConnectionName(ConnectionFactory connectionFactory) {
					return "rabbit.supplier.own.connection";
				}
			});
			cf.afterPropertiesSet();
			return cf;
		}
	}
}
