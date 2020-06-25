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

package org.springframework.cloud.stream.app.processor.semantic.segmentation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("semantic.segmentation")
@Validated
public class SemanticSegmentationProcessorProperties {

	enum OutputType {
		/** Input image augmented with the segmentation mask on top. */
		blended,
		/** Image of the segmentation mask. */
		mask
	}

	/**
	 * pre-trained tensorflow semantic segmentation model.
	 */
	private String model = "https://download.tensorflow.org/models/deeplabv3_mnv2_cityscapes_train_2018_02_05.tar.gz#frozen_inference_graph.pb";

	/**
	 * Specifies the output image type. You can return either the input image with the computed mask overlay, or
	 * the mask alone.
	 */
	private OutputType outputType = OutputType.blended;

	/**
	 * Every pre-trained model is based on certain object color maps.
	 * The pre-defined options are:
	 *  - classpath:/colormap/citymap_colormap.json
	 *  - classpath:/colormap/ade20k_colormap.json
	 *  - classpath:/colormap/black_white_colormap.json
	 *  - classpath:/colormap/mapillary_colormap.json
	 */
	private String colorMapUri = "classpath:/colormap/citymap_colormap.json";

	/**
	 * The alpha color of the computed segmentation mask image.
	 */
	private float maskTransparency = 0.45f;

	/**
	 * save output image inn the local debugOutputPath path.
	 */
	private boolean debugOutput = false;

	private String debugOutputPath = "semantic-segmentation-result.png";

	public OutputType getOutputType() {
		return outputType;
	}

	public void setOutputType(OutputType outputType) {
		this.outputType = outputType;
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

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getColorMapUri() {
		return colorMapUri;
	}

	public void setColorMapUri(String colorMapUri) {
		this.colorMapUri = colorMapUri;
	}

	public float getMaskTransparency() {
		return maskTransparency;
	}

	public void setMaskTransparency(float maskTransparency) {
		this.maskTransparency = maskTransparency;
	}

}
