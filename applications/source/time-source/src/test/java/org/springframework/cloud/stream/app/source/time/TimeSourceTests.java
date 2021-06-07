/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.cloud.stream.app.source.time;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.supplier.time.TimeSupplierConfiguration;
import org.springframework.cloud.fn.supplier.time.TimeSupplierProperties;
import org.springframework.cloud.fn.task.launch.request.TaskLaunchRequest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Soby Chacko
 * @author David Turanski
 * @author Artem Bilan
 */
public class TimeSourceTests {

	@Test
	public void testSourceFromSupplier() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(TimeSourceTestApplication.class))
								.web(WebApplicationType.NONE)
								.run("--spring.cloud.function.definition=timeSupplier")) {

			OutputDestination target = context.getBean(OutputDestination.class);
			Message<byte[]> sourceMessage = target.receive(10000, "timeSupplier-out-0");
			final String actual = new String(sourceMessage.getPayload());

			TimeSupplierProperties timeSupplierProperties = context.getBean(TimeSupplierProperties.class);
			SimpleDateFormat dateFormat = new SimpleDateFormat(timeSupplierProperties.getDateFormat());
			assertThatCode(() -> {
				Date date = dateFormat.parse(actual);
				assertThat(date).isNotNull();
			}).doesNotThrowAnyException();
		}
	}

	@Test
	public void testSourceComposedWithHeaderEnricher() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(TimeSourceTestApplication.class))
								.web(WebApplicationType.NONE)
								.run("--spring.cloud.function.definition=timeSupplier|headerEnricherFunction",
										"--header.enricher.headers=seconds=T(java.lang.Integer).valueOf(payload.substring(payload.length() - 2))")) {
			OutputDestination target = context.getBean(OutputDestination.class);
			Message<byte[]> sourceMessage = target.receive(10000, "timeSupplierheaderEnricherFunction-out-0");
			assertThat(((int) sourceMessage.getHeaders().get("seconds"))).isBetween(0, 60);
		}
	}

	@Test
	public void testSourceComposedWithOtherStuff() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(TimeSourceTestApplication.class))
								.web(WebApplicationType.NONE)
								.run("--spring.cloud.function.definition=timeSupplier|spelFunction|headerEnricherFunction|taskLaunchRequestFunction",
										"--spel.function.expression=payload.length()",
										"--header.enricher.headers=task-id=payload*2",
										"--spring.cloud.stream.bindings.output.destination=foo",
										"--task.launch.request.task-name-expression='task-'+headers['task-id']")) {

			OutputDestination target = context.getBean(OutputDestination.class);
			Message<byte[]> sourceMessage = target.receive(10000, "foo");
			TaskLaunchRequest taskLaunchRequest = objectMapper.readValue(sourceMessage.getPayload(),
					TaskLaunchRequest.class);
			assertThat(taskLaunchRequest.getTaskName()).isEqualTo("task-34");
			assertThat(context.getEnvironment().getProperty(
					"spring.cloud.stream.function.bindings.timeSupplierspelFunctionheaderEnricherFunctiontaskLaunchRequestFunction-out-0"))
							.isEqualTo("output");
		}
	}

	@SpringBootApplication
	@Import(TimeSupplierConfiguration.class)
	public static class TimeSourceTestApplication {
	}
}
