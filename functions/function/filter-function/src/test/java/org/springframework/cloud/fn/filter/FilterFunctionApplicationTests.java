/*
 * Copyright (c) 2011-2020 Pivotal Software Inc, All Rights Reserved.
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

package org.springframework.cloud.fn.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = "spel.function.expression=payload.length() > 5")
@DirtiesContext
public class FilterFunctionApplicationTests {

	@Autowired
	@Qualifier("filterFunction")
	Function<Message<?>, Message<?>> filter;

	@Test
	public void testFilter() {
		Message<?> filtered = this.filter.apply(new GenericMessage<>("hello"));
		assertThat(filtered).isNull();
		filtered = this.filter.apply(new GenericMessage<>("hello world"));
		assertThat(filtered).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("hello world");
	}

	@SpringBootApplication
	static class TestApplication {}
}
