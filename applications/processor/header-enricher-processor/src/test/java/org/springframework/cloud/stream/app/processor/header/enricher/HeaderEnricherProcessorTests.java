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

package org.springframework.cloud.stream.app.processor.header.enricher;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.header.enricher.HeaderEnricherFunctionConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.test.matcher.HeaderMatcher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author Christian Tzolov
 * @author Soby Chacko
 */
public class HeaderEnricherProcessorTests {

	@Test
	public void testHeaderEnricherProcessor() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(HeaderEnricherProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=headerEnricherFunction",
						"--header.enricher.headers=foo='bar' \n baz='fiz' \n buz=payload \n jaz=@value",
						"--header.enricher.overwrite=true")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			final Message<?> message = MessageBuilder.withPayload("hello")
					.setHeader("baz", "qux").build();
			processorInput.send(message);
			Message<byte[]> enriched = processorOutput.receive(10000);

			MatcherAssert.assertThat(enriched, HeaderMatcher.hasHeader("foo", equalTo("bar")));
			MatcherAssert.assertThat(enriched, HeaderMatcher.hasHeader("baz", equalTo("fiz")));
			MatcherAssert.assertThat(enriched, HeaderMatcher.hasHeader("buz", equalTo("hello")));
			MatcherAssert.assertThat(enriched, HeaderMatcher.hasHeader("jaz", equalTo("beanValue")));
		}
	}

	@SpringBootApplication
	@Import({HeaderEnricherFunctionConfiguration.class})
	public static class HeaderEnricherProcessorTestApplication {

		@Bean
		public String value() {
			return "beanValue";
		}
	}

}
