/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.router;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class RouterSinkIntegrationTests {

	@Test
	public void testDefaultRouter() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(RouterSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=routerSinkConsumer",
						"--spring.cloud.stream.output-bindings=baz",
						"--router.resolutionRequired=true")) {

			InputDestination processorInput = context.getBean(InputDestination.class);

			Message<?> message = MessageBuilder.withPayload("hello").setHeader("routeTo", "baz").build();
			processorInput.send(message);

			OutputDestination processorOutput = context.getBean(OutputDestination.class);
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("hello");
		}
	}

	@Test
	public void testDefaultRouterWithByteArrayPayload() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(RouterSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=routerSinkConsumer")) {

			InputDestination processorInput = context.getBean(InputDestination.class);

			Message<?> message = MessageBuilder.withPayload("hello".getBytes()).setHeader("routeTo", "qux").build();
			processorInput.send(message);

			OutputDestination processorOutput = context.getBean(OutputDestination.class);
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("hello");
		}
	}

	@Test
	public void testRouterWithExpression() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(RouterSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=routerSinkConsumer",
						"--router.expression=headers['route']")) {

			InputDestination processorInput = context.getBean(InputDestination.class);

			Message<?> message = MessageBuilder.withPayload("foo")
					.setHeader("route", "foo").build();
			processorInput.send(message);

			OutputDestination processorOutput = context.getBean(OutputDestination.class);
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("foo");
		}
	}

	@Test
	public void testRouterWithChannelMappings() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(RouterSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=routerSinkConsumer",
						"--router.expression=headers['route']",
						"--router.destinationMappings=foo=baz \n bar=qux")) {

			InputDestination processorInput = context.getBean(InputDestination.class);

			Message<?> message = MessageBuilder.withPayload("foo")
					.setHeader("route", "foo").build();
			processorInput.send(message);

			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			Message<byte[]> sourceMessage = processorOutput.receive(10000, "baz");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("foo");

			message = MessageBuilder.withPayload("bar")
					.setHeader("route", "bar").build();
			processorInput.send(message);

			sourceMessage = processorOutput.receive(10000, "qux");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("bar");
		}
	}

	@Test
	public void testWithDiscardedChannels() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(RouterSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=routerSinkConsumer",
						"--router.expression=headers['route']",
						"--router.defaultOutputBinding=discards",
						"--router.destinationMappings=foo=foo \n bar=bar",
						"--spring.cloud.stream.output-bindings=foo;bar;discards")) {

			InputDestination processorInput = context.getBean(InputDestination.class);

			Message<?> message = MessageBuilder.withPayload("foo")
					.setHeader("route", "foo").build();
			processorInput.send(message);

			message = MessageBuilder.withPayload("bar")
					.setHeader("route", "bar").build();
			processorInput.send(message);

			message = MessageBuilder.withPayload("hello")
					.setHeader("route", "baz").build();
			processorInput.send(message);

			OutputDestination processorOutput = context.getBean(OutputDestination.class);
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "foo");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("foo");

			sourceMessage = processorOutput.receive(10000, "bar");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("bar");

			sourceMessage = processorOutput.receive(10000, "discards");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("hello");

			BindingService bindingService = context.getBean(BindingService.class);
			assertThat(bindingService.getProducerBindingNames()).containsExactlyInAnyOrder("bar", "foo", "discards");
		}
	}

	@Test
	public void testWithGroovyScript() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(RouterSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=routerSinkConsumer",
						"--router.script=classpath:/routertest.groovy",
						"--router.variables=foo=baz",
						"--router.variablesLocation=classpath:/routertest.properties")) {

			InputDestination processorInput = context.getBean(InputDestination.class);

			Message<?> message = MessageBuilder.withPayload("foo")
					.setHeader("route", "foo").build();
			processorInput.send(message);

			OutputDestination processorOutput = context.getBean(OutputDestination.class);
			Message<byte[]> sourceMessage = processorOutput.receive(10000, "baz");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("foo");


			message = MessageBuilder.withPayload("bar")
					.setHeader("route", "bar").build();
			processorInput.send(message);

			sourceMessage = processorOutput.receive(10000, "qux");
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("bar");
		}
	}

	@SpringBootApplication
	public static class RouterSinkTestApplication {

	}

}
