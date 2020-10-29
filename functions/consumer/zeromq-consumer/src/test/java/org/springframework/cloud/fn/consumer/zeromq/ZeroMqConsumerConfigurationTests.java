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

package org.springframework.cloud.fn.consumer.zeromq;

import java.util.function.Function;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Frey
 * @since 3.1.0
 */
@SpringBootTest(properties = {
		"zeromq.consumer.topic='test-topic'"
})
@DirtiesContext
public class ZeroMqConsumerConfigurationTests {

	private static final ZContext CONTEXT = new ZContext();
	private static ZMQ.Socket socket;
	private static ZMQ.Poller poller;

	@Autowired
	Function<Flux<Message<?>>, Mono<Void>> subject;

	@BeforeAll
	static void setup() {

		socket = CONTEXT.createSocket(SocketType.SUB);
		socket.setReceiveTimeOut(10_000);
		int bindPort = socket.bindToRandomPort("tcp://*");
		socket.subscribe("test-topic");

		poller = CONTEXT.createPoller(1);
		poller.register(socket, ZMQ.Poller.POLLIN);

		System.setProperty("zeromq.consumer.connectUrl", "tcp://localhost:" + bindPort);

	}

	@AfterAll
	static void tearDown() {
		socket.close();
		CONTEXT.close();
	}

	@Test
	void testMessageHandlerConfiguration() throws InterruptedException {

		Thread.sleep(2000);

		Message<?> testMessage = MessageBuilder.withPayload("test").setHeader("topic", "test-topic").build();
		subject.apply(Flux.just(testMessage))
				.subscribe();

		ZMsg received = null;
		while (received == null) {

			poller.poll(10000);
			if (poller.pollin(0)) {

				received = ZMsg.recvMsg(socket);
				assertThat(received).isNotNull();
				assertThat(received.unwrap().getString(ZMQ.CHARSET)).isEqualTo("test-topic");
				assertThat(received.getLast().getString(ZMQ.CHARSET)).isEqualTo("test");

			}

		}

	}

	@SpringBootApplication
	public static class ZeroMqConsumerTestApplication {
	}

}
