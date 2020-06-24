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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.tensorflow.deprecated.JsonMapperFunction;
import org.springframework.cloud.fn.object.detection.ObjectDetectionImageAugmenter;
import org.springframework.cloud.fn.object.detection.ObjectDetectionService;
import org.springframework.cloud.fn.object.detection.domain.ObjectDetection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

/**
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties(ObjectDetectionProcessorProperties.class)
public class ObjectDetectionProcessorConfiguration {

	private static final Log logger = LogFactory.getLog(ObjectDetectionProcessorConfiguration.class);

	/**
	 * Name of the Message header containing the JSON encoded detected objects.
	 */
	public static final String DETECTED_OBJECTS_HEADER = "detected_objects";

	@Bean
	public ObjectDetectionService objectDetectionService(ObjectDetectionProcessorProperties properties) {
		return new ObjectDetectionService(properties.getModel(),
				properties.getLabels(), properties.getConfidence(), properties.isWithMasks(),
				properties.isCacheModel());
	}

	@Bean
	public Function<Message<byte[]>, Message<byte[]>> objectDetection(
			ObjectDetectionService objectDetectionService,
			ObjectDetectionProcessorProperties properties) {

		return input -> {
			// You can use file:, http: or classpath: to provide the path to the input image.
			byte[] inputImage = input.getPayload();

			List<ObjectDetection> detectedObjects = objectDetectionService.detect(inputImage);

			if (!CollectionUtils.isEmpty(detectedObjects) && properties.getResponseSize() < detectedObjects.size()) {
				detectedObjects = detectedObjects.subList(0, properties.getResponseSize());
			}

			// Draw the predicted labels on top of the input image.
			byte[] augmentedImage = new ObjectDetectionImageAugmenter().apply(inputImage, detectedObjects);

			String jsonDetectedObjects = new JsonMapperFunction().apply(detectedObjects);

			Message<byte[]> outMessage = MessageBuilder
					.withPayload(augmentedImage)
					.setHeader(DETECTED_OBJECTS_HEADER, jsonDetectedObjects)
					.build();

			if (properties.isDebugOutput()) {
				try {
					logger.info("detected objects = " + jsonDetectedObjects);
					IOUtils.write(augmentedImage, new FileOutputStream(properties.getDebugOutputPath()));
				}
				catch (IOException e) {
					logger.warn("Cloud not produce debug output", e);
				}
			}

			return outMessage;

		};
	}

}
