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

package org.springframework.cloud.stream.app.source.zeromq;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.supplier.zeromq.ZeroMqSupplierConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.zeromq.ZeroMqHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ZeroMqSource.
 *
 * @author Daniel Frey
 * @author Gregory Green
 * @since 3.1.0
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cloud.function.definition=zeromqSupplier",
				"zeromq.supplier.topics=test-topic"
		}
)
@DirtiesContext
public class ZeroMqSourceTests {

	private static final ZContext CONTEXT = new ZContext();
	private static ZMQ.Socket socket;

	@Autowired
	OutputDestination outputDestination;

	@BeforeAll
	static void setup() {

		String socketAddress = "tcp://*";
		socket = CONTEXT.createSocket(SocketType.PUB);
		int bindPort = socket.bindToRandomPort(socketAddress);

		System.setProperty("zeromq.supplier.connectUrl", "tcp://localhost:" + bindPort);

	}

	@AfterAll
	static void teardown() {

		socket.close();
		CONTEXT.close();

	}

	@Test
	public void testSourceFromSupplier() throws InterruptedException {

		Thread.sleep(5000);

		ZMsg msg = ZMsg.newStringMsg("test");
		msg.wrap(new ZFrame("test-topic"));
		msg.send(socket);

		Message<byte[]> sourceMessage = outputDestination.receive(10000, "zeromqSupplier-out-0");
		final String actualPayload = new String(sourceMessage.getPayload());
		final String actualHeader = sourceMessage.getHeaders().get(ZeroMqHeaders.TOPIC, String.class);
		assertThat(actualPayload).isEqualTo("test");
		assertThat(actualHeader).isEqualTo("test-topic");

	}

	@SpringBootApplication
	@Import({ZeroMqSupplierConfiguration.class, TestChannelBinderConfiguration.class, BindingServiceConfiguration.class})
	public static class ZeroMqSourceTestApplication { }

}
