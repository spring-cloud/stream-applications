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

package org.springframework.cloud.stream.app.source.geode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.geode.cache.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.supplier.geode.GeodeSupplierConfiguration;
import org.springframework.cloud.fn.test.support.geode.GeodeContainer;
import org.springframework.cloud.fn.test.support.geode.GeodeContainerIntializer;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GeodeSourceTests {

	private ApplicationContextRunner applicationContextRunner;

	private GeodeContainer geode;

	@BeforeAll
	void setup() {
		GeodeContainerIntializer initializer = new GeodeContainerIntializer(
				geodeContainer -> {
					geodeContainer.connectAndExecGfsh("create region --name=myRegion --type=REPLICATE");
				});

		applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(
						TestChannelBinderConfiguration.getCompleteConfiguration(GeodeSourceTestApplication.class));

		geode = initializer.geodeContainer();
	}

	@Test
	void getCacheEvents() {
		applicationContextRunner
				.withPropertyValues("geode.region.regionName=myRegion",
						"geode.supplier.entry-event-expression=key+':'+newValue",
						"geode.pool.hostAddresses=" + "localhost:" + geode.getLocatorPort(),
						"spring.cloud.function.definition=geodeSupplier")
				.run(context -> {

					Stream.of(context.getBeanNamesForAnnotation(EnableBinding.class)).forEach(System.out::println);

					// Using local region here since it's faster
					Region<String, String> region = context.getBean(Region.class);

					region.put("foo", "bar");
					region.put("name", "dave");
					region.put("hello", "world");
					OutputDestination outputDestination = context.getBean(OutputDestination.class);

					List<String> values = new ArrayList();
					for (int i = 0; i < 3; i++) {
						Message<byte[]> message = outputDestination.receive(Duration.ofSeconds(3).toMillis());
						assertThat(message).isNotNull();
						values.add(new String(message.getPayload()));
					}

					assertThat(values).containsExactly("foo:bar", "name:dave", "hello:world");
				});
	}

	@SpringBootApplication
	@Import(GeodeSupplierConfiguration.class)
	static class GeodeSourceTestApplication {

	}
}
