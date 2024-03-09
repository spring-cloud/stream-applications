/*
 * Copyright 2020-2024 the original author or authors.
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

package org.springframework.cloud.stream.app.rsocket.sink;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.rsocket.context.RSocketServerBootstrap;
import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.consumer.rsocket.RsocketConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Soby Chacko
 */
@SpringBootTest(properties = {"spring.rsocket.server.port=0"}, classes = RSocketSinkTests.RSocketServerApplication.class)
@DirtiesContext
public class RSocketSinkTests {

	private static ApplicationContextRunner applicationContextRunner;

	@BeforeAll
	static void setup() {
		applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(TestChannelBinderConfiguration.getCompleteConfiguration(RsocketSinkTestApplication.class));
	}

	@Autowired
	ApplicationContext applicationContext;

	@Test
	void testRsocketConsumer() {

		RSocketServerBootstrap serverBootstrap = applicationContext.getBean(RSocketServerBootstrap.class);
		RSocketServer server = (RSocketServer) ReflectionTestUtils.getField(serverBootstrap, "server");
		final int port = server.address().getPort();

		applicationContextRunner.withPropertyValues(
						"spring.cloud.function.definition=rsocketFunctionConsumer",
						"rsocket.consumer.port=" + port,
						"rsocket.consumer.route=test-route")
				.run(context -> {
					final StepVerifier stepVerifier = StepVerifier.create(RSocketServerApplication.fireForgetPayloads.asMono())
							.expectNext("Hello RSocket")
							.thenCancel()
							.verifyLater();

					final Message<String> message = MessageBuilder.withPayload("Hello RSocket").build();
					InputDestination source = context.getBean(InputDestination.class);
					source.send(message);

					stepVerifier.verify(Duration.ofSeconds(10));
				});
	}

	@EnableAutoConfiguration(exclude = RsocketConsumerConfiguration.class)
	@SpringBootConfiguration
	@Controller
	static class RSocketServerApplication {
		static final Sinks.One<String> fireForgetPayloads = Sinks.one();

		@MessageMapping("test-route")
		void someMethod(String payload) {
			this.fireForgetPayloads.tryEmitValue(payload);
		}
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	static class RsocketSinkTestApplication {

	}
}
