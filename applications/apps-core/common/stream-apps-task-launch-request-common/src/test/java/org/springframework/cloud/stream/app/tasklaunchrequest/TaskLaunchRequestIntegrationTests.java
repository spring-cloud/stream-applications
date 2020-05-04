/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.tasklaunchrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.app.tasklaunchrequest.support.CommandLineArgumentsMessageMapper;
import org.springframework.cloud.stream.app.tasklaunchrequest.support.TaskNameMessageMapper;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollectorAutoConfiguration;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author David Turanski
 **/
public class TaskLaunchRequestIntegrationTests {

	private ApplicationContextRunner applicationContextRunner;

	@Before
	public void setUp() {
		applicationContextRunner =
				new ApplicationContextRunner().withUserConfiguration(TestChannelBinderConfiguration.class, TestApp.class);
	}

	@Test
	public void noTaskLaunchRequestPropertiesAreRequired() {

		applicationContextRunner.withPropertyValues("spring.jmx.enabled=false")
				.run(context -> {
					MessageChannel input = context.getBean("input", MessageChannel.class);

					OutputDestination target = context.getBean(OutputDestination.class);

					Message<byte[]> message =
							MessageBuilder.withPayload("hello".getBytes()).build();
					input.send(message);

					Message<byte[]> response = target.receive(1000);
					assertThat(response.getPayload()).isEqualTo(message.getPayload());
				});
	}

	@Test
	public void simpleDataflowTaskLaunchRequest() {

		applicationContextRunner.withPropertyValues(
				"spring.jmx.enabled=false",
				"spring.cloud.stream.function.definition=taskLaunchRequest",
				"task.launch.request.task-name=foo")
				.run(context -> {
					DataFlowTaskLaunchRequest dataFlowTaskLaunchRequest = verifyAndreceiveDataFlowTaskLaunchRequest(context);

					assertThat(dataFlowTaskLaunchRequest.getTaskName()).isEqualTo("foo");
					assertThat(dataFlowTaskLaunchRequest.getCommandlineArguments()).hasSize(0);
					assertThat(dataFlowTaskLaunchRequest.getDeploymentProperties()).hasSize(0);
				});
	}

	@Test
	public void dataflowTaskLaunchRequestWithArgsAndDeploymentProperties() {

		applicationContextRunner.withPropertyValues(
				"spring.jmx.enabled=false", "spring.cloud.stream.function.definition=taskLaunchRequest",
				"task.launch.request.task-name=foo", "task.launch.request.args=foo=bar,baz=boo",
				"task.launch.request.deploymentProperties=count=3")
				.run(context -> {
					DataFlowTaskLaunchRequest dataFlowTaskLaunchRequest = verifyAndreceiveDataFlowTaskLaunchRequest(context);

					assertThat(dataFlowTaskLaunchRequest.getTaskName()).isEqualTo("foo");
					assertThat(dataFlowTaskLaunchRequest.getCommandlineArguments()).containsExactlyInAnyOrder("foo=bar",
							"baz=boo");
					assertThat(dataFlowTaskLaunchRequest.getDeploymentProperties()).containsOnly(entry("count", "3"));
				});
	}

	@Test
	public void dataflowTaskLaunchRequestWithCommandLineArgsMessageMapper() {

		applicationContextRunner.withPropertyValues(
				"spring.jmx.enabled=false", "spring.cloud.stream.function.definition=taskLaunchRequest",
				"task.launch.request.task-name=foo", "enhanceTLRArgs=true")
				.run(context -> {

					DataFlowTaskLaunchRequest dataFlowTaskLaunchRequest = verifyAndreceiveDataFlowTaskLaunchRequest(context);

					assertThat(dataFlowTaskLaunchRequest.getTaskName()).isEqualTo("foo");
					assertThat(dataFlowTaskLaunchRequest.getCommandlineArguments()).hasSize(1);
					assertThat(dataFlowTaskLaunchRequest.getCommandlineArguments()).containsExactly("runtimeArg");
				});
	}

	@Test
	public void taskLaunchRequestWithArgExpressions() {
		applicationContextRunner.withPropertyValues(
				"spring.jmx.enabled=false",
				"spring.cloud.stream.function.definition=taskLaunchRequest",
				"task.launch.request.task-name=foo",
				"task.launch.request.arg-expressions=foo=payload.toUpperCase(),bar=payload.substring(0,2)")
				.run(context -> {

					MessageChannel input = context.getBean("input", MessageChannel.class);

					OutputDestination target = context.getBean(OutputDestination.class);

					Message<String> message = MessageBuilder.withPayload("hello").build();

					input.send(message);

					ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

					Message<byte[]> response = target.receive(1000);

					assertThat(response).isNotNull();

					DataFlowTaskLaunchRequest request = objectMapper.readValue(response.getPayload(),
							DataFlowTaskLaunchRequest.class);

					assertThat(request.getCommandlineArguments()).containsExactlyInAnyOrder("foo=HELLO", "bar=he");

				});
	}

	@Test
	public void taskLaunchRequestWithIntPayload() {
		applicationContextRunner.withPropertyValues(
				"spring.jmx.enabled=false", "spring.cloud.stream.function.definition=taskLaunchRequest",
				"task.launch.request.task-name=foo",
				"task.launch.request.arg-expressions=i=payload")
				.run(context -> {

					ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

					MessageChannel input = context.getBean("input", MessageChannel.class);

					OutputDestination target = context.getBean(OutputDestination.class);

					Message<Integer> message =
							MessageBuilder.withPayload(123).build();

					input.send(message);

					Message<byte[]> response = target.receive(1000);

					assertThat(response).isNotNull();

					DataFlowTaskLaunchRequest request = objectMapper.readValue(response.getPayload(),
							DataFlowTaskLaunchRequest.class);

					assertThat(request.getCommandlineArguments()).containsExactly("i=123");

				});
	}

	@Test
	public void taskNameExpression() {
		applicationContextRunner.withPropertyValues(
				"spring.jmx.enabled=false", "spring.cloud.stream.function.definition=taskLaunchRequest",
				"task.launch.request.task-name-expression=payload+'_task'")
				.run(context -> {
					MessageChannel input = context.getBean("input", MessageChannel.class);

					OutputDestination target = context.getBean(OutputDestination.class);

					ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

					Message<String> message = MessageBuilder.withPayload("foo").build();
					input.send(message);

					Message<byte[]> response = target.receive(1000);
					assertThat(response).isNotNull();

					DataFlowTaskLaunchRequest request = objectMapper.readValue(response.getPayload(),
							DataFlowTaskLaunchRequest.class);

					assertThat(request.getTaskName()).isEqualTo("foo_task");
				});
	}

	@Test
	public void customTaskNameExtractor() {
		applicationContextRunner.withPropertyValues(
				"spring.jmx.enabled=false", "spring.cloud.stream.function.definition=taskLaunchRequest",
				"customTaskNameExtractor=true")
				.run(context -> {
                    MessageChannel input = context.getBean("input", MessageChannel.class);

                    OutputDestination target = context.getBean(OutputDestination.class);

                    ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

                    Message<String> message = MessageBuilder.withPayload("foo").build();
                    input.send(message);

                    Message<byte[]> response = target.receive(1000);
                    assertThat(response).isNotNull();

                    DataFlowTaskLaunchRequest request = objectMapper.readValue(response.getPayload(),
                            DataFlowTaskLaunchRequest.class);

                    assertThat(request.getTaskName()).isEqualTo("fooTask");
                        });
        //TODO: Workaround for https://github.com/spring-cloud/spring-cloud-stream/issues/1876
        applicationContextRunner.withPropertyValues(
                "spring.jmx.enabled=false", "spring.cloud.stream.function.definition=taskLaunchRequest",
                "customTaskNameExtractor=true")
                .run(context -> {
                    MessageChannel input = context.getBean("input", MessageChannel.class);

                    OutputDestination target = context.getBean(OutputDestination.class);

                    ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

                    Message<String> message = MessageBuilder.withPayload("bar").build();
                    input.send(message);

                    Message<byte[]> response = target.receive(1000);
                    assertThat(response).isNotNull();

                    DataFlowTaskLaunchRequest request = objectMapper.readValue(response.getPayload(),
                            DataFlowTaskLaunchRequest.class);

                    assertThat(request.getTaskName()).isEqualTo("defaultTask");
                });
	}

	private DataFlowTaskLaunchRequest verifyAndreceiveDataFlowTaskLaunchRequest(ApplicationContext context)
		throws IOException {
		MessageChannel input = context.getBean("input", MessageChannel.class);

		OutputDestination target = context.getBean(OutputDestination.class);

		MessageBuilder<byte[]> builder = MessageBuilder.withPayload(new byte[] {});

		ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

		input.send(builder.build());

		Message<byte[]> message = target.receive(1000);

		assertThat(message).isNotNull();

		return objectMapper.readValue(message.getPayload(),
			DataFlowTaskLaunchRequest.class);
	}

	@EnableAutoConfiguration(exclude = { TestSupportBinderAutoConfiguration.class,
		MessageCollectorAutoConfiguration.class })
	@EnableBinding(Processor.class)
	static class TestApp {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		@ConditionalOnProperty("customTaskNameExtractor")
		TaskNameMessageMapper taskNameExtractor() {
				return message -> ((String)(message.getPayload())).equalsIgnoreCase("foo") ?
						"fooTask" :
						"defaultTask";
		}

		@Bean
		@ConditionalOnProperty("enhanceTLRArgs")
		CommandLineArgumentsMessageMapper commandLineArgumentsProvider(){
			return message -> Collections.singletonList("runtimeArg");
		}

		@Bean
		public IntegrationFlow flow() {

			return IntegrationFlows.from(Processor.INPUT)
				.channel(Processor.OUTPUT).get();
		}
	}
}
