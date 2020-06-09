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

package org.springframework.cloud.fn.test.support.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public class WebsocketConsumerClientHandler extends AbstractWebSocketHandler {

	final List<String> receivedMessages = new ArrayList<>();

	final int waitMessageCount;

	final CountDownLatch latch;

	final long timeout;

	final String id;

	public WebsocketConsumerClientHandler(String id, int waitMessageCount, long timeout) {
		this.id = id;
		this.waitMessageCount = waitMessageCount;
		this.latch = new CountDownLatch(waitMessageCount);
		this.timeout = timeout;
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) {
		receivedMessages.add(message.getPayload());
		latch.countDown();
	}

	public void await() throws InterruptedException {
		latch.await(timeout, TimeUnit.MILLISECONDS);
	}

	public List<String> getReceivedMessages() {
		return receivedMessages;
	}
}
