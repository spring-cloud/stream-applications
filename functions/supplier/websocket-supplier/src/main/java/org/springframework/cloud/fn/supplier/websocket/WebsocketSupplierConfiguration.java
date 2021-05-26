/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.cloud.fn.supplier.websocket;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.ServerWebSocketContainer;
import org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter;
import org.springframework.messaging.Message;

/**
 * A supplier that receives data over WebSocket.
 *
 * @author Krishnaprasad A S
 * @author Artem Bilan
 *
 */
@Configuration
@EnableConfigurationProperties(WebsocketSupplierProperties.class)
public class WebsocketSupplierConfiguration {

	@Autowired
	WebsocketSupplierProperties properties;

	@Bean
	public Supplier<Flux<Message<?>>> websocketSupplier(Publisher<Message<?>> websocketPublisher,
														WebSocketInboundChannelAdapter webSocketInboundChannelAdapter) {
		return () -> Flux.from(websocketPublisher)
				.doOnSubscribe(subscription -> webSocketInboundChannelAdapter.start())
				.doOnTerminate(webSocketInboundChannelAdapter::stop);
	}

	@Bean
	public Publisher<Message<byte[]>> websocketPublisher(IntegrationWebSocketContainer serverWebSocketContainer) {
		return IntegrationFlows.from(
				webSocketInboundChannelAdapter(serverWebSocketContainer))
				.toReactivePublisher();
	}

	private WebSocketInboundChannelAdapter webSocketInboundChannelAdapter(
			IntegrationWebSocketContainer serverWebSocketContainer) {
		WebSocketInboundChannelAdapter webSocketInboundChannelAdapter =
				new WebSocketInboundChannelAdapter(serverWebSocketContainer);
		webSocketInboundChannelAdapter.setAutoStartup(false);
		return webSocketInboundChannelAdapter;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "websocket.sockJs", name = "enable", havingValue = "true")
	public ServerWebSocketContainer.SockJsServiceOptions sockJsServiceOptions() {
		// TODO Expose SockJsServiceOptions as configuration properties
		return new ServerWebSocketContainer.SockJsServiceOptions();
	}

	@Bean
	public IntegrationWebSocketContainer serverWebSocketContainer(
			ObjectProvider<ServerWebSocketContainer.SockJsServiceOptions> sockJsServiceOptions) {
		return new ServerWebSocketContainer(properties.getPath())
				.setAllowedOrigins(properties.getAllowedOrigins())
				.withSockJs(sockJsServiceOptions.getIfAvailable());
	}

}
