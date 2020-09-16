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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	private ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void testImageRecognitionProcessorMobileNetV2() throws IOException {
		List<Map<String, Object>> expected = deserializeAndRoundToNPlaces(
				"[{\"label\":\"giant panda, panda, panda bear, coon bear, Ailuropoda melanoleuca\",\"probability\":0.962329626083374},"
						+
						"{\"label\":\"badger\",\"probability\":0.006058811210095882}," +
						"{\"label\":\"ram, tup\",\"probability\":0.0010668420000001788}]",
				6);

		imageRecognitionProcessorMobileNetV2(verify(expected));
	}

	private void imageRecognitionProcessorMobileNetV2(Consumer<Message<byte[]>> consumer) throws IOException {
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
			consumer.accept(sourceMessage);
		}
	}

	@Test
	public void testImageRecognitionProcessorMobileNetV1() throws IOException {
		List<Map<String, Object>> expected = deserializeAndRoundToNPlaces(
				"[{\"label\":\"giant panda, panda, panda bear, coon bear, Ailuropoda melanoleuca\",\"probability\":0.984053909778595},"
						+
						"{\"label\":\"ram, tup\",\"probability\":0.0019619385711848736}," +
						"{\"label\":\"Staffordshire bullterrier, Staffordshire bull terrier\",\"probability\":0.0018697341438382864}]",
				6);

		imageRecognitionProcessorMobileNetV1(verify(expected));
	}

	private void imageRecognitionProcessorMobileNetV1(Consumer<Message<byte[]>> consumer) throws IOException {
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
			consumer.accept(sourceMessage);
		}
	}

	@Test
	public void testImageRecognitionProcessorInception() throws IOException {
		List<Map<String, Object>> expected = deserializeAndRoundToNPlaces(
				"[{\"label\":\"giant panda\",\"probability\":0.9946685433387756}," +
						"{\"label\":\"Arctic fox\",\"probability\":0.003663112409412861}," +
						"{\"label\":\"ice bear\",\"probability\":3.378273395355791E-4}]",
				6);
		imageRecognitionProcessorInception(verify(expected));
	}

	private void imageRecognitionProcessorInception(Consumer<Message<byte[]>> consumer) throws IOException {
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

			consumer.accept(sourceMessage);
		}
	}

	private Consumer<Message<byte[]>> verify(List<Map<String, Object>> expected) {
		return message -> {
			List<Map<String, Object>> actual = deserializeAndRoundToNPlaces((String) message.getHeaders()
					.get(ImageRecognitionProcessorConfiguration.RECOGNIZED_OBJECTS_HEADER), 6);
			assertThat(expected)
					.isEqualTo(actual);
		};
	}

	private List<Map<String, Object>> deserializeAndRoundToNPlaces(String json, int places) {
		List<Map<String, Object>> result = null;
		try {
			result = objectMapper.readValue(json, ArrayList.class);
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		result.forEach(map -> {
			if (map.containsKey("probability")) {
				map.put("probability", round((double) map.get("probability"), places));
			}
		});

		return result;

	}

	private static double round(double value, int places) {
		if (places < 0) {
			throw new IllegalArgumentException();
		}

		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	@SpringBootApplication
	@Import({ ImageRecognitionProcessorConfiguration.class })
	public static class ImageRecognitionProcessorTestApplication {
	}

}
