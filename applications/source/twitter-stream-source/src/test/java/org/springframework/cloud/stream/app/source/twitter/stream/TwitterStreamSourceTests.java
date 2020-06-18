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

package org.springframework.cloud.stream.app.source.twitter.stream;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.supplier.twitter.stream.TwitterStreamSupplierConfiguration;
import org.springframework.cloud.fn.supplier.twitter.stream.TwitterStreamSupplierProperties;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext
public class TwitterStreamSourceTests {

	@Test
	@Ignore
	public void testSourceFromSupplier() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(SampleConfiguration.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.stream.function.definition=twitterStreamSupplier",
						"--twitter.connection.consumerKey=consumerKey666",
						"--twitter.connection.consumerSecret=consumerSecret666",
						"--twitter.connection.accessToken=accessToken666",
						"--twitter.connection.accessTokenSecret=accessTokenSecret666")) {

			OutputDestination target = context.getBean(OutputDestination.class);
			Message<byte[]> sourceMessage = target.receive(10000);
			final String actual = new String(sourceMessage.getPayload());

			TwitterStreamSupplierProperties twitterStreamSupplierProperties = context.getBean(TwitterStreamSupplierProperties.class);
			//SimpleDateFormat dateFormat = new SimpleDateFormat(twitterStreamSupplierProperties.getDateFormat());
			//assertThatCode(() -> {
			//	Date date = dateFormat.parse(actual);
			//	assertThat(date).isNotNull();
			//}).doesNotThrowAnyException();
		}
	}

	@SpringBootApplication
	@Import({ TwitterStreamSupplierConfiguration.class })
	public static class SampleConfiguration {

	}
}
