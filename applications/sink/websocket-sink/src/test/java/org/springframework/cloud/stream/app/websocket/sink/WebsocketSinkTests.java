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

package org.springframework.cloud.stream.app.websocket.sink;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.consumer.websocket.WebsocketConsumerConfiguration;
import org.springframework.cloud.fn.consumer.websocket.WebsocketConsumerProperties;
import org.springframework.cloud.fn.consumer.websocket.WebsocketConsumerServer;
import org.springframework.cloud.fn.test.support.websocket.WebsocketConsumerClientHandler;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"websocket.consumer.port=0",
				"websocket.consumer.path=/some_websocket_path",
				"websocket.consumer.logLevel=DEBUG",
				"websocket.consumer.threads=2"
		})
@DirtiesContext
public class WebsocketSinkTests {

	public static final int TIMEOUT = 10000;

	public static final int MESSAGE_COUNT = 100;

	@Autowired
	private InputDestination inputDestination;

	@Autowired
	private WebsocketConsumerProperties properties;

	@Autowired
	private WebsocketConsumerServer sinkServer;

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

	private WebSocketSession doHandshake(WebsocketConsumerClientHandler handler)
			throws InterruptedException, ExecutionException {
		String wsEndpoint = "ws://localhost:" + this.sinkServer.getPort() + this.properties.getPath();
		return new StandardWebSocketClient().doHandshake(handler, wsEndpoint).get();
	}

	private List<String> submitMultipleMessages(int messageCount) {
		List<String> messagesToSend = new ArrayList<>(messageCount);
		for (int i = 0; i < messageCount; i++) {
			String message = "message_" + i;
			messagesToSend.add(message);
			inputDestination.send(MessageBuilder.withPayload(message).build());
		}
		return messagesToSend;
	}

	@SpringBootApplication
	@Import({TestChannelBinderConfiguration.class, WebsocketConsumerConfiguration.class})
	public static class SampleConfiguration {

	}
}
