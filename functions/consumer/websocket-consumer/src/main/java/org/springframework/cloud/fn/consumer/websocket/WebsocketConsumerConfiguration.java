/*
 * Copyright 2014-2022 the original author or authors.
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

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
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
 * @author Chris Bono
 */
@Configuration
@EnableConfigurationProperties(WebsocketConsumerProperties.class)
public class WebsocketConsumerConfiguration {

	private static final Log logger = LogFactory.getLog(WebsocketConsumerConfiguration.class);

	@Value("${endpoints.websocketconsumertrace.enabled:false}")
	private boolean traceEndpointEnabled;

	@Autowired
	private WebsocketConsumerServer websocketConsumerServer;

	@PostConstruct
	public void init() throws InterruptedException {
		websocketConsumerServer.run();
	}

	@Bean
	@ConditionalOnProperty(value = "endpoints.websocketsinktrace.enabled", havingValue = "true")
	public WebsocketConsumerTraceEndpoint websocketTraceEndpoint(InMemoryTraceRepository websocketTraceRepository) {
		return new WebsocketConsumerTraceEndpoint(websocketTraceRepository);
	}

	@Bean
	public Consumer<Message<?>> websocketConsumer(InMemoryTraceRepository websocketTraceRepository) {
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
				addMessageToTraceRepository(websocketTraceRepository, message);
			}
		};
	}

	private void addMessageToTraceRepository(InMemoryTraceRepository websocketTraceRepository, Message<?> message) {
		Map<String, Object> trace = new LinkedHashMap<>();
		trace.put("type", "text");
		trace.put("direction", "out");
		trace.put("id", message.getHeaders().getId());
		trace.put("payload", message.getPayload().toString());
		websocketTraceRepository.add(trace);
	}

	@Configuration
	static class WebsocketConsumerServerConfiguration {
		@Bean
		public InMemoryTraceRepository websocketTraceRepository() {
			return new InMemoryTraceRepository();
		}

		@Bean
		public WebsocketConsumerServer server(WebsocketConsumerProperties properties, WebsocketConsumerServerInitializer initializer) {
			return new WebsocketConsumerServer(properties, initializer);
		}

		@Bean
		public WebsocketConsumerServerInitializer initializer(InMemoryTraceRepository websocketTraceRepository) {
			return new WebsocketConsumerServerInitializer(websocketTraceRepository);
		}
	}

}
