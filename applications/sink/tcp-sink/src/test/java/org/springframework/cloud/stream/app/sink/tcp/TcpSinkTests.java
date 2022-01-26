/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.consumer.tcp.TcpConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

public class TcpSinkTests {

	private static TestTCPServer server;

	@BeforeAll
	public static void setup() {
		server = new TestTCPServer();
	}

	@AfterAll
	public static void shutdown() {
		server.shutDown();
	}

	@Test
	public void testFileSink() throws Exception {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(TcpSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=tcpConsumer",
						"--tcp.consumer.host=localhost",
						"--tcp.port=${tcp.consumer.test.port}")) {

			server.setDecoder(new ByteArrayCrLfSerializer());

			Message<String> message1 = MessageBuilder.withPayload("foo").build();
			InputDestination source = context.getBean(InputDestination.class);
			source.send(message1);

			String received = server.queue.poll(10, TimeUnit.SECONDS);
			assertThat(received).isEqualTo("foo");

			Message<String> message2 = MessageBuilder.withPayload("bar").build();
			source.send(message2);

			received = server.queue.poll(10, TimeUnit.SECONDS);
			assertThat(received).isEqualTo("bar");
		}
	}

	/**
	 * TCP server that uses the supplied {@link AbstractByteArraySerializer}
	 * to decode the input stream and put the resulting message in a queue.
	 *
	 */
	private static class TestTCPServer implements Runnable {

		private static final Log logger = LogFactory.getLog(TestTCPServer.class);

		private final ServerSocket serverSocket;

		private final ExecutorService executor;

		private volatile AbstractByteArraySerializer decoder;

		private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

		private volatile boolean stopped;

		TestTCPServer() {
			ServerSocket serverSocket = null;
			ExecutorService executor = null;
			try {
				serverSocket = ServerSocketFactory.getDefault().createServerSocket(0);
				System.setProperty("tcp.consumer.test.port", Integer.toString(serverSocket.getLocalPort()));
				executor = Executors.newSingleThreadExecutor();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			this.serverSocket = serverSocket;
			this.executor = executor;
			this.decoder = new ByteArrayCrLfSerializer();
			executor.execute(this);
		}

		private void setDecoder(AbstractByteArraySerializer decoder) {
			this.decoder = decoder;
		}

		@Override
		public void run() {
			while (true) {
				Socket socket = null;
				try {
					logger.info("Server listening on " + this.serverSocket.getLocalPort());
					socket = this.serverSocket.accept();
					while (true) {
						byte[] data = decoder.deserialize(socket.getInputStream());
						queue.offer(new String(data));
					}
				}
				catch (SoftEndOfStreamException e) {
					// normal close
				}
				catch (IOException e) {
					try {
						if (socket != null) {
							socket.close();
						}
					}
					catch (IOException e1) {
					}
					logger.error(e.getMessage());
					if (this.stopped) {
						logger.info("Server stopped on " + this.serverSocket.getLocalPort());
						break;
					}
				}
			}
		}

		private void shutDown() {
			try {
				this.stopped = true;
				this.serverSocket.close();
				this.executor.shutdownNow();
			}
			catch (IOException e) {
			}
		}
	}


	@SpringBootApplication
	@Import(TcpConsumerConfiguration.class)
	public static class TcpSinkTestApplication {
	}
}
