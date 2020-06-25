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

package org.springframework.cloud.fn.semantic.segmentation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.tensorflow.Operand;
import org.tensorflow.Tensor;
import org.tensorflow.op.Ops;
import org.tensorflow.op.core.Gather;
import org.tensorflow.op.core.Placeholder;
import org.tensorflow.op.core.Squeeze;
import org.tensorflow.op.core.ZerosLike;
import org.tensorflow.op.dtypes.Cast;
import org.tensorflow.op.image.DecodeJpeg;
import org.tensorflow.op.image.ExtractJpegShape;
import org.tensorflow.op.math.Add;
import org.tensorflow.op.math.Div;
import org.tensorflow.op.math.Equal;
import org.tensorflow.types.UInt8;

import org.springframework.cloud.fn.common.tensorflow.Functions;
import org.springframework.cloud.fn.common.tensorflow.GraphRunner;
import org.springframework.cloud.fn.common.tensorflow.GraphRunnerMemory;
import org.springframework.cloud.fn.common.tensorflow.ProtoBufGraphDefinition;
import org.springframework.cloud.fn.common.tensorflow.deprecated.GraphicsUtils;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * @author Christian Tzolov
 */
public class SemanticSegmentation implements AutoCloseable {

	private static final long CHANNELS = 3;
	private static final float REQUIRED_INPUT_IMAGE_SIZE = 513f;
	private final GraphRunner imageNormalization;
	private final GraphRunner semanticSegmentation;
	private final GraphRunner maskImageEncoding;
	private final GraphRunner alphaBlending;
	private final Tensor<Integer> colorMapTensor;
	private final Tensor<Float> maskTransparencyTensor;

	@Override
	public void close() {
		this.imageNormalization.close();
		this.semanticSegmentation.close();
		this.maskImageEncoding.close();
		this.alphaBlending.close();

		this.colorMapTensor.close();
		this.maskTransparencyTensor.close();
	}

	public SemanticSegmentation(String modelUrl, int[][] colorMap, long[] labelFilter, float maskTransparency) {

		this.imageNormalization = new GraphRunner("input_image", "resized_image")
				.withGraphDefinition(tf -> {
					Placeholder<String> input = tf.withName("input_image").placeholder(String.class);
					ExtractJpegShape<Integer> imageShapeAndChannel = tf.image.extractJpegShape(input);
					Gather<Integer> imageShape = tf.gather(imageShapeAndChannel, tf.constant(new int[] { 0, 1 }), tf.constant(0));

					Cast<Float> maxSize = tf.dtypes.cast(tf.max(imageShape, tf.constant(0)), Float.class);
					Div<Float> scale = tf.math.div(tf.constant(REQUIRED_INPUT_IMAGE_SIZE), maxSize);
					Cast<Integer> newSize = tf.dtypes.cast(tf.math.mul(scale, tf.dtypes.cast(imageShape, Float.class)), Integer.class);

					final Operand<Float> decodedImage =
							tf.dtypes.cast(tf.image.decodeJpeg(input, DecodeJpeg.channels(CHANNELS)), Float.class);

					final Operand<Float> resizedImageFloat =
							tf.image.resizeBilinear(tf.expandDims(decodedImage, tf.constant(0)), newSize);

					tf.withName("resized_image").dtypes.cast(resizedImageFloat, UInt8.class);
				});

		this.semanticSegmentation = new GraphRunner("ImageTensor:0", "SemanticPredictions:0")
				.withGraphDefinition(new ProtoBufGraphDefinition(new DefaultResourceLoader().getResource(modelUrl), true));

		this.colorMapTensor = Tensor.create(colorMap).expect(Integer.class);

		this.maskImageEncoding = new GraphRunner(Arrays.asList("color_map", "mask_pixels"), Arrays.asList("mask_png", "mask_rgb"))
				.withGraphDefinition(tf -> {
					Placeholder<Integer> colorTable = tf.withName("color_map").placeholder(Integer.class);

					Placeholder<Long> batchedMask = tf.withName("mask_pixels").placeholder(Long.class);
					// Remove batch dimension
					Squeeze<Long> mask = tf.squeeze(batchedMask, Squeeze.axis(Arrays.asList(0L)));

					Operand<Long> filteredMask = labelFilter(tf, mask, labelFilter);

					// The mask can contain label values larger than the list of colors provided in the color map.
					// To avoid out-of-index errors we will "normalize" the label values in the mask to MOD max-color-table-value.
					Operand<Long> mask3 = NativeImageUtils.normalizeMaskLabels(tf, colorTable, filteredMask);

					Gather<Integer> maskRgb = tf.withName("mask_rgb").gather(colorTable, mask3, tf.constant(0));

					Operand<String> png = tf.withName("mask_png").image.encodePng(tf.dtypes.cast(maskRgb, UInt8.class));

				});

		this.maskTransparencyTensor = Tensor.create(maskTransparency).expect(Float.class);

		this.alphaBlending = new GraphRunner(
				Arrays.asList("input_image", "mask_image", "mask_transparency"), Arrays.asList("blended_png"))
				.withGraphDefinition(tf -> {
					// Input image [B, H, W, 3]
					Cast<Float> inputImageRgb = tf.dtypes.cast(tf.withName("input_image").placeholder(UInt8.class), Float.class);

					Placeholder<Integer> a = tf.withName("mask_image").placeholder(Integer.class);
					Cast<Float> maskRgb = tf.dtypes.cast(a, Float.class);

					Squeeze<Float> inputImageRgb2 = tf.squeeze(inputImageRgb, Squeeze.axis(Arrays.asList(0L)));

					Placeholder<Float> maskTransparencyHolder = tf.withName("mask_transparency").placeholder(Float.class);

					// Blend the transparent maskImage on top of the input image.
					Operand<Float> blended = NativeImageUtils.alphaBlending(tf, maskRgb, inputImageRgb2, maskTransparencyHolder);

					// Cut
					//Operand<Boolean> condition = tf.math.equal(a, tf.zerosLike(a));
					//Operand<Float> blended = tf.where3(condition, tf.zerosLike(maskRgb), inputImageRgb2);

					// Encode PNG
					tf.withName("blended_png").image.encodePng(tf.dtypes.cast(blended, UInt8.class));

				});
	}

	public byte[] blendMask(byte[] image) {
		try (Tensor inputTensor = Tensor.create(image); GraphRunnerMemory memory = new GraphRunnerMemory()) {

			Map<String, Tensor<?>> blendedTensors =
					this.imageNormalization.andThen(memory)                         // (input_image) -> (resized_image)   and memorize (resized_image)
							.andThen(this.semanticSegmentation).andThen(memory)     // (ImageTensor:0) -> (SemanticPredictions:0) and memorize (SemanticPredictions:0)
							.andThen(Functions.rename("SemanticPredictions:0", "mask_pixels")) // (SemanticPredictions:0) -> (mask_pixels)
							.andThen(Functions.enrichWith("color_map", this.colorMapTensor))  // (mask_pixels) -> (mask_pixels, color_map)
							.andThen(this.maskImageEncoding).andThen(memory)        // (color_map, mask_pixels) -> (mask_png, mask_rgb) and memorize (mask_png, mask_rgb)
							.andThen(Functions.enrichFromMemory(
									memory, "resized_image"))     // (mask_png, mask_rgb) -> (mask_png, mask_rgb, resized_image), e.g. join the normalizedImageTensor
							.andThen(Functions.rename(
									"resized_image", "input_image",
									"mask_rgb", "mask_image"))                     // (mask_png, mask_rgb, resized_image) -> (mask_image, input_image)
							.andThen(Functions.enrichWith("mask_transparency", this.maskTransparencyTensor)) // (mask_image, input_image) ->  (mask_image, input_image, mask_transparency)
							.andThen(this.alphaBlending).andThen(memory)           // (mask_image, input_image, mask_transparency) -> (blended_png)
							.apply(Collections.singletonMap("input_image", inputTensor)); // () -> (input_image)

			byte[] blendedImage = blendedTensors.get("blended_png").bytesValue();

			memory.getTensorMap().entrySet().stream().forEach(e -> System.out.println(e));

			return blendedImage;
		}
	}

	public long[][] maskPixels(byte[] image) {
		try (Tensor inputTensor = Tensor.create(image); GraphRunnerMemory memory = new GraphRunnerMemory()) {

			return this.imageNormalization.andThen(memory)                       // (input_image) -> (resized_image)   and memorize (resized_image)
					.andThen(this.semanticSegmentation).andThen(memory)          // (ImageTensor:0) -> (SemanticPredictions:0) and memorize (SemanticPredictions:0)
					.andThen(tensorMap -> {
						Tensor<?> maskTensor = tensorMap.get("SemanticPredictions:0");
						int width = (int) maskTensor.shape()[1];
						int height = (int) maskTensor.shape()[2];
						return maskTensor.copyTo(new long[1][width][height])[0]; // 1 == batch size
					})
					.apply(Collections.singletonMap("input_image", inputTensor)); // () -> (input_image)
		}
	}

	public byte[] maskImage(byte[] image) {

		try (Tensor inputTensor = Tensor.create(image); GraphRunnerMemory memory = new GraphRunnerMemory()) {

			return this.imageNormalization.andThen(memory)                        // (input_image) -> (resized_image)   and memorize (resized_image)
					.andThen(this.semanticSegmentation).andThen(memory)           // (ImageTensor:0) -> (SemanticPredictions:0) and memorize (SemanticPredictions:0)
					.andThen(Functions.rename("SemanticPredictions:0", "mask_pixels")) // (SemanticPredictions:0) -> (mask_pixels)
					.andThen(Functions.enrichWith("color_map", this.colorMapTensor))  // (mask_pixels) -> (mask_pixels, color_map)
					.andThen(this.maskImageEncoding).andThen(memory)              // (color_map, mask_pixels) -> (mask_png, mask_rgb) and memorize (mask_png, mask_rgb)
					.andThen(tensorMap -> tensorMap.get("mask_png").bytesValue())
					.apply(Collections.singletonMap("input_image", inputTensor)); // () -> (input_image)
		}
	}

	private Operand<Long> labelFilter(Ops tf, Operand<Long> mask, long[] labels) {

		if (labels == null || labels.length == 0) {
			return mask;
		}

		ZerosLike<Long> zeroMask = tf.zerosLike(mask);
		Operand<Long> result = zeroMask;
		for (long label : labels) {
			Add<Long> labelMask = tf.math.add(tf.zerosLike(mask), tf.constant(label));
			Equal condition = tf.math.equal(mask, labelMask);
			result = tf.math.add(result, tf.where3(condition, labelMask, zeroMask));
		}
		return result;
	}

	public static void main(String[] args) throws IOException {

		try (SemanticSegmentation segmentationService = new SemanticSegmentation(
				"https://download.tensorflow.org/models/deeplabv3_mnv2_cityscapes_train_2018_02_05.tar.gz#frozen_inference_graph.pb",
				SegmentationColorMap.loadColorMap("classpath:/colormap/citymap_colormap.json"), null, 0.45f)
		) {
			byte[] inputImage = GraphicsUtils.loadAsByteArray("classpath:/images/amsterdam-cityscape1.jpg");

			// 1. Mask pixels
			long[][] maskPixels = segmentationService.maskPixels(inputImage);

			String json = new ObjectMapper().writeValueAsString(maskPixels);

			// 2. Alpha Blending
			byte[] blended = segmentationService.blendMask(inputImage);
			ImageIO.write(ImageIO.read(new ByteArrayInputStream(blended)), "png",
					new File("./functions/function/semantic-segmentation-function/target/blendedImage.png"));

			// 3. Mask Image
			byte[] maskImage = segmentationService.maskImage(inputImage);
			ImageIO.write(ImageIO.read(new ByteArrayInputStream(maskImage)), "png",
					new File("./functions/function/semantic-segmentation-function/target/maskImage.png"));
		}

		try (SemanticSegmentation segmentationService = new SemanticSegmentation(
				"https://download.tensorflow.org/models/deeplabv3_xception_ade20k_train_2018_05_29.tar.gz#frozen_inference_graph.pb",
				SegmentationColorMap.loadColorMap("classpath:/colormap/ade20k_colormap.json"), null, 0.45f)
		) {
			byte[] inputImage = GraphicsUtils.loadAsByteArray("classpath:/images/interior.jpg");

			// 1. Mask pixels
			long[][] maskPixels = segmentationService.maskPixels(inputImage);

			// 2. Alpha Blending
			byte[] blended = segmentationService.blendMask(inputImage);
			ImageIO.write(ImageIO.read(new ByteArrayInputStream(blended)), "png",
					new File("./functions/function/semantic-segmentation-function/target/inventory-blendedImage.png"));

			// 3. Mask Image
			byte[] maskImage = segmentationService.maskImage(inputImage);
			ImageIO.write(ImageIO.read(new ByteArrayInputStream(maskImage)), "png",
					new File("./functions/function/semantic-segmentation-function/target/inventory-MaskImage.png"));
		}

		try (SemanticSegmentation segmentationService = new SemanticSegmentation(
				"https://download.tensorflow.org/models/deeplabv3_mnv2_pascal_trainval_2018_01_29.tar.gz#frozen_inference_graph.pb",
				SegmentationColorMap.loadColorMap("classpath:/colormap/black_white_colormap.json"), null, 0.45f)
		) {
			byte[] inputImage = GraphicsUtils.loadAsByteArray("classpath:/images/VikiMaxiAdi.jpg");

			// 1. Mask pixels
			long[][] maskPixels = segmentationService.maskPixels(inputImage);

			// 2. Alpha Blending
			byte[] blended = segmentationService.blendMask(inputImage);
			ImageIO.write(ImageIO.read(new ByteArrayInputStream(blended)), "png",
					new File("./functions/function/semantic-segmentation-function/target/pascal-blendedImage.png"));

			// 3. Mask Image
			byte[] maskImage = segmentationService.maskImage(inputImage);
			ImageIO.write(ImageIO.read(new ByteArrayInputStream(maskImage)), "png",
					new File("./functions/function/semantic-segmentation-function/target/pascal-MaskImage.png"));
		}

	}
}
