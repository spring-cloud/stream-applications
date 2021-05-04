/*
 * Copyright 2020-2021 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.test.support.sftp.SftpTestSupport;
import org.springframework.http.MediaType;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.json.JsonPathUtils;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

public class SftpSupplierApplicationTests extends SftpTestSupport {

	ApplicationContextRunner defaultApplicationContextRunner;

	@BeforeEach
	void setUpDefaultProperties() {
		defaultApplicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(SftpSupplierTestApplication.class)
				.withPropertyValues(
						"sftp.supplier.factory.host=localhost",
						"sftp.supplier.factory.port=${sftp.factory.port}",
						"sftp.supplier.factory.username=user",
						"sftp.supplier.factory.password=pass",
						"sftp.supplier.factory.cache-sessions=true",
						"sftp.supplier.factory.allowUnknownKeys=true",
						"sftp.supplier.localDir=" + this.targetLocalDirectory.getAbsolutePath(),
						"sftp.supplier.remoteDir=sftpSource");
	}

	@Test
	void supplierForListOnly() {
		defaultApplicationContextRunner
				.withPropertyValues("sftp.supplier.listOnly=true")
				.run(context -> {
					Supplier<Flux<Message<String>>> sftpSupplier = context.getBean("sftpSupplier",
							Supplier.class);
					SftpSupplierProperties properties = context.getBean(SftpSupplierProperties.class);
					HashSet<String> fileNames = new HashSet<>();
					fileNames.add(String.join(properties.getRemoteFileSeparator(), properties.getRemoteDir(),
							"sftpSource1.txt"));
					fileNames.add(String.join(properties.getRemoteFileSeparator(), properties.getRemoteDir(),
							"sftpSource2.txt"));
					final AtomicReference<Set<String>> expectedFileNames = new AtomicReference<>(fileNames);
					StepVerifier.create(sftpSupplier.get())
							.assertNext(message -> {
								assertThat(expectedFileNames.get()).contains(message.getPayload());
								expectedFileNames.get().remove(message.getPayload());
								assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE))
										.isEqualTo(MediaType.TEXT_PLAIN);
							})
							.assertNext(message -> {
								assertThat(expectedFileNames.get()).contains(message.getPayload());
								assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE))
										.isEqualTo(MediaType.TEXT_PLAIN);
							})
							.expectTimeout(Duration.ofMillis(1000))
							.verify(Duration.ofSeconds(30));

				});
	}

	@Test
	void supplierForListOnlyWithPatternFilter() {
		defaultApplicationContextRunner
				.withPropertyValues("sftp.supplier.listOnly=true", "sftp.supplier.file-name-pattern=.*1.txt")
				.run(context -> {
					Supplier<Flux<Message<String>>> sftpSupplier = context.getBean("sftpSupplier",
							Supplier.class);
					SftpSupplierProperties properties = context.getBean(SftpSupplierProperties.class);

					final AtomicReference<String> expectedFileName = new AtomicReference<>(
							properties.getRemoteDir() + "/sftpSource1.txt");
					StepVerifier.create(sftpSupplier.get())
							.assertNext(message -> assertThat(expectedFileName.get()).contains(message.getPayload()))
							.expectTimeout(Duration.ofMillis(1000))
							.verify(Duration.ofSeconds(30));

				});
	}

	@Test
	void supplierForListSortedByFilenameAsc() {
		defaultApplicationContextRunner
				.withPropertyValues("sftp.supplier.listOnly=true", "sftp.supplier.sortBy.attribute=filename", "sftp.supplier.sortBy.dir=asc")
				.run(context -> {
					Supplier<Flux<Message<String>>> sftpSupplier = context.getBean("sftpSupplier",
							Supplier.class);
					SftpSupplierProperties properties = context.getBean(SftpSupplierProperties.class);
					List<String> fileNames = new ArrayList<>();
					fileNames.add(String.join(properties.getRemoteFileSeparator(), properties.getRemoteDir(),
							"sftpSource1.txt"));
					fileNames.add(String.join(properties.getRemoteFileSeparator(), properties.getRemoteDir(),
							"sftpSource2.txt"));
					final AtomicReference<List<String>> expectedFileNames = new AtomicReference<>(fileNames);
					StepVerifier.create(sftpSupplier.get())
							.assertNext(message -> {
								assertThat(message.getPayload()).isEqualTo(expectedFileNames.get().get(0));
								assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE))
										.isEqualTo(MediaType.TEXT_PLAIN);
							})
							.assertNext(message -> {
								assertThat(message.getPayload()).isEqualTo(expectedFileNames.get().get(1));
								assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE))
										.isEqualTo(MediaType.TEXT_PLAIN);
							})
							.expectTimeout(Duration.ofMillis(1000))
							.verify(Duration.ofSeconds(30));

				});
	}

	@Test
	void supplierForListSortedByFilenameDesc() {
		defaultApplicationContextRunner
				.withPropertyValues("sftp.supplier.listOnly=true", "sftp.supplier.sortBy.attribute=filename", "sftp.supplier.sortBy.dir=desc")
				.run(context -> {
					Supplier<Flux<Message<String>>> sftpSupplier = context.getBean("sftpSupplier",
							Supplier.class);
					SftpSupplierProperties properties = context.getBean(SftpSupplierProperties.class);
					List<String> fileNames = new ArrayList<>();
					fileNames.add(String.join(properties.getRemoteFileSeparator(), properties.getRemoteDir(),
							"sftpSource2.txt"));
					fileNames.add(String.join(properties.getRemoteFileSeparator(), properties.getRemoteDir(),
							"sftpSource1.txt"));
					final AtomicReference<List<String>> expectedFileNames = new AtomicReference<>(fileNames);
					StepVerifier.create(sftpSupplier.get())
							.assertNext(message -> {
								assertThat(message.getPayload()).isEqualTo(expectedFileNames.get().get(0));
								assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE))
										.isEqualTo(MediaType.TEXT_PLAIN);
							})
							.assertNext(message -> {
								assertThat(message.getPayload()).isEqualTo(expectedFileNames.get().get(1));
								assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE))
										.isEqualTo(MediaType.TEXT_PLAIN);
							})
							.expectTimeout(Duration.ofMillis(1000))
							.verify(Duration.ofSeconds(30));

				});
	}

	@Test
	void supplierForFileRef() {
		defaultApplicationContextRunner
				.withPropertyValues(
						"sftp.supplier.localDir=" + getTargetLocalDirectory().getAbsolutePath(),
						"file.consumer.mode=ref")
				.run(context -> {
					Supplier<Flux<Message<File>>> sftpSupplier = context.getBean("sftpSupplier",
							Supplier.class);
					SftpSupplierProperties properties = context.getBean(SftpSupplierProperties.class);
					MetadataStore metadataStore = context.getBean(MetadataStore.class);
					HashSet<String> fileNames = new HashSet<>();
					fileNames.add(properties.getLocalDir() + File.separator + "sftpSource1.txt");
					fileNames.add(properties.getLocalDir() + File.separator + "sftpSource2.txt");
					final AtomicReference<Set<String>> expectedFileNames = new AtomicReference<>(fileNames);
					StepVerifier.create(sftpSupplier.get())
							.assertNext(message -> {
								File file = message.getPayload();
								assertThat(expectedFileNames.get()).contains(file.getAbsolutePath());
								expectedFileNames.get().remove(file.getAbsolutePath());
							})
							.expectNextMatches(
									message -> expectedFileNames.get().contains(message.getPayload().getAbsolutePath()))
							.thenCancel()
							.verify(Duration.ofSeconds(30));

					assertThat(metadataStore.get("sftpSource/sftpSource1.txt")).isNotNull();
					assertThat(metadataStore.get("sftpSource/sftpSource2.txt")).isNotNull();
					assertThat(Files.exists(Paths.get(getTargetLocalDirectory().getAbsolutePath(), "sftpSource1.txt")))
							.isTrue();
					assertThat(Files.exists(Paths.get(getTargetLocalDirectory().getAbsolutePath(), "sftpSource2.txt")))
							.isTrue();
				});
	}

	@Test
	void deleteRemoteFiles() {
		defaultApplicationContextRunner
				.withPropertyValues(
						"sftp.supplier.stream=true",
						"sftp.supplier.delete-remote-files=true")
				.run(context -> {
					Supplier<Flux<Message<byte[]>>> sftpSupplier = context.getBean("sftpSupplier",
							Supplier.class);
					StepVerifier.create(sftpSupplier.get())
							.expectNextMatches(message -> message.getPayload().length > 0)
							.expectNextMatches(message -> message.getPayload().length > 0)
							.thenCancel()
							.verify(Duration.ofSeconds(30));
					await().atMost(Duration.ofSeconds(30))
							.until(() -> getSourceRemoteDirectory().list().length == 0);
				});

	}

	@Test
	void renameRemoteFilesStream() {
		defaultApplicationContextRunner
				.withPropertyValues(
						"sftp.supplier.stream=true",
						"sftp.supplier.delete-remote-files=false",
						"sftp.supplier.rename-remote-files-to='/sftpTarget/' + headers.file_remoteFile")
				.run(this::doTestRenameRemoteFiles);
	}

	@Test
	void renameRemoteFiles() {
		defaultApplicationContextRunner
				.withPropertyValues(
						"sftp.supplier.stream=false",
						"sftp.supplier.delete-remote-files=false",
						"sftp.supplier.rename-remote-files-to='/sftpTarget/' + headers.file_remoteFile")
				.run(this::doTestRenameRemoteFiles);
	}

	private void doTestRenameRemoteFiles(AssertableApplicationContext context) {
		Supplier<Flux<Message<byte[]>>> sftpSupplier = context.getBean("sftpSupplier",
				Supplier.class);

		final Set<String> expectedTargetFiles = Arrays.stream(getSourceRemoteDirectory().list())
				.collect(Collectors.toSet());

		StepVerifier.create(sftpSupplier.get())
				.expectNextMatches(message -> message.getPayload().length > 0)
				.expectNextMatches(message -> message.getPayload().length > 0)
				.thenCancel()
				.verify(Duration.ofSeconds(30));
		await().atMost(Duration.ofSeconds(30))
				.until(() ->
						expectedTargetFiles.equals(
								Arrays.stream(getTargetRemoteDirectory().list())
								.collect(Collectors.toSet()))
				);
	}

	@Test
	public void streamSourceFilesInLineMode() {
		defaultApplicationContextRunner
				.withPropertyValues(
						"sftp.supplier.stream=true",
						"sftp.supplier.factory.private-key = classpath:id_rsa_pp",
						"sftp.supplier.factory.passphrase = secret",
						"sftp.supplier.factory.password = badPassword", // ensure public key was used
						"sftp.supplier.delete-remote-files=true",
						"file.consumer.mode=lines",
						"file.consumer.with-markers=true",
						"file.consumer.markers-json=true")
				.run(context -> {
					Supplier<Flux<Message<String>>> sftpSupplier = context.getBean("sftpSupplier",
							Supplier.class);
					StepVerifier.create(sftpSupplier.get())
							.assertNext(message -> {
								final Object evaluate;
								try {
									evaluate = JsonPathUtils.evaluate(message.getPayload(), "$.mark");
									assertThat(evaluate).isEqualTo(FileSplitter.FileMarker.Mark.START.name());
								}
								catch (IOException e) {
									fail(e.getMessage());
								}
							})
							.expectNextMatches(message -> message.getPayload().startsWith("source"))
							.assertNext(message -> {
								final Object evaluate;
								try {
									evaluate = JsonPathUtils.evaluate(message.getPayload(), "$.mark");
									assertThat(evaluate).isEqualTo(FileSplitter.FileMarker.Mark.END.name());
								}
								catch (IOException e) {
									fail(e.getMessage());
								}
							})
							.thenCancel()
							.verify(Duration.ofSeconds(30));
				});
	}

	@Test
	void supplierWithMultiSourceAndStreamContentsSource3ComesSecond() throws Exception {
		Path newSource = createNewRemoteSource(
				Paths.get(remoteTemporaryFolder.toString(), "sftpSecondSource", "doesNotMatter.txt"),
				"source3");
		try {
			new ApplicationContextRunner()
					.withUserConfiguration(SftpSupplierTestApplication.class)
					.withPropertyValues(
							"sftp.supplier.stream=true",
							"sftp.supplier.factories.one.host=localhost",
							"sftp.supplier.factories.one.port=${sftp.factory.port}",
							"sftp.supplier.factories.one.username=user",
							"sftp.supplier.factories.one.password=pass",
							"sftp.supplier.factories.one.cache-sessions=true",
							"sftp.supplier.factories.one.allowUnknownKeys=true",
							"sftp.supplier.factories.two.host=localhost",
							"sftp.supplier.factories.two.port=${sftp.factory.port}",
							"sftp.supplier.factories.two.username = user",
							"sftp.supplier.factories.two.password = pass",
							"sftp.supplier.factories.two.cache-sessions = true",
							"sftp.supplier.factories.two.allowUnknownKeys = true",
							"sftp.supplier.directories=one.sftpSource,two.sftpSecondSource",
							"sftp.supplier.max-fetch=1",
							"sftp.supplier.fair=true")
					.run(context -> {

						Supplier<Flux<Message<byte[]>>> sftpSupplier = context.getBean("sftpSupplier", Supplier.class);
						HashSet<String> contents = new HashSet<>();
						contents.add("source1");
						contents.add("source2");
						final AtomicReference<Set<String>> expectedContentsOfAllFiles = new AtomicReference<>(contents);

						StepVerifier.create(sftpSupplier.get())
								.assertNext(message -> {
									String payload = new String(message.getPayload());
									assertThat(expectedContentsOfAllFiles.get()).contains(payload);
									expectedContentsOfAllFiles.get().remove(payload);
								})
								.expectNextMatches(message -> new String(message.getPayload()).equals("source3"))
								.expectNextMatches(message -> expectedContentsOfAllFiles.get()
										.contains(new String(message.getPayload())))
								.thenCancel()
								.verify(Duration.ofSeconds(30));
					});
		}
		finally {
			deleteNewSource(newSource);
		}
	}

	@Test
	void supplierMultiSourceRefTestsFor200Alex() throws Exception {
		Path newSource = createNewRemoteSource(
				Paths.get(remoteTemporaryFolder.toString(), "sftpSecondSource", "sftpSource3.txt"),
				"doesNotMatter");
		try {
			new ApplicationContextRunner()
					.withUserConfiguration(SftpSupplierTestApplication.class)
					.withPropertyValues(
							"file.consumer.mode = ref",
							"sftp.supplier.localDir=" + this.targetLocalDirectory.getAbsolutePath(),
							"sftp.supplier.factories.one.host=localhost",
							"sftp.supplier.factories.one.port=${sftp.factory.port}",
							"sftp.supplier.factories.one.username = user",
							"sftp.supplier.factories.one.password = pass",
							"sftp.supplier.factories.one.cache-sessions = true",
							"sftp.supplier.factories.one.allowUnknownKeys = true",
							"sftp.supplier.factories.two.host=localhost",
							"sftp.supplier.factories.two.port=${sftp.factory.port}",
							"sftp.supplier.factories.two.username = user",
							"sftp.supplier.factories.two.password = pass",
							"sftp.supplier.factories.two.cache-sessions = true",
							"sftp.supplier.factories.two.allowUnknownKeys = true",
							"sftp.supplier.factories.empty.host=localhost",
							"sftp.supplier.factories.empty.port=${sftp.factory.port}",
							"sftp.supplier.factories.empty.username=user",
							"sftp.supplier.factories.empty.password=pass",
							"sftp.supplier.factories.empty.allowUnknownKeys = true",
							"sftp.supplier.directories=one.sftpSource,two.sftpSecondSource,empty.sftpSource",
							"sftp.supplier.max-fetch=1",
							"sftp.supplier.fair=true")
					.run(context -> {
						Supplier<Flux<Message<File>>> sftpSupplier = context.getBean("sftpSupplier", Supplier.class);
						SftpSupplierProperties properties = context.getBean(SftpSupplierProperties.class);
						String localDir = properties.getLocalDir().getPath();
						HashSet<String> firstSourceFiles = new HashSet<>();
						firstSourceFiles.add(Paths.get(localDir, "sftpSource1.txt").toString());
						firstSourceFiles.add(Paths.get(localDir, "sftpSource2.txt").toString());
						final AtomicReference<Set<String>> expectedFirstSourcePaths = new AtomicReference<>(
								firstSourceFiles);

						StepVerifier.create(sftpSupplier.get())
								.assertNext(message -> {
									assertThat(expectedFirstSourcePaths.get()).contains(message.getPayload().getPath());
									expectedFirstSourcePaths.get().remove(message.getPayload().getPath());
								})
								.expectNextMatches(message -> message.getPayload().getPath().equals(
										Paths.get(localDir, "sftpSource3.txt").toString()))
								.expectNextMatches(message -> expectedFirstSourcePaths.get()
										.contains(message.getPayload().getPath()))
								.thenCancel()
								.verify(Duration.ofSeconds(10));
					});
		}
		finally {
			deleteNewSource(newSource);
		}
	}

	private Path createNewRemoteSource(Path remotePath, String contents) throws Exception {
		Files.createDirectory(remotePath.getParent());
		Files.write(Files.createFile(remotePath), contents.getBytes());
		return remotePath;
	}

	private void deleteNewSource(Path newSource) throws IOException {
		Files.delete(newSource);
		Files.delete(newSource.getParent());
	}

	@SpringBootApplication
	static class SftpSupplierTestApplication {

	}

}
