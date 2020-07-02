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

package org.springframework.cloud.fn.common.aws.s3;

import java.util.Optional;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Artem Bilan
 * @author Timo Salm
 */
@Configuration
@ConditionalOnMissingAmazonClient(AmazonS3.class)
public class AmazonS3Configuration {

	@Bean
	@ConditionalOnMissingBean
	public AmazonS3 amazonS3(AWSCredentialsProvider awsCredentialsProvider, RegionProvider regionProvider,
							Optional<EndpointConfiguration> endpointConfiguration) {
		final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		endpointConfiguration.ifPresent(builder::setEndpointConfiguration);
		if (!endpointConfiguration.isPresent()) {
			builder.setRegion(regionProvider.getRegion().getName());
		}
		return builder
				.withCredentials(awsCredentialsProvider)
				.build();
	}

}
