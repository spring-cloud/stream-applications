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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.aws.outbound.S3MessageHandler;
import org.springframework.integration.aws.support.AwsHeaders;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsS3ConsumerProperties.class)
public class AwsS3ConsumerConfiguration {

	@Bean
	public Consumer<Message<?>> s3Consumer(IntegrationFlow s3ConsumerFlow) {
		return s3ConsumerFlow.getInputChannel()::send;
	}

	@Bean
	public IntegrationFlow s3ConsumerFlow(@Nullable TransferListener transferListener,
			MessageHandler amazonS3MessageHandler) {

		return flow -> flow
				.enrichHeaders(headers -> headers.header(AwsHeaders.TRANSFER_LISTENER, transferListener))
				.handle(amazonS3MessageHandler);
	}

	@Bean
	public MessageHandler amazonS3MessageHandler(S3TransferManager s3TransferManager,
			AwsS3ConsumerProperties s3ConsumerProperties,
			BeanFactory beanFactory,
			@Nullable BiConsumer<PutObjectRequest.Builder, Message<?>> uploadMetadataProvider) {

		Expression bucketExpression = s3ConsumerProperties.getBucketExpression();
		if (s3ConsumerProperties.getBucket() != null) {
			bucketExpression = new ValueExpression<>(s3ConsumerProperties.getBucket());
		}

		S3MessageHandler s3MessageHandler = new S3MessageHandler(s3TransferManager, bucketExpression);
		s3MessageHandler.setKeyExpression(s3ConsumerProperties.getKeyExpression());

		Expression aclExpression;

		if (s3ConsumerProperties.getAcl() != null) {
			aclExpression = new ValueExpression<>(s3ConsumerProperties.getAcl());
		}
		else {
			aclExpression = s3ConsumerProperties.getAclExpression();
		}

		BiConsumer<PutObjectRequest.Builder, Message<?>> metadataProviderToUse = uploadMetadataProvider;

		if (aclExpression != null) {
			EvaluationContext evaluationContext = IntegrationContextUtils.getEvaluationContext(beanFactory);

			metadataProviderToUse =
					(builder, message) -> {
						Object aclValue = aclExpression.getValue(evaluationContext, message);
						Assert.notNull(aclValue,
								() -> String.format("The expression '%s' for message '%s' returned null",
										aclExpression, message));

						builder.acl(aclValue.toString());

						if (uploadMetadataProvider != null) {
							uploadMetadataProvider.accept(builder, message);
						}
					};
		}

		if (metadataProviderToUse != null) {
			s3MessageHandler.setUploadMetadataProvider(metadataProviderToUse);
		}
		return s3MessageHandler;
	}

}
