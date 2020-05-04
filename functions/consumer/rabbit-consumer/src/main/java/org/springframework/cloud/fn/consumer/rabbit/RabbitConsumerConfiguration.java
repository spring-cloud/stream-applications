/*
 * Copyright 2019-2020 the original author or authors.
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

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.amqp.dsl.AmqpOutboundChannelAdapterSpec;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

@EnableConfigurationProperties(RabbitConsumerProperties.class)
@Configuration
public class RabbitConsumerConfiguration implements DisposableBean {

	@Autowired
	private RabbitProperties bootProperties;

	@Autowired
	private ObjectProvider<ConnectionNameStrategy> connectionNameStrategy;

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
						: MessageDeliveryMode.NON_PERSISTENT);

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

	private ConnectionFactory buildLocalConnectionFactory() throws Exception {
		this.ownConnectionFactory = new AutoConfig.Creator().rabbitConnectionFactory(
				this.bootProperties, this.connectionNameStrategy);
		return this.ownConnectionFactory;
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

}

class AutoConfig extends RabbitAutoConfiguration {

	static class Creator extends RabbitConnectionFactoryCreator {

		@Override
		public CachingConnectionFactory rabbitConnectionFactory(RabbitProperties config,
																ObjectProvider<ConnectionNameStrategy> connectionNameStrategy)
				throws Exception {
			CachingConnectionFactory cf = super.rabbitConnectionFactory(config,
					connectionNameStrategy);
			cf.setConnectionNameStrategy(
					connectionFactory -> "rabbit.sink.own.connection");
			cf.afterPropertiesSet();
			return cf;
		}

	}

}
