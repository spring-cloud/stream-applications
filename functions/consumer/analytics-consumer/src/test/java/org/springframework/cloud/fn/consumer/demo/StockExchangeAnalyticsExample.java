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

package org.springframework.cloud.fn.consumer.demo;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.fn.consumer.analytics.AnalyticsConsumerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Sample Spring Boot Application that uses the analyticsConsumer to compute running stats from
 * stock exchange messages.
 *
 * Counter configuration:
 * <code>
 *    --analytics.meter-type=counter
 *    --analytics.name=stocks
 *    --analytics.tag.expression.symbol=#jsonPath(payload,'$.data.symbol')
 *    --analytics.tag.expression.exchange=#jsonPath(payload,'$.data.exchange')
 * </code>
 *
 * Gauge configuration:
 * <code>
 *    --analytics.meter-type=gauge
 *    --analytics.name=stocks
 *    --analytics.tag.expression.symbol=#jsonPath(payload,'$.data.symbol')
 *    --analytics.tag.expression.exchange=#jsonPath(payload,'$.data.exchange')
 *    --analytics.amount-expression=#jsonPath(payload,'$.data.volume')
 * </code>
 *
 * Sample Wavefront configuration:
 * <code>
 *    --management.metrics.export.wavefront.enabled=true
 *    --management.metrics.export.wavefront.uri=YOUR_WAVEFRONT_SERVER_URI
 *    --management.metrics.export.wavefront.api-token=YOUR_API_TOKEN
 *    --management.metrics.export.wavefront.source=stock-exchange-demo
 * </code>
 *
 * @author Christian Tzolov
 */
@Import(AnalyticsConsumerConfiguration.class)
@SpringBootApplication
public class StockExchangeAnalyticsExample {

	public static void main(String[] args) {
		SpringApplication.run(StockExchangeAnalyticsExample.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(Consumer<Message<?>> analyticsConsumer,
			MeterRegistry meterRegistry, Supplier<String> stockMessageGenerator) {

		// Run every second.
		return args -> Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {

			String message = stockMessageGenerator.get();

			// Submit new message using the stockMessageGenerator to generate random stock messages.
			analyticsConsumer.accept(MessageBuilder.withPayload(message).build());

			// Print current stock meters
			System.out.println(meterRegistry.getMeters().stream()
					.filter(meter -> meter.getId().getName().contains("stocks"))
					.map(meter -> meter.getId().getType() + " | " + meter.getId() + " | " + meter.measure())
					.collect(Collectors.joining("\n")) +
					"\n=========================================================================");

		}, 0, 1000, TimeUnit.MILLISECONDS);
	}

	@Bean
	public Supplier<String> stockMessageGenerator() {
		final Random random = new Random();
		final String[][] STOCKS = new String[][] { { "NASDAQ", "GOOGL" }, { "AMS", "TOM2" }, { "NYSE", "CLDR" },
				{ "NYSE", "VMW" }, { "NYSE", "IBM" }, { "NASDAQ", "MSFT" }, { "NASDAQ", "AAPL" } };

		return () -> {
			int stockIndex = random.nextInt(STOCKS.length);
			return "{\n" +
					"  \"data\": {\n" +
					"      \"symbol\": \"" + STOCKS[stockIndex][1] + "\",\n" +
					"      \"exchange\": \"" + STOCKS[stockIndex][0] + "\",\n" +
					"      \"open\": " + (1 + 10 * random.nextDouble()) + ",\n" +
					"      \"close\": " + (1 + 10 * random.nextDouble()) + ",\n" +
					"      \"volume\": " + (1000 + 100000 * random.nextDouble()) + "\n" +
					"  }\n" +
					"}";
		};
	}
}

