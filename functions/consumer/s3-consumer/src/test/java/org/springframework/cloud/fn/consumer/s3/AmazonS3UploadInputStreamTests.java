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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.Md5Utils;
import software.amazon.awssdk.utils.StringInputStream;

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
		S3AsyncClient amazonS3Client =
				TestUtils.getPropertyValue(this.s3TransferManager, "s3AsyncClient", S3AsyncClient.class);

		InputStream payload = new StringInputStream("a");
		Message<?> message = MessageBuilder.withPayload(payload)
				.setHeader("key", "myInputStream")
				.build();

		this.s3Consumer.accept(message);

		ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor =
				ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<AsyncRequestBody> asyncRequestBodyArgumentCaptor =
				ArgumentCaptor.forClass(AsyncRequestBody.class);
		verify(amazonS3Client, atLeastOnce())
				.putObject(putObjectRequestArgumentCaptor.capture(), asyncRequestBodyArgumentCaptor.capture());

		PutObjectRequest putObjectRequest = putObjectRequestArgumentCaptor.getValue();
		assertThat(putObjectRequest.bucket()).isEqualTo(S3_BUCKET);
		assertThat(putObjectRequest.key()).isEqualTo("myInputStream");
		assertThat(putObjectRequest.contentMD5()).isEqualTo(Md5Utils.md5AsBase64(payload));
		assertThat(putObjectRequest.contentLength()).isEqualTo(1L);
		assertThat(putObjectRequest.contentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(putObjectRequest.contentDisposition()).isEqualTo("test.json");


		AsyncRequestBody asyncRequestBody = asyncRequestBodyArgumentCaptor.getValue();
		StepVerifier.create(asyncRequestBody.map(buffer -> new String(buffer.array())))
				.expectNext("a")
				.expectComplete()
				.verify();
	}

}
