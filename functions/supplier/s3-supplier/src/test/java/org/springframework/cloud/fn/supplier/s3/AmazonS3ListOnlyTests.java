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

package org.springframework.cloud.fn.supplier.s3;

import java.time.Duration;
import java.util.HashSet;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
		"s3.supplier.list-only=true"
})
public class AmazonS3ListOnlyTests extends AbstractAwsS3SupplierMockTests {

	@Test
	public void test() {
		final Flux<Message<?>> messageFlux = s3Supplier.get();
		final HashSet<String> keys = new HashSet<>();
		keys.add("1.test");
		keys.add("2.test");
		keys.add("otherFile");
		StepVerifier stepVerifier = StepVerifier.create(messageFlux)
				.assertNext(message -> {
					S3ObjectSummary summary = (S3ObjectSummary) message.getPayload();
					assertThat(summary.getBucketName()).isEqualTo(S3_BUCKET);
					assertThat(keys.contains(summary.getKey()));
					keys.remove(summary.getKey());
				})
				.assertNext(message -> {
					S3ObjectSummary summary = (S3ObjectSummary) message.getPayload();
					assertThat(summary.getBucketName()).isEqualTo(S3_BUCKET);
					assertThat(keys.contains(summary.getKey()));
					keys.remove(summary.getKey());
				})
				.assertNext(message -> {
					S3ObjectSummary summary = (S3ObjectSummary) message.getPayload();
					assertThat(summary.getBucketName()).isEqualTo(S3_BUCKET);
					assertThat(keys.contains(summary.getKey()));
					keys.remove(summary.getKey());
				})
				.expectTimeout(Duration.ofSeconds(1))
				.verifyLater();
		standardIntegrationFlow.start();
		stepVerifier.verify(Duration.ofSeconds(10));
		standardIntegrationFlow.stop();
	}
}
