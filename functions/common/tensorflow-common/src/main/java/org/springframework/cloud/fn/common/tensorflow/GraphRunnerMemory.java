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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.tensorflow.Tensor;

import org.springframework.cloud.fn.common.tensorflow.util.AutoCloseables;


/**
 * Keeps all tensorMap input parameters.
 */
public class GraphRunnerMemory implements Function<Map<String, Tensor<?>>, Map<String, Tensor<?>>>, AutoCloseable {

	private AtomicReference<PMap<String, Tensor<?>>> tensorMap = new AtomicReference<>(HashTreePMap.empty());

	public Map<String, Tensor<?>> getTensorMap() {
		return tensorMap.get();
	}

	@Override
	public Map<String, Tensor<?>> apply(Map<String, Tensor<?>> tensorMap) {
		this.tensorMap.getAndUpdate(pmap -> pmap.plusAll(tensorMap));
		return tensorMap;
	}

	@Override
	public void close() {
		AutoCloseables.all(this.tensorMap.get());
		//this.tensorMap.get().clear();
	}
}

