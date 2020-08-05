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

package org.springframework.cloud.stream.app.processor.image.recognition;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("image.recognition")
@Validated
public class ImageRecognitionProcessorProperties {

	enum ModelType {
		inception,
		mobilenetv1,
		mobilenetv2
	}

	/**
	 * Supports three different pre-trained tensorflow image recognition models: Inception, MobileNetV1 and MobileNetV2
	 *
	 * 1. Inception graph uses 'input' as input and 'output' as output.
	 * 2. MobileNetV2 pre-trained models: https://github.com/tensorflow/models/tree/master/research/slim/nets/mobilenet#pretrained-models
	 * 	 - normalized image size is always square (e.g. H=W)
	 * 	 - graph uses 'input' as input and 'MobilenetV2/Predictions/Reshape_1' as output.
	 *  3. MobileNetV1 pre-trained models: https://github.com/tensorflow/models/blob/master/research/slim/nets/mobilenet_v1.md#pre-trained-models
	 * 	 - graph uses 'input' as input and 'MobilenetV1/Predictions/Reshape_1' as output.
	 */
	private ModelType modelType = ModelType.mobilenetv2;

	/**
	 * pre-trained tensorflow image recognition model. Note that the model must match the selected model type!
	 */
	private String model = "https://storage.googleapis.com/mobilenet_v2/checkpoints/mobilenet_v2_1.4_224.tgz#mobilenet_v2_1.4_224_frozen.pb";

	/**
	 * cache the pre-trained tensorflow model.
	 */
	private boolean cacheModel = true;

	/**
	 * Normalized image size.
	 */
	private int normalizedImageSize = 224;

	/**
	 * number of recognized images.
	 */
	private int responseSize = 5;

	private boolean debugOutput = false;

	private String debugOutputPath = "image-recognition-result.png";

	public ModelType getModelType() {
		return modelType;
	}

	public void setModelType(ModelType modelType) {
		this.modelType = modelType;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public boolean isCacheModel() {
		return cacheModel;
	}

	public void setCacheModel(boolean cacheModel) {
		this.cacheModel = cacheModel;
	}

	public int getNormalizedImageSize() {
		return normalizedImageSize;
	}

	public void setNormalizedImageSize(int normalizedImageSize) {
		this.normalizedImageSize = normalizedImageSize;
	}

	public int getResponseSize() {
		return responseSize;
	}

	public void setResponseSize(int responseSize) {
		this.responseSize = responseSize;
	}

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
}
