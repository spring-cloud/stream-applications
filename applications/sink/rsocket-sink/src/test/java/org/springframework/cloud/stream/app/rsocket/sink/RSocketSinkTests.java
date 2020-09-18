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

package org.springframework.cloud.stream.app.rsocket.sink;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.ReplayProcessor;
import reactor.test.StepVerifier;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.consumer.rsocket.RsocketConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

public class RSocketSinkTests {

	@Test
	public void testRsocketSink() throws Exception {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(RsocketSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.rsocket.server.port=7000",
						"--rsocket.consumer.route=test-route",
						"--spring.cloud.function.definition=rsocketConsumer")) {

			TestController controller = context.getBean(TestController.class);
			final StepVerifier stepVerifier = StepVerifier.create(controller.fireForgetPayloads)
					.expectNext("Hello RSocket")
					.thenCancel()
					.verifyLater();

			final Message<String> message = MessageBuilder.withPayload("Hello RSocket").build();
			InputDestination source = context.getBean(InputDestination.class);
			source.send(message);

			stepVerifier.verify();
		}
	}

	@Controller
	static class TestController {
		final ReplayProcessor<String> fireForgetPayloads = ReplayProcessor.create();

		@MessageMapping("test-route")
		void someMethod(String payload) {
			this.fireForgetPayloads.onNext(payload);
		}
	}

	@SpringBootApplication
	@ComponentScan
	@Import(RsocketConsumerConfiguration.class)
	public static class RsocketSinkTestApplication {
	}
}
