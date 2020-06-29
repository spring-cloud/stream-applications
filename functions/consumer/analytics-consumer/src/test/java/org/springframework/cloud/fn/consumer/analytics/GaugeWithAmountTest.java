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

package org.springframework.cloud.fn.consumer.analytics;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@TestPropertySource(properties = {
		"analytics.meter-type=gauge",
		"analytics.name=myGauge",
		"analytics.tag.expression.foo='bar'",
		"analytics.amount-expression=payload.length()"
})
class GaugeWithAmountTest extends AnalyticsConsumerParentTest {

	@Test
	void testАnalyticsSink() {
		String messageSmall = "hello";
		analyticsConsumer.accept(new GenericMessage(messageSmall));
		assertThat(meterRegistry.find("myGauge").gauge().value()).isEqualTo(size(messageSmall));

		assertThat(meterRegistry.find("myGauge").gauge().getId().getTags()).hasSize(1);
		assertThat(meterRegistry.find("myGauge").gauge().getId().getTag("foo")).isEqualTo("bar");

		String messageMiddle = "hello world";
		analyticsConsumer.accept(new GenericMessage(messageMiddle));
		assertThat(meterRegistry.find("myGauge").gauge().value()).isEqualTo(size(messageMiddle));

		String messageLarge = "hello world, hello people!";
		analyticsConsumer.accept(new GenericMessage(messageLarge));
		assertThat(meterRegistry.find("myGauge").gauge().value()).isEqualTo(size(messageLarge));

	}

	private double size(String msg) {
		return Long.valueOf(msg.length()).doubleValue();
	}
}
