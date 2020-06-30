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

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Timo Salm
 */
class WavefrontFormatTest {

	@BeforeEach
	public void init() {
		Locale.setDefault(Locale.US);
	}

	@Test
	void testGetFormattedString() throws IOException {
		final long timestamp = new Date().getTime();
		final String dataJsonString = "{ \"value\": 1.5, \"timestamp\": " + timestamp + ", "
				+ "\"testProp1\": \"testvalue1\", \"testProp2\": \"testvalue2\" }";

		final Map<String, String> pointTagsJsonPathsPointValueMap = Stream.of(
				new AbstractMap.SimpleEntry<>("testpoint1", "$.testProp1"),
				new AbstractMap.SimpleEntry<>("testpoint2", "$.testProp2")
		).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		final WavefrontConsumerProperties properties = new WavefrontConsumerProperties("testMetricName", "testSource",
				"$.value", "$.timestamp", pointTagsJsonPathsPointValueMap, null, null, null);

		final String result = new WavefrontFormat(properties, dataJsonString).getFormattedString();
		assertThat(result).isEqualTo("\"testMetricName\" 1.5 " + timestamp + " source=testSource testpoint2=\"testvalue2\""
				+ " testpoint1=\"testvalue1\"", result);
	}

	@Test
	void testGetFormattedStringWithoutTimeStamp() throws IOException {
		final String dataJsonString = "{ \"value\": 1.5, \"testProp1\": \"testvalue1\", \"testProp2\": \"testvalue2\" }";

		final Map<String, String> pointTagsJsonPathsPointValueMap =
				Stream.of(new AbstractMap.SimpleEntry<>("testpoint1", "$.testProp1"))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		final WavefrontConsumerProperties properties = new WavefrontConsumerProperties("testMetricName", "testSource",
				"$.value", null, pointTagsJsonPathsPointValueMap, null, null, null);

		final String result = new WavefrontFormat(properties, dataJsonString).getFormattedString();
		assertThat(result).isEqualTo("\"testMetricName\" 1.5 source=testSource testpoint1=\"testvalue1\"");
	}

	@Test
	void testGetFormattedStringWithoutPointTags() throws IOException {
		final long timestamp = new Date().getTime();
		final String dataJsonString = "{ \"value\": 1.5, \"timestamp\": " + timestamp + "}";

		final WavefrontConsumerProperties properties = new WavefrontConsumerProperties("testMetricName", "testSource",
				"$.value", "$.timestamp", Collections.emptyMap(), null, null, null);

		final String result = new WavefrontFormat(properties, dataJsonString).getFormattedString();
		assertThat(result).isEqualTo("\"testMetricName\" 1.5 " + timestamp + " source=testSource");
	}

	@Test
	void testInvalidMetricValue() {
		final String dataJsonString = "{ \"value\": a}";
		final WavefrontConsumerProperties properties = new WavefrontConsumerProperties("testMetricName", "testSource",
				"$.value", null, Collections.emptyMap(), null, null, null);
		final Exception exception = Assertions.assertThrows(RuntimeException.class, () ->
				new WavefrontFormat(properties, dataJsonString).getFormattedString());
		assertThat(exception.getLocalizedMessage().startsWith("The metric value has to be a double-precision floating"))
				.isTrue();
	}

	@Test
	void testInvalidTimestampValue() {
		final String dataJsonString = "{ \"value\": 1.5, \"timestamp\": 2020-06-02T13:53:18+0000}";
		final WavefrontConsumerProperties properties = new WavefrontConsumerProperties("testMetricName", "testSource",
				"$.value", "$.timestamp", Collections.emptyMap(), null, null, null);
		final Exception exception = Assertions.assertThrows(RuntimeException.class,
				() -> new WavefrontFormat(properties, dataJsonString).getFormattedString());
		assertThat(exception.getLocalizedMessage().startsWith("The timestamp value has to be a number")).isTrue();
	}

	@Test
	void testInvalidPointTagsLengthWarning() throws IOException {
		final Logger logger = (Logger) LoggerFactory.getLogger(WavefrontFormat.class);
		final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.start();
		logger.addAppender(listAppender);

		final String testPointTagKey = createStringOfLength(127);

		final Map<String, String> pointTagsJsonPathsPointValueMap =
				Stream.of(new AbstractMap.SimpleEntry<>(testPointTagKey, "$.testPoint1"))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		final WavefrontConsumerProperties properties = new WavefrontConsumerProperties("testMetricName", "testSource",
				"$.value", null, pointTagsJsonPathsPointValueMap, null, null, null);

		final String dataJsonString = "{ \"value\": 1.5, \"testPoint1\": \"" +
				createStringOfLength(254 - testPointTagKey.length()) + "\" }";
		new WavefrontFormat(properties, dataJsonString).getFormattedString();
		assertThat(listAppender.list.stream()
				.filter(event -> event.getMessage().startsWith("Maximum allowed length for a combination"))
				.count()).isEqualTo(0);

		final String dataJsonStringWithTooLongValue = "{ \"value\": 1.5, \"testPoint1\": \"" +
				createStringOfLength(255 - testPointTagKey.length()) + "\" }";
		new WavefrontFormat(properties, dataJsonStringWithTooLongValue).getFormattedString();

		assertThat(listAppender.list.stream()
				.filter(event -> event.getMessage().startsWith("Maximum allowed length for a combination")
						&& event.getLevel().equals(Level.WARN))
				.count()).isEqualTo(1);
	}

	@Test
	void testInvalidPointTagKeys() throws IOException {
		final String dataJsonString = "{ \"value\": 1.5, \"testPoint1\": \"testvalue1\" }";

		final List<String> validPointTagKeys = Arrays.asList("b", "B", "2", ".", "_", "-", "c.8W-2h_dE_J-h");
		for (String validPointTagKey : validPointTagKeys) {
			final Map<String, String> pointTagsJsonPathsPointValueMap =
					Stream.of(new AbstractMap.SimpleEntry<>(validPointTagKey, "$.testPoint1"))
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			final WavefrontConsumerProperties properties = new WavefrontConsumerProperties("testMetricName", "testSource",
					"$.value", null, pointTagsJsonPathsPointValueMap, null, null, null);
			new WavefrontFormat(properties, dataJsonString).getFormattedString();
		}

		final List<String> invalidPointTagKeys = Arrays.asList(" ", ":", "a B", "#", "/", ",");

		invalidPointTagKeys.forEach(invalidPointTagKey -> {
			final Map<String, String> pointTagsJsonPathsPointValueMap =
					Stream.of(new AbstractMap.SimpleEntry<>(invalidPointTagKey, "$.testPoint1"))
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			final WavefrontConsumerProperties properties = new WavefrontConsumerProperties("testMetricName", "testSource",
					"$.value", null, pointTagsJsonPathsPointValueMap, null, null, null);

			final Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
				new WavefrontFormat(properties, dataJsonString).getFormattedString();
			});
			assertThat(exception.getLocalizedMessage()
					.startsWith("Point tag key \"" + invalidPointTagKey + "\" contains invalid characters")).isTrue();
		});
	}

	private String createStringOfLength(int length) {
		char[] chars = new char[length];
		Arrays.fill(chars, 'a');
		return new String(chars);
	}
}
