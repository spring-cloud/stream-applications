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

package org.springframework.cloud.stream.app.source.file;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.supplier.mail.MailSupplierConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.test.mail.TestMailServer;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight integration test to verify using {@link MailSupplierConfiguration} in a {@code @SpringBootApplication}.
 *
 * @author Soby Chacko
 * @author Chris Bono
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cloud.function.definition=mailSupplier",
				"mail.supplier.url=imap://user:pw@localhost:${test.mail.server.port}/INBOX",
				"mail.supplier.mark-as-read=true",
				"mail.supplier.delete=false",
				"mail.supplier.user-flag=testSIUserFlag",
				"mail.supplier.java-mail-properties=mail.imap.socketFactory.fallback=true\\n mail.store.protocol=imap\\n mail.debug=true"
		})
@DirtiesContext
class MailSourceTests {

	private static TestMailServer.MailServer MAIL_SERVER;

	@BeforeAll
	public static void startImapServer() throws Throwable {
		startMailServer(TestMailServer.imap(0));
	}

	@AfterAll
	public static void cleanup() {
		System.clearProperty("test.mail.server.port");
		MAIL_SERVER.stop();
	}

	private static void startMailServer(TestMailServer.MailServer mailServer)
			throws InterruptedException {
		MAIL_SERVER = mailServer;
		System.setProperty("test.mail.server.port", "" + MAIL_SERVER.getPort());
		int n = 0;
		while (n++ < 100 && (!MAIL_SERVER.isListening())) {
			Thread.sleep(100);
		}
		assertThat(n < 100).isTrue();
	}

	@Test
	void mailMessagesAreSuppliedToOutputDestination(@Autowired OutputDestination target) {
		Message<byte[]> sourceMessage = target.receive(10000, "mailSupplier-out-0");
		final String actual = new String(sourceMessage.getPayload());
		assertThat(actual.endsWith("\r\n\r\nfoo\r\n\r\n")).isTrue();
	}

	@SpringBootApplication
	@Import({TestChannelBinderConfiguration.class, MailSupplierConfiguration.class})
	public static class MailSourceTestApplication {
	}
}
