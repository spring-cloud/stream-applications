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

package org.springframework.cloud.fn.consumer.rsocket;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Controller;

@SpringBootTest(properties = {"spring.rsocket.server.port=7000", "rsocket.consumer.route=test-route"})
public class RsocketConsumerTests {

	@Autowired
	Function<Message<?>, Mono<Void>> rsocketConsumer;

	@Autowired
	TestController controller;

	@Test
	void testRsocketConsumer() {

		rsocketConsumer.apply(new GenericMessage<>("Hello RSocket"))
				.subscribe();

		StepVerifier.create(this.controller.fireForgetPayloads)
				.expectNext("Hello RSocket")
				.thenCancel()
				.verify();
	}

	@SpringBootApplication
	@ComponentScan
	static class RSocketConsumerTestApplication {

	}

	@Controller
	static class TestController {
		final ReplayProcessor<String> fireForgetPayloads = ReplayProcessor.create();

		@MessageMapping("test-route")
		void someMethod(String payload) {
			this.fireForgetPayloads.onNext(payload);
		}
	}
}


