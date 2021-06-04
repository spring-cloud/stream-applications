/*
 * Copyright 2016-2021 the original author or authors.
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"cloud.aws.stack.auto=false",
				"cloud.aws.credentials.accessKey=" + AwsS3SourceTests.AWS_ACCESS_KEY,
				"cloud.aws.credentials.secretKey=" + AwsS3SourceTests.AWS_SECRET_KEY,
				"cloud.aws.region.static=" + AwsS3SourceTests.AWS_REGION,
				"s3.supplier.remoteDir=" + AwsS3SourceTests.S3_BUCKET,
				"file.consumer.mode=ref",
				"s3.common.endpointUrl=foo",
				"s3.supplier.filenameRegex=.*\\\\.test$" })
@DirtiesContext
public class AwsS3SourceTests {

	@TempDir
	protected static Path temporaryRemoteFolder;

	protected static final String AWS_ACCESS_KEY = "test.accessKey";

	protected static final String AWS_SECRET_KEY = "test.secretKey";

	protected static final String AWS_REGION = "us-gov-west-1";

	protected static final String S3_BUCKET = "S3_BUCKET";

	protected static List<S3Object> S3_OBJECTS;

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

		S3_OBJECTS = new ArrayList<>();

		for (File file : f.listFiles()) {
			S3Object s3Object = new S3Object();
			s3Object.setBucketName(S3_BUCKET);
			s3Object.setKey(file.getName());
			s3Object.setObjectContent(new FileInputStream(file));
			S3_OBJECTS.add(s3Object);
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
				.isEqualTo(new File(this.awsS3SupplierProperties.getLocalDir() + File.separator + "1.test"));
		sourceMessage = output.receive(10_000);
		actual = new String(sourceMessage.getPayload());
		assertThat(new File(actual.replaceAll("\"", "")))
				.isEqualTo(new File(this.awsS3SupplierProperties.getLocalDir() + File.separator + "2.test"));
	}

	@SpringBootApplication
	@Import({ TestChannelBinderConfiguration.class, AwsS3SupplierConfiguration.class })
	public static class SampleConfiguration {

		@Bean
		@Primary
		public AmazonS3 amazonS3Mock() {
			AmazonS3 amazonS3 = mock(AmazonS3.class);
			willReturn(Region.US_West).given(amazonS3).getRegion();

			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);

			willAnswer(invocation -> {
				ObjectListing objectListing = new ObjectListing();
				List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
				for (S3Object s3Object : S3_OBJECTS) {
					S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
					s3ObjectSummary.setBucketName(S3_BUCKET);
					s3ObjectSummary.setKey(s3Object.getKey());
					s3ObjectSummary.setLastModified(calendar.getTime());
					objectSummaries.add(s3ObjectSummary);
				}
				return objectListing;
			}).given(amazonS3).listObjects(any(ListObjectsRequest.class));

			for (final S3Object s3Object : S3_OBJECTS) {
				willAnswer(invocation -> s3Object).given(amazonS3).getObject(S3_BUCKET, s3Object.getKey());
			}
			return amazonS3;
		}

	}

}
