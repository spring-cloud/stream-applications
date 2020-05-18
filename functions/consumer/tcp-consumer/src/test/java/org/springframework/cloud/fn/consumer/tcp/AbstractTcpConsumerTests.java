/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ServerSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TCP Consumer.
 *
 * @author Gary Russell
 * @author Soby Chacko
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = { "tcp.consumer.host = localhost", "tcp.port = ${tcp.consumer.test.port}" })
@DirtiesContext
public class AbstractTcpConsumerTests {

	private static TestTCPServer server;

	@Autowired
	protected AbstractClientConnectionFactory connectionFactory;

	@Autowired
	Consumer<Message<?>> tcpConsumer;

	@BeforeAll
	public static void startup() {
		server = new TestTCPServer();
	}

	@AfterAll
	public static void shutDown() {
		server.shutDown();
	}


	/*
	 * Sends two messages and asserts they arrive as expected on the other side using
	 * the supplied decoder.
	 */
	protected void doTest(AbstractByteArraySerializer decoder) throws Exception {
		server.setDecoder(decoder);
		Message<String> message = new GenericMessage<>("foo");
		tcpConsumer.accept(message);
		String received = server.queue.poll(10, TimeUnit.SECONDS);
		assertThat(received).isEqualTo("foo");

		tcpConsumer.accept(message);
		received = server.queue.poll(10, TimeUnit.SECONDS);
		assertThat(received).isEqualTo("foo");
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
	public static class TcpConsumerTestApplication {

	}
}
