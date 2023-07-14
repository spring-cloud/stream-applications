/*
 * Copyright 2020-2023 the original author or authors.
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

package org.springframework.cloud.fn.common.aws.s3;

import io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration;
import io.awspring.cloud.autoconfigure.s3.S3CrtAsyncClientAutoConfiguration;
import io.awspring.cloud.core.region.StaticRegionProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Timo Salm
 * @author Artem Bilan
 */
public class AmazonS3ConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(
							S3AutoConfiguration.class,
							S3CrtAsyncClientAutoConfiguration.class,
							AmazonS3Configuration.class))
			.withUserConfiguration(TestConfiguration.class);

	private static final String TEST_REGION_NAME = "eu-central-1";

	@Test
	public void testAmazonS3Configuration() {
		runner.withPropertyValues().run(context -> {
			S3Client amazonS3 = context.getBean(S3Client.class);
			Assertions.assertNotNull(amazonS3);
			S3Utilities utilities = amazonS3.utilities();
			Assertions.assertEquals(TEST_REGION_NAME,
					TestUtils.getPropertyValue(utilities, "region", Region.class).id());
			Assertions.assertTrue(
					utilities.getUrl(GetUrlRequest.builder().bucket("b").key("k").build()).toString()
							.startsWith("https://s3.eu-central-1.amazonaws.com"));
		});
	}

	@Test
	public void testAmazonS3ConfigurationForS3CompatibleStorage() {
		runner.withPropertyValues(
				"spring.cloud.aws.s3.endpoint=http://localhost:8080"
		).run(context -> {
			S3Client amazonS3 = context.getBean(S3Client.class);
			Assertions.assertNotNull(amazonS3);
			S3Utilities utilities = amazonS3.utilities();
			Assertions.assertTrue(utilities.getUrl(GetUrlRequest.builder().bucket("b").key("k").build()).toString()
					.startsWith("http://localhost:8080"));
		});
	}

	private static class TestConfiguration {

		@Bean
		AwsRegionProvider regionProvider() {
			return new StaticRegionProvider(TEST_REGION_NAME);
		}

		@Bean
		AwsCredentialsProvider awsCredentialsProvider() {
			return StaticCredentialsProvider.create(AwsBasicCredentials.create("accessKey", "secretKey"));
		}

	}

}
