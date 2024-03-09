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

package org.springframework.cloud.stream.app.source.ftp;

import java.io.File;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.supplier.ftp.FtpSupplierProperties;
import org.springframework.cloud.fn.test.support.ftp.FtpTestSupport;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"ftp.factory.username = foo",
				"ftp.factory.password = foo",
				"file.consumer.mode = ref",
				"ftp.factory.cacheSessions = true",
				"spring.cloud.function.definition=ftpSupplier"
		})
@DirtiesContext
public class FtpSourceTests extends FtpTestSupport {

	@Autowired
	private OutputDestination output;

	@Autowired
	FtpSupplierProperties config;

	@Test
	public void testFtpSource() {
		Message<byte[]> message = output.receive(10000, "ftpSupplier-out-0");
		assertThat(new File(new String(message.getPayload()).replaceAll("\"", ""))).isEqualTo(
				new File(this.config.getLocalDir(), "ftpSource1.txt"));
		message = output.receive(10000, "ftpSupplier-out-0");
		assertThat(new File(new String(message.getPayload()).replaceAll("\"", ""))).isEqualTo(
				new File(this.config.getLocalDir(), "ftpSource2.txt"));
	}

	@SpringBootApplication
	@Import(TestChannelBinderConfiguration.class)
	public static class SampleConfiguration {

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
