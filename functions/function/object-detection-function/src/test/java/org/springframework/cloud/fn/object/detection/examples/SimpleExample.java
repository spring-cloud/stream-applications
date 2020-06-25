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


import java.util.List;

import org.springframework.cloud.fn.object.detection.ObjectDetectionService;
import org.springframework.cloud.fn.object.detection.domain.ObjectDetection;

/**
 * @author Christian Tzolov
 */
public class SimpleExample {

	public static void main(String[] args) {
		// Select a pre-trained model from the model zoo: https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md
		// Just use the notation <model zoo url>#<name of the frozen model file in the zoo's tar.gz>
		String model = "https://download.tensorflow.org/models/object_detection/ssd_mobilenet_v1_ppn_shared_box_predictor_300x300_coco14_sync_2018_07_03.tar.gz#frozen_inference_graph.pb";

		// All labels for the pre-trained models are available at: https://github.com/tensorflow/models/tree/master/research/object_detection/data
		String labels = "https://raw.githubusercontent.com/tensorflow/models/master/research/object_detection/data/mscoco_label_map.pbtxt";

		ObjectDetectionService detectionService = new ObjectDetectionService(model, labels,
				0.4f, // Only object fromMemory confidence above the threshold are returned. Confidence range is [0, 1].
				false, // No instance segmentation
				true); // cache the TF model locally

		// You can use file:, http: or classpath: to provide the path to the input image.
		List<ObjectDetection> detectedObjects = detectionService.detect("classpath:/images/object-detection.jpg");
		detectedObjects.stream().map(o -> o.toString()).forEach(System.out::println);
	}
}
