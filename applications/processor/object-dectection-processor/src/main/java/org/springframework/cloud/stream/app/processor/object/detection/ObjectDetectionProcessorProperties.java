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

package org.springframework.cloud.stream.app.processor.object.detection;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("object.detection")
@Validated
public class ObjectDetectionProcessorProperties {

	/**
	 * pre-trained tensorflow object detection model.
	 */
	private String model = "https://download.tensorflow.org/models/object_detection/ssdlite_mobilenet_v2_coco_2018_05_09.tar.gz#frozen_inference_graph.pb";

	/**
	 * Labels URI.
	 */
	private String labels = "https://storage.googleapis.com/scdf-tensorflow-models/object-detection/mscoco_label_map.pbtxt";

	private float confidence = 0.4f;

	private boolean withMasks;

	private boolean cacheModel = true;

	private boolean debugOutput = false;

	private String debugOutputPath = "object-detection-result.png";

	private int responseSize = Integer.MAX_VALUE;

	public boolean isDebugOutput() {
		return debugOutput;
	}

	public void setDebugOutput(boolean debugOutput) {
		this.debugOutput = debugOutput;
	}

	public String getDebugOutputPath() {
		return debugOutputPath;
	}

	public void setDebugOutputPath(String debugOutputPath) {
		this.debugOutputPath = debugOutputPath;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getLabels() {
		return labels;
	}

	public void setLabels(String labels) {
		this.labels = labels;
	}

	public float getConfidence() {
		return confidence;
	}

	public void setConfidence(float confidence) {
		this.confidence = confidence;
	}

	public boolean isWithMasks() {
		return withMasks;
	}

	public void setWithMasks(boolean withMasks) {
		this.withMasks = withMasks;
	}

	public boolean isCacheModel() {
		return cacheModel;
	}

	public void setCacheModel(boolean cacheModel) {
		this.cacheModel = cacheModel;
	}

	public int getResponseSize() {
		return responseSize;
	}

	public void setResponseSize(int responseSize) {
		this.responseSize = responseSize;
	}
}
