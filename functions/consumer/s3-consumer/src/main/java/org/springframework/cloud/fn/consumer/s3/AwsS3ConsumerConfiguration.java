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

import java.util.function.Consumer;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.internal.S3ProgressListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.aws.outbound.S3MessageHandler;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

@Configuration
@EnableConfigurationProperties(AwsS3ConsumerProperties.class)
public class AwsS3ConsumerConfiguration {

	@Autowired(required = false)
	private S3MessageHandler.UploadMetadataProvider uploadMetadataProvider;

	@Autowired(required = false)
	private S3ProgressListener s3ProgressListener;

	@Bean
	public Consumer<Message<?>> s3Consumer() {
		return amazonS3MessageHandler(null, null, null)::handleMessage;
	}

	@Bean
	public MessageHandler amazonS3MessageHandler(AmazonS3 amazonS3, ResourceIdResolver resourceIdResolver,
												AwsS3ConsumerProperties s3ConsumerProperties) {
		S3MessageHandler s3MessageHandler;
		if (s3ConsumerProperties.getBucket() != null) {
			s3MessageHandler = new S3MessageHandler(amazonS3, s3ConsumerProperties.getBucket());
		}
		else {
			s3MessageHandler = new S3MessageHandler(amazonS3, s3ConsumerProperties.getBucketExpression());
		}
		s3MessageHandler.setResourceIdResolver(resourceIdResolver);
		s3MessageHandler.setKeyExpression(s3ConsumerProperties.getKeyExpression());
		if (s3ConsumerProperties.getAcl() != null) {
			s3MessageHandler.setObjectAclExpression(new ValueExpression<>(s3ConsumerProperties.getAcl()));
		}
		else {
			s3MessageHandler.setObjectAclExpression(s3ConsumerProperties.getAclExpression());
		}
		s3MessageHandler.setUploadMetadataProvider(this.uploadMetadataProvider);
		s3MessageHandler.setProgressListener(this.s3ProgressListener);
		return s3MessageHandler;
	}
}
