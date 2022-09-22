/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.mail;

import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringIntegrationTest(noAutoStartup = "*")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
		"mail.supplier.mark-as-read=true",
		"mail.supplier.delete=false",
		"mail.supplier.user-flag=testSIUserFlag",
		"mail.supplier.java-mail-properties=mail.imap.socketFactory.fallback=true\\n mail.store.protocol=imap\\n mail.debug=true" })
@DirtiesContext
public class AbstractMailSupplierTests {

	private static TestMailServer.MailServer MAIL_SERVER;

	@Autowired
	protected Supplier<Flux<Message<?>>> mailSupplier;

	@Autowired
	private StandardIntegrationFlow integrationFlow;

	protected static void startMailServer(TestMailServer.MailServer mailServer)
			throws InterruptedException {
		MAIL_SERVER = mailServer;
		System.setProperty("test.mail.server.port", "" + MAIL_SERVER.getPort());
		int n = 0;
		while (n++ < 100 && (!MAIL_SERVER.isListening())) {
			Thread.sleep(100);
		}
		assertThat(n < 100).isTrue();
	}

	@BeforeEach
	void start() {
		integrationFlow.start();
	}

	@AfterEach
	void stop() {
		integrationFlow.stop();
	}

	@AfterAll
	public static void cleanup() {
		System.clearProperty("test.mail.server.port");
	}

	@SpringBootApplication
	public static class MailSupplierTestApplication {

	}
}
