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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.test.support.websocket.WebsocketConsumerClientHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oliver Moser
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"websocket.consumer.port=0",
				"websocket.consumer.path=/some_websocket_path",
				"websocket.consumer.logLevel=DEBUG",
				"websocket.consumer.threads=2"
		})
@DirtiesContext
public class WebsocketConsumerTests {

	public static final int TIMEOUT = 10000;

	public static final int MESSAGE_COUNT = 100;

	public static final int CLIENT_COUNT = 10;

	@Autowired
	private WebsocketConsumerProperties properties;

	@Autowired
	private WebsocketConsumerServer consumerServer;

	@Autowired
	Consumer<Message<?>> websocketConsumer;

	@Test
	public void checkCmdlineArgs() {
		assertThat(properties.getPath()).isEqualTo("/some_websocket_path");
		assertThat(properties.getPort()).isEqualTo((0));
		assertThat(properties.getLogLevel()).isEqualTo(("DEBUG"));
		assertThat(properties.getThreads()).isEqualTo((2));
	}

	@Test
	@Timeout(TIMEOUT)
	public void testMultipleMessageSingleSubscriber() throws Exception {
		WebsocketConsumerClientHandler handler = new WebsocketConsumerClientHandler("handler_0", MESSAGE_COUNT, TIMEOUT);
		doHandshake(handler);

		List<String> messagesToSend = submitMultipleMessages(MESSAGE_COUNT);
		handler.await();

		assertThat(handler.getReceivedMessages().size()).isEqualTo(MESSAGE_COUNT);
		messagesToSend.forEach(s -> assertThat(handler.getReceivedMessages().contains(s)).isTrue());
	}

	@Test
	@Timeout(TIMEOUT)
	public void testSingleMessageMultipleSubscribers() throws Exception {

		// create multiple handlers
		List<WebsocketConsumerClientHandler> handlers = createHandlerList(CLIENT_COUNT, 1);

		// submit a single message
		String payload = UUID.randomUUID().toString();
		websocketConsumer.accept(MessageBuilder.withPayload(payload).build());

		// await completion on each handler
		for (WebsocketConsumerClientHandler handler : handlers) {
			handler.await();
			assertThat(handler.getReceivedMessages().size()).isEqualTo(1);
			assertThat(handler.getReceivedMessages().get(0)).isEqualTo(payload);
		}
	}

	@Test
	@Timeout(TIMEOUT)
	public void testMultipleMessagesMultipleSubscribers() throws Exception {

		// create multiple handlers
		List<WebsocketConsumerClientHandler> handlers = createHandlerList(CLIENT_COUNT, MESSAGE_COUNT);

		// submit mulitple  message
		List<String> messagesReceived = submitMultipleMessages(MESSAGE_COUNT);

		// wait on each handle
		for (WebsocketConsumerClientHandler handler : handlers) {
			handler.await();
			assertThat(handler.getReceivedMessages().size()).isEqualTo(messagesReceived.size());
			assertThat(handler.getReceivedMessages()).isEqualTo(messagesReceived);
		}
	}

	private WebSocketSession doHandshake(WebsocketConsumerClientHandler handler)
			throws InterruptedException, ExecutionException {
		String wsEndpoint = "ws://localhost:" + this.consumerServer.getPort() + this.properties.getPath();
		return new StandardWebSocketClient().doHandshake(handler, wsEndpoint).get();
	}

	private List<String> submitMultipleMessages(int messageCount) {
		List<String> messagesToSend = new ArrayList<>(messageCount);
		for (int i = 0; i < messageCount; i++) {
			String message = "message_" + i;
			messagesToSend.add(message);
			websocketConsumer.accept(MessageBuilder.withPayload(message).build());
		}

		return messagesToSend;
	}

	private List<WebsocketConsumerClientHandler> createHandlerList(int handlerCount, int messageCount) throws
			InterruptedException,
			ExecutionException {

		List<WebsocketConsumerClientHandler> handlers = new ArrayList<>(handlerCount);
		for (int i = 0; i < handlerCount; i++) {
			WebsocketConsumerClientHandler handler = new WebsocketConsumerClientHandler("handler_" + i, messageCount, TIMEOUT);
			doHandshake(handler);
			handlers.add(handler);
		}
		return handlers;
	}

	@SpringBootApplication
	public static class WebsocketConsumerTestApplication {

	}
}
