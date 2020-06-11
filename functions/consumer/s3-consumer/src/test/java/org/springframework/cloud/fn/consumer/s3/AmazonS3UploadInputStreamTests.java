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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.Md5Utils;
import com.amazonaws.util.StringInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.http.MediaType;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = "s3.consumer.key-expression=headers.key")
public class AmazonS3UploadInputStreamTests extends AbstractAwsS3ConsumerMockTests {

	@Test
	public void test() throws Exception {
		AmazonS3 amazonS3Client = TestUtils.getPropertyValue(this.s3MessageHandler, "transferManager.s3",
				AmazonS3.class);

		InputStream payload = new StringInputStream("a");
		Message<?> message = MessageBuilder.withPayload(payload)
				.setHeader("key", "myInputStream")
				.build();

		this.s3Consumer.accept(message);

		ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor =
				ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(amazonS3Client, atLeastOnce()).putObject(putObjectRequestArgumentCaptor.capture());

		PutObjectRequest putObjectRequest = putObjectRequestArgumentCaptor.getValue();
		assertThat(putObjectRequest.getBucketName()).isEqualTo(S3_BUCKET);
		assertThat(putObjectRequest.getKey()).isEqualTo("myInputStream");
		assertThat(putObjectRequest.getFile()).isNull();
		assertThat(putObjectRequest.getInputStream()).isNotNull();

		ObjectMetadata metadata = putObjectRequest.getMetadata();
		assertThat(metadata.getContentMD5()).isEqualTo(Md5Utils.md5AsBase64(payload));
		assertThat(metadata.getContentLength()).isEqualTo(1L);
		assertThat(metadata.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(metadata.getContentDisposition()).isEqualTo("test.json");
	}
}
