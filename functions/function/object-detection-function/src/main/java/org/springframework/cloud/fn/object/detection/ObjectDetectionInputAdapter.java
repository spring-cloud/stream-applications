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

package org.springframework.cloud.fn.object.detection;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tensorflow.Operand;
import org.tensorflow.Tensor;
import org.tensorflow.op.core.Placeholder;
import org.tensorflow.op.image.DecodeJpeg;
import org.tensorflow.types.UInt8;

import org.springframework.cloud.fn.common.tensorflow.GraphRunner;

/**
 * Converts byte array image into a input Tensor for the Object Detection API.
 *
 * @author Christian Tzolov
 */
public class ObjectDetectionInputAdapter implements Function<byte[], Map<String, Tensor<?>>>, AutoCloseable {

	private static final Log logger = LogFactory.getLog(ObjectDetectionInputAdapter.class);

	/** Make checkstyle happy. **/
	public static final String RAW_IMAGE = "raw_image";
	/** Make checkstyle happy. **/
	public static final String NORMALIZED_IMAGE = "normalized_image";
	/** Make checkstyle happy. **/
	public static final long CHANNELS = 3;

	private final GraphRunner imageLoaderGraph;

	public ObjectDetectionInputAdapter() {

		this.imageLoaderGraph = new GraphRunner(RAW_IMAGE, NORMALIZED_IMAGE)
				.withGraphDefinition(tf -> {
					Placeholder<String> rawImage = tf.withName(RAW_IMAGE).placeholder(String.class);
					Operand<UInt8> decodedImage = tf.dtypes.cast(
							tf.image.decodeJpeg(rawImage, DecodeJpeg.channels(CHANNELS)), UInt8.class);
					// Expand dimensions since the model expects images to have shape: [1, H, W, 3]
					tf.withName(NORMALIZED_IMAGE).expandDims(decodedImage, tf.constant(0));
				});
	}

	@Override
	public Map<String, Tensor<?>> apply(byte[] inputImage) {
		try (Tensor inputTensor = Tensor.create(inputImage)) {
			return this.imageLoaderGraph.apply(Collections.singletonMap(RAW_IMAGE, inputTensor));
		}
	}

	@Override
	public void close() {
		if (this.imageLoaderGraph != null) {
			this.imageLoaderGraph.close();
		}
	}
}
