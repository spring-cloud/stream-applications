package org.springframework.cloud.fn.tensorflow;

import java.util.Collections;
import java.util.Map;

import org.tensorflow.Tensor;

/**
 * @author Christian Tzolov
 */
public class ReleaseTensorParameters implements AutoCloseable {

	private final GraphRunner graph1;
	private final GraphRunner graph2;

	public ReleaseTensorParameters() {
		this.graph1 = new GraphRunner("x1", "y1")
				.withGraphDefinition(tf -> tf.withName("y1").math.mul(
						tf.withName("x1").placeholder(Integer.class),
						tf.constant(2)));

		this.graph2 = new GraphRunner("x2", "y2")
				.withGraphDefinition(tf -> tf.withName("y2").math.add(
						tf.withName("x2").placeholder(Integer.class),
						tf.constant(20)));
	}

	// y = (x * 2) + 20
	public int compute(Integer input) {
		try (
				Tensor x = Tensor.create(input);
				GraphRunnerMemory memory = new GraphRunnerMemory();
		) {

			Map<String, Tensor<?>> result =
					this.graph1.andThen(memory)
							.andThen(graph2).andThen(memory)
							.apply(Collections.singletonMap("x", x));

			memory.getTensorMap().entrySet().forEach(e -> System.out.println("  " + e));

			return result.get("y2").intValue();
		}
	}

	@Override
	public void close() {
		this.graph1.close();
		this.graph2.close();
	}

	public static void main(String[] args) {
		try (ReleaseTensorParameters example = new ReleaseTensorParameters()) {

			for (int x = 0; x < 5; x++) {
				System.out.println("For x = " + x + ", y = " + example.compute(x));
			}
		}
	}
}

