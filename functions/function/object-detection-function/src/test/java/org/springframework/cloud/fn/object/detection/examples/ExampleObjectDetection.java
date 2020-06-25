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

package org.springframework.cloud.fn.object.detection.examples;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

import org.springframework.cloud.fn.common.tensorflow.deprecated.JsonMapperFunction;
import org.springframework.cloud.fn.object.detection.ObjectDetectionImageAugmenter;
import org.springframework.cloud.fn.object.detection.ObjectDetectionService;
import org.springframework.cloud.fn.object.detection.domain.ObjectDetection;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;

/**
 * @author Christian Tzolov
 */
public class ExampleObjectDetection {

	public static void main(String[] args) throws IOException {

		// You can download pre-trained models directly from the zoo: https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md
		// Just use the notation <zoo model tar.gz url>#<name of the frozen model file name>
		// For performance reasons you may consider downloading the model locally and use the file:/<path to my model> URI instead!
		String model = "https://download.tensorflow.org/models/object_detection/faster_rcnn_nas_coco_2018_01_28.tar.gz#frozen_inference_graph.pb";
		//Resource model = resourceLoader.getResource("https://download.tensorflow.org/models/object_detection/faster_rcnn_resnet101_fgvc_2018_07_19.tar.gz#frozen_inference_graph.pb");
		//Resource model = resourceLoader.getResource("https://download.tensorflow.org/models/object_detection/faster_rcnn_resnet50_fgvc_2018_07_19.tar.gz#frozen_inference_graph.pb");

		// All labels for the pre-trained models are available at:
		// https://github.com/tensorflow/models/tree/master/research/object_detection/data
		// Use the labels applicable for the model.
		// Also, for performance reasons you may consider to download the labels and load them from file: instead.
		String labels = "https://raw.githubusercontent.com/tensorflow/models/master/research/object_detection/data/mscoco_label_map.pbtxt";
		//Resource labels = resourceLoader.getResource("https://raw.githubusercontent.com/tensorflow/models/master/research/object_detection/data/fgvc_2854_classes_label_map.pbtxt");

		// You can cache the TF model on the local file system to improve the bootstrap performance on consecutive runs!
		boolean CACHE_TF_MODEL = true;

		// For the pre-trained models fromMemory mask you can set the INSTANCE_SEGMENTATION to enable object instance segmentation as well
		boolean NO_INSTANCE_SEGMENTATION = false;

		// Only object fromMemory confidence above the threshold are returned
		float CONFIDENCE_THRESHOLD = 0.4f;

		ObjectDetectionService detectionService =
				new ObjectDetectionService(model, labels, CONFIDENCE_THRESHOLD, NO_INSTANCE_SEGMENTATION, CACHE_TF_MODEL);

		// You can use file:, http: or classpath: to provide the path to the input image.
		String inputImageUri = "classpath:/images/object-detection.jpg";
		try (InputStream is = new DefaultResourceLoader().getResource(inputImageUri).getInputStream()) {

			byte[] image = StreamUtils.copyToByteArray(is);

			// Returns a list ObjectDetection domain classes to allow programmatic accesses to the detected objects's metadata
			List<ObjectDetection> detectedObjects = detectionService.detect(image);

			// Get JSON representation of the detected objects
			String jsonObjectDetections = new JsonMapperFunction().apply(detectedObjects);
			System.out.println(jsonObjectDetections);

			// Draw the detected object metadata on top of the original image and store the result
			byte[] annotatedImage = new ObjectDetectionImageAugmenter(NO_INSTANCE_SEGMENTATION).apply(image, detectedObjects);
			IOUtils.write(annotatedImage, new FileOutputStream("./object-detection-function/target/object-detection-augmented.jpg"));
		}
	}
}
