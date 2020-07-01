/*
 * Copyright 2020-2020 the original author or authors.
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

import java.util.Optional;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3Client;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.cloud.aws.core.region.StaticRegionProvider;

/**
 * @author Timo Salm
 */
public class AmazonS3ConfigurationTest {

	private final AWSCredentials credentials = new BasicAWSCredentials("accessKey", "secretKey");
	private final AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
	private final String testRegionName = "eu-central-1";

	@Test
	public void testAmazonS3Configuration() {
		final StaticRegionProvider regionProvider = new StaticRegionProvider(testRegionName);
		final AmazonS3Client amazonS3 = (AmazonS3Client) new AmazonS3Configuration()
				.amazonS3(credentialsProvider, regionProvider, Optional.empty());
		Assert.assertNotNull(amazonS3);
		Assert.assertEquals(regionProvider.getRegion().getName(), amazonS3.getRegionName());
		Assert.assertTrue(amazonS3.getResourceUrl("b", "k").startsWith("https://s3.eu-central-1.amazonaws.com"));
	}

	@Test
	public void testAmazonS3ConfigurationForS3CompatibleStorage() {
		final EndpointConfiguration endpointConfiguration = new EndpointConfiguration("https://object.ecstestdrive.com",
				testRegionName);
		final AmazonS3Client amazonS3 = (AmazonS3Client) new AmazonS3Configuration()
				.amazonS3(credentialsProvider, new StaticRegionProvider(testRegionName), Optional.of(endpointConfiguration));
		Assert.assertNotNull(amazonS3);
		Assert.assertTrue(amazonS3.getResourceUrl("b", "k").startsWith("https://object.ecstestdrive.com"));
	}
}
