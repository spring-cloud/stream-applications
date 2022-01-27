/*
 * Copyright 2019-2021 the original author or authors.
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

package org.springframework.cloud.fn.consumer.rabbit;

import java.util.function.Function;

import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.CachingConnectionFactoryConfigurer;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;
import org.springframework.boot.autoconfigure.amqp.RabbitConnectionFactoryBeanConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.expression.Expression;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.amqp.dsl.AmqpOutboundChannelAdapterSpec;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * A configuration for RabbitMQ Consumer function. Uses a
 * {@link AmqpOutboundChannelAdapterSpec} to save payload contents to RabbitMQ.
 *
 * @author Soby Chako
 * @author Nicolas Labrot
 * @author Chris Bono
 */
@EnableConfigurationProperties(RabbitConsumerProperties.class)
@Configuration
public class RabbitConsumerConfiguration implements DisposableBean {

	@Autowired
	private RabbitProperties bootProperties;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private ObjectProvider<CredentialsProvider> credentialsProvider;

	@Autowired
	private ObjectProvider<CredentialsRefreshService> credentialsRefreshService;

	@Autowired
	private ObjectProvider<ConnectionFactoryCustomizer> connectionFactoryCustomizers;

	@Autowired
	private RabbitConsumerProperties properties;

	@Value("#{${rabbit.converterBeanName:null}}")
	private MessageConverter messageConverter;

	private CachingConnectionFactory ownConnectionFactory;

	@Bean
	public Function<Message<?>, Object> rabbitConsumer(@Qualifier("amqpChannelAdapter") MessageHandler messageHandler) {
		return o -> {
			messageHandler.handleMessage(o);
			return "";
		};
	}

	@Bean
	public MessageHandler amqpChannelAdapter(ConnectionFactory rabbitConnectionFactory)
			throws Exception {

		AmqpOutboundChannelAdapterSpec handler = Amqp
				.outboundAdapter(rabbitTemplate(this.properties.isOwnConnection()
						? buildLocalConnectionFactory() : rabbitConnectionFactory))
				.mappedRequestHeaders(properties.getMappedRequestHeaders())
				.defaultDeliveryMode(properties.getPersistentDeliveryMode()
						? MessageDeliveryMode.PERSISTENT
						: MessageDeliveryMode.NON_PERSISTENT)
				.headersMappedLast(this.properties.isHeadersMappedLast());

		Expression exchangeExpression = this.properties.getExchangeExpression();
		if (exchangeExpression != null) {
			handler.exchangeNameExpression(exchangeExpression);
		}
		else {
			handler.exchangeName(this.properties.getExchange());
		}

		Expression routingKeyExpression = this.properties.getRoutingKeyExpression();
		if (routingKeyExpression != null) {
			handler.routingKeyExpression(routingKeyExpression);
		}
		else {
			handler.routingKey(this.properties.getRoutingKey());
		}
		return handler.get();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory rabbitConnectionFactory) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(rabbitConnectionFactory);
		if (this.messageConverter != null) {
			rabbitTemplate.setMessageConverter(this.messageConverter);
		}
		return rabbitTemplate;
	}


	@Bean
	@ConditionalOnProperty(name = "rabbit.converterBeanName",
			havingValue = RabbitConsumerProperties.JSON_CONVERTER)
	public Jackson2JsonMessageConverter jsonConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Override
	public void destroy() {
		if (this.ownConnectionFactory != null) {
			this.ownConnectionFactory.destroy();
		}
	}

	private ConnectionFactory buildLocalConnectionFactory() throws Exception {
		this.ownConnectionFactory = rabbitConnectionFactory(this.bootProperties, this.resourceLoader,
				this.credentialsProvider, this.credentialsRefreshService, this.connectionFactoryCustomizers);
		return this.ownConnectionFactory;
	}

	private CachingConnectionFactory rabbitConnectionFactory(RabbitProperties properties, ResourceLoader resourceLoader,
			ObjectProvider<CredentialsProvider> credentialsProvider,
			ObjectProvider<CredentialsRefreshService> credentialsRefreshService,
			ObjectProvider<ConnectionFactoryCustomizer> connectionFactoryCustomizers) throws Exception {

		/* NOTE: This is based on RabbitAutoConfiguration.RabbitConnectionFactoryCreator
		 * 		https://github.com/spring-projects/spring-boot/blob/c820ad01a108d419d8548265b8a34ed7c5591f7c/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/amqp/RabbitAutoConfiguration.java#L95
		 * [UPGRADE_CONSIDERATION] this should stay somewhat in sync w/ the functionality provided by its original source.
		 */
		RabbitConnectionFactoryBean connectionFactoryBean = new RabbitConnectionFactoryBean();
		RabbitConnectionFactoryBeanConfigurer connectionFactoryBeanConfigurer = new RabbitConnectionFactoryBeanConfigurer(resourceLoader, properties);
		connectionFactoryBeanConfigurer.setCredentialsProvider(credentialsProvider.getIfUnique());
		connectionFactoryBeanConfigurer.setCredentialsRefreshService(credentialsRefreshService.getIfUnique());
		connectionFactoryBeanConfigurer.configure(connectionFactoryBean);
		connectionFactoryBean.afterPropertiesSet();

		com.rabbitmq.client.ConnectionFactory connectionFactory = connectionFactoryBean.getObject();
		connectionFactoryCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(connectionFactory));

		CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(connectionFactory);
		CachingConnectionFactoryConfigurer cachingConnectionFactoryConfigurer = new CachingConnectionFactoryConfigurer(properties);
		cachingConnectionFactoryConfigurer.setConnectionNameStrategy(cf -> "rabbit.sink.own.connection");
		cachingConnectionFactoryConfigurer.configure(cachingConnectionFactory);
		cachingConnectionFactory.afterPropertiesSet();

		return cachingConnectionFactory;
	}

}
