/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.cloud.fn.supplier.sftp;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.MonoProcessor;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.file.FileConsumerProperties;
import org.springframework.cloud.fn.common.file.FileUtils;
import org.springframework.cloud.fn.common.file.remote.RemoteFileDeletingAdvice;
import org.springframework.cloud.fn.common.file.remote.RemoteFileRenamingAdvice;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.integration.aop.ReceiveMessageAdvice;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.dsl.SftpInboundChannelAdapterSpec;
import org.springframework.integration.sftp.dsl.SftpOutboundGatewaySpec;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.integration.util.IntegrationReactiveUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Chris Schaefer
 * @author Christian Tzolov
 * @author David Turanski
 */

@Configuration
@EnableConfigurationProperties({ SftpSupplierProperties.class, FileConsumerProperties.class })
@Import({ SftpSupplierFactoryConfiguration.class })
public class SftpSupplierConfiguration {

	private static final String METADATA_STORE_PREFIX = "sftpSource/";

	private static final String FILE_MODIFIED_TIME_HEADER = "FILE_MODIFIED_TIME";

	@Bean
	public MonoProcessor<Boolean> subscriptionBarrier() {
		return MonoProcessor.create();
	}

	@Bean
	public Supplier<Flux<? extends Message<?>>> sftpSupplier(MessageSource<?> sftpMessageSource,
															@Nullable Publisher<Message<Object>> sftpReadingFlow,
															MonoProcessor<Boolean> subscriptionBarrier,
															SftpSupplierProperties sftpSupplierProperties) {

		Flux<? extends Message<?>> flux = sftpReadingFlow == null
				? sftpMessageFlux(sftpMessageSource, sftpSupplierProperties, subscriptionBarrier)
				: Flux.from(sftpReadingFlow);

		return () -> flux.doOnRequest(l -> subscriptionBarrier.onNext(true));
	}

	@Bean
	@Primary
	public MessageSource<?> sftpMessageSource(
			MessageSource<?> messageSource,
			BeanFactory beanFactory,
			@Nullable List<ReceiveMessageAdvice> receiveMessageAdvice) {

		if (CollectionUtils.isEmpty(receiveMessageAdvice)) {
			return messageSource;
		}

		ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
		proxyFactoryBean.setTarget(messageSource);
		proxyFactoryBean.setBeanFactory(beanFactory);
		receiveMessageAdvice.stream().map(advice -> {
			NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(advice);
			advisor.addMethodName("receive");
			return advisor;
		}).forEach(proxyFactoryBean::addAdvisor);

		return (MessageSource<?>) proxyFactoryBean.getObject();
	}

	/*
	 * Configure the standard filters for SFTP inbound adapters.
	 */
	@Bean
	public FileListFilter<LsEntry> chainFilter(SftpSupplierProperties sftpSupplierProperties,
			ConcurrentMetadataStore metadataStore) {

		ChainFileListFilter<LsEntry> chainFilter = new ChainFileListFilter<>();

		if (StringUtils.hasText(sftpSupplierProperties.getFilenamePattern())) {
			chainFilter.addFilter(new SftpSimplePatternFileListFilter(sftpSupplierProperties.getFilenamePattern()));
		}
		else if (sftpSupplierProperties.getFilenameRegex() != null) {
			chainFilter.addFilter(new SftpRegexPatternFileListFilter(sftpSupplierProperties.getFilenameRegex()));
		}

		chainFilter.addFilter(new SftpPersistentAcceptOnceFileListFilter(metadataStore, METADATA_STORE_PREFIX));
		return chainFilter;
	}

	/*
	 * Create a Flux from a MessageSource that will be used by the supplier.
	 */
	private Flux<? extends Message<?>> sftpMessageFlux(MessageSource<?> sftpMessageSource,
			SftpSupplierProperties sftpSupplierProperties, MonoProcessor<?> subscriptionBarrier) {

		return IntegrationReactiveUtils.messageSourceToFlux(sftpMessageSource)
				.delaySubscription(subscriptionBarrier)
				.subscriberContext(context -> context.put(IntegrationReactiveUtils.DELAY_WHEN_EMPTY_KEY,
						sftpSupplierProperties.getDelayWhenEmpty()));

	}

	private static String remoteDirectory(SftpSupplierProperties sftpSupplierProperties) {
		return sftpSupplierProperties.isMultiSource()
				? SftpSupplierProperties.keyDirectories(sftpSupplierProperties).get(0).getDirectory()
				: sftpSupplierProperties.getRemoteDir();
	}

	@Configuration
	@ConditionalOnProperty(prefix = "sftp.supplier", name = "stream")
	static class StreamingConfiguration {

		@Bean
		public SftpRemoteFileTemplate sftpTemplate(SftpSupplierFactoryConfiguration.DelegatingFactoryWrapper wrapper) {
			return new SftpRemoteFileTemplate(wrapper.getFactory());
		}

		/**
		 * Streaming {@link MessageSource} that provides an InputStream for each remote file. It
		 * does not synchronize files to a local directory.
		 * @return a {@link MessageSource}.
		 */
		@Bean
		public MessageSource<?> targetMessageSource(SftpRemoteFileTemplate sftpTemplate,
				SftpSupplierProperties sftpSupplierProperties,
				FileListFilter<LsEntry> fileListFilter) {

			return Sftp.inboundStreamingAdapter(sftpTemplate)
					.remoteDirectory(remoteDirectory(sftpSupplierProperties))
					.remoteFileSeparator(sftpSupplierProperties.getRemoteFileSeparator())
					.filter(fileListFilter)
					.maxFetchSize(sftpSupplierProperties.getMaxFetch()).get();
		}

		@Bean
		public Publisher<Message<Object>> sftpReadingFlow(
				MessageSource<?> sftpMessageSource,
				MonoProcessor<?> subscriptionBarrier,
				SftpSupplierProperties sftpSupplierProperties,
				FileConsumerProperties fileConsumerProperties) {

			return FileUtils.enhanceStreamFlowForReadingMode(IntegrationFlows
							.from(IntegrationReactiveUtils.messageSourceToFlux(sftpMessageSource)
									.delaySubscription(subscriptionBarrier)
									.subscriberContext(
											context -> (context.put(IntegrationReactiveUtils.DELAY_WHEN_EMPTY_KEY,
													sftpSupplierProperties.getDelayWhenEmpty())))),
					fileConsumerProperties)
					.toReactivePublisher();
		}

		@Bean
		@ConditionalOnProperty(prefix = "sftp.supplier", value = "delete-remote-files")
		public RemoteFileDeletingAdvice remoteFileDeletingAdvice(SftpRemoteFileTemplate sftpTemplate,
				SftpSupplierProperties sftpSupplierProperties) {

			return new RemoteFileDeletingAdvice(sftpTemplate, sftpSupplierProperties.getRemoteFileSeparator());
		}

		@Bean
		@ConditionalOnProperty(prefix = "sftp.supplier", value = "rename-remote-files-to")
		public RemoteFileRenamingAdvice remoteFileRenamingAdvice(SftpRemoteFileTemplate sftpTemplate,
																SftpSupplierProperties sftpSupplierProperties) {

			return new RemoteFileRenamingAdvice(sftpTemplate, sftpSupplierProperties.getRemoteFileSeparator(),
					sftpSupplierProperties.getRenameRemoteFilesTo());
		}
	}

	@Configuration
	@ConditionalOnExpression("environment['sftp.supplier.stream']!='true'")
	static class NonStreamingConfiguration {

		/**
		 * Enrich the flow to provide some standard headers, depending on
		 * {@link FileConsumerProperties}, when consuming file contents.
		 * @param sftpMessageSource the {@link MessageSource}.
		 * @param fileConsumerProperties the {@code FileConsumerProperties}.
		 * @return a {@code Publisher<Message>}.
		 */
		@Bean
		@ConditionalOnExpression("environment['file.consumer.mode']!='ref' && environment['sftp.supplier.list-only']!='true'")
		public Publisher<Message<Object>> sftpReadingFlow(
				MessageSource<?> sftpMessageSource,
				MonoProcessor<?> subscriptionBarrier,
				SftpSupplierProperties sftpSupplierProperties,
				FileConsumerProperties fileConsumerProperties,
				@Nullable MessageHandler renameRemoteFileHandler) {

			IntegrationFlowBuilder flowBuilder = FileUtils.enhanceFlowForReadingMode(IntegrationFlows
							.from(IntegrationReactiveUtils.messageSourceToFlux(sftpMessageSource)
									.delaySubscription(subscriptionBarrier)
									.subscriberContext(
											context -> (context.put(IntegrationReactiveUtils.DELAY_WHEN_EMPTY_KEY,
													sftpSupplierProperties.getDelayWhenEmpty())))),
					fileConsumerProperties);

			if (renameRemoteFileHandler != null) {
				flowBuilder.publishSubscribeChannel(pubsub ->
						pubsub.subscribe(subFlow -> subFlow.handle(renameRemoteFileHandler).nullChannel())
				);
			}

			return flowBuilder.toReactivePublisher();
		}

		/**
		 * A {@link MessageSource} that synchronizes files to a local directory.
		 * @return the {code MessageSource}.
		 */
		@ConditionalOnExpression("environment['sftp.supplier.list-only'] != 'true'")
		@Bean
		public SftpInboundChannelAdapterSpec targetMessageSource(SftpSupplierProperties sftpSupplierProperties,
				SftpSupplierFactoryConfiguration.DelegatingFactoryWrapper delegatingFactoryWrapper,
				FileListFilter<LsEntry> fileListFilter) {

			return Sftp
					.inboundAdapter(delegatingFactoryWrapper.getFactory())
					.preserveTimestamp(sftpSupplierProperties.isPreserveTimestamp())
					.autoCreateLocalDirectory(sftpSupplierProperties.isAutoCreateLocalDir())
					.deleteRemoteFiles(sftpSupplierProperties.isDeleteRemoteFiles())
					.localDirectory(sftpSupplierProperties.getLocalDir())
					.remoteDirectory(remoteDirectory(sftpSupplierProperties))
					.remoteFileSeparator(sftpSupplierProperties.getRemoteFileSeparator())
					.temporaryFileSuffix(sftpSupplierProperties.getTmpFileSuffix())
					.metadataStorePrefix(METADATA_STORE_PREFIX)
					.maxFetchSize(sftpSupplierProperties.getMaxFetch())
					.filter(fileListFilter);
		}

		@Bean
		@ConditionalOnProperty(prefix = "sftp.supplier", value = "rename-remote-files-to")
		public SftpOutboundGatewaySpec renameRemoteFileHandler(
				SftpSupplierFactoryConfiguration.DelegatingFactoryWrapper delegatingFactoryWrapper,
				SftpSupplierProperties sftpSupplierProperties) {

			return Sftp.outboundGateway(delegatingFactoryWrapper.getFactory(),
							AbstractRemoteFileOutboundGateway.Command.MV.getCommand(),
							String.format("headers.get('%s') + '%s' + headers.get('%s')",
									FileHeaders.REMOTE_DIRECTORY,
									sftpSupplierProperties.getRemoteFileSeparator(),
									FileHeaders.REMOTE_FILE))
					.renameExpression(sftpSupplierProperties.getRenameRemoteFilesTo());
		}
	}

	/*
	 * List only configuration
	 */
	@Configuration
	@ConditionalOnProperty(prefix = "sftp.supplier", name = "list-only")
	static class ListingOnlyConfiguration {

		@Bean
		PollableChannel listingChannel() {
			return new QueueChannel();
		}

		@Bean
		@SuppressWarnings("unchecked")
		public MessageSource<?> targetMessageSource(PollableChannel listingChannel,
				SftpListingMessageProducer sftpListingMessageProducer) {
			return () -> {
				sftpListingMessageProducer.listNames();
				return (Message<Object>) listingChannel.receive();
			};

		}

		@Bean
		public SftpListingMessageProducer sftpListingMessageProducer(SftpSupplierProperties sftpSupplierProperties,
				SftpSupplierFactoryConfiguration.DelegatingFactoryWrapper delegatingFactoryWrapper) {

			return new SftpListingMessageProducer(delegatingFactoryWrapper.getFactory(),
					remoteDirectory(sftpSupplierProperties),
					sftpSupplierProperties.getRemoteFileSeparator(),
					sftpSupplierProperties.getSortBy()
			);
		}

		@Bean
		GenericSelector<String> listOnlyFilter(SftpSupplierProperties sftpSupplierProperties) {
			Predicate<String> predicate = s -> true;
			if (StringUtils.hasText(sftpSupplierProperties.getFilenamePattern())) {
				predicate = Pattern.compile(sftpSupplierProperties.getFilenamePattern()).asPredicate();
			}
			else if (sftpSupplierProperties.getFilenameRegex() != null) {
				predicate = sftpSupplierProperties.getFilenameRegex().asPredicate();
			}

			return predicate::test;
		}

		@Bean
		public IntegrationFlow listingFlow(MessageProducerSupport listingMessageProducer,
				MessageChannel listingChannel, MessageProcessor<?> lsEntryToStringTransformer,
				GenericSelector<Message<?>> duplicateFilter,
				GenericSelector<String> listOnlyFilter) {

			return IntegrationFlows.from(listingMessageProducer)
					.split()
					.transform(lsEntryToStringTransformer)
					.filter(duplicateFilter)
					.filter(listOnlyFilter)
					.channel(listingChannel)
					.get();
		}

		@Bean
		public MessageProcessor<Message<?>> lsEntryToStringTransformer() {
			return (Message<?> message) -> {

				LsEntry lsEntry = (LsEntry) message.getPayload();

				String fileName = message.getHeaders().get(FileHeaders.REMOTE_DIRECTORY) + lsEntry.getFilename();

				return MessageBuilder.withPayload(fileName)
						.copyHeaders(message.getHeaders())
						.setHeader(FILE_MODIFIED_TIME_HEADER, String.valueOf(lsEntry.getAttrs().getMTime()))
						.setHeader(MessageHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
						.build();
			};

		}

		@Bean
		GenericSelector<Message<?>> duplicateFilter(ConcurrentMetadataStore metadataStore) {
			return new GenericSelector<Message<?>>() {
				@Override
				public boolean accept(Message<?> message) {

					String lastModifiedTime = (String) message.getHeaders().get(FILE_MODIFIED_TIME_HEADER);
					String storedLastModifiedTime = metadataStore.get(METADATA_STORE_PREFIX + message.getPayload());

					boolean result = !lastModifiedTime.equals(storedLastModifiedTime);

					if (result) {
						metadataStore.put(
								METADATA_STORE_PREFIX + message.getPayload(),
								message.getHeaders().get(FILE_MODIFIED_TIME_HEADER).toString());
					}
					return result;
				}
			};
		}

		static class SftpListingMessageProducer extends MessageProducerSupport {

			private final String remoteDirectory;

			private final SessionFactory<LsEntry> sessionFactory;

			private final String remoteFileSeparator;

			private final SftpSupplierProperties.SortSpec sort;

			SftpListingMessageProducer(SessionFactory<LsEntry> sessionFactory, String remoteDirectory,
					String remoteFileSeparator, SftpSupplierProperties.SortSpec sort) {

				this.sessionFactory = sessionFactory;
				this.remoteDirectory = remoteDirectory;
				this.remoteFileSeparator = remoteFileSeparator;
				this.sort = sort;
			}

			public void listNames() {
				Stream<LsEntry> stream;
				try {
					stream = Stream.of(this.sessionFactory.getSession().list(this.remoteDirectory))
							.filter(x -> !(x.getAttrs().isDir() || x.getAttrs().isLink()));

					if (sort != null) {
						stream = stream.sorted(sort.comparator());
					}
				}
				catch (IOException e) {
					throw new MessagingException(e.getMessage(), e);
				}
				sendMessage(MessageBuilder.withPayload(stream)
						.setHeader(FileHeaders.REMOTE_DIRECTORY, this.remoteDirectory + this.remoteFileSeparator)
						.build());
			}

		}

	}

}
