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

package org.springframework.cloud.stream.app.composite.function.common;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionBindingsEnvironmentPostProcessorTests {

	@Test
	void destinationBindings() {
		ApplicationContext context = new SpringApplication(TestApp.class).run(
				"--spring.cloud.stream.bindings.output.destination=foo",
				"--spring.cloud.stream.bindings.input.destination=bar",
				"--spring.cloud.function.definition=firstFunction|secondFunction");
		assertThat(context.getEnvironment().getProperty("spring.cloud.stream.function.bindings.firstFunctionsecondFunction-out-0"))
				.isEqualTo("output");
		assertThat(context.getEnvironment().getProperty("spring.cloud.stream.function.bindings.firstFunctionsecondFunction-in-0"))
				.isEqualTo("input");
		assertThat(context.getEnvironment().getProperty("spring.cloud.function.definition")).isEqualTo("firstFunction|secondFunction");
	}

	@Test
	void destinationBindingsWithSingleQuotes() {
		ApplicationContext context = new SpringApplication(TestApp.class).run(
				"--spring.cloud.stream.bindings.output.destination=foo",
				"--spring.cloud.stream.bindings.input.destination=bar",
				"--spring.cloud.function.definition='firstFunction|secondFunction'");
		assertThat(context.getEnvironment().getProperty("spring.cloud.stream.function.bindings.firstFunctionsecondFunction-out-0"))
				.isEqualTo("output");
		assertThat(context.getEnvironment().getProperty("spring.cloud.stream.function.bindings.firstFunctionsecondFunction-in-0"))
				.isEqualTo("input");
		assertThat(context.getEnvironment().getProperty("spring.cloud.function.definition")).isEqualTo("firstFunction|secondFunction");
	}

	@Test
	void destinationBindingsWithCommaDelimiter() {
		ApplicationContext context = new SpringApplication(TestApp.class).run(
				"--spring.cloud.stream.bindings.output.destination=foo",
				"--spring.cloud.stream.bindings.input.destination=bar",
				"--spring.cloud.function.definition=firstFunction,secondFunction");
		assertThat(context.getEnvironment().getProperty("spring.cloud.stream.function.bindings.firstFunctionsecondFunction-out-0"))
				.isEqualTo("output");
		assertThat(context.getEnvironment().getProperty("spring.cloud.stream.function.bindings.firstFunctionsecondFunction-in-0"))
				.isEqualTo("input");
		assertThat(context.getEnvironment().getProperty("spring.cloud.function.definition")).isEqualTo("firstFunction|secondFunction");
	}

	@SpringBootApplication
	static class TestApp {

	}
}
