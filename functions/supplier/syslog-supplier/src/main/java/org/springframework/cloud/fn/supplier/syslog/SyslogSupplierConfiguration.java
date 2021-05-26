/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.cloud.fn.supplier.syslog;

import java.util.function.Supplier;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLfSerializer;
import org.springframework.integration.syslog.DefaultMessageConverter;
import org.springframework.integration.syslog.MessageConverter;
import org.springframework.integration.syslog.RFC5424MessageConverter;
import org.springframework.integration.syslog.inbound.RFC6587SyslogDeserializer;
import org.springframework.integration.syslog.inbound.SyslogReceivingChannelAdapterSupport;
import org.springframework.integration.syslog.inbound.TcpSyslogReceivingChannelAdapter;
import org.springframework.integration.syslog.inbound.UdpSyslogReceivingChannelAdapter;
import org.springframework.messaging.Message;

/**
 * Configuration class for SYSLOG Supplier.
 *
 * @author Soby Chacko
 */
@Configuration
@EnableConfigurationProperties(SyslogSupplierProperties.class)
public class SyslogSupplierConfiguration {

	@Autowired
	private SyslogSupplierProperties properties;

	@Bean
	public FluxMessageChannel syslogInputChannel() {
		return new FluxMessageChannel();
	}

	@Bean
	public Supplier<Flux<Message<?>>> syslogSupplier(ObjectProvider<UdpSyslogReceivingChannelAdapter> udpAdapterProvider,
												ObjectProvider<TcpSyslogReceivingChannelAdapter> tcpAdapterProvider) {
		return () -> Flux.from(syslogInputChannel())
				.doOnSubscribe(subscription -> {
					final UdpSyslogReceivingChannelAdapter udpAdapter = udpAdapterProvider.getIfAvailable();
					final TcpSyslogReceivingChannelAdapter tcpAdapter = tcpAdapterProvider.getIfAvailable();
					if (udpAdapter != null) {
						udpAdapter.start();
					}
					if (tcpAdapter != null) {
						tcpAdapter.start();
					}
				});
	}

	@Bean
	@ConditionalOnProperty(name = "syslog.supplier.protocol", havingValue = "udp")
	public UdpSyslogReceivingChannelAdapter udpAdapter() {
		return createUdpAdapter();
	}

	@Bean
	@ConditionalOnProperty(name = "syslog.supplier.protocol", havingValue = "both")
	public UdpSyslogReceivingChannelAdapter udpBothAdapter() {
		return createUdpAdapter();
	}

	private UdpSyslogReceivingChannelAdapter createUdpAdapter() {
		UdpSyslogReceivingChannelAdapter adapter = new UdpSyslogReceivingChannelAdapter();
		setAdapterProperties(adapter);
		return adapter;
	}

	@Bean
	@ConditionalOnProperty(name = "syslog.supplier.protocol", havingValue = "tcp", matchIfMissing = true)
	public TcpSyslogReceivingChannelAdapter tcpAdapter(
			@Qualifier("syslogSupplierConnectionFactory") AbstractServerConnectionFactory connectionFactory) {
		return createTcpAdapter(connectionFactory);
	}

	@Bean
	@ConditionalOnProperty(name = "syslog.supplier.protocol", havingValue = "both")
	public TcpSyslogReceivingChannelAdapter tcpBothAdapter(
			@Qualifier("syslogSupplierConnectionFactory") AbstractServerConnectionFactory connectionFactory) {
		return createTcpAdapter(connectionFactory);
	}

	@Bean
	public MessageConverter syslogConverter() {
		if (this.properties.getRfc().equals("5424")) {
			return new RFC5424MessageConverter();
		}
		else {
			return new DefaultMessageConverter();
		}
	}

	private TcpSyslogReceivingChannelAdapter createTcpAdapter(AbstractServerConnectionFactory connectionFactory) {
		TcpSyslogReceivingChannelAdapter adapter = new TcpSyslogReceivingChannelAdapter();
		adapter.setConnectionFactory(connectionFactory);
		setAdapterProperties(adapter);
		return adapter;
	}

	private void setAdapterProperties(SyslogReceivingChannelAdapterSupport adapter) {
		adapter.setPort(this.properties.getPort());
		adapter.setConverter(syslogConverter());
		adapter.setOutputChannel(syslogInputChannel());
		adapter.setAutoStartup(false);
	}


	@Configuration
	@ConditionalOnProperty(name = "syslog.supplier.protocol", havingValue = "tcp", matchIfMissing = true)
	protected static class TcpBits {

		@Autowired
		private SyslogSupplierProperties properties;

		@Bean
		public AbstractServerConnectionFactory syslogSupplierConnectionFactory(
				@Qualifier("syslogSupplierDecoder") Deserializer<?> decoder) throws Exception {
			AbstractServerConnectionFactory factory;
			if (this.properties.isNio()) {
				factory = new TcpNioServerConnectionFactory(this.properties.getPort());
			}
			else {
				factory = new TcpNetServerConnectionFactory(this.properties.getPort());
			}
			factory.setLookupHost(this.properties.isReverseLookup());
			factory.setDeserializer(decoder);
			factory.setSoTimeout(this.properties.getSocketTimeout());
			return factory;
		}

		@Bean
		public Deserializer<?> syslogSupplierDecoder() {
			ByteArrayLfSerializer decoder = new ByteArrayLfSerializer();
			decoder.setMaxMessageSize(this.properties.getBufferSize());
			if (this.properties.getRfc().equals("5424")) {
				return new RFC6587SyslogDeserializer(decoder);
			}
			else {
				return decoder;
			}
		}
	}

	@Configuration
	@ConditionalOnProperty(name = "syslog.supplier.protocol", havingValue = "both")
	protected static class BothBits extends TcpBits {

	}

}
