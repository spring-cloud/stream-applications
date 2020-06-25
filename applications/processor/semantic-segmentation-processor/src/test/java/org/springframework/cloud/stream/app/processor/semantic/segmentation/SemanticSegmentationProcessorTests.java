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
public class SemanticSegmentationProcessorTests {

	@Test
	public void testSemanticSegmentationProcessor() throws IOException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(SemanticSegmentationTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=semanticSegmentationFunction",
						"--semantic.segmentation.model=https://download.tensorflow.org/models/deeplabv3_mnv2_cityscapes_train_2018_02_05.tar.gz#frozen_inference_graph.pb",
						"--semantic.segmentation.colorMapUri=classpath:/colormap/citymap_colormap.json",
						"--semantic.segmentation.outputType=blended",
						"--semantic.segmentation.debugOutput=true",
						"--semantic.segmentation.debugOutputPath=./target/semantic-segmentation-1.png")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			byte[] inputImage = GraphicsUtils.loadAsByteArray("classpath:/images/amsterdam-cityscape1.jpg");
			processorInput.send(new GenericMessage<>(inputImage));
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			String jsonRecognizedObjects = (String) sourceMessage.getHeaders().get(
					SemanticSegmentationProcessorConfiguration.SEMANTIC_SEGMENTATION_HEADER);
			assertThat(jsonRecognizedObjects).isNotEmpty();
			//assertThat(jsonRecognizedObjects)
			//		.isEqualTo("[{\"label\":\"giant panda, panda, panda bear, coon bear, Ailuropoda melanoleuca\",\"probability\":0.962329626083374}," +
			//				"{\"label\":\"badger\",\"probability\":0.006058811210095882}," +
			//				"{\"label\":\"ram, tup\",\"probability\":0.0010668420000001788}]");
		}
	}


	@SpringBootApplication
	@Import(SemanticSegmentationProcessorConfiguration.class)
	public static class SemanticSegmentationTestApplication {
	}

}
