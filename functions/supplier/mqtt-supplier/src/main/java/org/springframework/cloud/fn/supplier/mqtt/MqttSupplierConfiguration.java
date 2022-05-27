/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.cloud.fn.supplier.mqtt;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.config.ComponentCustomizer;
import org.springframework.cloud.fn.common.mqtt.MqttConfiguration;
import org.springframework.cloud.fn.common.mqtt.MqttProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * A source module that receives data from Mqtt.
 *
 * @author Janne Valkealahti
 * @author Soby Chacko
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({MqttProperties.class, MqttSupplierProperties.class})
@Import(MqttConfiguration.class)
public class MqttSupplierConfiguration {

	@Autowired
	private MqttSupplierProperties properties;

	@Autowired
	private MqttPahoClientFactory mqttClientFactory;

	@Autowired
	private BeanFactory beanFactory;

	@Bean
	public Supplier<Flux<Message<?>>> mqttSupplier(Publisher<Message<?>> mqttPublisher) {
		return () -> Flux.from(mqttPublisher);
	}

	@Bean
	public MqttPahoMessageDrivenChannelAdapter mqttInbound(
			@Nullable ComponentCustomizer<MqttPahoMessageDrivenChannelAdapter> mqttMessageProducerCustomizer) {

		MqttPahoMessageDrivenChannelAdapter adapter =
				new MqttPahoMessageDrivenChannelAdapter(this.properties.getClientId(), this.mqttClientFactory,
						this.properties.getTopics());
		adapter.setQos(this.properties.getQos());
		adapter.setConverter(pahoMessageConverter(this.beanFactory));
		adapter.setAutoStartup(false);

		if (mqttMessageProducerCustomizer != null) {
			mqttMessageProducerCustomizer.customize(adapter);
		}

		return adapter;
	}

	@Bean
	public Publisher<Message<byte[]>> mqttPublisher(MqttPahoMessageDrivenChannelAdapter mqttInbound) {
		return IntegrationFlows.from(mqttInbound)
				.toReactivePublisher(true);
	}

	private DefaultPahoMessageConverter pahoMessageConverter(BeanFactory beanFactory) {
		DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter(properties.getCharset());
		converter.setPayloadAsBytes(properties.isBinary());
		converter.setBeanFactory(beanFactory);
		return converter;
	}

}
