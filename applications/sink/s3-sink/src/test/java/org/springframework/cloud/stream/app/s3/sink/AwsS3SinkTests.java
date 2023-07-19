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

package org.springframework.cloud.stream.app.s3.sink;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.Md5Utils;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.consumer.s3.AwsS3ConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cloud.aws.credentials.accessKey=" + AwsS3SinkTests.AWS_ACCESS_KEY,
				"spring.cloud.aws.credentials.secretKey=" + AwsS3SinkTests.AWS_SECRET_KEY,
				"spring.cloud.aws.region.static=" + AwsS3SinkTests.AWS_REGION,
				"s3.consumer.bucket=" + AwsS3SinkTests.S3_BUCKET,
				"s3.consumer.acl=PUBLIC_READ_WRITE"})
@DirtiesContext
public class AwsS3SinkTests {

	protected static final String AWS_ACCESS_KEY = "test.accessKey";

	protected static final String AWS_SECRET_KEY = "test.secretKey";

	protected static final String AWS_REGION = "us-gov-west-1";

	protected static final String S3_BUCKET = "S3_BUCKET";

	@TempDir
	protected static Path temporaryRemoteFolder;

	@Autowired
	private S3AsyncClient amazonS3;

	@Autowired
	private S3TransferManager s3TransferManager;

	@Autowired
	protected CountDownLatch aclLatch;

	@Autowired
	protected CountDownLatch transferCompletedLatch;

	@Autowired
	private InputDestination inputDestination;

	@BeforeEach
	public void setupTest() {
		S3AsyncClient amazonS3 = spy(this.amazonS3);

		willReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().build()))
				.given(amazonS3)
				.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));

		new DirectFieldAccessor(this.s3TransferManager).setPropertyValue("s3AsyncClient", amazonS3);
	}

	@Test
	public void testS3SinkWithBinderBasic() throws Exception {
		S3AsyncClient amazonS3Client =
				TestUtils.getPropertyValue(this.s3TransferManager, "s3AsyncClient", S3AsyncClient.class);

		File file = new File(temporaryRemoteFolder.toFile(), "foo.mp3");
		file.createNewFile();
		Message<?> message = MessageBuilder.withPayload(file)
				.build();

		this.inputDestination.send(message);

		ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor =
				ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<AsyncRequestBody> asyncRequestBodyArgumentCaptor =
				ArgumentCaptor.forClass(AsyncRequestBody.class);
		verify(amazonS3Client, atLeastOnce())
				.putObject(putObjectRequestArgumentCaptor.capture(), asyncRequestBodyArgumentCaptor.capture());

		PutObjectRequest putObjectRequest = putObjectRequestArgumentCaptor.getValue();
		assertThat(putObjectRequest.bucket()).isEqualTo(S3_BUCKET);
		assertThat(putObjectRequest.key()).isEqualTo("foo.mp3");
		assertThat(putObjectRequest.contentMD5()).isEqualTo(Md5Utils.md5AsBase64(file));
		assertThat(putObjectRequest.contentLength()).isEqualTo(0L);
		assertThat(putObjectRequest.contentType()).isEqualTo("audio/mpeg");
		assertThat(putObjectRequest.acl()).isEqualTo(ObjectCannedACL.PUBLIC_READ_WRITE);

		AsyncRequestBody asyncRequestBody = asyncRequestBodyArgumentCaptor.getValue();
		StepVerifier.create(asyncRequestBody)
				.assertNext(buffer -> assertThat(buffer.array()).isEmpty())
				.expectComplete()
				.verify();

		assertThat(this.transferCompletedLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@SpringBootApplication
	@Import({AwsS3ConsumerConfiguration.class, TestChannelBinderConfiguration.class})
	public static class SampleConfiguration {

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

	}

}
