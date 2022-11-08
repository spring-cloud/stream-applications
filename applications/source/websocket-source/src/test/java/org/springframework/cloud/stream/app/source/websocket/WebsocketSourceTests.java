/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.cloud.stream.app.source.websocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.fn.supplier.websocket.WebsocketSupplierConfiguration;
import org.springframework.cloud.fn.supplier.websocket.WebsocketSupplierProperties;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.integration.websocket.ClientWebSocketContainer;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.Base64Utils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "websocket.supplier.path=some_websocket_path")
@DirtiesContext
public class WebsocketSourceTests {

	@Autowired
	private OutputDestination output;

	@Autowired
	private Supplier<Flux<Message<?>>> websocketSupplier;

	@LocalServerPort
	private int port;

	@Autowired
	private WebsocketSupplierProperties properties;

	@Autowired
	private SecurityProperties securityProperties;

	@Test
	public void testWebsocketSource() throws IOException {
		StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
		ClientWebSocketContainer clientWebSocketContainer =
				new ClientWebSocketContainer(webSocketClient, "ws://localhost:{port}/{path}",
						this.port,
						this.properties.getPath());

		HttpHeaders httpHeaders = new HttpHeaders();
		String token = Base64Utils.encodeToString(
				(this.securityProperties.getUser().getName() + ":" + this.securityProperties.getUser().getPassword())
						.getBytes(StandardCharsets.UTF_8));
		httpHeaders.set(HttpHeaders.AUTHORIZATION, "Basic " + token);
		clientWebSocketContainer.setHeaders(httpHeaders);
		clientWebSocketContainer.start();
		WebSocketSession session = clientWebSocketContainer.getSession(null);
		session.sendMessage(new TextMessage("foo"));
		session.close();

		Message<byte[]> sourceMessage = output.receive(10000, "websocketSupplier-out-0");
		final String actual = new String(sourceMessage.getPayload());
		assertThat(actual).isEqualTo("foo");

		clientWebSocketContainer.stop();
		clientWebSocketContainer.destroy();
	}

	@SpringBootApplication
	@Import({TestChannelBinderConfiguration.class, WebsocketSupplierConfiguration.class})
	public static class SampleConfiguration {

	}
}
