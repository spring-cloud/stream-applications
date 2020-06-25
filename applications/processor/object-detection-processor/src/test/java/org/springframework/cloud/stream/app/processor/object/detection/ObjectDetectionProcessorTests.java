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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.common.tensorflow.deprecated.GraphicsUtils;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
public class ObjectDetectionProcessorTests {

	@Test
	public void testObjectDetectionProcessor() throws IOException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ObjectDetectionProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=objectDetection",
						"--object.detection.model=https://download.tensorflow.org/models/object_detection/ssdlite_mobilenet_v2_coco_2018_05_09.tar.gz#frozen_inference_graph.pb",
						"--object.detection.labels=https://storage.googleapis.com/scdf-tensorflow-models/object-detection/mscoco_label_map.pbtxt",
						"--object.detection.responseSize=10",
						"--object.detection.debugOutput=true",
						"--object.detection.debugOutputPath=./target/object-detection-1.png")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			byte[] inputImage = GraphicsUtils.loadAsByteArray("classpath:/images/pivotal.jpeg");
			processorInput.send(new GenericMessage<>(inputImage));
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			String jsonRecognizedObjects = (String) sourceMessage.getHeaders().get(ObjectDetectionProcessorConfiguration.DETECTED_OBJECTS_HEADER);
			assertThat(jsonRecognizedObjects).isNotEmpty();
			//assertThat(jsonRecognizedObjects)
			//		.isEqualTo("[{\"label\":\"giant panda, panda, panda bear, coon bear, Ailuropoda melanoleuca\",\"probability\":0.962329626083374}," +
			//				"{\"label\":\"badger\",\"probability\":0.006058811210095882}," +
			//				"{\"label\":\"ram, tup\",\"probability\":0.0010668420000001788}]");
		}
	}

	//@Test
	public void testObjectDetectionProcessoriNaturalistSpecies() throws IOException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ObjectDetectionProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run(
						//"--spring.cloud.function.definition=objectDetection",
						"--object.detection.model=https://download.tensorflow.org/models/object_detection/faster_rcnn_resnet101_fgvc_2018_07_19.tar.gz#frozen_inference_graph.pb",
						"--object.detection.labels=https://raw.githubusercontent.com/tensorflow/models/master/research/object_detection/data/fgvc_2854_classes_label_map.pbtxt",
						"--object.detection..debugOutput=true",
						"--object.detection..debugOutputPath=./target/object-detection-2.png")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			byte[] inputImage = GraphicsUtils.loadAsByteArray("classpath:/images/animals2.jpg");
			processorInput.send(new GenericMessage<>(inputImage));
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			String jsonRecognizedObjects = (String) sourceMessage.getHeaders().get(ObjectDetectionProcessorConfiguration.DETECTED_OBJECTS_HEADER);
			//assertThat(jsonRecognizedObjects)
			//		.isEqualTo("[{\"label\":\"giant panda, panda, panda bear, coon bear, Ailuropoda melanoleuca\",\"probability\":0.962329626083374}," +
			//				"{\"label\":\"badger\",\"probability\":0.006058811210095882}," +
			//				"{\"label\":\"ram, tup\",\"probability\":0.0010668420000001788}]");
		}
	}

	@SpringBootApplication
	@Import({ ObjectDetectionProcessorConfiguration.class })
	public static class ObjectDetectionProcessorTestApplication {
	}

}
