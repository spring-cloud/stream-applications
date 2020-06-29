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

package org.springframework.cloud.stream.app.sink.analytics;

import java.nio.charset.StandardCharsets;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.consumer.analytics.AnalyticsConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
public class AnalyticsSinkTests {

	@Test
	public void testAnalyticsSink() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(AnalyticsSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=byteArrayTextToString|analyticsConsumer",
						"--analytics.name=counter666",
						"--analytics.amount-expression=payload.length()",
						"--analytics.tag.expression.foo='bar'")) {

			SimpleMeterRegistry meterRegistry = context.getBean(SimpleMeterRegistry.class);

			String message = "hello world message";
			InputDestination source = context.getBean(InputDestination.class);
			source.send(new GenericMessage<>(message.getBytes(StandardCharsets.UTF_8)));

			Counter counter = meterRegistry.find("counter666").counter();
			assertThat(counter.count()).isEqualTo(message.length());
			assertThat(counter.getId().getTag("foo")).isEqualTo("bar");
		}
	}

	@SpringBootApplication
	@Import({ AnalyticsConsumerConfiguration.class})
	public static class AnalyticsSinkTestApplication {
	}

}
