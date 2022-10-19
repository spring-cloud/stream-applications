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

package org.springframework.cloud.fn.header.enricher;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.test.matcher.HeaderMatcher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 *
 * @author Gary Russell
 * @author Soby Chacko
 */
@SpringBootTest(properties = {
		"header.enricher.headers=foo='bar' \\n baz='fiz' \\n buz=payload \\n jaz=@value",
		"header.enricher.overwrite = true" })
@DirtiesContext
public class HeaderEnricherFunctionApplicationTests {

	@Autowired
	Function<Message<?>, Message<?>> headerEnricherFunction;

	@Test
	public void testDefault() {
		final Message<?> message = MessageBuilder.withPayload("hello")
				.setHeader("baz", "qux").build();
		final Message<?> enriched = headerEnricherFunction.apply(message);

		assertThat(enriched, HeaderMatcher.hasHeader("foo", equalTo("bar")));
		assertThat(enriched, HeaderMatcher.hasHeader("baz", equalTo("fiz")));
		assertThat(enriched, HeaderMatcher.hasHeader("buz", equalTo("hello")));
		assertThat(enriched, HeaderMatcher.hasHeader("jaz", equalTo("beanValue")));
	}

	@SpringBootApplication
	static class HeaderEnricherFunctionTestApplication {

		@Bean
		public String value() {
			return "beanValue";
		}

	}

}
