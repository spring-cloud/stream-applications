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

package org.springframework.cloud.stream.app.processor.header.filter;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.header.filter.HeaderFilterFunctionConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author Soby Chacko
 */
public class HeaderFilterProcessorTests {

	@Test
	public void testHeaderFilterProcessor() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(HeaderEnricherProcessorTestApplication.class))
			.web(WebApplicationType.NONE)
			.run("--spring.cloud.function.definition=headerFilterFunction",
				"--header.filter.remove=foo,bar")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			final Message<?> message = MessageBuilder.withPayload("hello")
				.setHeader("bar", "foo")
				.setHeader("foo", "bar")
				.setHeader("foo-bar", "fubar")
				.build();
			processorInput.send(message);
			Message<byte[]> results = processorOutput.receive(10000);
			Set<String> headers = getNonReadOnlyHeaders(results);
			assertThat(headers).isEqualTo(Set.of("foo-bar", "contentType", "target-protocol"));
		}
	}

	@NotNull
	private static Set<String> getNonReadOnlyHeaders(Message<byte[]> message) {
		var headers = new HashSet<>(message.getHeaders().keySet());
		var accessor = new IntegrationMessageHeaderAccessor(message);
		headers.removeIf(accessor::isReadOnly);
		return headers;
	}

	@Test
	public void testHeaderFilterRemoveAllProcessor() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(HeaderEnricherProcessorTestApplication.class))
			.web(WebApplicationType.NONE)
			.run("--spring.cloud.function.definition=headerFilterFunction",
				"--header.filter.delete-all=true")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			final Message<?> message = MessageBuilder.withPayload("hello")
				.setHeader("bar", "foo")
				.setHeader("foo", "bar")
				.setHeader("foo-bar", "fubar")
				.build();
			processorInput.send(message);
			Message<byte[]> result = processorOutput.receive(10000);
			var headers = getNonReadOnlyHeaders(result);
			assertThat(headers).isEqualTo(Set.of("contentType"));
		}
	}

	@SpringBootApplication
	@Import({HeaderFilterFunctionConfiguration.class})
	public static class HeaderEnricherProcessorTestApplication {

		@Bean
		public String value() {
			return "beanValue";
		}
	}

}
