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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.geode.cache.Region;
import org.apache.geode.pdx.PdxInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.common.geode.JsonPdxFunctions;
import org.springframework.cloud.fn.supplier.geode.GeodeSupplierConfiguration;
import org.springframework.cloud.stream.app.source.geodeserver.GeodeServerTestConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.gemfire.tests.integration.ForkingClientServerIntegrationTestsSupport;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
public class GeodeSourceTests {

	private static ApplicationContextRunner applicationContextRunner;


	private ObjectMapper objectMapper = new ObjectMapper();

	@BeforeAll
	static void setup() throws IOException {
		ForkingClientServerIntegrationTestsSupport.startGemFireServer(
				GeodeServerTestConfiguration.class, "-Dspring.data.gemfire.logging.level=warn");

		applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(
						TestChannelBinderConfiguration.getCompleteConfiguration(GeodeSourceTestApplication.class));

	}

	@AfterAll
	static void stopServer() {
		ForkingClientServerIntegrationTestsSupport.stopGemFireServer();
		ForkingClientServerIntegrationTestsSupport.clearCacheServerPortAndPoolPortProperties();
	}

	@Test
	void getCacheEvents() {
		applicationContextRunner
				.withPropertyValues("geode.region.regionName=myRegion",
						"geode.supplier.event-expression=key+':'+newValue",
						"geode.pool.connectType=server",
						"geode.pool.hostAddresses=" + "localhost:" + System.getProperty("spring.data.gemfire.cache.server.port"),
						"spring.cloud.function.definition=geodeSupplier")
				.run(context -> {

					// Using local region here since it's faster
					Region<String, String> region = context.getBean(Region.class);

					region.put("foo", "bar");
					region.put("name", "dave");
					region.put("hello", "world");
					OutputDestination outputDestination = context.getBean(OutputDestination.class);

					List<String> values = new ArrayList();
					for (int i = 0; i < 3; i++) {
						Message<byte[]> message = outputDestination.receive(Duration.ofSeconds(3).toMillis(), "geodeSupplier-out-0");
						assertThat(message).isNotNull();
						values.add(new String(message.getPayload()));
					}

					assertThat(values).containsExactly("foo:bar", "name:dave", "hello:world");
				});
	}

	@Test
	void pdxReadSerialized() {
		applicationContextRunner
				.withPropertyValues(
						"spring.cloud.function.definition=geodeSupplier",
						"geode.region.regionName=myRegion",
						"geode.client.pdx-read-serialized=true",
						"geode.supplier.query=Select * from /myRegion where symbol='XXX' and price > 140",
						"geode.pool.connectType=server",
						"geode.pool.hostAddresses=" + "localhost:" + System.getProperty("spring.data.gemfire.cache.server.port"))
				.run(context -> {
					OutputDestination outputDestination = context.getBean(OutputDestination.class);
					// Using local region here
					Region<String, PdxInstance> region = context.getBean(Region.class);
					putStockEvent(region, new Stock("XXX", 140.00));
					putStockEvent(region, new Stock("XXX", 140.20));
					putStockEvent(region, new Stock("YYY", 110.00));
					putStockEvent(region, new Stock("YYY", 110.01));
					putStockEvent(region, new Stock("XXX", 139.80));

					Message<byte[]> message = outputDestination.receive(Duration.ofSeconds(3).toMillis(), "geodeSupplier-out-0");
					assertThat(message).isNotNull();
					Stock result = objectMapper.readValue(message.getPayload(), Stock.class);
					assertThat(result).isEqualTo(new Stock("XXX", 140.20));
				});
	}

	private void putStockEvent(Region<String, PdxInstance> region, Stock stock) throws JsonProcessingException {
		String json = objectMapper.writeValueAsString(stock);
		region.put(stock.getSymbol(), JsonPdxFunctions.jsonToPdx().apply(json));
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class Stock {
		private String symbol;

		private double price;
	}

	@SpringBootApplication
	@Import(GeodeSupplierConfiguration.class)
	static class GeodeSourceTestApplication {

	}
}
