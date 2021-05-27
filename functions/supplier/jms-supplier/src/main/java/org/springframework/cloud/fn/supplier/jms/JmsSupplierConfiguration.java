/*
 * Copyright 2016-2021 the original author or authors.
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

import javax.jms.ConnectionFactory;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.messaging.Message;

@Configuration
@EnableConfigurationProperties(JmsSupplierProperties.class)
public class JmsSupplierConfiguration {

	@Autowired
	JmsSupplierProperties properties;

	@Autowired
	private JmsProperties jmsProperties;

	@Autowired
	private ConnectionFactory connectionFactory;

	@Bean
	public Supplier<Flux<Message<?>>> jmsSupplier(Publisher<Message<?>> jmsPublisher, JmsMessageDrivenEndpoint adapter) {
		return () -> Flux.from(jmsPublisher)
				.doOnSubscribe(subscription -> adapter.start())
				.doOnTerminate(adapter::stop);
	}

	@Bean
	public Publisher<Message<byte[]>> jmsPublisher() {
		return IntegrationFlows.from(
				Jms.messageDrivenChannelAdapter(container())
						.autoStartup(false))
				.toReactivePublisher();
	}

	@Bean
	public AbstractMessageListenerContainer container() {
		AbstractMessageListenerContainer container;
		JmsProperties.Listener listenerProperties = this.jmsProperties.getListener();
		if (this.properties.isSessionTransacted()) {
			DefaultMessageListenerContainer dmlc = new DefaultMessageListenerContainer();
			dmlc.setSessionTransacted(true);
			if (listenerProperties.getConcurrency() != null) {
				dmlc.setConcurrentConsumers(listenerProperties.getConcurrency());
			}
			if (listenerProperties.getMaxConcurrency() != null) {
				dmlc.setMaxConcurrentConsumers(listenerProperties.getMaxConcurrency());
			}
			container = dmlc;
		}
		else {
			SimpleMessageListenerContainer smlc = new SimpleMessageListenerContainer();
			smlc.setSessionTransacted(false);
			if (listenerProperties != null  && listenerProperties.getConcurrency() != null) {
				smlc.setConcurrentConsumers(listenerProperties.getConcurrency());
			}
			container = smlc;
		}
		container.setConnectionFactory(this.connectionFactory);
		if (this.properties.getClientId() != null) {
			container.setClientId(this.properties.getClientId());
		}
		container.setDestinationName(this.properties.getDestination());
		if (this.properties.getMessageSelector() != null) {
			container.setMessageSelector(this.properties.getMessageSelector());
		}
		container.setPubSubDomain(this.jmsProperties.isPubSubDomain());
		if (this.properties.getMessageSelector() != null
				&& listenerProperties.getAcknowledgeMode() != null) {
			container.setSessionAcknowledgeMode(listenerProperties.getAcknowledgeMode().getMode());
		}
		if (this.properties.getSubscriptionDurable() != null) {
			container.setSubscriptionDurable(this.properties.getSubscriptionDurable());
		}
		if (this.properties.getSubscriptionName() != null) {
			container.setSubscriptionName(this.properties.getSubscriptionName());
		}
		if (this.properties.getSubscriptionShared() != null) {
			container.setSubscriptionShared(this.properties.getSubscriptionShared());
		}
		return container;
	}
}
