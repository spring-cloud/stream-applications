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

package org.springframework.cloud.fn.consumer.s3;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.SetObjectAclRequest;
import com.amazonaws.services.s3.transfer.PersistableTransfer;
import com.amazonaws.services.s3.transfer.internal.S3ProgressListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.integration.aws.outbound.S3MessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.spy;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"cloud.aws.stack.auto=false",
				"cloud.aws.credentials.accessKey=" + AbstractAwsS3ConsumerMockTests.AWS_ACCESS_KEY,
				"cloud.aws.credentials.secretKey=" + AbstractAwsS3ConsumerMockTests.AWS_SECRET_KEY,
				"cloud.aws.region.static=" + AbstractAwsS3ConsumerMockTests.AWS_REGION,
				"s3.common.endpointUrl=foo",
				"s3.consumer.bucket=" + AbstractAwsS3ConsumerMockTests.S3_BUCKET })
public abstract class AbstractAwsS3ConsumerMockTests {

	protected static final String AWS_ACCESS_KEY = "test.accessKey";

	protected static final String AWS_SECRET_KEY = "test.secretKey";

	protected static final String AWS_REGION = "us-gov-west-1";

	protected static final String S3_BUCKET = "S3_BUCKET";

	@TempDir
	protected static Path temporaryRemoteFolder;

	@Autowired
	private AmazonS3Client amazonS3;

	@Autowired
	protected S3MessageHandler s3MessageHandler;

	@Autowired
	protected CountDownLatch aclLatch;

	@Autowired
	protected CountDownLatch transferCompletedLatch;

	@Autowired
	protected Consumer<Message<?>> s3Consumer;

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

	@SpringBootApplication
	public static class S3ConsumerTestApplication {

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
