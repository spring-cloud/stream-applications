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

package org.springframework.cloud.fn.consumer.counter;

import java.util.function.Function;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Christian Tzolov
 */
public class ConverterFunctionAdapter<S, T> implements Converter<S, T> {

	private Function<S, T> function;

	public ConverterFunctionAdapter(Function<S, T> function) {
		this.function = function;
	}

	@Override
	public T convert(S s) {
		return this.function.apply(s);
	}
}
