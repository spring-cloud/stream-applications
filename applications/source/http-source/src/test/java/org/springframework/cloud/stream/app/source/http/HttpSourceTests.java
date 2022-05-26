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

package org.springframework.cloud.stream.app.source.http;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.fn.supplier.http.HttpSupplierConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Soby Chacko
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"spring.cloud.function.definition=httpSupplier", "debug=true"})
public class HttpSourceTests {

	@Autowired
	OutputDestination outputDestination;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	public void testSourceFromSupplier() {
		testRestTemplate.postForObject("/", "test1", Object.class);
		Message<byte[]> sourceMessage = outputDestination.receive(10000, "httpSupplier-out-0");
		final String actual = new String(sourceMessage.getPayload());
		assertThat(actual).isEqualTo("test1");
	}

	@SpringBootApplication
	@Import({HttpSupplierConfiguration.class, TestChannelBinderConfiguration.class, BindingServiceConfiguration.class})
	public static class HttpSourceTestApplication {
	}
}
