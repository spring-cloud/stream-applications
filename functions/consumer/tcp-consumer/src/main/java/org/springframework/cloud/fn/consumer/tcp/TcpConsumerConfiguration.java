/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.cloud.fn.consumer.tcp;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.tcp.EncoderDecoderFactoryBean;
import org.springframework.cloud.fn.common.tcp.TcpConnectionFactoryProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.ip.config.TcpConnectionFactoryFactoryBean;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpMessageMapper;
import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.messaging.Message;

/**
 * A consumer that sends data over TCP.
 *
 * @author Gary Russell
 * @author Christian Tzolov
 * @author Chris Bono
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({TcpConsumerProperties.class, TcpConnectionFactoryProperties.class})
public class TcpConsumerConfiguration {

	private TcpConsumerProperties properties;
	private TcpConnectionFactoryProperties tcpConnectionProperties;

	public TcpConsumerConfiguration(TcpConsumerProperties properties,
									TcpConnectionFactoryProperties tcpConnectionProperties) {
		this.properties = properties;
		this.tcpConnectionProperties = tcpConnectionProperties;
	}

	@Bean
	public Consumer<Message<?>> tcpConsumer(TcpSendingMessageHandlerSmartLifeCycle handler) {
		return handler::handleMessage;
	}

	@Bean
	public TcpSendingMessageHandlerSmartLifeCycle handler(@Qualifier("tcpSinkConnectionFactory") AbstractConnectionFactory connectionFactory) {
		TcpSendingMessageHandlerSmartLifeCycle tcpMessageHandler = new TcpSendingMessageHandlerSmartLifeCycle();
		tcpMessageHandler.setConnectionFactory(connectionFactory);
		return tcpMessageHandler;
	}

	@Bean
	public TcpConnectionFactoryFactoryBean tcpSinkConnectionFactory(
			@Qualifier("tcpSinkEncoder") AbstractByteArraySerializer encoder,
			@Qualifier("tcpSinkMapper") TcpMessageMapper mapper) throws Exception {
		TcpConnectionFactoryFactoryBean factoryBean = new TcpConnectionFactoryFactoryBean();
		factoryBean.setType("client");
		factoryBean.setHost(this.properties.getHost());
		factoryBean.setPort(this.tcpConnectionProperties.getPort());
		factoryBean.setUsingNio(this.tcpConnectionProperties.isNio());
		factoryBean.setUsingDirectBuffers(this.tcpConnectionProperties.isUseDirectBuffers());
		factoryBean.setLookupHost(this.tcpConnectionProperties.isReverseLookup());
		factoryBean.setSerializer(encoder);
		factoryBean.setSoTimeout(this.tcpConnectionProperties.getSocketTimeout());
		factoryBean.setMapper(mapper);
		factoryBean.setSingleUse(this.properties.isClose());
		return factoryBean;
	}

	@Bean
	public EncoderDecoderFactoryBean tcpSinkEncoder() {
		return new EncoderDecoderFactoryBean(this.properties.getEncoder());
	}

	@Bean
	public TcpMessageMapper tcpSinkMapper() {
		TcpMessageMapper mapper = new TcpMessageMapper();
		mapper.setCharset(this.properties.getCharset());
		return mapper;
	}

	static class TcpSendingMessageHandlerSmartLifeCycle extends TcpSendingMessageHandler implements SmartLifecycle {

		@Override
		public boolean isAutoStartup() {
			return true;
		}

		@Override
		public int getPhase() {
			return Integer.MIN_VALUE;
		}
	}
}
