/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.xmpp;

import java.util.function.Supplier;

import org.jivesoftware.smack.XMPPConnection;
import reactor.core.publisher.Flux;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.xmpp.XmppConnectionFactoryConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.xmpp.inbound.ChatMessageListeningEndpoint;
import org.springframework.messaging.Message;

/**
 * A source module that receives data from ZeroMQ.
 *
 * @author Daniel Frey
 * @since 4.0.0
 */
@Configuration
@EnableConfigurationProperties(XmppSupplierProperties.class)
@Import(XmppConnectionFactoryConfiguration.class)
public class XmppSupplierConfiguration {

	private FluxMessageChannel output = new FluxMessageChannel();

	@Bean
	public ChatMessageListeningEndpoint chatMessageListeningEndpoint(XMPPConnection xmppConnection, XmppSupplierProperties properties) {

		ChatMessageListeningEndpoint chatMessageListeningEndpoint = new ChatMessageListeningEndpoint(xmppConnection);

		if(properties.getPayloadExpression() != null) {
			chatMessageListeningEndpoint.setPayloadExpression(properties.getPayloadExpression());
		}

		chatMessageListeningEndpoint.setStanzaFilter(properties.getStanzaFilter());
		chatMessageListeningEndpoint.setOutputChannel(output);
		chatMessageListeningEndpoint.setAutoStartup(false);

		return chatMessageListeningEndpoint;
	}

	@Bean
	public Supplier<Flux<Message<?>>> xmppSupplier(ChatMessageListeningEndpoint chatMessageListeningEndpoint) {
		return () -> Flux.from(output).doOnSubscribe(subscription -> chatMessageListeningEndpoint.start());
	}

}
