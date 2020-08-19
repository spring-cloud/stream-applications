package org.springframework.cloud.fn.aggregator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

public class AggregatorProcessorTests {

	@Test
	public void testFilterProcessor() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(AggregatorProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=aggregatorFunction",
						"--aggregator.message-store-type=jdbc")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(
					MessageBuilder.withPayload("2")
							.setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, "my_correlation")
							.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, 2)
							.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, 2)
							.build());
			processorInput.send(
					MessageBuilder.withPayload("1")
							.setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, "my_correlation")
							.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, 1)
							.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, 2)
							.build());

			Message<byte[]> receive = processorOutput.receive(10_000);

			assertThat(receive).isNotNull()
					.extracting(Message::getPayload)
					.extracting(String::new)
					.isEqualTo("[\"2\",\"1\"]");
		}
	}

	@SpringBootApplication
	public static class AggregatorProcessorTestApplication {

	}

}
