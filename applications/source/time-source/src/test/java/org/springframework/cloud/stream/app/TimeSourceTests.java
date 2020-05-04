/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.supplier.time.TimeProperties;
import org.springframework.cloud.fn.supplier.time.TimeSupplierConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Soby Chacko
 */
public class TimeSourceTests {

	@Test
	public void testSourceFromSupplier() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(TimeSourceConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=timeSupplier")) {

			OutputDestination target = context.getBean(OutputDestination.class);
			Message<byte[]> sourceMessage = target.receive(10000);
			final String actual = new String(sourceMessage.getPayload());

			TimeProperties timeProperties = context.getBean(TimeProperties.class);
			SimpleDateFormat dateFormat = new SimpleDateFormat(timeProperties.getDateFormat());
			assertThatCode(() -> {
				Date date = dateFormat.parse(actual);
				assertThat(date).isNotNull();
			}).doesNotThrowAnyException();
		}
	}

	@EnableAutoConfiguration
	@Import(TimeSupplierConfiguration.class)
	public static class TimeSourceConfiguration {}
}
