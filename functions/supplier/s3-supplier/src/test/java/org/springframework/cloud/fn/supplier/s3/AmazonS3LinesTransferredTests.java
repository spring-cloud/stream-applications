/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.cloud.fn.supplier.s3;

import java.io.File;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.integration.file.FileHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
		"file.consumer.mode=lines",
		"s3.supplier.filenamePattern=*/otherFile",
		"file.consumer.with-markers=false"})
public class AmazonS3LinesTransferredTests extends AbstractAwsS3SupplierMockTests {

	@Test
	public void test() {
		final Flux<Message<?>> messageFlux = s3Supplier.get();
		StepVerifier stepVerifier =
				StepVerifier.create(messageFlux)
						.assertNext((message) -> {
									assertThat(message.getPayload()).isEqualTo("Other");
									assertThat(message.getHeaders()).containsKey(FileHeaders.ORIGINAL_FILE);
									assertThat(message.getHeaders()).containsValue(
											new File(this.awsS3SupplierProperties.getLocalDir(), "subdir/otherFile"));
								}
						)
						.assertNext((message) -> {
							assertThat(message.getPayload().toString()).isEqualTo("Other2");
						})
						.thenCancel()
						.verifyLater();
		standardIntegrationFlow.start();
		stepVerifier.verify(Duration.ofSeconds(10));

		assertThat(this.awsS3SupplierProperties.getLocalDir().list()).hasSize(1);
	}
}
