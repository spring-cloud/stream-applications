/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.cloud.stream.app.source.s3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.supplier.s3.AwsS3SupplierConfiguration;
import org.springframework.cloud.fn.supplier.s3.AwsS3SupplierProperties;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cloud.aws.credentials.accessKey=" + AwsS3SourceTests.AWS_ACCESS_KEY,
				"spring.cloud.aws.credentials.secretKey=" + AwsS3SourceTests.AWS_SECRET_KEY,
				"spring.cloud.aws.region.static=" + AwsS3SourceTests.AWS_REGION,
				"s3.supplier.remoteDir=" + AwsS3SourceTests.S3_BUCKET,
				"file.consumer.mode=ref",
				"spring.cloud.aws.s3.endpoint=s3://foo",
				"s3.supplier.filenameRegex=.*\\\\.test$"})
@DirtiesContext
public class AwsS3SourceTests {

	@TempDir
	protected static Path temporaryRemoteFolder;

	protected static final String AWS_ACCESS_KEY = "test.accessKey";

	protected static final String AWS_SECRET_KEY = "test.secretKey";

	protected static final String AWS_REGION = "us-gov-west-1";

	protected static final String S3_BUCKET = "S3_BUCKET";

	protected static Map<S3Object, InputStream> S3_OBJECTS;

	@Autowired
	private OutputDestination output;

	@Autowired
	protected AwsS3SupplierProperties awsS3SupplierProperties;

	@BeforeAll
	public static void setup() throws IOException {
		final String remote = temporaryRemoteFolder.toAbsolutePath() + "/remote";
		File f = new File(remote);
		f.mkdirs();
		File aFile = new File(f, "1.test");
		FileCopyUtils.copy("Hello".getBytes(), aFile);
		File bFile = new File(f, "2.test");
		FileCopyUtils.copy("Bye".getBytes(), bFile);

		S3_OBJECTS = new HashMap<>();

		Instant instant = Instant.now().plus(Period.ofDays(1));

		for (File file : f.listFiles()) {
			S3Object s3Object =
					S3Object.builder()
							.key("subdir/" + file.getName())
							.lastModified(instant)
							.build();

			S3_OBJECTS.put(s3Object, new FileInputStream(file));
		}

		final String local = temporaryRemoteFolder.toAbsolutePath() + "/local";
		File f1 = new File(local);
		f1.mkdirs();
		System.setProperty("s3.supplier.localDir", f1.getAbsolutePath());
	}

	@AfterAll
	public static void tearDown() {
		System.clearProperty("s3.supplier.localDir");
	}

	@Test
	public void testS3SourceWithBinderBasic() {
		Message<byte[]> sourceMessage = output.receive(10_000, "s3Supplier-out-0");
		String actual = new String(sourceMessage.getPayload());
		assertThat(new File(actual.replaceAll("\"", "")))
				.isEqualTo(new File(this.awsS3SupplierProperties.getLocalDir() + File.separator + "subdir" + File.separator + "1.test"));
		sourceMessage = output.receive(10_000);
		actual = new String(sourceMessage.getPayload());
		assertThat(new File(actual.replaceAll("\"", "")))
				.isEqualTo(new File(this.awsS3SupplierProperties.getLocalDir() + File.separator + "subdir" + File.separator + "2.test"));
	}

	@SpringBootApplication
	@Import({TestChannelBinderConfiguration.class, AwsS3SupplierConfiguration.class})
	public static class SampleConfiguration {

		@Bean
		@Primary
		public S3Client amazonS3Mock() {
			S3Client amazonS3 = mock(S3Client.class);

			ListObjectsResponse listObjectsResponse =
					ListObjectsResponse.builder().contents(S3_OBJECTS.keySet()).isTruncated(false).build();

			willAnswer(invocation -> listObjectsResponse)
					.given(amazonS3)
					.listObjects(any(ListObjectsRequest.class));

			for (Map.Entry<S3Object, InputStream> s3Object : S3_OBJECTS.entrySet()) {
				willAnswer(invocation ->
						new ResponseInputStream<>(GetObjectResponse.builder().build(), s3Object.getValue()))
						.given(amazonS3)
						.getObject(GetObjectRequest.builder()
								.bucket(S3_BUCKET)
								.key(s3Object.getKey().key())
								.build());
			}
			return amazonS3;
		}

	}

}
