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

package org.springframework.cloud.stream.app.s3.sink;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.SetObjectAclRequest;
import com.amazonaws.services.s3.transfer.PersistableTransfer;
import com.amazonaws.services.s3.transfer.internal.S3ProgressListener;
import com.amazonaws.services.s3.transfer.internal.S3ProgressPublisher;
import com.amazonaws.util.Md5Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.consumer.s3.AwsS3ConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.integration.aws.outbound.S3MessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"cloud.aws.stack.auto=false",
				"cloud.aws.credentials.accessKey=" + AwsS3SinkTests.AWS_ACCESS_KEY,
				"cloud.aws.credentials.secretKey=" + AwsS3SinkTests.AWS_SECRET_KEY,
				"cloud.aws.region.static=" + AwsS3SinkTests.AWS_REGION,
				"s3.consumer.bucket=" + AwsS3SinkTests.S3_BUCKET,
				"s3.consumer.acl=PublicReadWrite"})
@DirtiesContext
public class AwsS3SinkTests {

	protected static final String AWS_ACCESS_KEY = "test.accessKey";

	protected static final String AWS_SECRET_KEY = "test.secretKey";

	protected static final String AWS_REGION = "us-gov-west-1";

	protected static final String S3_BUCKET = "S3_BUCKET";

	@TempDir
	protected static Path temporaryRemoteFolder;

	@Autowired
	private AmazonS3Client amazonS3;

	@Autowired
	private S3MessageHandler s3MessageHandler;

	@Autowired
	protected CountDownLatch aclLatch;

	@Autowired
	protected CountDownLatch transferCompletedLatch;

	@Autowired
	private InputDestination inputDestination;

	@BeforeEach
	public void setupTest() {
		Object transferManager = TestUtils.getPropertyValue(this.s3MessageHandler, "transferManager");

		AmazonS3 amazonS3 = spy(this.amazonS3);

		willAnswer(invocation -> new PutObjectResult()).given(amazonS3)
				.putObject(any(PutObjectRequest.class));

		willAnswer(invocation -> {
			aclLatch.countDown();
			return null;
		}).given(amazonS3)
				.setObjectAcl(any(SetObjectAclRequest.class));

		new DirectFieldAccessor(transferManager).setPropertyValue("s3", amazonS3);
	}

	@Test
	public void testS3SourceWithBinderBasic() throws Exception {
		AmazonS3 amazonS3Client = TestUtils.getPropertyValue(this.s3MessageHandler, "transferManager.s3",
				AmazonS3.class);

		File file = new File(temporaryRemoteFolder.toFile(), "foo.mp3");
		file.createNewFile();
		Message<?> message = MessageBuilder.withPayload(file)
				.build();

		this.inputDestination.send(message);

		ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor =
				ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(amazonS3Client, atLeastOnce()).putObject(putObjectRequestArgumentCaptor.capture());

		PutObjectRequest putObjectRequest = putObjectRequestArgumentCaptor.getValue();
		assertThat(putObjectRequest.getBucketName()).isEqualTo(S3_BUCKET);
		assertThat(putObjectRequest.getKey()).isEqualTo("foo.mp3");
		assertThat(putObjectRequest.getFile()).isNotNull();
		assertThat(putObjectRequest.getInputStream()).isNull();

		ObjectMetadata metadata = putObjectRequest.getMetadata();
		assertThat(metadata.getContentMD5()).isEqualTo(Md5Utils.md5AsBase64(file));
		assertThat(metadata.getContentLength()).isEqualTo(0L);
		assertThat(metadata.getContentType()).isEqualTo("audio/mpeg");

		ProgressListener listener = putObjectRequest.getGeneralProgressListener();
		S3ProgressPublisher.publishProgress(listener, ProgressEventType.TRANSFER_COMPLETED_EVENT);

		assertThat(this.transferCompletedLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.aclLatch.await(10, TimeUnit.SECONDS)).isTrue();

		ArgumentCaptor<SetObjectAclRequest> setObjectAclRequestArgumentCaptor =
				ArgumentCaptor.forClass(SetObjectAclRequest.class);
		verify(amazonS3Client).setObjectAcl(setObjectAclRequestArgumentCaptor.capture());

		SetObjectAclRequest setObjectAclRequest = setObjectAclRequestArgumentCaptor.getValue();

		assertThat(setObjectAclRequest.getBucketName()).isEqualTo(S3_BUCKET);
		assertThat(setObjectAclRequest.getKey()).isEqualTo("foo.mp3");
		assertThat(setObjectAclRequest.getAcl()).isNull();
		assertThat(setObjectAclRequest.getCannedAcl()).isEqualTo(CannedAccessControlList.PublicReadWrite);
	}

	@SpringBootApplication
	@Import({AwsS3ConsumerConfiguration.class, TestChannelBinderConfiguration.class})
	public static class SampleConfiguration {

		@Bean
		public CountDownLatch aclLatch() {
			return new CountDownLatch(1);
		}

		@Bean
		public CountDownLatch transferCompletedLatch() {
			return new CountDownLatch(1);
		}

		@Bean
		public S3ProgressListener s3ProgressListener() {
			return new S3ProgressListener() {

				@Override
				public void onPersistableTransfer(PersistableTransfer persistableTransfer) {

				}

				@Override
				public void progressChanged(ProgressEvent progressEvent) {
					if (ProgressEventType.TRANSFER_COMPLETED_EVENT.equals(progressEvent.getEventType())) {
						transferCompletedLatch().countDown();
					}
				}
			};
		}

		@Bean
		public S3MessageHandler.UploadMetadataProvider uploadMetadataProvider() {
			return (metadata, message) -> {
				if (message.getPayload() instanceof InputStream) {
					metadata.setContentLength(1);
					metadata.setContentType(MediaType.APPLICATION_JSON_VALUE);
					metadata.setContentDisposition("test.json");
				}
			};
		}
	}
}
