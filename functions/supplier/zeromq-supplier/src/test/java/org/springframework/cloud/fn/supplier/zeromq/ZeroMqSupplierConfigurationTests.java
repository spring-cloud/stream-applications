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

package org.springframework.cloud.fn.supplier.zeromq;

import java.time.Duration;
import java.util.function.Supplier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.integration.zeromq.inbound.ZeroMqMessageProducer;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Frey
 * since 3.1.0
 */
public class ZeroMqSupplierConfigurationTests {

	private static final ZContext CONTEXT = new ZContext();

	@AfterAll
	static void tearDown() {
		CONTEXT.close();
	}

	@Test
	void testSubscriptionConfiguration() throws InterruptedException {

		String socketAddress = "tcp://*";
		ZMQ.Socket socket = CONTEXT.createSocket(SocketType.PUB);
		int bindPort = socket.bindToRandomPort(socketAddress);

		ZeroMqSupplierProperties properties = new ZeroMqSupplierProperties();
		properties.setConnectUrl("tcp://localhost:" + bindPort);
		properties.setTopics("test-topic");

		ZeroMqSupplierConfiguration configuration = new ZeroMqSupplierConfiguration();
		ZeroMqMessageProducer adapter = configuration.adapter(properties, CONTEXT, null);
		Supplier<Flux<Message<?>>> zeromqSupplier = configuration.zeromqSupplier(adapter);

		StepVerifier stepVerifier =
				StepVerifier.create(zeromqSupplier.get())
						.assertNext((message) ->
								assertThat(message.getPayload())
										.asInstanceOf(InstanceOfAssertFactories.type(byte[].class))
										.isEqualTo("test".getBytes(ZMQ.CHARSET))

						)
					.thenCancel()
					.verifyLater();

		Thread.sleep(2000);

		ZMsg msg = ZMsg.newStringMsg("test");
		msg.wrap(new ZFrame("test-topic"));
		msg.send(socket);

		stepVerifier.verify(Duration.ofSeconds(10));

		adapter.destroy();
		socket.close();
	}

}
