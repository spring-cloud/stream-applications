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

package org.springframework.cloud.fn.image.recognition;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import org.springframework.cloud.fn.common.tensorflow.deprecated.GraphicsUtils;

/**
 * @author Christian Tzolov
 */
public final class ImageRecognitionExample2 {

	private ImageRecognitionExample2() {

	}

	public static void main(String[] args) throws IOException {

		ImageRecognitionAugmenter augmenter = new ImageRecognitionAugmenter();

		byte[] inputImage = GraphicsUtils.loadAsByteArray("classpath:/images/giant_panda_in_beijing_zoo_1.jpg");

		ImageRecognition inceptions = ImageRecognition.inception(
				"https://storage.googleapis.com/scdf-tensorflow-models/image-recognition/tensorflow_inception_graph.pb",
				224, 10, true);
		System.out.println(inceptions.recognizeMax(inputImage));
		System.out.println(inceptions.recognizeTopK(inputImage));
		System.out.println(ImageRecognition.toRecognitionResponse(inceptions.recognizeTopK(inputImage)));

		IOUtils.write(augmenter.apply(inputImage, ImageRecognition.toRecognitionResponse(inceptions.recognizeTopK(inputImage))),
				new FileOutputStream("./functions/function/image-recognition-function/target/image-augmented-inceptions.jpg"));
		inceptions.close();

		ImageRecognition mobileNetV2 = ImageRecognition.mobileNetV2(
				"https://storage.googleapis.com/mobilenet_v2/checkpoints/mobilenet_v2_1.4_224.tgz#mobilenet_v2_1.4_224_frozen.pb",
				224, 10, true);
		System.out.println(mobileNetV2.recognizeMax(inputImage));
		System.out.println(mobileNetV2.recognizeTopK(inputImage));
		IOUtils.write(augmenter.apply(inputImage, ImageRecognition.toRecognitionResponse(mobileNetV2.recognizeTopK(inputImage))),
				new FileOutputStream("./functions/function/image-recognition-function/target/image-augmented-mobilnetV2.jpg"));
		mobileNetV2.close();

		ImageRecognition mobileNetV1 = ImageRecognition.mobileNetV1(
				"https://download.tensorflow.org/models/mobilenet_v1_2018_08_02/mobilenet_v1_1.0_224.tgz#mobilenet_v1_1.0_224_frozen.pb",
				224, 10, true);
		System.out.println(mobileNetV1.recognizeMax(inputImage));
		System.out.println(mobileNetV1.recognizeTopK(inputImage));
		IOUtils.write(augmenter.apply(inputImage, ImageRecognition.toRecognitionResponse(mobileNetV1.recognizeTopK(inputImage))),
				new FileOutputStream("./functions/function/image-recognition-function/target/image-augmented-mobilnetV1.jpg"));
		mobileNetV1.close();
	}
}
