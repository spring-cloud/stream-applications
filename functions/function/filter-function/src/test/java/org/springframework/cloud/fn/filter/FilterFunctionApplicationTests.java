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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author David Turanski
 * @author Corneil du Plessis
 */
@SpringBootTest(properties = "filter.function.expression=payload.length() > 5")
@DirtiesContext
public class FilterFunctionApplicationTests {

	@Autowired
	@Qualifier("filterFunction")
	Function<Message<?>, Message<?>> filter;

	@Test
	public void testFilter() {
		// given
		GenericMessage<?> message1 = new GenericMessage<>("hello");
		GenericMessage<?> message2 = new GenericMessage<>("hello world");
		// when
		Message<?> result1 = this.filter.apply(message1);
		// then: filter 'hello' not longer than 5
		assertThat(result1).isNull();
		// when
		Message<?> result2 = this.filter.apply(message2);
		// then: pass 'hello world' longer 5
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload()).isNotNull();
		assertThat(result2.getPayload()).isEqualTo("hello world");

	}

	@SpringBootApplication
	static class FilterFunctionTestApplication {

	}

}
