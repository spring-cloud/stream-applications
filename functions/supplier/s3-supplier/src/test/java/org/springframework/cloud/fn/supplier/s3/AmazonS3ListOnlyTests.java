/*
 * Copyright 2020-2023 the original author or authors.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.integration.json.JsonPathUtils;
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
		keys.add("subdir/1.test");
		keys.add("subdir/2.test");
		keys.add("subdir/otherFile");
		StepVerifier stepVerifier = StepVerifier.create(messageFlux)
				.assertNext(message -> {
					String s3Object = (String) message.getPayload();
					String key = jsonPathKey(s3Object);
					assertThat(keys).contains(key);
					keys.remove(key);
				})
				.assertNext(message -> {
					String s3Object = (String) message.getPayload();
					String key = jsonPathKey(s3Object);
					assertThat(keys).contains(key);
					keys.remove(key);
				})
				.assertNext(message -> {
					String s3Object = (String) message.getPayload();
					String key = jsonPathKey(s3Object);
					assertThat(keys).contains(key);
					keys.remove(key);
				})
				.thenCancel()
				.verifyLater();
		standardIntegrationFlow.start();
		stepVerifier.verify(Duration.ofSeconds(10));
	}

	private static String jsonPathKey(String s3Object) {
		try {
			return JsonPathUtils.evaluate(s3Object, "$.key");
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
