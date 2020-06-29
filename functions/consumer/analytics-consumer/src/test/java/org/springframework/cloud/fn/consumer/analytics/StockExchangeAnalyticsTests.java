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

import java.io.IOException;
import java.util.Collection;

import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */

@TestPropertySource(properties = {
		"analytics.meter-type=counter",
		"analytics.name=stocks",
		"analytics.tag.expression.symbol=#jsonPath(payload,'$.data.symbol')",
		"analytics.tag.expression.exchange=#jsonPath(payload,'$.data.exchange')"
})
public class StockExchangeAnalyticsTests extends AnalyticsConsumerParentTest {

	@Test
	public void testCounter() throws IOException {
		byte[] messageAppl = StreamUtils.copyToByteArray(
				new DefaultResourceLoader().getResource("classpath:/data/stock_appl.json").getInputStream());

		analyticsConsumer.accept(MessageBuilder.withPayload(messageAppl).build());
		analyticsConsumer.accept(MessageBuilder.withPayload(messageAppl).build());
		analyticsConsumer.accept(MessageBuilder.withPayload(messageAppl).build());

		byte[] messageVmw = StreamUtils.copyToByteArray(
				new DefaultResourceLoader().getResource("classpath:/data/stock_vmw.json").getInputStream());

		analyticsConsumer.accept(MessageBuilder.withPayload(messageVmw).build());
		analyticsConsumer.accept(MessageBuilder.withPayload(messageVmw).build());

		Collection<Counter> counters = meterRegistry.find("stocks").counters();

		assertThat(counters).hasSize(2);

		//Iterator<Counter> itr = counters.iterator();
		//
		//Counter applCounter = itr.next();
		//assertThat(applCounter.count()).isEqualTo(3);
		//assertThat(applCounter.getId().getTags()).contains(Tag.of("symbol", "AAPL"), Tag.of("exchange", "XNAS"));
		//
		//Counter vmwCounter = itr.next();
		//assertThat(vmwCounter.count()).isEqualTo(2);
		//assertThat(vmwCounter.getId().getTags()).contains(Tag.of("symbol", "VMW"), Tag.of("exchange", "NYSE"));

	}
}
