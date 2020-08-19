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

import java.io.File;
import java.util.function.Supplier;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.test.support.ftp.FtpTestSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"ftp.factory.username = foo",
				"ftp.factory.password = foo",
				"file.consumer.mode = ref",
				"ftp.factory.cacheSessions = true"
		})
@DirtiesContext
public class FtpSupplierTests extends FtpTestSupport {

	@Autowired
	Supplier<Flux<Message<?>>> ftpSupplier;

	@Autowired
	private ConcurrentMetadataStore metadataStore;

	@Autowired
	FtpSupplierProperties config;

	@Autowired
	SessionFactory<FTPFile> sessionFactory;

	@Test
	public void testSourceFileAsRef() {
		final Flux<Message<?>> messageFlux = ftpSupplier.get();
		assertThat(this.sessionFactory).isInstanceOf(CachingSessionFactory.class);
		StepVerifier stepVerifier =
				StepVerifier.create(messageFlux)
						.assertNext((message) -> {
							assertThat(new File(message.getPayload().toString().replaceAll("\"", ""))).isEqualTo(
									new File(this.config.getLocalDir(), "ftpSource1.txt"));
								}
						)
						.assertNext((message) -> {
							assertThat(new File(message.getPayload().toString().replaceAll("\"", ""))).isEqualTo(
									new File(this.config.getLocalDir(), "ftpSource2.txt"));
						})
						.thenCancel()
						.verifyLater();
		stepVerifier.verify();
	}

	@SpringBootApplication
	static class FtpSupplierTestApplication {

		// These properties can be moved into the SpringBootApplication annotation, but providing here
		// as a way to demonstrate how we can provide ConfigurationProperties as a bean in the application itself
		// This way, it is less error-prone from typos.
		@Bean
		@Primary
		public FtpSupplierProperties ftpSupplierProperties() {
			final FtpSupplierProperties ftpSupplierProperties = new FtpSupplierProperties();
			ftpSupplierProperties.setRemoteDir("ftpSource");
			ftpSupplierProperties.setFilenamePattern("*");
			return ftpSupplierProperties;
		}
	}
}
