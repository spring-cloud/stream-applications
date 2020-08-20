/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.ftp;

import java.util.function.Supplier;

import org.apache.commons.net.ftp.FTPFile;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.file.FileConsumerProperties;
import org.springframework.cloud.fn.common.file.FileReadingMode;
import org.springframework.cloud.fn.common.file.FileUtils;
import org.springframework.cloud.fn.common.ftp.FtpSessionFactoryConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.dsl.FtpInboundChannelAdapterSpec;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.util.IntegrationReactiveUtils;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

/**
 * @author David Turanski
 * @author Marius Bogoevici
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties({ FtpSupplierProperties.class, FileConsumerProperties.class })
@Import(FtpSessionFactoryConfiguration.class)
public class FtpSupplierConfiguration {

	private final FtpSupplierProperties ftpSupplierProperties;

	private final FileConsumerProperties fileConsumerProperties;

	private ConcurrentMetadataStore metadataStore;

	SessionFactory<FTPFile> ftpSessionFactory;

	@Autowired
	@Lazy
	@Qualifier("ftpMessageSource")
	private FtpInboundFileSynchronizingMessageSource ftpMessageSource;

	public FtpSupplierConfiguration(FtpSupplierProperties ftpSupplierProperties,
			FileConsumerProperties fileConsumerProperties,
			ConcurrentMetadataStore metadataStore,
			SessionFactory<FTPFile> ftpSessionFactory) {
		this.ftpSupplierProperties = ftpSupplierProperties;
		this.fileConsumerProperties = fileConsumerProperties;
		this.metadataStore = metadataStore;
		this.ftpSessionFactory = ftpSessionFactory;
	}

	@Bean
	public FtpInboundChannelAdapterSpec ftpMessageSource() {
		FtpInboundChannelAdapterSpec messageSourceBuilder = Ftp.inboundAdapter(ftpSessionFactory)
				.preserveTimestamp(this.ftpSupplierProperties.isPreserveTimestamp())
				.remoteDirectory(this.ftpSupplierProperties.getRemoteDir())
				.remoteFileSeparator(this.ftpSupplierProperties.getRemoteFileSeparator())
				.localDirectory(this.ftpSupplierProperties.getLocalDir())
				.autoCreateLocalDirectory(this.ftpSupplierProperties.isAutoCreateLocalDir())
				.temporaryFileSuffix(this.ftpSupplierProperties.getTmpFileSuffix())
				.deleteRemoteFiles(this.ftpSupplierProperties.isDeleteRemoteFiles());

		ChainFileListFilter<FTPFile> chainFileListFilter = new ChainFileListFilter<>();

		if (StringUtils.hasText(this.ftpSupplierProperties.getFilenamePattern())) {
			chainFileListFilter.addFilter(new FtpSimplePatternFileListFilter(this.ftpSupplierProperties.getFilenamePattern()));
		}
		else if (this.ftpSupplierProperties.getFilenameRegex() != null) {
			chainFileListFilter.addFilter(new FtpRegexPatternFileListFilter(this.ftpSupplierProperties.getFilenameRegex()));
		}

		chainFileListFilter.addFilter(new FtpPersistentAcceptOnceFileListFilter(this.metadataStore, "ftpSource/"));

		messageSourceBuilder.filter(chainFileListFilter);
		return messageSourceBuilder;
	}

	@Bean
	public Flux<Message<?>> ftpMessageFlux() {
		return Mono.<Message<?>>create(monoSink ->
				monoSink.onRequest(value ->
						monoSink.success(this.ftpMessageSource.receive())))
				.subscribeOn(Schedulers.boundedElastic())
				.repeatWhenEmpty(it -> it.delayElements(this.ftpSupplierProperties.getDelayWhenEmpty()))
				.repeat();
	}

	@Bean
	@ConditionalOnExpression("environment['file.consumer.mode'] != 'ref'")
	public Publisher<Message<Object>> ftpReadingFlow() {
		return FileUtils.enhanceFlowForReadingMode(IntegrationFlows
				.from(IntegrationReactiveUtils.messageSourceToFlux(ftpMessageSource().get())), fileConsumerProperties)
				.toReactivePublisher();
	}

	@Bean
	public Supplier<Flux<Message<?>>> ftpSupplier() {
		if (this.fileConsumerProperties.getMode() == FileReadingMode.ref) {
			return this::ftpMessageFlux;
		}
		else {
			return () -> Flux.from(ftpReadingFlow());
		}
	}
}
