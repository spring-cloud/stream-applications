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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tensorflow.Tensor;
import org.tensorflow.types.UInt8;

import org.springframework.cloud.fn.common.tensorflow.deprecated.GraphicsUtils;

/**
 * Converts byte array image into a input Tensor for the Object Detection API. The computed image tensors uses the
 * 'image_tensor' model placeholder.
 *
 * @author Christian Tzolov
 */
public class ObjectDetectionInputConverter implements Function<byte[][], Map<String, Tensor<?>>> {

	private static final Log logger = LogFactory.getLog(ObjectDetectionInputConverter.class);

	private static final long CHANNELS = 3;

	/** Make checkstyle happy. **/
	public static final String IMAGE_TENSOR_FEED_NAME = "image_tensor";

	@Override
	public Map<String, Tensor<?>> apply(byte[][] input) {
		return Collections.singletonMap(IMAGE_TENSOR_FEED_NAME, makeImageTensor(input));
	}

	private static Tensor<UInt8> makeImageTensor(byte[][] imageBytesArray) {
		try {
			int batchSize = imageBytesArray.length;
			ByteBuffer byteBuffer = null;
			long[] shape = null;
			for (int batchIndex = 0; batchIndex < batchSize; batchIndex++) {
				byte[] imageBytes = imageBytesArray[batchIndex];
				ByteArrayInputStream is = new ByteArrayInputStream(imageBytes);
				BufferedImage img = ImageIO.read(is);

				if (img.getType() != BufferedImage.TYPE_3BYTE_BGR) {
					img = GraphicsUtils.toBufferedImageType(img, BufferedImage.TYPE_3BYTE_BGR);
				}

				if (byteBuffer == null) {
					byteBuffer = ByteBuffer.allocate((int) (batchSize * img.getHeight() * img.getWidth() * CHANNELS));
					shape = new long[] { batchSize, img.getHeight(), img.getWidth(), CHANNELS };
				}

				byte[] data = ((DataBufferByte) img.getData().getDataBuffer()).getData();
				// ImageIO.read produces BGR-encoded images, while the model expects RGB.
				bgrToRgb(data);
				byteBuffer.put(data);
			}
			byteBuffer.flip();

			return Tensor.create(UInt8.class, shape, byteBuffer);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Incorrect image format", e);
		}

	}

	private static void bgrToRgb(byte[] data) {
		for (int i = 0; i < data.length; i += 3) {
			byte tmp = data[i];
			data[i] = data[i + 2];
			data[i + 2] = tmp;
		}
	}
}
