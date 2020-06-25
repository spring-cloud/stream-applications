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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.tensorflow.deprecated.JsonMapperFunction;
import org.springframework.cloud.fn.semantic.segmentation.SegmentationColorMap;
import org.springframework.cloud.fn.semantic.segmentation.SemanticSegmentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties(SemanticSegmentationProcessorProperties.class)
public class SemanticSegmentationProcessorConfiguration {

	private static final Log logger = LogFactory.getLog(SemanticSegmentationProcessorConfiguration.class);

	/**
	 * Output header name.
	 */
	public static final String SEMANTIC_SEGMENTATION_HEADER = "semantic_segmentation";

	@Bean
	public SemanticSegmentation semanticSegmentation(SemanticSegmentationProcessorProperties properties) {
		return new SemanticSegmentation(properties.getModel(),
				SegmentationColorMap.loadColorMap(properties.getColorMapUri()), null,
				properties.getMaskTransparency());
	}

	@Bean
	public Function<Message<byte[]>, Message<byte[]>> semanticSegmentationFunction(
			SemanticSegmentation semanticSegmentation,
			SemanticSegmentationProcessorProperties properties) {

		return input -> {
			// You can use file:, http: or classpath: to provide the path to the input image.
			byte[] inputImage = input.getPayload();

			byte[] outputImage = (properties.getOutputType() == SemanticSegmentationProcessorProperties.OutputType.blended) ?
					semanticSegmentation.blendMask(inputImage) : semanticSegmentation.maskImage(inputImage);

			long[][] maskPixels = semanticSegmentation.maskPixels(inputImage);
			String jsonMaskPixels = new JsonMapperFunction().apply(maskPixels);

			Message<byte[]> outMessage = MessageBuilder
					.withPayload(outputImage)
					.setHeader(SEMANTIC_SEGMENTATION_HEADER, jsonMaskPixels)
					.build();

			if (properties.isDebugOutput()) {
				try {
					IOUtils.write(outputImage, new FileOutputStream(properties.getDebugOutputPath()));
				}
				catch (IOException e) {
					logger.warn("Cloud not produce debug output", e);
				}
			}

			return outMessage;
		};
	}
}
