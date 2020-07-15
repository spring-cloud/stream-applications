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

package org.springframework.cloud.fn.task.launch.request;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author David Turanski
 **/
public class TaskLaunchRequestFunctionApplicationTests {

	private SpringApplicationBuilder springApplicationBuilder;

	@BeforeEach
	public void setUp() {
		springApplicationBuilder = new SpringApplicationBuilder(TaskLaunchRequestFunctionTestApplication.class)
				.web(WebApplicationType.NONE);
	}

	@Test
	@DirtiesContext
	public void simpleDataflowTaskLaunchRequest() throws IOException {

		ApplicationContext context = springApplicationBuilder.properties(
				"spring.jmx.enabled=false",
				"spring.cloud.function.definition=taskLaunchRequestFunction",
				"task.launch.request.task-name=foo")
				.run();

		TaskLaunchRequest taskLaunchRequest = verifyAndreceiveTaskLaunchRequest(context);

		assertThat(taskLaunchRequest.getTaskName()).isEqualTo("foo");
		assertThat(taskLaunchRequest.getCommandlineArguments()).hasSize(0);
		assertThat(taskLaunchRequest.getDeploymentProperties()).hasSize(0);
	}

	@Test
	@DirtiesContext
	public void dataflowTaskLaunchRequestWithArgsAndDeploymentProperties() throws IOException {

		ApplicationContext context = springApplicationBuilder.properties(
				"spring.jmx.enabled=false", "spring.cloud.function.definition=taskLaunchRequestFunction",
				"task.launch.request.task-name=foo", "task.launch.request.args=foo=bar,baz=boo",
				"task.launch.request.deploymentProperties=count=3")
				.run();
		TaskLaunchRequest taskLaunchRequest = verifyAndreceiveTaskLaunchRequest(context);

		assertThat(taskLaunchRequest.getTaskName()).isEqualTo("foo");
		assertThat(taskLaunchRequest.getCommandlineArguments()).containsExactlyInAnyOrder("foo=bar",
				"baz=boo");
		assertThat(taskLaunchRequest.getDeploymentProperties()).containsOnly(entry("count", "3"));
	}

	@Test
	@DirtiesContext
	public void taskLaunchRequestWithCommandLineArgsMessageMapper() throws IOException {

		ApplicationContext context = springApplicationBuilder.properties(
				"spring.jmx.enabled=false", "spring.cloud.function.definition=taskLaunchRequestFunction",
				"task.launch.request.task-name=foo", "enhanceTLRArgs=true")
				.run();

		TaskLaunchRequest taskLaunchRequest = verifyAndreceiveTaskLaunchRequest(context);

		assertThat(taskLaunchRequest.getTaskName()).isEqualTo("foo");
		assertThat(taskLaunchRequest.getCommandlineArguments()).hasSize(1);
		assertThat(taskLaunchRequest.getCommandlineArguments()).containsExactly("runtimeArg");

	}

	@Test
	@DirtiesContext
	public void taskLaunchRequestWithArgExpressions() throws IOException {
		ApplicationContext context = springApplicationBuilder.properties(
				"spring.jmx.enabled=false",
				"spring.cloud.function.definition=taskLaunchRequestFunction",
				"task.launch.request.task-name=foo",
				"task.launch.request.arg-expressions=foo=payload.toUpperCase(),bar=payload.substring(0,2)")
				.run();

		Message<String> message = MessageBuilder.withPayload("hello").build();

		TaskLaunchRequestFunction taskLaunchRequestFunction = context.getBean(TaskLaunchRequestFunction.class);

		Message<TaskLaunchRequest> response = taskLaunchRequestFunction.apply(message);

		assertThat(response).isNotNull();
		TaskLaunchRequest request = response.getPayload();
		assertThat(request.getCommandlineArguments()).containsExactlyInAnyOrder("foo=HELLO", "bar=he");
	}

	@Test
	@DirtiesContext
	public void taskLaunchRequestWithIntPayload() throws IOException {
		ApplicationContext context = springApplicationBuilder.properties(
				"spring.jmx.enabled=false", "spring.cloud.function.definition=taskLaunchRequestFunction",
				"task.launch.request.task-name=foo",
				"task.launch.request.arg-expressions=i=payload")
				.run();

		TaskLaunchRequestFunction taskLaunchRequestFunction = context.getBean(TaskLaunchRequestFunction.class);

		Message<Integer> message = MessageBuilder.withPayload(123).build();

		Message<TaskLaunchRequest> response = taskLaunchRequestFunction.apply(message);

		assertThat(response).isNotNull();

		TaskLaunchRequest request = response.getPayload();
		assertThat(request.getCommandlineArguments()).containsExactly("i=123");
	}

	@Test
	@DirtiesContext
	public void taskNameExpression() throws IOException {
		ApplicationContext context = springApplicationBuilder.properties(
				"spring.jmx.enabled=false", "spring.cloud.function.definition=taskLaunchRequestFunction",
				"task.launch.request.task-name-expression=payload+'_task'")
				.run();

		TaskLaunchRequestFunction taskLaunchRequestFunction = context.getBean(TaskLaunchRequestFunction.class);

		Message<?> message = MessageBuilder.withPayload("foo").build();

		Message<TaskLaunchRequest> response = taskLaunchRequestFunction.apply(message);
		assertThat(response).isNotNull();

		TaskLaunchRequest request = response.getPayload();
		assertThat(request.getTaskName()).isEqualTo("foo_task");
	}

	@Test
	@DirtiesContext
	public void customTaskNameExtractor() throws IOException {
		ApplicationContext context = springApplicationBuilder.properties(
				"spring.jmx.enabled=false", "spring.cloud.function.definition=taskLaunchRequestFunction",
				"customTaskNameExtractor=true")
				.run();
		TaskLaunchRequestFunction taskLaunchRequestFunction = context.getBean(TaskLaunchRequestFunction.class);

		Message<String> message = MessageBuilder.withPayload("foo").build();

		Message<TaskLaunchRequest> response = taskLaunchRequestFunction.apply(message);
		assertThat(response).isNotNull();

		TaskLaunchRequest request = response.getPayload();
		assertThat(request.getTaskName()).isEqualTo("fooTask");

		message = MessageBuilder.withPayload("bar").build();
		response = taskLaunchRequestFunction.apply(message);
		request = response.getPayload();

		assertThat(request.getTaskName()).isEqualTo("defaultTask");
	}

	private TaskLaunchRequest verifyAndreceiveTaskLaunchRequest(ApplicationContext context)
			throws IOException {
		TaskLaunchRequestFunction taskLaunchRequestFunction = context.getBean(TaskLaunchRequestFunction.class);
		Message<TaskLaunchRequest> message = taskLaunchRequestFunction
				.apply(MessageBuilder.withPayload(new byte[] {}).build());
		assertThat(message).isNotNull();
		return message.getPayload();
	}

	@SpringBootApplication
	protected static class TaskLaunchRequestFunctionTestApplication {

		@Bean
		@ConditionalOnProperty("customTaskNameExtractor")
		TaskNameMessageMapper taskNameExtractor() {
			return message -> ((String) (message.getPayload())).equalsIgnoreCase("foo") ? "fooTask" : "defaultTask";
		}

		@Bean
		@ConditionalOnProperty("enhanceTLRArgs")
		CommandLineArgumentsMessageMapper commandLineArgumentsProvider() {
			return message -> Collections.singletonList("runtimeArg");
		}
	}
}
