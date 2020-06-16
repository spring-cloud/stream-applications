/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.websocket;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.consumer.websocket.actuator.WebsocketConsumerTraceEndpoint;
import org.springframework.cloud.fn.consumer.websocket.trace.InMemoryTraceRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;

/**
 * @author Oliver Moser
 * @author Gary Russell
 * @author Artem Bilan
 */
@Configuration
@EnableConfigurationProperties(WebsocketConsumerProperties.class)
public class WebsocketConsumerConfiguration {


	private static final Log logger = LogFactory.getLog(WebsocketConsumerConfiguration.class);

	private final InMemoryTraceRepository websocketTraceRepository = new InMemoryTraceRepository();

	@Value("${endpoints.websocketconsumertrace.enabled:false}")
	private boolean traceEndpointEnabled;

	@PostConstruct
	public void init() throws InterruptedException {
		server().run();
	}

	@Bean
	public WebsocketConsumerServer server() {
		return new WebsocketConsumerServer();
	}

	@Bean
	public WebsocketConsumerServerInitializer initializer() {
		return new WebsocketConsumerServerInitializer(this.websocketTraceRepository);
	}

	@Bean
	@ConditionalOnProperty(value = "endpoints.websocketsinktrace.enabled", havingValue = "true")
	public WebsocketConsumerTraceEndpoint websocketTraceEndpoint() {
		return new WebsocketConsumerTraceEndpoint(this.websocketTraceRepository);
	}

	@Bean
	public Consumer<Message<?>> websocketConsumer() {
		return message -> {
			if (logger.isTraceEnabled()) {
				logger.trace("Handling message: " + message);
			}
			SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
			headers.setMessageTypeIfNotSet(SimpMessageType.MESSAGE);
			String messagePayload = message.getPayload().toString();
			for (Channel channel : WebsocketConsumerServer.channels) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Writing message %s to channel %s", messagePayload, channel.localAddress()));
				}

				channel.write(new TextWebSocketFrame(messagePayload));
				channel.flush();
			}

			if (this.traceEndpointEnabled) {
				addMessageToTraceRepository(message);
			}
		};
	}

	private void addMessageToTraceRepository(Message<?> message) {
		Map<String, Object> trace = new LinkedHashMap<>();
		trace.put("type", "text");
		trace.put("direction", "out");
		trace.put("id", message.getHeaders().getId());
		trace.put("payload", message.getPayload().toString());
		this.websocketTraceRepository.add(trace);
	}
}
