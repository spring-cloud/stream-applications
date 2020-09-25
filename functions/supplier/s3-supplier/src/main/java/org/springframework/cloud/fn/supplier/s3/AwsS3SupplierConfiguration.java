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
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.MonoProcessor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.integration.endpoint.ReactiveMessageSourceProducer;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.util.IntegrationReactiveUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 * @author David Turanski
 */
@Configuration
@EnableConfigurationProperties({ AwsS3SupplierProperties.class, FileConsumerProperties.class })
public abstract class AwsS3SupplierConfiguration {

	protected static final String METADATA_STORE_PREFIX = "s3-metadata-";

	protected final AwsS3SupplierProperties awsS3SupplierProperties;

	protected final FileConsumerProperties fileConsumerProperties;

	protected final AmazonS3 amazonS3;

	protected final S3SessionFactory s3SessionFactory;

	protected final ConcurrentMetadataStore metadataStore;

	public AwsS3SupplierConfiguration(AwsS3SupplierProperties awsS3SupplierProperties,
			FileConsumerProperties fileConsumerProperties,
			AmazonS3 amazonS3,
			S3SessionFactory s3SessionFactory, ConcurrentMetadataStore metadataStore) {
		this.awsS3SupplierProperties = awsS3SupplierProperties;
		this.fileConsumerProperties = fileConsumerProperties;
		this.amazonS3 = amazonS3;
		this.s3SessionFactory = s3SessionFactory;
		this.metadataStore = metadataStore;
	}

	@Configuration
	@ConditionalOnProperty(prefix = "s3.supplier", name = "list-only", havingValue = "false", matchIfMissing = true)
	static class SynchronizingConfiguration extends AwsS3SupplierConfiguration {

		private final MonoProcessor<Subscription> downstreamSubscription = MonoProcessor.create();

		@Bean
		public Supplier<Flux<Message<?>>> s3Supplier(Publisher<Message<?>> s3SupplierFlow) {
			return () -> Flux.from(s3SupplierFlow).doOnSubscribe(this.downstreamSubscription::onNext);
		}

		@Bean
		public ChainFileListFilter<S3ObjectSummary> filter(ConcurrentMetadataStore metadataStore) {
			ChainFileListFilter<S3ObjectSummary> chainFilter = new ChainFileListFilter<>();
			if (StringUtils.hasText(this.awsS3SupplierProperties.getFilenamePattern())) {
				chainFilter.addFilter(
						new S3SimplePatternFileListFilter(this.awsS3SupplierProperties.getFilenamePattern()));
			}
			else if (this.awsS3SupplierProperties.getFilenameRegex() != null) {
				chainFilter
						.addFilter(new S3RegexPatternFileListFilter(this.awsS3SupplierProperties.getFilenameRegex()));
			}

			chainFilter.addFilter(new S3PersistentAcceptOnceFileListFilter(metadataStore, METADATA_STORE_PREFIX));
			return chainFilter;
		}

		SynchronizingConfiguration(AwsS3SupplierProperties awsS3SupplierProperties,
				FileConsumerProperties fileConsumerProperties,
				AmazonS3 amazonS3,
				S3SessionFactory s3SessionFactory,
				ConcurrentMetadataStore concurrentMetadataStore) {
			super(awsS3SupplierProperties, fileConsumerProperties, amazonS3, s3SessionFactory,
					concurrentMetadataStore);
		}

		@Bean
		public Publisher<Message<Object>> s3SupplierFlow(MessageSource<?> s3MessageSource) {
			return FileUtils.enhanceFlowForReadingMode(
					IntegrationFlows.from(
							IntegrationReactiveUtils.messageSourceToFlux(s3MessageSource)
									.delaySubscription(this.downstreamSubscription)),
					fileConsumerProperties)
					.toReactivePublisher();
		}

		@Bean
		public S3InboundFileSynchronizer s3InboundFileSynchronizer(ChainFileListFilter<S3ObjectSummary> filter) {

			S3InboundFileSynchronizer synchronizer = new S3InboundFileSynchronizer(s3SessionFactory);
			synchronizer.setDeleteRemoteFiles(this.awsS3SupplierProperties.isDeleteRemoteFiles());
			synchronizer.setPreserveTimestamp(this.awsS3SupplierProperties.isPreserveTimestamp());
			String remoteDir = this.awsS3SupplierProperties.getRemoteDir();
			synchronizer.setRemoteDirectory(remoteDir);
			synchronizer.setRemoteFileSeparator(this.awsS3SupplierProperties.getRemoteFileSeparator());
			synchronizer.setTemporaryFileSuffix(this.awsS3SupplierProperties.getTmpFileSuffix());
			synchronizer.setFilter(filter);

			return synchronizer;
		}

		@Bean
		public MessageSource<File> s3MessageSource(S3InboundFileSynchronizer s3InboundFileSynchronizer) {
			S3InboundFileSynchronizingMessageSource s3MessageSource = new S3InboundFileSynchronizingMessageSource(
					s3InboundFileSynchronizer);
			s3MessageSource.setLocalDirectory(this.awsS3SupplierProperties.getLocalDir());
			s3MessageSource.setAutoCreateLocalDirectory(this.awsS3SupplierProperties.isAutoCreateLocalDir());
			return s3MessageSource;
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "s3.supplier", name = "list-only", havingValue = "true")
	static class ListOnlyConfiguration extends AwsS3SupplierConfiguration {

		ListOnlyConfiguration(AwsS3SupplierProperties awsS3SupplierProperties,
				FileConsumerProperties fileConsumerProperties,
				AmazonS3 amazonS3, S3SessionFactory s3SessionFactory, ConcurrentMetadataStore metadataStore) {
			super(awsS3SupplierProperties, fileConsumerProperties, amazonS3, s3SessionFactory,
					metadataStore);
		}

		private final MonoProcessor<Subscription> downstreamSubscription = MonoProcessor.create();

		@Bean
		public Supplier<Flux<Message<Object>>> s3Supplier(Publisher<Message<Object>> s3SupplierFlow) {
			return () -> Flux.from(s3SupplierFlow)
					.doOnSubscribe(downstreamSubscription::onNext);
		}

		@Bean
		public Publisher<Message<Object>> s3SupplierFlow(ReactiveMessageSourceProducer s3ListingProducer) {
			return IntegrationFlows.from(s3ListingProducer).split().toReactivePublisher();
		}

		@Bean
		Predicate<S3ObjectSummary> listOnlyFilter() {
			Predicate<S3ObjectSummary> predicate = s -> true;
			if (StringUtils.hasText(this.awsS3SupplierProperties.getFilenamePattern())) {
				Pattern pattern = Pattern.compile(this.awsS3SupplierProperties.getFilenamePattern());
				predicate = (S3ObjectSummary summary) -> pattern.matcher(summary.getKey()).matches();
			}
			else if (this.awsS3SupplierProperties.getFilenameRegex() != null) {
				predicate = (S3ObjectSummary summary) -> this.awsS3SupplierProperties.getFilenameRegex()
						.matcher(summary.getKey()).matches();
			}
			predicate = predicate.and((S3ObjectSummary summary) -> {
				final String key = METADATA_STORE_PREFIX + summary.getBucketName() + "-" + summary.getKey();
				final String lastModified = String.valueOf(summary.getLastModified().getTime());
				final String storedLastModified = this.metadataStore.get(key);
				boolean result = !lastModified.equals(storedLastModified);
				if (result) {
					metadataStore.put(key, lastModified);
				}
				return result;
			});

			return predicate;
		}

		@Bean
		ReactiveMessageSourceProducer s3ListingMessageProducer(AmazonS3 amazonS3,
				AwsS3SupplierProperties awsS3SupplierProperties, Predicate<S3ObjectSummary> filter) {
			ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
			listObjectsRequest.setBucketName(awsS3SupplierProperties.getRemoteDir());
			return new ReactiveMessageSourceProducer(
					(MessageSource<List<S3ObjectSummary>>) () -> {
						List<S3ObjectSummary> summaryList = amazonS3.listObjects(listObjectsRequest)
								.getObjectSummaries().stream()
								.filter(filter).collect(Collectors.toList());
						return summaryList.isEmpty() ? null : new GenericMessage<>(summaryList);
					}) {
				@Override
				protected void subscribeToPublisher(Publisher<? extends Message<?>> publisher) {
					super.subscribeToPublisher(Flux.from(publisher).delaySubscription(downstreamSubscription));
				}
			};
		}
	}
}
