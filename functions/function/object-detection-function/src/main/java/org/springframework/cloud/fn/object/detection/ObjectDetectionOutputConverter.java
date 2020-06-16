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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.protobuf.TextFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tensorflow.Tensor;

import org.springframework.cloud.fn.object.detection.domain.ObjectDetection;
import org.springframework.cloud.fn.object.detection.protos.StringIntLabelMapOuterClass;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Converts the Tensorflow Object Detection result into {@link ObjectDetection} list.
 * The pre-trained Object Detection models (http://bit.ly/2osxMAY) produce 3 tensor outputs:
 *  (1) detection_classes - containing the ids of detected objects, (2) detection_scores - confidence probabilities of the
 *  detected object and (3) detection_boxes - the object bounding boxes withing the images.
 *
 *  The MASK based models provide to 2 additional tensors: (4) num_detections and (5) detection_masks.
 *
 * All outputs tensors are float arrays, having:
 * - 1 as the first dimension
 * - maxObjects as the second dimension
 * While boxesT will have 4 as the third dimension (2 sets of (x, y) coordinates).
 * This can be verified by looking at scoresT.shape() etc.
 *
 * The format detected classes (e.g. labels) names is defined by the 'string_int_labels_map.proto'. The input list
 * is available at: https://github.com/tensorflow/models/tree/master/research/object_detection/data
 *
 * @author Christian Tzolov
 */
public class ObjectDetectionOutputConverter implements Function<Map<String, Tensor<?>>, List<List<ObjectDetection>>> {

	private static final Log logger = LogFactory.getLog(ObjectDetectionOutputConverter.class);

	/** DETECTION_CLASSES. */
	public static final String DETECTION_CLASSES = "detection_classes";
	/** DETECTION_SCORES. */
	public static final String DETECTION_SCORES = "detection_scores";
	/** DETECTION_BOXES. */
	public static final String DETECTION_BOXES = "detection_boxes";
	/** DETECTION_MASKS. */
	public static final String DETECTION_MASKS = "detection_masks";
	/** NUM_DETECTIONS. */
	public static final String NUM_DETECTIONS = "num_detections";

	private final String[] labels;
	private float confidence;
	private List<String> modelFetch;

	public ObjectDetectionOutputConverter(Resource labelsResource, float confidence, List<String> modelFetch) {
		this.confidence = confidence;
		this.modelFetch = modelFetch;
		try {
			this.labels = loadLabels(labelsResource);
			Assert.notNull(this.labels, String.format("Failed to initialize object labels [%s].", labelsResource));
		}
		catch (Exception e) {
			throw new RuntimeException(String.format("Failed to initialize object labels [%s].", labelsResource), e);
		}

		logger.info(String.format("Object labels [%s] loaded.", labelsResource));
	}

	/**
	 * Loads object labels in the string_int_label_map.proto.
	 * @param labelsResource location of the labels as a resource
	 * @return
	 */
	private static String[] loadLabels(Resource labelsResource) throws Exception {
		try (InputStream is = labelsResource.getInputStream()) {
			String text = StreamUtils.copyToString(is, Charset.forName("UTF-8"));
			StringIntLabelMapOuterClass.StringIntLabelMap.Builder builder =
					StringIntLabelMapOuterClass.StringIntLabelMap.newBuilder();
			TextFormat.merge(text, builder);
			StringIntLabelMapOuterClass.StringIntLabelMap proto = builder.build();

			int maxLabelId = proto.getItemList().stream()
					.map(StringIntLabelMapOuterClass.StringIntLabelMapItem::getId)
					.max(Comparator.comparing(i -> i))
					.orElse(-1);

			String[] labelIdToNameMap = new String[maxLabelId + 1];
			for (StringIntLabelMapOuterClass.StringIntLabelMapItem item : proto.getItemList()) {
				if (!StringUtils.isEmpty(item.getDisplayName())) {
					labelIdToNameMap[item.getId()] = item.getDisplayName();
				}
				else {
					// Common practice is to set the name to a MID or Synsets Id. Synset is a set of synonyms that
					// share a common meaning: https://en.wikipedia.org/wiki/WordNet
					labelIdToNameMap[item.getId()] = item.getName();
				}
			}
			return labelIdToNameMap;
		}
	}

	@Override
	public List<List<ObjectDetection>> apply(Map<String, Tensor<?>> tensorMap) {

		try (Tensor<Float> scoresTensor = tensorMap.get(DETECTION_SCORES).expect(Float.class);
			Tensor<Float> classesTensor = tensorMap.get(DETECTION_CLASSES).expect(Float.class);
			Tensor<Float> boxesTensor = tensorMap.get(DETECTION_BOXES).expect(Float.class)
		) {
			// All these tensors have:
			// - 1 as the first dimension
			// - maxObjects as the second dimension
			// While boxesT will have 4 as the third dimension (2 sets of (x, y) coordinates).
			// This can be verified by looking at scoresT.shape() etc.
			int batchSize = (int) scoresTensor.shape()[0];
			int maxObjects = (int) scoresTensor.shape()[1];
			float[][] scores = scoresTensor.copyTo(new float[batchSize][maxObjects]);
			float[][] classes = classesTensor.copyTo(new float[batchSize][maxObjects]);
			float[][][] boxes = boxesTensor.copyTo(new float[batchSize][maxObjects][4]);

			List<List<ObjectDetection>> batchObjectDetections = new ArrayList<>();

			for (int batchIndex = 0; batchIndex < batchSize; batchIndex++) {


				List<ObjectDetection> objectDetections = new ArrayList<>();

				// Collect only the objects whose scores are at above the configured confidence threshold.
				for (int i = 0; i < scores[batchIndex].length; ++i) {
					if (scores[batchIndex][i] >= confidence) {
						String category = labels[(int) classes[batchIndex][i]];
						float score = scores[batchIndex][i];

						ObjectDetection od = new ObjectDetection();
						od.setName(category);
						od.setConfidence(score);
						od.setX1(boxes[batchIndex][i][1]);
						od.setY1(boxes[batchIndex][i][0]);
						od.setX2(boxes[batchIndex][i][3]);
						od.setY2(boxes[batchIndex][i][2]);
						od.setCid((int) classes[batchIndex][i]);

						// Mask allows image-segmentation
						if (modelFetch.contains(DETECTION_MASKS) && modelFetch.contains(NUM_DETECTIONS)) {
							Tensor<Float> masksTensor = tensorMap.get(DETECTION_MASKS).expect(Float.class);
							Tensor<Float> numDetections = tensorMap.get(NUM_DETECTIONS).expect(Float.class);
							float nd = numDetections.copyTo(new float[batchSize])[0];

							if (masksTensor != null) {
								long[] shape = masksTensor.shape();
								float[][][][] masks = masksTensor.copyTo(new float[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]]);
								od.setMask(masks[batchIndex][i]);
								logger.debug(String.format("Num detections: %s, Masks: %s", nd, masks));
							}
						}

						objectDetections.add(od);
					}
				}

				batchObjectDetections.add(objectDetections);
			}
			return batchObjectDetections;
		}
	}
}
