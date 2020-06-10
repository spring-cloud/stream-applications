/*
 * Copyright 2016-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"file.consumer.mode=ref",
		"s3.supplier.filenameRegex=.*\\\\.test$"})
public class AmazonS3FilesTransferredTests extends AbstractAwsS3SupplierMockTests {

	@Test
	public void test() {
		final Flux<Message<?>> messageFlux = s3Supplier.get();
		StepVerifier stepVerifier =
				StepVerifier.create(messageFlux)
						.assertNext((message) -> {
									assertThat(new File(message.getPayload().toString().replaceAll("\"", "")))
											.isEqualTo(new File(this.awsS3SupplierProperties.getLocalDir() + File.separator + "1.test"));
								}
						)
						.assertNext((message) -> {
							assertThat(new File(message.getPayload().toString().replaceAll("\"", "")))
									.isEqualTo(new File(this.awsS3SupplierProperties.getLocalDir() + File.separator + "2.test"));
						})
						.thenCancel()
						.verifyLater();
		standardIntegrationFlow.start();
		stepVerifier.verify();
	}

}
