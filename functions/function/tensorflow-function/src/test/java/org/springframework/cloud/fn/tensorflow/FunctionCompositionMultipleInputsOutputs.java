package org.springframework.cloud.fn.tensorflow;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.tensorflow.Tensor;
import org.tensorflow.op.core.Placeholder;

/**
 * @author Christian Tzolov
 */
public class FunctionCompositionMultipleInputsOutputs {

	// y = (x * 2) + (x * 3)
	//
	// y11 = x1 * 2 , where x1 == x
	// y12 = x1 * 3 , where x1 == x
	// y2 = x21 + x22 , where x21 == y11, x22 == y12 and y == y2
	public static void main(String[] args) {
		try (
				GraphRunner graph1 = new GraphRunner(Arrays.asList("x1"), Arrays.asList("y11", "y12"))
						.withGraphDefinition(tf -> {
							Placeholder<Integer> x1 = tf.withName("x1").placeholder(Integer.class);
							tf.withName("y11").math.mul(x1, tf.constant(2));
							tf.withName("y12").math.mul(x1, tf.constant(3));
						});
				GraphRunner graph2 = new GraphRunner(Arrays.asList("x21", "x22"), Arrays.asList("y2"))
						.withGraphDefinition(tf -> tf.withName("y2").math.add(
								tf.withName("x21").placeholder(Integer.class),
								tf.withName("x22").placeholder(Integer.class)));
				Tensor x = Tensor.create(10);
		) {

			Map<String, Tensor<?>> result =
					graph1
							.andThen(Functions.rename(
									"y11", "x21",
									"y12", "x22"
							))
							.andThen(graph2)
							.apply(Collections.singletonMap("x", x));

			System.out.println("Result is: " + result.get("y2").intValue()); // Result is: 50
		}
	}
}

