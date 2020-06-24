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
public class ImageRecognitionProcessorTests {

	@Test
	public void testImageRecognitionProcessorMobileNetV2() throws IOException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ImageRecognitionProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=imageRecognitionFunction",
						"--image.recognition.modelType=mobilenetv2",
						"--image.recognition.responseSize=3",
						"--image.recognition.debugOutput=true",
						"--image.recognition.debugOutputPath=./target/image-recognition-mobilenetv2.png")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			byte[] inputImage = GraphicsUtils.loadAsByteArray("classpath:/images/giant_panda_in_beijing_zoo_1.jpg");
			processorInput.send(new GenericMessage<>(inputImage));
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			String jsonRecognizedObjects = (String) sourceMessage.getHeaders().get(ImageRecognitionProcessorConfiguration.RECOGNIZED_OBJECTS_HEADER);
			assertThat(jsonRecognizedObjects)
					.isEqualTo("[{\"label\":\"giant panda, panda, panda bear, coon bear, Ailuropoda melanoleuca\",\"probability\":0.962329626083374}," +
							"{\"label\":\"badger\",\"probability\":0.006058811210095882}," +
							"{\"label\":\"ram, tup\",\"probability\":0.0010668420000001788}]");
		}
	}

	@Test
	public void testImageRecognitionProcessorMobileNetV1() throws IOException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ImageRecognitionProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--image.recognition.model=https://download.tensorflow.org/models/mobilenet_v1_2018_08_02/mobilenet_v1_1.0_224.tgz#mobilenet_v1_1.0_224_frozen.pb",
						"--image.recognition.modelType=mobilenetv1",
						"--image.recognition.responseSize=3",
						"--image.recognition.debugOutput=true",
						"--image.recognition.debugOutputPath=./target/image-recognition-mobilenetv1.png")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			byte[] inputImage = GraphicsUtils.loadAsByteArray("classpath:/images/giant_panda_in_beijing_zoo_1.jpg");
			processorInput.send(new GenericMessage<>(inputImage));
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			String jsonRecognizedObjects = (String) sourceMessage.getHeaders().get(ImageRecognitionProcessorConfiguration.RECOGNIZED_OBJECTS_HEADER);
			assertThat(jsonRecognizedObjects)
					.isEqualTo("[{\"label\":\"giant panda, panda, panda bear, coon bear, Ailuropoda melanoleuca\",\"probability\":0.984053909778595},{\"label\":\"ram, tup\",\"probability\":0.0019619385711848736},{\"label\":\"Staffordshire bullterrier, Staffordshire bull terrier\",\"probability\":0.0018697341438382864}]");
		}
	}

	@Test
	public void testImageRecognitionProcessorInception() throws IOException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(ImageRecognitionProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--image.recognition.model=https://storage.googleapis.com/scdf-tensorflow-models/image-recognition/tensorflow_inception_graph.pb",
						"--image.recognition.modelType=inception",
						"--image.recognition.responseSize=3",
						"--image.recognition.debugOutput=true",
						"--image.recognition.debugOutputPath=./target/image-recognition-inception.png")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			byte[] inputImage = GraphicsUtils.loadAsByteArray("classpath:/images/giant_panda_in_beijing_zoo_1.jpg");
			processorInput.send(new GenericMessage<>(inputImage));
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			String jsonRecognizedObjects = (String) sourceMessage.getHeaders().get(ImageRecognitionProcessorConfiguration.RECOGNIZED_OBJECTS_HEADER);
			assertThat(jsonRecognizedObjects)
					.isEqualTo("[{\"label\":\"giant panda\",\"probability\":0.9946685433387756},{\"label\":\"Arctic fox\",\"probability\":0.0036631159018725157},{\"label\":\"ice bear\",\"probability\":3.378273395355791E-4}]");
		}
	}

	@SpringBootApplication
	@Import({ ImageRecognitionProcessorConfiguration.class })
	public static class ImageRecognitionProcessorTestApplication {
	}

}
