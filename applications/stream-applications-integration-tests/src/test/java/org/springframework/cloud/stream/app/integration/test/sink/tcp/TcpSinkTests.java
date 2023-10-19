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

package org.springframework.cloud.stream.app.integration.test.sink.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.wait.strategy.Wait;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils;
import org.springframework.cloud.stream.app.test.integration.TestTopicSender;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.BaseContainerExtension;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;
@Tag("integration")
@ExtendWith(BaseContainerExtension.class)
abstract class TcpSinkTests {

	private static int tcpPort;

	private static Socket socket;

	private static final AtomicBoolean socketReady = new AtomicBoolean();

	private static StreamAppContainer sink;

	@Autowired
	private TestTopicSender testTopicSender;

	@BeforeAll
	static void configureSink() {
		tcpPort = StreamAppContainerTestUtils.findAvailablePort();
		startTcpServer();
		sink = BaseContainerExtension.containerInstance()
				.withEnv("TCP_CONSUMER_HOST", StreamAppContainerTestUtils.localHostAddress())
				.withEnv("TCP_PORT", String.valueOf(tcpPort))
				.withEnv("TCP_CONSUMER_ENCODER", "CRLF")
				.waitingFor(Wait.forLogMessage(".*Started TcpSink.*", 1));
		sink.start();
	}

	@SuppressWarnings("resource")
	static void startTcpServer() {
		socketReady.set(false);
		new Thread(() -> {
			try {
				socket = new ServerSocket(tcpPort, 16, InetAddress.getLocalHost())
					.accept();
				socketReady.set(true);
			}
			catch (IOException e) {
				throw new RuntimeException("failed to bind to port " + tcpPort + ": " + e.getMessage(), e);
			}
		}).start();
	}

	@Test
	void postData() throws IOException {
		// Sink will not connect until it receives a message.
		String text = "Hello, world!";
		testTopicSender.send(sink.getInputDestination(), text);

		await().atMost(DEFAULT_DURATION).untilTrue(socketReady);
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		await().atMost(Duration.ofSeconds(10)).until(() -> reader.readLine().equals(text));
	}

	@AfterAll
	static void cleanUp() throws IOException {
		sink.stop();
		socket.close();
	}
}
