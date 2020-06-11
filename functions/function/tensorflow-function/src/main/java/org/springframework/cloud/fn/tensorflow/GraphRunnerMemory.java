package org.springframework.cloud.fn.tensorflow;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.tensorflow.Tensor;

import org.springframework.cloud.fn.tensorflow.util.AutoCloseables;

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

