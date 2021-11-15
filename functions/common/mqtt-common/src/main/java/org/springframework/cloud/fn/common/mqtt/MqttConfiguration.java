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

package org.springframework.cloud.fn.common.mqtt;

import java.util.Map;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.util.ObjectUtils;

/**
 * Generic mqtt configuration.
 *
 * @author Janne Valkealahti
 * @author Artem Bilan
 */
@Configuration
public class MqttConfiguration {

	@Autowired
	private MqttProperties mqttProperties;

	@Bean
	public MqttPahoClientFactory mqttClientFactory() {

		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setServerURIs(mqttProperties.getUrl());
		mqttConnectOptions.setUserName(mqttProperties.getUsername());
		mqttConnectOptions.setPassword(mqttProperties.getPassword().toCharArray());
		mqttConnectOptions.setCleanSession(mqttProperties.isCleanSession());
		mqttConnectOptions.setConnectionTimeout(mqttProperties.getConnectionTimeout());
		mqttConnectOptions.setKeepAliveInterval(mqttProperties.getKeepAliveInterval());

		Map<String, String> sslProperties = mqttProperties.getSslProperties();

		if (!sslProperties.isEmpty()) {
			Properties sslProps = new Properties();
			sslProps.putAll(sslProperties);
			mqttConnectOptions.setSSLProperties(sslProps);
		}

		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		factory.setConnectionOptions(mqttConnectOptions);

		if (ObjectUtils.nullSafeEquals(mqttProperties.getPersistence(), "file")) {
			factory.setPersistence(new MqttDefaultFilePersistence(mqttProperties.getPersistenceDirectory()));
		}
		else if (ObjectUtils.nullSafeEquals(mqttProperties.getPersistence(), "memory")) {
			factory.setPersistence(new MemoryPersistence());
		}
		return factory;
	}

}
