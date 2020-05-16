/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.tcp;

import java.util.function.Supplier;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.tcp.EncoderDecoderFactoryBean;
import org.springframework.cloud.fn.common.tcp.TcpConnectionFactoryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.ip.config.TcpConnectionFactoryFactoryBean;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.messaging.Message;

/**
 * A source module that receives data over TCP.
 *
 * @author Gary Russell
 * @author Christian Tzolov
 * @author Soby Chacko
 */
@Configuration
@EnableConfigurationProperties({TcpSupplierProperties.class, TcpConnectionFactoryProperties.class})
public class TcpSupplierConfiguration {

	@Autowired
	private TcpSupplierProperties properties;

	@Autowired
	private TcpConnectionFactoryProperties tcpConnectionProperties;

	@Qualifier("tcpSourceConnectionFactory")
	@Autowired
	private AbstractConnectionFactory connectionFactory;

	@Bean
	public Supplier<Flux<Message<?>>> tcpSupplier() {
		return () -> Flux.from(output())
				.doOnSubscribe(subscription -> adapter().start());
	}

	@Bean
	public TcpReceivingChannelAdapter adapter() {
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(connectionFactory);
		adapter.setOutputChannel(output());
		adapter.setAutoStartup(false);
		return adapter;
	}

	@Bean
	public FluxMessageChannel output() {
		return new FluxMessageChannel();
	}

	@Bean
	public TcpConnectionFactoryFactoryBean tcpSourceConnectionFactory(
			@Qualifier("tcpSourceDecoder") AbstractByteArraySerializer decoder) {
		TcpConnectionFactoryFactoryBean factoryBean = new TcpConnectionFactoryFactoryBean();
		factoryBean.setType("server");
		factoryBean.setPort(this.tcpConnectionProperties.getPort());
		factoryBean.setUsingNio(this.tcpConnectionProperties.isNio());
		factoryBean.setUsingDirectBuffers(this.tcpConnectionProperties.isUseDirectBuffers());
		factoryBean.setLookupHost(this.tcpConnectionProperties.isReverseLookup());
		factoryBean.setDeserializer(decoder);
		factoryBean.setSoTimeout(this.tcpConnectionProperties.getSocketTimeout());
		return factoryBean;
	}

	@Bean
	public EncoderDecoderFactoryBean tcpSourceDecoder() {
		EncoderDecoderFactoryBean factoryBean = new EncoderDecoderFactoryBean(this.properties.getDecoder());
		factoryBean.setMaxMessageSize(this.properties.getBufferSize());
		return factoryBean;
	}
}
