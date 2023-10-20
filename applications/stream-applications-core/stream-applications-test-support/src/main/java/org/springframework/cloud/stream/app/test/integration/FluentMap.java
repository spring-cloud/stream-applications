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

package org.springframework.cloud.stream.app.test.integration;

import java.util.LinkedHashMap;

/**
 * Fluent API wrapper for {@link java.util.LinkedHashMap}.
 *
 * @param <K> key type.
 * @param <V> value type.
 * @author David Turanski
 * @author Corneil du Plessis
 */
public class FluentMap<K, V> extends LinkedHashMap<K, V> {
	@SuppressWarnings("rawtypes")
	public static FluentMap fluentMap() {
		return new FluentMap();
	}

	public static <K, V> FluentMap<K, V> fluentMap(Class<K> kClass, Class<V> cClass) {
		return new FluentMap<>();
	}

	public static FluentMap<String, String> fluentStringMap() {
		return new FluentMap<String, String>();
	}

	public FluentMap<K, V> withEntry(K key, V value) {
		put(key, value);
		return this;
	}
}
