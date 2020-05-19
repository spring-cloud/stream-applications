/*
 * Copyright 2017-2020 the original author or authors.
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

import java.util.function.Consumer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.mqtt.MqttConfiguration;
import org.springframework.cloud.fn.common.mqtt.MqttProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * A consumer that sends data to Mqtt.
 *
 * @author Janne Valkealahti
 *
 */
@Configuration
@EnableConfigurationProperties({ MqttProperties.class, MqttConsumerProperties.class })
@Import(MqttConfiguration.class)
public class MqttConsumerConfiguration {

	@Autowired
	private MqttConsumerProperties properties;

	@Autowired
	private MqttPahoClientFactory mqttClientFactory;

	@Autowired
	private BeanFactory beanFactory;

	@Bean
	public Consumer<Message<?>> mqttConsumer() {
		return mqttOutbound()::handleMessage;
	}

	@Bean
	public MessageHandler mqttOutbound() {
		MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(properties.getClientId(), mqttClientFactory);
		messageHandler.setAsync(properties.isAsync());
		messageHandler.setDefaultTopic(properties.getTopic());
		messageHandler.setConverter(pahoMessageConverter());
		return messageHandler;
	}

	public DefaultPahoMessageConverter pahoMessageConverter() {
		DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter(properties.getQos(),
				properties.isRetained(), properties.getCharset());
		converter.setBeanFactory(beanFactory);
		return converter;
	}
}
