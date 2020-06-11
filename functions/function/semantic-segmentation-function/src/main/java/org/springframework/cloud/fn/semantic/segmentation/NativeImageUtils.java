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

import java.util.Arrays;

import org.tensorflow.Operand;
import org.tensorflow.op.Ops;
import org.tensorflow.op.core.Concat;
import org.tensorflow.op.core.ExpandDims;
import org.tensorflow.op.core.Gather;
import org.tensorflow.op.core.Range;
import org.tensorflow.op.core.ReduceMax;
import org.tensorflow.op.core.Tile;
import org.tensorflow.op.math.Add;
import org.tensorflow.op.math.Mul;
import org.tensorflow.op.math.Sub;

/**
 * @author Christian Tzolov
 */
public final class NativeImageUtils {

	private NativeImageUtils() {
	}

	/**
	 * grayscaleToRgb.
	 * https://github.com/tensorflow/tensorflow/blob/r1.13/tensorflow/python/ops/image_ops_impl.py#L1536
	 */
	public static <T> Operand<T> grayscaleToRgb(Ops tf, Operand<T> images) {
		ExpandDims<Integer> rank_1 = tf.expandDims(
				tf.math.sub(tf.rank(images), tf.constant(1)),
				tf.constant(0));
		// Create once 1D vector of the shape defined by the rank_1.
		// E.g. for rank [2] will produce matrix [1, 1]. For [3] rank will produce a cube [1, 1, 1]
		Add<Integer> ones = tf.math.add(tf.zeros(rank_1, Integer.class), tf.constant(1));
		// Convert scalar 3 into 1D array [3]
		ExpandDims<Integer> channelsAs1D = tf.expandDims(tf.constant(3), tf.constant(0));
		Concat<Integer> shapeList = tf.concat(Arrays.asList(ones, channelsAs1D), tf.constant(0));
		Tile<T> tile = tf.withName("grayscaleToRgb").tile(images, shapeList);
		return tile;
	}

	public static Operand<Float> normalizeMask(Ops tf, Operand<Float> mask, float newValue) {
		// generate array representing the axis indexes.
		// For example of tensor of rank K the axisRange is {0, 1, 2 ...K}
		Range<Integer> axisRange = tf.range(tf.constant(0),  // from
				tf.dtypes.cast(tf.rank(mask), Integer.class), // to
				tf.constant(1)); // step

		ReduceMax<Float> max = tf.reduceMax(mask, axisRange);
		//Mul<Float> input2Float1 = tf.math.mul(tf.math.div(input2Float, max), tf.constant(1f));
		Mul<Float> normalizedMask = tf.math.mul(tf.math.div(mask, max), tf.constant(newValue));

		return normalizedMask;
	}

	/**
	 * Alpha Blending .
	 * https://en.wikipedia.org/wiki/Alpha_compositing#Alpha_blending
	 */
	public static Operand<Float> alphaBlending(Ops tf, Operand<Float> srcRgb, Operand<Float> dstRgb, Operand<Float> srcAlpha) {
		Sub<Float> alpha = tf.math.sub(tf.onesLike(srcRgb), srcAlpha);
		Mul<Float> src = tf.math.mul(srcRgb, alpha);
		Mul<Float> dst = tf.math.mul(dstRgb, tf.math.sub(tf.constant(1.0f), alpha));
		Add<Float> out = tf.math.add(dst, src);

		//Mul<Float> out = tf.math.mul(srcRgbNormalized, dstRgb);
		//Squeeze<Float> squeeze = tf.withName("squeeze").squeeze(out, Squeeze.axis(Arrays.asList(0L)));

		return out;
	}

	/**
	 * The mask can contain label values larger than the list of colors provided in the color map.
	 * To avoid out-of-index errors we will "normalize" the label values in the mask to MOD max-color-table-value.
	 * @param tf - tensorflow
	 * @param colorTable Color map of shape [n, 3]. n is the count of label entries and 3 is the RGB color assigned
	 *                   to that label.
	 * @param mask Mask of shape [h, w] containing label vales.
	 * @return Mask of shape [h, w] fromMemory values normalized between [0, n]
	 */
	public static Operand<Long> normalizeMaskLabels(Ops tf, Operand<Integer> colorTable, Operand<Long> mask) {
		// The mask can contain label values larger than the list of colors provided in the color map.
		// To avoid out-of-index errors we will "normalize" the label values in the mask to MOD max-color-table-value.
		Sub<Long> colorTableShape = tf.math.sub(tf.shape(colorTable, Long.class), tf.constant(1L));
		// Color tables have shape [N, 3], where N is the count of label entries. Therefore the max label id is (N - 1).
		Gather<Long> colorTableSize = tf.gather(colorTableShape, tf.constant(new int[] { 0 }), tf.constant(0));
		// Normalize the label values in the mask so they don't exceed the max value in the color map.
		return tf.math.mod(mask, colorTableSize);
	}
}
