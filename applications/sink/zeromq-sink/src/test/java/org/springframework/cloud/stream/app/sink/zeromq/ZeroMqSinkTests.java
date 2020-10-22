/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.zeromq;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Timeout;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.consumer.zeromq.ZeroMqConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.zeromq.ZeroMqHeaders;
import org.springframework.integration.zeromq.outbound.ZeroMqMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ZeroMqSink.
 *
 * @author Daniel Frey
 * @since 3.1.0
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cloud.function.definition=zeromqConsumer",
				"zeromq.consumer.topic='test-topic'"
		}
)
@DirtiesContext
public class ZeroMqSinkTests {

	public static final int TIMEOUT = 10000;

	private static final ZContext CONTEXT = new ZContext();
	private static ZMQ.Socket socket;

	@Autowired
	ZeroMqMessageHandler zeromqMessageHandler;

	@Autowired
	InputDestination inputDestination;

	@Autowired
	ObjectMapper mapper;

	@BeforeAll
	static void setup() {

		String socketAddress = "tcp://*";
		socket = CONTEXT.createSocket(SocketType.SUB);
		int bindPort = socket.bindToRandomPort(socketAddress);

		System.setProperty("zeromq.consumer.connectUrl", "tcp://localhost:" + bindPort);

	}

	@AfterAll
	static void teardown() {

		socket.close();
		CONTEXT.close();

	}

	@Test
	@Timeout(TIMEOUT)
	public void testSinkFromFunction() throws InterruptedException, IOException {

		Thread.sleep(2000);
		socket.setReceiveTimeOut(TIMEOUT);
		socket.subscribe("test-topic");

		inputDestination.send(MessageBuilder.withPayload("test".getBytes(ZMQ.CHARSET)).setHeader(ZeroMqHeaders.TOPIC, "test-topic").build());

		ZMsg received = ZMsg.recvMsg(socket);
		assertThat(received.getFirst().getData()).isEqualTo("test-topic".getBytes(ZMQ.CHARSET));
		assertThat(received.getLast().getData()).isEqualTo("test".getBytes(ZMQ.CHARSET));

	}

	@SpringBootApplication
	@Import({ZeroMqConsumerConfiguration.class, TestChannelBinderConfiguration.class, BindingServiceConfiguration.class})
	public static class ZeroMqSourceTestApplication { }

}
