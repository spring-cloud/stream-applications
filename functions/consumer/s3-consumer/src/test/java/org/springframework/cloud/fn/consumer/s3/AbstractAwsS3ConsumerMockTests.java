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

package org.springframework.cloud.fn.consumer.s3;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.spy;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cloud.aws.credentials.accessKey=" + AbstractAwsS3ConsumerMockTests.AWS_ACCESS_KEY,
				"spring.cloud.aws.credentials.secretKey=" + AbstractAwsS3ConsumerMockTests.AWS_SECRET_KEY,
				"spring.cloud.aws.region.static=" + AbstractAwsS3ConsumerMockTests.AWS_REGION,
				"spring.cloud.aws.s3.endpoint=s3://foo",
				"s3.consumer.bucket=" + AbstractAwsS3ConsumerMockTests.S3_BUCKET})
public abstract class AbstractAwsS3ConsumerMockTests {

	protected static final String AWS_ACCESS_KEY = "test.accessKey";

	protected static final String AWS_SECRET_KEY = "test.secretKey";

	protected static final String AWS_REGION = "us-gov-west-1";

	protected static final String S3_BUCKET = "S3_BUCKET";

	@TempDir
	protected static Path temporaryRemoteFolder;

	@Autowired
	private S3AsyncClient amazonS3;

	@Autowired
	protected S3TransferManager s3TransferManager;

	@Autowired
	protected CountDownLatch transferCompletedLatch;

	@Autowired
	protected Consumer<Message<?>> s3Consumer;

	@BeforeEach
	public void setupTest() {
		S3AsyncClient amazonS3 = spy(this.amazonS3);

		willReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().build()))
				.given(amazonS3)
				.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));

		new DirectFieldAccessor(this.s3TransferManager).setPropertyValue("s3AsyncClient", amazonS3);
	}

	@SpringBootApplication
	public static class S3ConsumerTestApplication {

		@Bean
		public CountDownLatch transferCompletedLatch() {
			return new CountDownLatch(1);
		}

		@Bean
		public TransferListener transferListener() {
			return new TransferListener() {


				@Override
				public void transferComplete(Context.TransferComplete context) {
					transferCompletedLatch().countDown();
				}

			};
		}

		@Bean
		public BiConsumer<PutObjectRequest.Builder, Message<?>> uploadMetadataProvider() {
			return (builder, message) -> {
				if (message.getPayload() instanceof InputStream) {
					builder.contentLength(1L)
							.contentType(MediaType.APPLICATION_JSON_VALUE)
							.contentDisposition("test.json");
				}
			};
		}

	}

}
