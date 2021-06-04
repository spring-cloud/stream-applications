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

package org.springframework.cloud.stream.app.source.jms;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.supplier.jms.JmsSupplierConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"jms.supplier.destination = jmssource.test.queue"})
@DirtiesContext
public class JmsSourceTests {

	@Autowired
	private OutputDestination output;

	@Autowired
	private JmsTemplate template;

	@Test
	public void testJmsSource() {
		template.convertAndSend("jmssource.test.queue", "Hello, world!");
		Message<byte[]> sourceMessage = output.receive(10000, "jmsSupplier-out-0");
		final String actual = new String(sourceMessage.getPayload());
		assertThat(actual).isEqualTo("Hello, world!");
	}

	@SpringBootApplication
	@Import({TestChannelBinderConfiguration.class, JmsSupplierConfiguration.class})
	public static class SampleConfiguration {

	}
}
