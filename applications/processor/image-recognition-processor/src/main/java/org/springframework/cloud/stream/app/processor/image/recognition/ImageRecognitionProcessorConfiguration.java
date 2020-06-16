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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.tensorflow.deprecated.JsonMapperFunction;
import org.springframework.cloud.fn.image.recognition.ImageRecognition;
import org.springframework.cloud.fn.image.recognition.ImageRecognitionAugmenter;
import org.springframework.cloud.fn.image.recognition.RecognitionResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties(ImageRecognitionProcessorProperties.class)
public class ImageRecognitionProcessorConfiguration {

	private static final Log logger = LogFactory.getLog(ImageRecognitionProcessorConfiguration.class);

	/**
	 * Name of the Message header containing the JSON encoded recognition response.
	 */
	public static final String RECOGNIZED_OBJECTS_HEADER = "recognized_objects";

	@Bean
	public Function<Message<byte[]>, Message<byte[]>> imageRecognitionFunction(ImageRecognitionProcessorProperties properties) {

		return input -> {
			// You can use file:, http: or classpath: to provide the path to the input image.
			byte[] inputImage = input.getPayload();

			try (ImageRecognition imageRecognition = createImageRecognitionFunction(properties)) {

				List<RecognitionResponse> recognizedObjects =
						ImageRecognition.toRecognitionResponse(imageRecognition.recognizeTopK(inputImage));

				// Draw the predicted labels on top of the input image.
				byte[] augmentedImage = new ImageRecognitionAugmenter().apply(inputImage, recognizedObjects);

				String jsonRecognizedObjects = new JsonMapperFunction().apply(recognizedObjects);

				Message<byte[]> outMessage = MessageBuilder
						.withPayload(augmentedImage)
						.setHeader(RECOGNIZED_OBJECTS_HEADER, jsonRecognizedObjects)
						.build();

				if (properties.isDebugOutput()) {
					try {
						logger.info("recognized objects = " + jsonRecognizedObjects);
						IOUtils.write(augmentedImage, new FileOutputStream(properties.getDebugOutputPath()));
					}
					catch (IOException e) {
						logger.warn("Cloud not produce debug output", e);
					}
				}

				return outMessage;
			}
		};
	}

	private static ImageRecognition createImageRecognitionFunction(ImageRecognitionProcessorProperties properties) {
		switch (properties.getModelType()) {
		case inception:
			return ImageRecognition.inception(
					properties.getModel(),
					properties.getNormalizedImageSize(),
					properties.getResponseSize(),
					properties.isCacheModel());
		case mobilenetv1:
			return ImageRecognition.mobileNetV1(
					properties.getModel(),
					properties.getNormalizedImageSize(),
					properties.getResponseSize(),
					properties.isCacheModel());
		case mobilenetv2:
			return ImageRecognition.mobileNetV2(
					properties.getModel(),
					properties.getNormalizedImageSize(),
					properties.getResponseSize(),
					properties.isCacheModel());
		default:
			throw new RuntimeException("Not supported Model Type: " + properties.getModelType());

		}
	}
}
