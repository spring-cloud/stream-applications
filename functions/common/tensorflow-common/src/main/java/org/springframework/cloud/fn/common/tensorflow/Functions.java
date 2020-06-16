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

package org.springframework.cloud.fn.common.tensorflow;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.tensorflow.Tensor;

/**
 * @author Christian Tzolov
 */
public final class Functions {

	private Functions() {

	}

	/**
	 * On every function call enrich the input tensorMap with an addition (tensorName, tensor) pair.
	 *
	 * @param tensorName tensor key to use in the map
	 * @param tensor new Tensor to add to the map
	 * @return Returns a copy of the input tensorMap enriched with the provided (tensorName, tensor).
	 */
	public static Function<Map<String, Tensor<?>>, Map<String, Tensor<?>>> enrichWith(
			String tensorName, Tensor<?> tensor) {
		return tensorMap -> enrich(tensorMap, tensorName, tensor);
	}

	/**
	 * On function call retrieves a named tensor from the provided {@link GraphRunnerMemory} and uses it to enrich
	 * the input tensorMap.
	 * @param memory GraphRunnerMemory to retrieve the tensor from
	 * @param tensorName name of the tensor in GraphRunnerMemory to retrieve.
	 * @return Returns copy of the input tensorMap enriched with the tensor from the memory.
	 */
	public static Function<Map<String, Tensor<?>>, Map<String, Tensor<?>>> enrichFromMemory(
			GraphRunnerMemory memory, String tensorName) {
		return tensorMap -> enrich(tensorMap, tensorName, memory.getTensorMap().get(tensorName));
	}

	private static Map<String, Tensor<?>> enrich(Map<String, Tensor<?>> inputTensorMap, String key, Tensor<?> value) {
		Map<String, Tensor<?>> newMap = new HashMap<>(inputTensorMap);
		newMap.put(key, value);
		return newMap;
	}

	/**
	 * Renames the tensor names in the incoming tensorMap with the providing mappings.
	 *
	 * @param mapping  Pairs of From and To names. E.g. fromName1, toName1, fromName2, toName2, ... fromNameN, toNameN
	 *                 Must be an even number.
	 * @return Map that renames the input tensorMap entries according to the mapping provided
	 */
	public static Function<Map<String, Tensor<?>>, Map<String, Tensor<?>>> rename(String... mapping) {

		Map<String, String> mappingMap = new HashMap<>();
		for (int i = 0; i < mapping.length; i = i + 2) {
			mappingMap.put(mapping[i], mapping[i + 1]);
		}

		return tensorMap -> tensorMap.entrySet().stream()
				.filter(e -> mappingMap.containsKey(e.getKey()))
				.collect(Collectors.toMap(
						kv -> mappingMap.get(kv.getKey()),
						kv -> kv.getValue()
				));
	}
}
