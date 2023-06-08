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

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
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
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight integration test to verify using {@link MailSupplierConfiguration} in a {@code @SpringBootApplication}.
 *
 * @author Soby Chacko
 * @author Chris Bono
 * @author Corneil du Plessis
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cloud.function.definition=mailSupplier",
				"mail.supplier.url=imap://user:pw@localhost:${test.mail.server.port}/INBOX",
				"mail.supplier.mark-as-read=true",
				"mail.supplier.delete=false",
				"mail.supplier.user-flag=testSIUserFlag",
				"mail.supplier.java-mail-properties=mail.store.protocol=imap\\nmail.debug=true"
		})
@DirtiesContext
class MailSourceTests {
	private static GreenMail mailServer;
	private static GreenMailUser mailUser;

	@BeforeAll
	public static void setup() {
		ServerSetup imap = ServerSetupTest.IMAP.dynamicPort();
		imap.setServerStartupTimeout(10000);
		mailServer = new GreenMail(imap);
		mailUser = mailServer.setUser("user", "pw");
		mailServer.start();
	}

	@DynamicPropertySource
	static void mongoDbProperties(DynamicPropertyRegistry registry) {
		registry.add("test.mail.server.port", mailServer.getImap().getServerSetup()::getPort);
	}

	@AfterAll
	public static void cleanup() {
		System.clearProperty("test.mail.server.port");
		mailServer.stop();
	}

	@Test
	void mailMessagesAreSuppliedToOutputDestination(@Autowired OutputDestination target) {
		// given
		mailUser.deliver(GreenMailUtil.createTextEmail("bar@bax", "test@test", "test", "\r\nfoo", mailServer.getImap().getServerSetup()));
		// when
		Message<byte[]> sourceMessage = target.receive(10000, "mailSupplier-out-0");
		// then
		final String actual = new String(sourceMessage.getPayload());
		assertThat(actual).isEqualTo("\r\nfoo");
	}

	@SpringBootApplication
	@Import({TestChannelBinderConfiguration.class, MailSupplierConfiguration.class})
	public static class MailSourceTestApplication {
	}
}
