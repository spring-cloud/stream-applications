/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.cloud.fn.supplier.tcp;

import java.net.Socket;
import java.util.function.Supplier;

import javax.net.SocketFactory;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TCP Supplier.
 *
 * @author Gary Russell
 * @author Soby Chacko
 * @author Artem Bilan
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "tcp.port = 0")
@DirtiesContext
public class AbstractTcpSupplierTests {

	@Autowired
	Supplier<Flux<Message<?>>> tcpSupplier;

	@Autowired
	protected AbstractServerConnectionFactory connectionFactory;

	@Autowired
	protected TcpSupplierProperties properties;

	/*
	 * Sends two messages with <prefix><payload><suffix> and asserts the
	 * payload is received on the other side.
	 */
	protected void doTest(String prefix, String payload, String suffix) throws Exception {

		final Flux<Message<?>> messageFlux = tcpSupplier.get();

		final StepVerifier stepVerifier = StepVerifier.create(messageFlux)
				.assertNext((message) -> {
							assertThat(message.getPayload())
									.isEqualTo(payload.getBytes());
						}
				)
				.assertNext((message) -> {
							assertThat(message.getPayload())
									.isEqualTo(payload.getBytes());
						}
				)
				.thenCancel()
				.verifyLater();


		int port = getPort();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write((prefix + payload + suffix).getBytes());
		if (prefix.length() == 0 && suffix.length() == 0) {
			socket.close(); // RAW - for the others, close AFTER the messages are decoded.
			socket = SocketFactory.getDefault().createSocket("localhost", port);
		}

		socket.getOutputStream().write((prefix + payload + suffix).getBytes());
		if (prefix.length() == 0 && suffix.length() == 0) {
			socket.close(); // RAW - for the others, close AFTER the messages are decoded.
		}
		socket.close();

		stepVerifier.verify();
	}

	private int getPort() throws Exception {
		int n = 0;
		while (n++ < 100 && !this.connectionFactory.isListening()) {
			Thread.sleep(100);
		}
		assertThat(this.connectionFactory.isListening()).isTrue();
		int port = this.connectionFactory.getPort();
		assertThat(port > 0).isTrue();
		return port;
	}

	@SpringBootApplication
	public static class TcpSupplierTestApplication {

	}
}
