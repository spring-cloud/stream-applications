/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.cloud.fn.header.filter;

import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
	HeaderFilterFunctionApplicationTests.HeaderFilterFunctionTestApplication.class,
	HeaderFilterFunctionConfiguration.class
},
	properties = {"header.filter.remove=foo,bar,pf-*"}
)
public class HeaderFilterFunctionApplicationTests {
	@Autowired
	protected Function<Message<?>, Message<?>> headerFilter;

	@Test
	public void testRemoveAll() {
		// given
		final Message<?> message = MessageBuilder.withPayload("hello")
			.setHeader("foo", "bar")
			.setHeader("bar", "foo")
			.build();
		Message<?> result = headerFilter.apply(message);
		var headers = HeaderUtils.getNonReadOnlyHeaders(result);
		assertThat(headers).isEmpty();
	}

	@Test
	public void testRemoveSome() {
		// given
		final Message<?> message = MessageBuilder.withPayload("hello")
			.setHeader("foo", "bar")
			.setHeader("foo-bar", "bar")
			.setHeader("bar", "foo")
			.build();
		Message<?> result = headerFilter.apply(message);
		var headers = HeaderUtils.getNonReadOnlyHeaders(result);
		assertThat(headers).isEqualTo(Set.of("foo-bar"));
	}

	@Test
	public void testRemoveSomeWithWildcard() {
		// given
		final Message<?> message = MessageBuilder.withPayload("hello")
			.setHeader("foo", "bar")
			.setHeader("pf-foo", "bar")
			.setHeader("pf-bar", "bar")
			.setHeader("pfBar", "bar")
			.setHeader("bar", "foo")
			.build();
		Message<?> result = headerFilter.apply(message);
		var headers = HeaderUtils.getNonReadOnlyHeaders(result);
		assertThat(headers).isEqualTo(Set.of("pfBar"));
	}

	@Test
	public void testRemoveLeavesIdTimestampAll() {
		// given
		final Message<?> message = MessageBuilder.withPayload("hello")
			.setHeader("foo", "bar")
			.setHeader("bar", "foo")
			.build();
		Message<?> result = headerFilter.apply(message);

		assertThat(result.getHeaders().keySet()).isEqualTo(Set.of("id", "timestamp"));
	}

	@SpringBootApplication
	static class HeaderFilterFunctionTestApplication {
		public static void main(String[] main) {

		}
	}

}
