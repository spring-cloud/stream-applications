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

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SetObjectAclRequest;
import com.amazonaws.services.s3.transfer.internal.S3ProgressPublisher;
import com.amazonaws.util.Md5Utils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = "s3.consumer.acl=PublicReadWrite")
public class AmazonS3UploadFileTests extends AbstractAwsS3ConsumerMockTests {

	@Test
	public void test() throws Exception {
		AmazonS3 amazonS3Client = TestUtils.getPropertyValue(this.s3MessageHandler, "transferManager.s3",
				AmazonS3.class);

		File file = new File(this.temporaryRemoteFolder.toFile(), "foo.mp3");
		file.createNewFile();
		Message<?> message = MessageBuilder.withPayload(file)
				.build();

		this.s3Consumer.accept(message);

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
}
