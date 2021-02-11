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

package org.springframework.cloud.fn.supplier.s3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Supplier;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.FileCopyUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"cloud.aws.stack.auto=false",
				"cloud.aws.credentials.accessKey=" + AbstractAwsS3SupplierMockTests.AWS_ACCESS_KEY,
				"cloud.aws.credentials.secretKey=" + AbstractAwsS3SupplierMockTests.AWS_SECRET_KEY,
				"cloud.aws.region.static=" + AbstractAwsS3SupplierMockTests.AWS_REGION,
				"s3.common.endpointUrl=foo",
				"s3.supplier.remoteDir=" + AbstractAwsS3SupplierMockTests.S3_BUCKET})
@DirtiesContext
@SpringIntegrationTest(noAutoStartup = "*")
public abstract class AbstractAwsS3SupplierMockTests {

	@TempDir
	protected static Path temporaryRemoteFolder;

	protected static final String AWS_ACCESS_KEY = "test.accessKey";

	protected static final String AWS_SECRET_KEY = "test.secretKey";

	protected static final String AWS_REGION = "us-gov-west-1";

	protected static final String S3_BUCKET = "S3_BUCKET";

	protected static List<S3Object> S3_OBJECTS;

	@Autowired
	Supplier<Flux<Message<?>>> s3Supplier;

	@Autowired
	protected AwsS3SupplierProperties awsS3SupplierProperties;

	@Autowired
	StandardIntegrationFlow standardIntegrationFlow;

	@BeforeAll
	public static void setup() throws IOException {
		final String remote = temporaryRemoteFolder.toAbsolutePath() + "/remote";
		File f = new File(remote);
		f.mkdirs();
		File aFile = new File(f, "1.test");
		FileCopyUtils.copy("Hello".getBytes(), aFile);
		File bFile = new File(f, "2.test");
		FileCopyUtils.copy("Bye".getBytes(), bFile);
		File otherFile = new File(f, "otherFile");
		FileCopyUtils.copy("Other\nOther2".getBytes(), otherFile);

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
		S3_OBJECTS.stream()
				.map(S3Object::getObjectContent)
				.forEach(stream -> {
					try {
						stream.close();
					}
					catch (IOException e) {
						// Ignore
					}
				});
	}

	@SpringBootApplication
	public static class S3SupplierTestApplication {

		@Bean
		@Primary
		public AmazonS3 amazonS3Mock() {
			AmazonS3 amazonS3 = mock(AmazonS3.class);
			willReturn(Region.US_West).given(amazonS3).getRegion();

			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);

			ObjectListing objectListing = new ObjectListing();
			List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
			for (S3Object s3Object : S3_OBJECTS) {
				S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
				s3ObjectSummary.setBucketName(S3_BUCKET);
				s3ObjectSummary.setKey(s3Object.getKey());
				s3ObjectSummary.setLastModified(calendar.getTime());
				objectSummaries.add(s3ObjectSummary);
			}

			willAnswer(invocation -> objectListing).given(amazonS3).listObjects(any(ListObjectsRequest.class));

			for (final S3Object s3Object : S3_OBJECTS) {
				willAnswer(invocation -> s3Object).given(amazonS3).getObject(S3_BUCKET, s3Object.getKey());
			}
			return amazonS3;
		}
	}
}
