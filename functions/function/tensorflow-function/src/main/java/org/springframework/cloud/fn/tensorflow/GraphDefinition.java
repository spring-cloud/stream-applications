package org.springframework.cloud.fn.tensorflow;

import org.tensorflow.op.Ops;

/**
 * @author Christian Tzolov
 */
@FunctionalInterface
public interface GraphDefinition {
	void defineGraph(Ops tf);
}
