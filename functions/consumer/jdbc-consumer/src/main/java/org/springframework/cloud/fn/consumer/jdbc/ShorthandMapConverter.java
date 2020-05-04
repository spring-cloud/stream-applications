/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.jdbc;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * A Converter from String to Map that accepts csv {@literal key:value} pairs
 * (similar to what comes out of the box in Spring Core) but also simple
 * {@literal key} items, in which case the value is assumed to be equal to the key.
 * <p>
 * <p>Additionally, commas and colons can be escaped by using a backslash, which is
 * useful if said mappings are to be used for SpEL for example.</p>
 *
 * @author Eric Bottard
 * @author Artem Bilan
 */
public class ShorthandMapConverter implements Converter<String, Map<String, String>> {

	@Override
	public Map<String, String> convert(String source) {
		Map<String, String> result = new LinkedHashMap<>();

		// Split on comma if not preceded by backslash
		String[] mappings = source.split("(?<!\\\\),");
		for (String mapping : mappings) {
			// Turn backslash-comma back to comma
			String unescaped = mapping.trim().replace("\\,", ",");
			if (unescaped.length() == 0) {
				continue;
			}
			// Split on colon, if not preceded by backslash
			String[] keyValuePair = unescaped.split("(?<!\\\\):");
			Assert.isTrue(keyValuePair.length <= 2, "'" + unescaped +
					"' could not be parsed to a 'key:value' pair or simple 'key' with implicit value");
			String key = keyValuePair[0].trim().replace("\\:", ":");
			String value = keyValuePair.length == 2 ? keyValuePair[1].trim().replace("\\:", ":") : key;
			result.put(key, value);
		}
		return result;
	}

}
