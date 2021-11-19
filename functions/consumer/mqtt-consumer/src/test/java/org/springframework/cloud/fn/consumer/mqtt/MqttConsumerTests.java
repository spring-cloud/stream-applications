/*
 * Copyright 2017-2021 the original author or authors.
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

package org.springframework.cloud.fn.consumer.mqtt;

import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"mqtt.consumer.topic=test",
		"mqtt.ssl-properties.com.ibm.ssl.protocol=TLS",
		"mqtt.ssl-properties.com.ibm.ssl.keyStoreType=TEST" })
@DirtiesContext
@Tag("integration")
public class MqttConsumerTests {

	static {
		GenericContainer<?> mosquitto =
				new GenericContainer<>("eclipse-mosquitto:2.0.13")
						.withCommand("mosquitto -c /mosquitto-no-auth.conf")
						.withReuse(true)
						.withExposedPorts(1883);
		mosquitto.start();
		final Integer mappedPort = mosquitto.getMappedPort(1883);
		System.setProperty("mqtt.url", "tcp://localhost:" + mappedPort);
	}

	@Autowired
	private MqttPahoMessageDrivenChannelAdapter mqttPahoMessageDrivenChannelAdapter;

	@Autowired
	private Consumer<Message<?>> mqttConsumer;

	@Autowired
	protected QueueChannel queue;

	@AfterAll
	public static void cleanup() {
		System.clearProperty("mqtt.url");
	}

	@Test
	public void testMqttConsumer() {
		MqttConnectOptions connectionInfo = this.mqttPahoMessageDrivenChannelAdapter.getConnectionInfo();
		Properties sslProperties = connectionInfo.getSSLProperties();
		assertThat(sslProperties)
				.containsEntry(SSLSocketFactoryFactory.SSLPROTOCOL, SSLSocketFactoryFactory.DEFAULT_PROTOCOL)
				.containsEntry(SSLSocketFactoryFactory.KEYSTORETYPE, "TEST");

		this.mqttConsumer.accept(MessageBuilder.withPayload("hello").build());
		Message<?> in = this.queue.receive(10000);
		assertThat(in).isNotNull();
		assertThat(in.getPayload()).isEqualTo("hello");
	}

	@SpringBootApplication
	static class MqttConsumerTestApplication {

		@Autowired
		private MqttPahoClientFactory mqttClientFactory;

		@Bean
		public MqttPahoMessageDrivenChannelAdapter mqttInbound(BeanFactory beanFactory) {
			MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("test",
					mqttClientFactory, "test");
			adapter.setQos(0);
			adapter.setConverter(pahoMessageConverter(beanFactory));
			adapter.setOutputChannelName("queue");
			return adapter;
		}

		public DefaultPahoMessageConverter pahoMessageConverter(BeanFactory beanFactory) {
			DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter(1, true, "UTF-8");
			converter.setPayloadAsBytes(false);
			converter.setBeanFactory(beanFactory);
			return converter;
		}

		@Bean
		public QueueChannel queue() {
			return new QueueChannel();
		}

	}

}
