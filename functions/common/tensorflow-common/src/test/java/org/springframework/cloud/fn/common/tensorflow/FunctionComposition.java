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

import java.util.Collections;
import java.util.Map;

import org.tensorflow.Tensor;

/**
 * @author Christian Tzolov
 */
public final class FunctionComposition {

	private FunctionComposition() {

	}

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

