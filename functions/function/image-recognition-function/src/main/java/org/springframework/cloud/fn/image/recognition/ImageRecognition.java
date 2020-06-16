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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.tensorflow.Operand;
import org.tensorflow.Tensor;
import org.tensorflow.op.core.Placeholder;
import org.tensorflow.op.image.DecodeJpeg;
import org.tensorflow.op.nn.TopK;

import org.springframework.cloud.fn.common.tensorflow.GraphRunner;
import org.springframework.cloud.fn.common.tensorflow.GraphRunnerMemory;
import org.springframework.cloud.fn.common.tensorflow.ProtoBufGraphDefinition;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/**
 * @author Christian Tzolov
 */
public class ImageRecognition implements AutoCloseable {

	private final List<String> labels;
	private final GraphRunner imageNormalization;
	private final GraphRunner imageRecognition;
	private final GraphRunner maxProbability;
	private final GraphRunner topKProbabilities;

	/**
	 * Instead of creating the {@link ImageRecognition} service explicitly via the constructor,
	 * you should consider the convenience factory methods below. E.g.
	 *
	 *  {@link #inception(String, int, int, boolean)}
	 *  {@link #mobileNetV1(String, int, int, boolean)}
	 *  {@link #mobileNetV2(String, int, int, boolean)}
	 *
	 * @param modelUri location of the pre-trained model to use.
	 * @param labelsUri location of the list fromMemory pre-trained categories used by the model.
	 * @param imageRecognitionGraphInputName name of the Model's input node to send the input image to.
	 * @param imageRecognitionGraphOutputName name of the Model's output node to retrieve the predictions from.
	 * @param imageHeight normalized image height.
	 * @param imageWidth normalized image width.
	 * @param mean mean value to normalize the input image.
	 * @param scale scale to normalize the input image.
	 * @param responseSize Max number of predictions per recognize.
	 * @param cacheModel if true the pre-trained model is cached on the local file system.
	 */
	public ImageRecognition(String modelUri, String labelsUri, int imageHeight, int imageWidth, float mean, float scale,
			String imageRecognitionGraphInputName, String imageRecognitionGraphOutputName, int responseSize, boolean cacheModel) {

		this.labels = labels(labelsUri);

		/**
		 * Normalizes the raw input image into format expected by the pre-trained Inception/MobileNetV1/MobileNetV2 models.
		 * Typically the model is trained fromMemory images scaled to certain size. Usually it is 224x224 pixels, but can be
		 * also 192x192, 160x160, 128128, 92x92. Use the (imageHeight, imageWidth) to set the desired size.
		 * The colors, represented as R, G, B in 1-byte each were converted to float using (Value - Mean)/Scale.
		 *
		 *  imageHeight normalized image height.
		 *  imageWidth normalized image width.
		 *  mean mean value to normalize the input image.
		 *  scale scale to normalize the input image.
		 */
		this.imageNormalization = new GraphRunner("raw_image", "normalized_image")
				.withGraphDefinition(tf -> {
					Placeholder<String> input = tf.withName("raw_image").placeholder(String.class);
					final Operand<Float> decodedImage =
							tf.dtypes.cast(tf.image.decodeJpeg(input, DecodeJpeg.channels(3L)), Float.class);
					final Operand<Float> resizedImage = tf.image.resizeBilinear(
							tf.expandDims(decodedImage, tf.constant(0)),
							tf.constant(new int[] { imageHeight, imageWidth }));
					tf.withName("normalized_image").math.div(tf.math.sub(resizedImage, tf.constant(mean)), tf.constant(scale));
				});

		this.imageRecognition = new GraphRunner(imageRecognitionGraphInputName, imageRecognitionGraphOutputName)
				.withGraphDefinition(new ProtoBufGraphDefinition(toResource(modelUri), cacheModel));

		this.maxProbability = new GraphRunner(Arrays.asList("recognition_result"), Arrays.asList("category", "probability"))
				.withGraphDefinition(tf -> {
					Placeholder<Float> input = tf.withName("recognition_result").placeholder(Float.class);
					tf.withName("category").math.argMax(input, tf.constant(1));
					tf.withName("probability").max(input, tf.constant(1));
				});

		this.topKProbabilities = new GraphRunner("recognition_result", "topK")
				.withGraphDefinition(tf -> {
					Placeholder<Float> input = tf.withName("recognition_result").placeholder(Float.class);
					tf.withName("topK").nn.topK(input, tf.constant(responseSize), TopK.sorted(true));
				});
	}

	/**
	 * Takes an byte encoded image and returns the most probable category recognized in the image along fromMemory its probability.
	 * @param inputImage Byte array encoded image to recognize.
	 * @return Returns a single map entry containing the names of the recognized categories as key and the confidence as value.
	 */
	public Map<String, Double> recognizeMax(byte[] inputImage) {

		try (Tensor inputTensor = Tensor.create(inputImage); GraphRunnerMemory memorize = new GraphRunnerMemory()) {

			Map<String, Tensor<?>> max = this.imageNormalization.andThen(memorize)
					.andThen(this.imageRecognition).andThen(memorize)
					.andThen(this.maxProbability).andThen(memorize)
					.apply(Collections.singletonMap("raw_image", inputTensor));

			long[] category = new long[1];
			max.get("category").copyTo(category);
			float[] probability = new float[1];
			max.get("probability").copyTo(probability);

			return Collections.singletonMap(labels.get((int) category[0]), Double.valueOf(probability[0]));
		}
	}

	/**
	 * Takes an byte encoded input image and returns the top K most probable categories recognized in the image
	 * along fromMemory their probabilities.
	 *
	 * @param inputImage Byte array encoded image to recognize.
	 * @return Returns a list of key-value pairs. Every key-value pair represents a single category recognized.
	 * The key stands for the name(s) of the category while the value states the confidence that there is an
	 * object of this category. The entries in the Map are ordered from the higher to the lower confidences.
	 */
	public Map<String, Double> recognizeTopK(byte[] inputImage) {

		try (Tensor inputTensor = Tensor.create(inputImage); GraphRunnerMemory memorize = new GraphRunnerMemory()) {

			Map<String, Tensor<?>> topKResults =
					this.imageNormalization.andThen(memorize)
							.andThen(this.imageRecognition).andThen(memorize)
							.andThen(this.topKProbabilities).andThen(memorize)
							.apply(Collections.singletonMap("raw_image", inputTensor));

			Tensor recognizedImagesTensor = memorize.getTensorMap().get(this.imageRecognition.getSingleFetchName());
			float[][] results = new float[(int) recognizedImagesTensor.shape()[0]][(int) recognizedImagesTensor.shape()[1]];
			recognizedImagesTensor.copyTo(results);

			Tensor<Float> topKTensor = topKResults.get("topK").expect(Float.class);
			float[][] topK = new float[(int) topKTensor.shape()[0]][(int) topKTensor.shape()[1]];
			topKTensor.copyTo(topK);

			float min = topK[0][topK[0].length - 1];

			Map<Float, Integer> valueToIndex = new HashMap<>();
			for (int i = 0; i < results[0].length; i++) {
				if (results[0][i] >= min) {
					valueToIndex.put(results[0][i], i);
				}
			}

			Map<String, Double> map = new LinkedHashMap<>();
			for (float tk : topK[0]) {
				map.put(labels.get(valueToIndex.get(tk)), (double) tk);
			}

			return map;
		}
	}

	private Resource toResource(String uri) {
		return new DefaultResourceLoader().getResource(uri);
	}

	/**
	 * Converts a labels resources into string list.
	 * @return Returns string lists. One line per different category.
	 */
	private List<String> labels(String labelsUri) {
		try (InputStream is = toResource(labelsUri).getInputStream()) {
			return Arrays.asList(StreamUtils.copyToString(is, Charset.forName("UTF-8")).split("\n"));
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to initialize the Vocabulary", e);
		}
	}

	/**
	 *
	 * The Inception graph uses "input" as input and "output" as output.
	 *
	 */
	public static ImageRecognition inception(String inceptionModelUri,
			int normalizedImageSize, int responseSize, boolean cacheModel) {
		return new ImageRecognition(inceptionModelUri, "classpath:/labels/inception_labels.txt",
				normalizedImageSize, normalizedImageSize, 117f, 1f,
				"input", "output",
				responseSize, cacheModel);
	}

	/**
	 * Convenience for MobileNetV2 pre-trained models:
	 * https://github.com/tensorflow/models/tree/master/research/slim/nets/mobilenet#pretrained-models
	 *
	 * The normalized image size is always square (e.g. H=W)
	 *
	 * The MobileNetV2 graph uses "input" as input and "MobilenetV2/Predictions/Reshape_1" as output.
	 *
	 * @param mobileNetV2ModelUri model uri
	 * @param normalizedImageSize Depends on the pre-trained model used. Usually 224px is used.
	 * @param responseSize Number of responses fot topK requests.
	 * @param cacheModel cache model
	 * @return ImageRecognition instance configured fromMemory a MobileNetV2 pre-trained model.
	 */
	public static ImageRecognition mobileNetV2(String mobileNetV2ModelUri,
			int normalizedImageSize, int responseSize, boolean cacheModel) {
		return new ImageRecognition(mobileNetV2ModelUri, "classpath:/labels/mobilenet_labels.txt",
				normalizedImageSize, normalizedImageSize, 0f, 127f,
				"input", "MobilenetV2/Predictions/Reshape_1",
				responseSize, cacheModel);
	}

	/**
	 * Convenience for MobileNetV1 pre-trained models:
	 * https://github.com/tensorflow/models/blob/master/research/slim/nets/mobilenet_v1.md#pre-trained-models
	 *
	 * The MobileNetV1 graph uses "input" as input and "MobilenetV1/Predictions/Reshape_1" as output.
	 *
	 */
	public static ImageRecognition mobileNetV1(String mobileNetV1ModelUri,
			int normalizedImageSize, int responseSize, boolean cacheModel) {
		return new ImageRecognition(mobileNetV1ModelUri, "classpath:/labels/mobilenet_labels.txt",
				normalizedImageSize, normalizedImageSize,
				0f, 127f,
				"input", "MobilenetV1/Predictions/Reshape_1",
				responseSize, cacheModel);
	}

	/**
	 * Convert image recognition results into {@link RecognitionResponse} domain list.
	 * @param recognitionMap map containing the category mames and its probability. Returned by the
	 * {@link ImageRecognition#recognizeMax(byte[])} and the ImageRecognition{@link #recognizeTopK(byte[])} methods
	 * @return List of {@link RecognitionResponse} objects representing the name-to-probability pairs in the input map.
	 */
	public static List<RecognitionResponse> toRecognitionResponse(Map<String, Double> recognitionMap) {
		return recognitionMap.entrySet().stream()
				.map(nameProbabilityPair -> new RecognitionResponse(nameProbabilityPair.getKey(), nameProbabilityPair.getValue()))
				.collect(Collectors.toList());
	}

	@Override
	public void close() {
		this.imageNormalization.close();
		this.imageRecognition.close();
		this.maxProbability.close();
		this.topKProbabilities.close();
	}
}
