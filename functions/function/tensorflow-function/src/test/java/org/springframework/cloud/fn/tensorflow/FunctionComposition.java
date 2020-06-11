package org.springframework.cloud.fn.tensorflow;

import java.util.Collections;
import java.util.Map;

import org.tensorflow.Tensor;

/**
 * @author Christian Tzolov
 */
public class FunctionComposition {

	// y = (x * 2) + 20
	//
	// y1 = x1 * 2 , where x1 == x
	// y2 = x2 + 20 , where x2 == y1 and y = y2
	public static void main(String[] args) {
		try (
				GraphRunner graph1 = new GraphRunner("x1", "y1")
						.withGraphDefinition(tf -> tf.withName("y1").math.mul(
								tf.withName("x1").placeholder(Integer.class),
								tf.constant(2)));
				GraphRunner graph2 = new GraphRunner("x2", "y2")
						.withGraphDefinition(tf -> tf.withName("y2").math.add(
								tf.withName("x2").placeholder(Integer.class),
								tf.constant(20)));
				Tensor x = Tensor.create(10);
		) {

			Map<String, Tensor<?>> result = graph1.andThen(graph2).apply(Collections.singletonMap("x", x));

			System.out.println("Result is: " + result.get("y2").intValue());
			// Result is: 40
		}

	}
}

