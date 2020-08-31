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

package org.springframework.cloud.fn.filter;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;

/**
 * @author Artem Bilan
 * @author David Turanski
 */
@SpringBootTest(properties = "filter.function.expression=payload.length() > 5")
@DirtiesContext
public class FilterFunctionApplicationTests {

	@Autowired
	@Qualifier("filterFunction")
	Function<Flux<Message<?>>, Flux<Message<?>>> filter;

	@Test
	public void testFilter() {
		Flux<Message<?>> messageFlux =
				Flux.just("hello", "hello world")
						.map(GenericMessage::new);
		Flux<Message<?>> result = this.filter.apply(messageFlux);
		result
				.map(Message::getPayload)
				.cast(String.class)
				.as(StepVerifier::create)
				.expectNext("hello world")
				.expectComplete()
				.verify();
	}

	@SpringBootApplication
	static class FilterFunctionTestApplication {

	}

}
