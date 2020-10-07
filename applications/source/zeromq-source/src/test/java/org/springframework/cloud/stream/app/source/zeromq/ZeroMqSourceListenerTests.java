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

import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.supplier.zeromq.ZeroMqSupplierConfiguration;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ZeroMqSource.
 *
 * @author Daniel Frey
 * @author Gregory Green
 * @since 3.1.0
 */
public class ZeroMqSourceListenerTests {

	private static final ZContext CONTEXT = new ZContext();

	@AfterAll
	static void teardown() {
		CONTEXT.close();
	}

	@Test
	public void testZeroMqSource() throws InterruptedException {

		Socket socket = CONTEXT.createSocket(SocketType.PUB);
		int boundPort = socket.bindToRandomPort("tcp://*");

		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(ZeroMqSourceTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=zeromqSupplier",
						"--zeromq.supplier.connectUrl=tcp://localhost:" + boundPort,
						"--zeromq.supplier.topics=test-topic"
				)) {

			Supplier<Flux<Message<?>>> zeromqSupplier = context.getBean(Supplier.class);

			StepVerifier stepVerifier =
					StepVerifier.create(zeromqSupplier.get())
							.assertNext((message) -> assertThat(message.getPayload()).isEqualTo("test".getBytes(ZMQ.CHARSET)))
							.thenCancel()
							.verifyLater();

			Thread.sleep(2000);

			ZMsg msg = ZMsg.newStringMsg("test");
			msg.wrap(new ZFrame("test-topic"));
			boolean sent = msg.send(socket);
			assertThat(sent).isTrue();

			stepVerifier.verify();

			socket.close();

		}

	}

	@SpringBootApplication
	@Import(ZeroMqSupplierConfiguration.class)
	public static class ZeroMqSourceTestApplication { }

}
