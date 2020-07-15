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

package org.springframework.cloud.fn.tasklauncher.test;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientAutoConfiguration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionTests {

	@Test
	void test0() {
		Function<Integer, String> intToStr = String::valueOf;
		Function<String, Integer> doubleit = i -> Integer.parseInt(i) * 2;
		Function<Integer, Integer> composite = intToStr.andThen(doubleit);
		composite.apply(10);
	}

	@Test
	void test1() {
		new ApplicationContextRunner()
				.withUserConfiguration(FunctionApp.class)
				.run(context -> {
					FunctionRegistry functionRegistry = context.getBean(FunctionRegistry.class);
					Function<Integer, Integer> composite = functionRegistry.lookup("convertInt|doubler");
					assertThat(composite.apply(10)).isEqualTo(20);
				});
	}

	@Test
	void test2() {
		new ApplicationContextRunner()
				.withUserConfiguration(FunctionApp.class)
				.run(context -> {
					FunctionRegistry functionRegistry = context.getBean(FunctionRegistry.class);
					Function<Integer, Integer> composite = functionRegistry.lookup("convertIntMessage|doubler");
					assertThat(composite.apply(10)).isEqualTo(20);
				});
	}

	@Test
	void test3() {
		new ApplicationContextRunner()
				.withUserConfiguration(FunctionApp.class)
				.run(context -> {
					FunctionRegistry functionRegistry = context.getBean(FunctionRegistry.class);
					SimpleFunctionRegistry.FunctionInvocationWrapper composite = functionRegistry
							.lookup("personSupplier|personConsumer");
					composite.get();
				});
	}

	@SpringBootApplication(exclude = DataFlowClientAutoConfiguration.class)
	static class FunctionApp {
		@Bean
		Function<Integer, Integer> doubler() {
			return i -> i * 2;
		}

		@Bean
		Function<Integer, String> convertInt() {
			return String::valueOf;
		}

		@Bean
		Function<Integer, Message<String>> convertIntMessage() {
			return i -> MessageBuilder.withPayload(String.valueOf(i)).build();
		}

		@Bean
		Consumer<Person> personConsumer() {
			return person -> System.out.println("Person:" + person.name);
		}

		@Bean
		Supplier<String> personSupplier() {
			return () -> {
				ObjectMapper objectMapper = new ObjectMapper();
				Person person = new Person();
				person.setName("Dave");
				try {
					return objectMapper.writeValueAsString(person);
				}
				catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			};
		}
	}

	static class Person {
		String name;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
