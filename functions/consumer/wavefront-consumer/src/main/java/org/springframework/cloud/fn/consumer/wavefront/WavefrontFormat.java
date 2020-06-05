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
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.integration.json.JsonPathUtils;

/**
 * @author Timo Salm
 */
public class WavefrontFormat {

	private final Logger log = LoggerFactory.getLogger(WavefrontFormat.class);

	private final WavefrontConsumerProperties properties;
	private final String dataJsonString;

	public WavefrontFormat(final WavefrontConsumerProperties properties, String dataJsonString) {
		this.properties = properties;
		this.dataJsonString = dataJsonString;
	}

	public String getFormattedString() throws IOException {
		final Number metricValue = extractMetricValueFromJson();

		final Map<String, Object> pointTagsMap = extractPointTagsMapFromJson(
				properties.getPointTagsJsonPathsPointValue(), dataJsonString);
		validatePointTagsKeyValuePairs(pointTagsMap);
		final String formattedPointTagsPart = getFormattedPointTags(pointTagsMap);

		if (properties.getTimestampJsonPath() == null) {
			return String.format("\"%s\" %s source=%s %s", properties.getMetricName(), metricValue,
					properties.getSource(), formattedPointTagsPart).trim();
		}

		final Long timestamp = extractTimestampFromJson();
		return String.format("\"%s\" %s %d source=%s %s", properties.getMetricName(), metricValue, timestamp,
				properties.getSource(), formattedPointTagsPart).trim();
	}

	private Long extractTimestampFromJson() throws IOException {
		try {
			return JsonPathUtils.evaluate(dataJsonString, properties.getTimestampJsonPath());
		}
		catch (ClassCastException e) {
			throw new ValidationException("The timestamp value has to be a number that reflects the epoch seconds of the " +
					"metric (e.g. 1382754475).", e);
		}
	}

	private Number extractMetricValueFromJson() throws IOException {
		try {
			return JsonPathUtils.evaluate(dataJsonString, properties.getMetricJsonPath());
		}
		catch (ClassCastException e) {
			throw new ValidationException("The metric value has to be a double-precision floating point number or a " +
					"long integer. It can be positive, negative, or 0.", e);
		}
	}

	private String getFormattedPointTags(Map<String, Object> pointTagsMap) {
		return pointTagsMap.entrySet().stream()
				.map(it -> String.format("%s=\"%s\"", it.getKey(), it.getValue()))
				.collect(Collectors.joining(" "));
	}

	private Map<String, Object> extractPointTagsMapFromJson(Map<String, String> pointTagsJsonPathsPointValue, String dataJsonString) {
		return pointTagsJsonPathsPointValue.entrySet().stream()
				.map(it -> {
					try {
						final Object pointValue = JsonPathUtils.evaluate(dataJsonString, it.getValue());
						return new AbstractMap.SimpleEntry<>(it.getKey(), pointValue);
					}
					catch (IOException e) {
						log.warn("Unable to extract point tag for key " + it.getKey() + " from json data", e);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private void validatePointTagsKeyValuePairs(Map<String, Object> pointTagsMap) {
		pointTagsMap.forEach((key, value) -> {
			if (!Pattern.matches("^[a-zA-Z0-9._-]+", key)) {
				throw new ValidationException("Point tag key \"" + key + "\" contains invalid characters: Valid " +
						"characters are alphanumeric, hyphen (\"-\"), underscore (\"_\"), dot (\".\")");
			}

			final int keyValueCombinationLength = key.length() + value.toString().length();
			if (keyValueCombinationLength > 254) {
				log.warn("Maximum allowed length for a combination of a point tag key and value " +
						"is 254 characters. The length of combination for key " + key + " is " +
						keyValueCombinationLength + ".");
			}
		});
	}
}
