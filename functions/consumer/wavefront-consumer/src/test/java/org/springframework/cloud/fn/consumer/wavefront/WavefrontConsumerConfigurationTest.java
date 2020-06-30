/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.wavefront;

import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.fn.consumer.wavefront.service.WavefrontService;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Timo Salm
 */
@SpringBootTest(properties = {
		"wavefront.metric-name=vehicle-location",
		"wavefront.source=vehicle-api",
		"wavefront.metric-expression=#jsonPath(payload,'$.mileage')",
		"wavefront.timestamp-expression=#jsonPath(payload,'$.receivedAt')",
		"wavefront.point-tags-expressions-point-value.vin=#jsonPath(payload,'$.vin')",
		"wavefront.point-tags-expressions-point-value.latitude=#jsonPath(payload,'$.location.latitude')",
		"wavefront.wavefront-proxy-url=testUrl"
})
class WavefrontConsumerConfigurationTest {

	@Autowired
	private Consumer<Message<String>> wavefrontConsumer;

	@Autowired
	private ApplicationContext applicationContext;

	@MockBean
	private WavefrontService wavefrontServiceMock;

	@BeforeEach
	public void init() {
		Locale.setDefault(Locale.US);
	}

	@Test
	void testWavefrontConsumer() {
		final long timestamp = new Date().getTime();
		final String dataJsonString = "{ \"mileage\": 1.5, \"receivedAt\": " + timestamp + ", \"vin\": \"test-vin\", " +
				"\"location\": {\"latitude\": 4.53, \"longitude\": 2.89 }}";

		wavefrontConsumer.accept(new GenericMessage<>(dataJsonString));

		final String formattedString = "\"vehicle-location\" 1.5 " + timestamp + " source=vehicle-api " +
				"latitude=\"4.53\" vin=\"test-vin\"";
		Mockito.verify(wavefrontServiceMock, Mockito.times(1)).send(formattedString);
	}

	@SpringBootApplication
	static class WavefrontConsumerTestApplication {
	}
}
