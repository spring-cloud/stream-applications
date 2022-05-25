/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.cloud.fn.supplier.file;

import java.io.File;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.config.ComponentCustomizer;
import org.springframework.cloud.fn.common.file.FileConsumerProperties;
import org.springframework.cloud.fn.common.file.FileReadingMode;
import org.springframework.cloud.fn.common.file.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.dsl.FileInboundChannelAdapterSpec;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 * @author Soby Chacko
 */
@Configuration
@EnableConfigurationProperties({FileSupplierProperties.class, FileConsumerProperties.class})
public class FileSupplierConfiguration {

	private static final String METADATA_STORE_PREFIX = "local-file-system-metadata-";

	private final FileSupplierProperties fileSupplierProperties;

	private final FileConsumerProperties fileConsumerProperties;

	@Autowired
	@Lazy
	@Qualifier("fileMessageSource")
	private FileReadingMessageSource fileMessageSource;

	public FileSupplierConfiguration(FileSupplierProperties fileSupplierProperties,
									FileConsumerProperties fileConsumerProperties) {

		this.fileSupplierProperties = fileSupplierProperties;
		this.fileConsumerProperties = fileConsumerProperties;
	}

	@Bean
	public ChainFileListFilter<File> filter(ConcurrentMetadataStore metadataStore) {
		ChainFileListFilter<File> chainFilter = new ChainFileListFilter<>();
		if (StringUtils.hasText(this.fileSupplierProperties.getFilenamePattern())) {
			chainFilter.addFilter(new SimplePatternFileListFilter(this.fileSupplierProperties.getFilenamePattern()));
		}
		else if (this.fileSupplierProperties.getFilenameRegex() != null) {
			chainFilter.addFilter(new RegexPatternFileListFilter(this.fileSupplierProperties.getFilenameRegex()));
		}

		if (this.fileSupplierProperties.isPreventDuplicates()) {
			chainFilter.addFilter(new FileSystemPersistentAcceptOnceFileListFilter(metadataStore, METADATA_STORE_PREFIX));
		}

		return chainFilter;
	}

	@Bean
	public FileInboundChannelAdapterSpec fileMessageSource(FileListFilter<File> fileListFilter,
			@Nullable ComponentCustomizer<FileInboundChannelAdapterSpec> fileInboundChannelAdapterSpecCustomizer) {

		FileInboundChannelAdapterSpec adapterSpec =
				Files.inboundAdapter(this.fileSupplierProperties.getDirectory())
				.filter(fileListFilter);
		if (fileInboundChannelAdapterSpecCustomizer != null) {
			fileInboundChannelAdapterSpecCustomizer.customize(adapterSpec, "fileMessageSource");
		}
		return adapterSpec;
	}

	@Bean
	public Flux<Message<?>> fileMessageFlux() {
		return Mono.<Message<?>>create(monoSink ->
				monoSink.onRequest(value ->
						monoSink.success(this.fileMessageSource.receive())))
				.subscribeOn(Schedulers.boundedElastic())
				.repeatWhenEmpty(it -> it.delayElements(this.fileSupplierProperties.getDelayWhenEmpty()))
				.repeat()
				.doOnRequest(r -> this.fileMessageSource.start());
	}

	@Bean
	@ConditionalOnExpression("environment['file.consumer.mode'] != 'ref'")
	public Publisher<Message<Object>> fileReadingFlow() {
		IntegrationFlowBuilder flowBuilder = IntegrationFlows.from(fileMessageFlux());
		return FileUtils.enhanceFlowForReadingMode(flowBuilder, this.fileConsumerProperties)
				.toReactivePublisher();
	}

	@Bean
	public Supplier<Flux<Message<?>>> fileSupplier() {
		if (this.fileConsumerProperties.getMode() == FileReadingMode.ref) {
			return this::fileMessageFlux;
		}
		else {
			return () -> Flux.from(fileReadingFlow());
		}
	}

}
