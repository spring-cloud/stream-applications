/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.stream.app.tasklaunchrequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

/**
 * Parses a comma delimited list of key value pairs in which the values can contain commas as well.
 *
 * @author Chris Schaeffer
 * @author David Turanski
 **/
abstract class KeyValueListParser {

	static Map<String, String> parseCommaDelimitedKeyValuePairs(String value) {
		Map<String, String> keyValuePairs = new HashMap<>();

		if (StringUtils.isEmpty(value)) {
			return keyValuePairs;
		}

		ArrayList<String> pairs = new ArrayList<>();

		String[] candidates = StringUtils.commaDelimitedListToStringArray(value);

		for (int i = 0; i < candidates.length; i++) {
			if (i > 0 && !candidates[i].contains("=")) {
				pairs.add(pairs.get(pairs.size() - 1) + "," + candidates[i]);
			}
			else {
				pairs.add(candidates[i]);
			}
		}

		for (String pair : pairs) {
			addKeyValuePair(pair, keyValuePairs);
		}

		return keyValuePairs;
	}

	private static void addKeyValuePair(String pair, Map<String, String> properties) {
		int firstEquals = pair.indexOf('=');
		if (firstEquals != -1) {
			properties.put(pair.substring(0, firstEquals).trim(), pair.substring(firstEquals + 1).trim());
		}
	}
}
