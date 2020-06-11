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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.fn.common.tensorflow.deprecated.GraphicsUtils;
import org.springframework.cloud.fn.common.tensorflow.deprecated.TensorFlowService;
import org.springframework.cloud.fn.object.detection.domain.ObjectDetection;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;

/**
 * Convenience class that leverages the the {@link ObjectDetectionInputConverter}, {@link ObjectDetectionOutputConverter} and {@link TensorFlowService}
 * in combination fromMemory the Tensorflow Object Detection API (https://github.com/tensorflow/models/tree/master/research/object_detection)
 * models for detection objects in input images.
 *
 * All pre-trained models (https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md) and labels are supported.
 *
 * You can download pre-trained models directly from the zoo: https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md
 * Just use the URI notation: (zoo model tar.gz url)#(name of the frozen model file name). To speedup the bootstrap
 * performance you should consider downloading the models locally and use the file:/"path to my model" URI instead!
 *
 * The object category labels for the pre-trained models are available at: https://github.com/tensorflow/models/tree/master/research/object_detection/data
 * Use the labels applicable for the model. Also, for performance reasons you may consider to download the labels
 * and load them from file: instead.
 *
 * @author Christian Tzolov
 */
public class ObjectDetectionService {

	/** Default list of fetch names for Box models. */
	public static List<String> FETCH_NAMES = Arrays.asList(
			ObjectDetectionOutputConverter.DETECTION_SCORES, ObjectDetectionOutputConverter.DETECTION_CLASSES,
			ObjectDetectionOutputConverter.DETECTION_BOXES, ObjectDetectionOutputConverter.NUM_DETECTIONS);

	/** Default list of fetch names for mask supporting models. */
	public static List<String> FETCH_NAMES_WITH_MASKS = Arrays.asList(
			ObjectDetectionOutputConverter.DETECTION_SCORES, ObjectDetectionOutputConverter.DETECTION_CLASSES,
			ObjectDetectionOutputConverter.DETECTION_BOXES, ObjectDetectionOutputConverter.DETECTION_MASKS,
			ObjectDetectionOutputConverter.NUM_DETECTIONS);

	private final ObjectDetectionInputConverter inputConverter;
	private final ObjectDetectionOutputConverter outputConverter;
	private final TensorFlowService tensorFlowService;

	public ObjectDetectionService() {
		this("https://download.tensorflow.org/models/object_detection/ssdlite_mobilenet_v2_coco_2018_05_09.tar.gz#frozen_inference_graph.pb",
				"https://storage.googleapis.com/scdf-tensorflow-models/object-detection/mscoco_label_map.pbtxt",
				0.4f, false, true);
	}

	/**
	 * Convenience constructor that would initialize all necessary internal components.
	 * @param modelUri URI of the pre-trained, frozen Tensorflow model.
	 * @param labelsUri URI of the pre-trained category labels.
	 * @param confidence Confidence threshold. Only objects detected wth confidence above this threshold will be returned.
	 * @param withMasks If a Mask model is selected then you can use this flag to extract the instance segmentation masks as well.
	 */
	public ObjectDetectionService(String modelUri, String labelsUri,
			float confidence, boolean withMasks, boolean cacheModel) {
		this.inputConverter = new ObjectDetectionInputConverter();
		List<String> fetchNames = withMasks ? FETCH_NAMES_WITH_MASKS : FETCH_NAMES;
		this.outputConverter = new ObjectDetectionOutputConverter(
				new DefaultResourceLoader().getResource(labelsUri), confidence, fetchNames);
		this.tensorFlowService = new TensorFlowService(
				new DefaultResourceLoader().getResource(modelUri), fetchNames, cacheModel);
	}

	/**
	 * Generic constructor thea allow the converter to be pre-configured before passed to the service.
	 * @param inputConverter Converter from byte array to object detection input image tensor
	 * @param outputConverter Covets the object detection output tensors into {@link ObjectDetection } list
	 * @param tensorFlowService Java tensorflow runner instance
	 */
	public ObjectDetectionService(ObjectDetectionInputConverter inputConverter,
			ObjectDetectionOutputConverter outputConverter, TensorFlowService tensorFlowService) {
		this.inputConverter = inputConverter;
		this.outputConverter = outputConverter;
		this.tensorFlowService = tensorFlowService;
	}

	/**
	 * Detects objects in a single input image identified by its URI.
	 *
	 * @param imageUri input image's URI
	 * @return Returns a list of {@link ObjectDetection} domain objects representing detected objects
	 */
	public List<ObjectDetection> detect(String imageUri) {
		try (InputStream is = new DefaultResourceLoader().getResource(imageUri).getInputStream()) {
			return this.detect(StreamUtils.copyToByteArray(is));
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Failed to detect the image:" + imageUri, e);
		}
	}

	/**
	 * Detects objects in a single {@link BufferedImage}.
	 *
	 * @param image Input image to detect objects from.
	 * @param format Image format (e.g. jpg, png ...) to use when converting the buffer into byte array.
	 * @return Returns a list of {@link ObjectDetection} domain objects representing detected objects in the input image
	 */
	public List<ObjectDetection> detect(BufferedImage image, String format) {
		return this.detect(GraphicsUtils.toImageByteArray(image, format));
	}

	/**
	 * Detects objects from a single input image encoded as byte array.
	 *
	 * @param image Input image encoded as byte array
	 * @return Returns a list of {@link ObjectDetection} domain objects representing detected objects in the input image
	 */
	public List<ObjectDetection> detect(byte[] image) {
		return this.inputConverter.andThen(this.tensorFlowService).andThen(this.outputConverter).apply(new byte[][] { image }).get(0);
	}

	/**
	 * Uses detects objects from a batch of input images encoded as byte array.
	 *
	 * @param batchedImages Batch of input images encoded as byte arrays. First dimension is the batch size and second the image bytes.
	 * @return Returns list of lists. For every input image in the batch a list of {@link ObjectDetection} domain objects representing detected objects in the input image.
	 */
	public List<List<ObjectDetection>> detect(byte[][] batchedImages) {
		return this.inputConverter.andThen(this.tensorFlowService).andThen(this.outputConverter).apply(batchedImages);
	}
}
