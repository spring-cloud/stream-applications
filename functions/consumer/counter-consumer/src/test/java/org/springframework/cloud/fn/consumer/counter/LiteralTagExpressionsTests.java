/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.consumer.counter;

import java.util.stream.IntStream;

import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@TestPropertySource(properties = {
		"counter.name=counter666",
		"counter.tag.expression.foo='bar'",
		"counter.tag.expression.gork='bork'"
})
public class LiteralTagExpressionsTests extends CounterConsumerParentTest {

	@Test
	void testCounterSink() {

		IntStream.range(0, 13).forEach(i -> counterConsumer.accept(new GenericMessage("hello")));

		Counter fooCounter = meterRegistry.find("counter666").tag("foo", "bar").counter();
		assertThat(fooCounter.count()).isEqualTo(13.0);

		Counter gorkCounter = meterRegistry.find("counter666").tag("gork", "bork").counter();
		assertThat(gorkCounter.count()).isEqualTo(13.0);

		assertThat(fooCounter.getId()).isEqualTo(gorkCounter.getId());
	}
}
