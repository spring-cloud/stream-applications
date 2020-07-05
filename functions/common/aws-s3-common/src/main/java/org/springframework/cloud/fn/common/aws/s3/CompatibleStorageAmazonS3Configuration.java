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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Timo Salm
 */
@Configuration
@EnableConfigurationProperties(AmazonS3Properties.class)
public class CompatibleStorageAmazonS3Configuration {

	@Bean
	@Primary
	@ConditionalOnProperty("s3.common.endpoint-url")
	public AmazonS3 compatibleStorageAmazonS3(AWSCredentialsProvider awsCredentialsProvider, RegionProvider regionProvider,
							AmazonS3Properties amazonS3Properties) {
		final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		final EndpointConfiguration endpointConfiguration = new EndpointConfiguration(
				amazonS3Properties.getEndpointUrl(), regionProvider.getRegion().getName());
		builder.setEndpointConfiguration(endpointConfiguration);
		return builder
				.withCredentials(awsCredentialsProvider)
				.build();
	}
}
