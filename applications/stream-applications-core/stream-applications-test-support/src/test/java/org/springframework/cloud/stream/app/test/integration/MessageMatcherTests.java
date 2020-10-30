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

package org.springframework.cloud.stream.app.test.integration;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageMatcherTests {

	@Test
	void testEquality() {

		Predicate<Message<?>> p1 = m -> m.getPayload().equals("hello");
		Predicate<Message<?>> p2 = m -> m.getPayload().equals("hello");
		MessageMatcher v1 = new MessageMatcher(p1);
		MessageMatcher v2 = new MessageMatcher(p1);

		assertThat(v1).isEqualTo(v2);
		assertThat(v1.getPredicate()).isEqualTo(p1);
	}

	@Test
	void testVerification() {
		Predicate<Message<?>> p = m -> m.getPayload().equals("hello");
		MessageMatcher v = new MessageMatcher(p);
		assertThat(v.test(MessageBuilder.withPayload("hello").build())).isTrue();
		assertThat(v.isSatisfied()).isTrue();
	}

	@Test
	void testWrapper() {
		Predicate<String> p = s -> s.equals("hello");
		MessageMatcher v = MessageMatcher.payloadMatcher(p);
		assertThat(v.getPredicate()).isEqualTo(p);
		assertThat(v.test(MessageBuilder.withPayload("hello").build())).isTrue();
		assertThat(v.isSatisfied()).isTrue();
	}
}
