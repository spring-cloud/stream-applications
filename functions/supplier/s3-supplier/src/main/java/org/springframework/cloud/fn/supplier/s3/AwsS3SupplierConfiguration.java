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

package org.springframework.cloud.fn.supplier.s3;

import java.io.File;
import java.util.Arrays;
import java.util.function.Supplier;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.fn.common.file.FileConsumerProperties;
import org.springframework.cloud.fn.common.file.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.aws.inbound.S3InboundFileSynchronizer;
import org.springframework.integration.aws.inbound.S3InboundFileSynchronizingMessageSource;
import org.springframework.integration.aws.support.S3SessionFactory;
import org.springframework.integration.aws.support.filters.S3PersistentAcceptOnceFileListFilter;
import org.springframework.integration.aws.support.filters.S3RegexPatternFileListFilter;
import org.springframework.integration.aws.support.filters.S3SimplePatternFileListFilter;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.util.IntegrationReactiveUtils;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 */
@Configuration
@EnableConfigurationProperties({AwsS3SupplierProperties.class, FileConsumerProperties.class})
public class AwsS3SupplierConfiguration {

	private final AwsS3SupplierProperties awsS3SupplierProperties;
	private final FileConsumerProperties fileConsumerProperties;
	private final AmazonS3 amazonS3;
	private final ResourceIdResolver resourceIdResolver;

	public AwsS3SupplierConfiguration(AwsS3SupplierProperties awsS3SupplierProperties,
									FileConsumerProperties fileConsumerProperties,
									AmazonS3 amazonS3,
									ResourceIdResolver resourceIdResolver) {
		this.awsS3SupplierProperties = awsS3SupplierProperties;
		this.fileConsumerProperties = fileConsumerProperties;
		this.amazonS3 = amazonS3;
		this.resourceIdResolver = resourceIdResolver;
	}

	@Bean
	public S3InboundFileSynchronizer s3InboundFileSynchronizer() {
		S3SessionFactory s3SessionFactory = new S3SessionFactory(this.amazonS3, this.resourceIdResolver);
		S3InboundFileSynchronizer synchronizer = new S3InboundFileSynchronizer(s3SessionFactory);
		synchronizer.setDeleteRemoteFiles(this.awsS3SupplierProperties.isDeleteRemoteFiles());
		synchronizer.setPreserveTimestamp(this.awsS3SupplierProperties.isPreserveTimestamp());
		String remoteDir = this.awsS3SupplierProperties.getRemoteDir();
		synchronizer.setRemoteDirectory(remoteDir);
		synchronizer.setRemoteFileSeparator(this.awsS3SupplierProperties.getRemoteFileSeparator());
		synchronizer.setTemporaryFileSuffix(this.awsS3SupplierProperties.getTmpFileSuffix());

		FileListFilter<S3ObjectSummary> filter = null;
		if (StringUtils.hasText(this.awsS3SupplierProperties.getFilenamePattern())) {
			filter = new S3SimplePatternFileListFilter(this.awsS3SupplierProperties.getFilenamePattern());
		}
		else if (this.awsS3SupplierProperties.getFilenameRegex() != null) {
			filter = new S3RegexPatternFileListFilter(this.awsS3SupplierProperties.getFilenameRegex());
		}
		if (filter != null) {
			synchronizer.setFilter(new ChainFileListFilter<>(Arrays.asList(filter,
					new S3PersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "s3-metadata-"))));
		}
		return synchronizer;
	}

	@Bean
	public MessageSource<File> s3MessageSource() {
		S3InboundFileSynchronizingMessageSource s3MessageSource =
				new S3InboundFileSynchronizingMessageSource(s3InboundFileSynchronizer());
		s3MessageSource.setLocalDirectory(this.awsS3SupplierProperties.getLocalDir());
		s3MessageSource.setAutoCreateLocalDirectory(this.awsS3SupplierProperties.isAutoCreateLocalDir());
		return s3MessageSource;
	}

	@Bean
	public Publisher<Message<Object>> s3SupplierFlow() {
		return FileUtils.enhanceFlowForReadingMode(IntegrationFlows
				.from(IntegrationReactiveUtils.messageSourceToFlux(s3MessageSource())), fileConsumerProperties)
				.toReactivePublisher();
	}

	@Bean
	public Supplier<Flux<Message<?>>> s3Supplier() {
		return () -> Flux.from(s3SupplierFlow());
	}
}
