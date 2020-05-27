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

package org.springframework.cloud.fn.supplier.geode;

import java.time.Duration;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Region;
import org.apache.geode.pdx.PdxInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.common.geode.JsonPdxFunctions;
import org.springframework.cloud.fn.test.support.geode.GeodeContainer;
import org.springframework.cloud.fn.test.support.geode.GeodeContainerIntializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class GeodeSupplierApplicationTests {

	private static ApplicationContextRunner applicationContextRunner;

	private static GeodeContainer geode;

	private ObjectMapper objectMapper = new ObjectMapper();

	@BeforeAll
	static void setup() {
		GeodeContainerIntializer initializer = new GeodeContainerIntializer(
				geodeContainer -> {
					geodeContainer.connectAndExecGfsh("create region --name=myRegion --type=REPLICATE");
				});

		applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(GeodeSupplierTestApplication.class);

		geode = initializer.geodeContainer();
	}

	@Test
	void getServerEntryEvents() {
		applicationContextRunner
				.withPropertyValues("geode.region.regionName=myRegion",
						"geode.supplier.event-expression=#root",
						"geode.pool.hostAddresses=" + "localhost:" + geode.getLocatorPort())
				.run(context -> {
					geode.connectAndExecGfsh(
							"put --key=hello --value=world --region=myRegion",
							"put --key=foo --value=bar --region=myRegion",
							"put --key=hello --value=dave --region=myRegion");

					Supplier<Flux<EntryEvent>> geodeSupplier = context.getBean("geodeSupplier", Supplier.class);

					StepVerifier.create(geodeSupplier.get()).assertNext(cacheEvent -> {
						assertThat(cacheEvent.getOperation().isCreate()).isTrue();
						assertThat(cacheEvent.getKey()).isEqualTo("hello");
						assertThat(cacheEvent.getNewValue()).isEqualTo("world");
					}).assertNext(cacheEvent -> {
						assertThat(cacheEvent.getOperation().isCreate()).isTrue();
						assertThat(cacheEvent.getKey()).isEqualTo("foo");
						assertThat(cacheEvent.getNewValue()).isEqualTo("bar");
					}).assertNext(cacheEvent -> {
						assertThat(cacheEvent.getOperation().isUpdate()).isTrue();
						assertThat(cacheEvent.getKey()).isEqualTo("hello");
						assertThat(cacheEvent.getNewValue()).isEqualTo("dave");

					}).thenCancel().verify(Duration.ofSeconds(10));
				});
	}

	@Test
	void pdxReadSerialized() {
		applicationContextRunner
				.withPropertyValues(
						"geode.region.regionName=myRegion",
						"geode.client.pdx-read-serialized=true",
						"geode.pool.hostAddresses=" + "localhost:" + geode.getLocatorPort())
				.run(context -> {
					Supplier<Flux<String>> geodeSupplier = context.getBean("geodeSupplier", Supplier.class);
					// Using local region here
					Region<String, PdxInstance> region = context.getBean(Region.class);
					Stock stock = new Stock("XXX", 140.00);
					ObjectMapper objectMapper = new ObjectMapper();
					String json = objectMapper.writeValueAsString(stock);
					region.put(stock.getSymbol(), JsonPdxFunctions.jsonToPdx().apply(json));

					StepVerifier.create(geodeSupplier.get()).assertNext(val -> {
						try {
							assertThat(objectMapper.readValue(val, Stock.class)).isEqualTo(stock);
						}
						catch (JsonProcessingException e) {
							fail(e.getMessage());
						}
					}).thenCancel().verify(Duration.ofSeconds(10));
				});
	}

	@Test
	void connectTypeServer() {
		applicationContextRunner
				.withPropertyValues("geode.region.regionName=myRegion",
						"geode.pool.connect-type=server",
						"geode.supplier.event-expression=key+':'+newValue",
						"geode.pool.hostAddresses=" + "localhost:" + geode.getCacheServerPort())
				.run(context -> {

					// Using local region here since it's faster
					Region<String, String> region = context.getBean(Region.class);

					region.put("foo", "bar");
					Supplier<Flux<String>> geodeSupplier = context.getBean("geodeSupplier", Supplier.class);

					StepVerifier.create(geodeSupplier.get()).assertNext(val -> {
						assertThat(val).isEqualTo("foo:bar");
					}).thenCancel().verify(Duration.ofSeconds(10));
				});
	}

	@Test
	void continuousQuery() {
		applicationContextRunner
				.withPropertyValues(
						"geode.region.regionName=myRegion",
						"geode.client.pdx-read-serialized=true",
						"geode.supplier.query=Select * from /myRegion where symbol='XXX' and price > 140",
						"geode.pool.hostAddresses=" + "localhost:" + geode.getLocatorPort())
				.run(context -> {
					Supplier<Flux<String>> geodeCqSupplier = context.getBean("geodeSupplier", Supplier.class);
					// Using local region here
					Region<String, PdxInstance> region = context.getBean(Region.class);
					putStockEvent(region, new Stock("XXX", 140.00));
					putStockEvent(region, new Stock("XXX", 140.20));
					putStockEvent(region, new Stock("YYY", 110.00));
					putStockEvent(region, new Stock("YYY", 110.01));
					putStockEvent(region, new Stock("XXX", 139.80));

					StepVerifier.create(geodeCqSupplier.get()).assertNext(val -> {
						try {
							assertThat(objectMapper.readValue(val, Stock.class)).isEqualTo(new Stock("XXX", 140.20));
						}
						catch (JsonProcessingException e) {
							fail(e.getMessage());
						}
					}).thenCancel().verify(Duration.ofSeconds(10));
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
	static class GeodeSupplierTestApplication {
	}
}
