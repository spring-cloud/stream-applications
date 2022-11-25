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

package org.springframework.cloud.stream.app.mqtt.sink;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.consumer.mqtt.MqttConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
@Tag("integration")
public class MqttSinkTests {

	static {
		GenericContainer mosquitto = new GenericContainer("cyrilix/rabbitmq-mqtt")
			.withExposedPorts(1883)
			.withStartupTimeout(Duration.ofSeconds(120))
			.withStartupAttempts(3);
		mosquitto.start();
		final Integer mappedPort = mosquitto.getMappedPort(1883);
		System.setProperty("mqtt.url", "tcp://localhost:" + mappedPort);
	}

	@AfterAll
	public static void cleanup() {
		System.clearProperty("mqtt.url");
	}

	@Test
	public void testMqttSink() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(MqttSinkTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=mqttConsumer",
						"--mqtt.consumer.topic=test")) {

			final Message<String> message = MessageBuilder.withPayload("hello").build();
			InputDestination source = context.getBean(InputDestination.class);
			source.send(message);

			QueueChannel queueChannel = context.getBean(QueueChannel.class);
			Message<?> in = queueChannel.receive(10000);
			assertThat(in).isNotNull();
			assertThat(in.getPayload()).isEqualTo("hello");
		}
	}

	@EnableAutoConfiguration
	@Import(MqttConsumerConfiguration.class)
	public static class MqttSinkTestConfiguration {

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
