/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.cloud.stream.app.source.tcp;

import java.net.Socket;

import javax.net.SocketFactory;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.supplier.tcp.TcpSupplierConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

public class TcpSourceTests {

	@Test
	public void testBasicTcpSourceWithBinder() throws Exception {

		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(TcpSourceTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=tcpSupplier")) {

			AbstractServerConnectionFactory connectionFactory = context.getBean(AbstractServerConnectionFactory.class);

			doTest("", "foo", "\r\n", connectionFactory);

			OutputDestination target = context.getBean(OutputDestination.class);
			Message<byte[]> sourceMessage = target.receive(10000, "tcpSupplier-out-0");
			String actual = new String(sourceMessage.getPayload());
			assertThat(actual).isEqualTo("foo");

			sourceMessage = target.receive(10000, "tcpSupplier-out-0");
			actual = new String(sourceMessage.getPayload());
			assertThat(actual).isEqualTo("foo");
		}
	}

	protected void doTest(String prefix, String payload, String suffix,
						AbstractServerConnectionFactory connectionFactory) throws Exception {
		int port = getPort(connectionFactory);
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

	}

	private int getPort(AbstractServerConnectionFactory connectionFactory) throws Exception {
		int n = 0;
		while (n++ < 100 && !connectionFactory.isListening()) {
			Thread.sleep(100);
		}
		assertThat(connectionFactory.isListening()).isTrue();
		int port = connectionFactory.getPort();
		assertThat(port > 0).isTrue();
		return port;
	}


	@SpringBootApplication
	@Import(TcpSupplierConfiguration.class)
	public static class TcpSourceTestApplication {
	}
}
