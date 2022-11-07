/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.cloud.fn.supplier.xmpp;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.test.support.xmpp.XmppTestContainerSupport;
import org.springframework.context.annotation.Import;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Frey
 */
@SpringBootTest
public class XmppSupplierConfigurationTests implements XmppTestContainerSupport {

	@DynamicPropertySource
	static void registerConfigurationProperties(DynamicPropertyRegistry registry) {
		registry.add("xmpp.factory.host", () -> XmppTestContainerSupport.getXmppHost());
		registry.add("xmpp.factory.port", () -> XmppTestContainerSupport.getXmppMappedPort());
		registry.add("xmpp.factory.user", () -> JANE_USER);										// Connect as user intended to listen for messages on behalf of
		registry.add("xmpp.factory.password", () -> USER_PW);
		registry.add("xmpp.factory.service-name", () -> SERVICE_NAME);
		registry.add("xmpp.factory.security-mode", () -> "disabled");
	}

	@Autowired
	Supplier<Flux<Message<?>>> subject;

	// A client source connection is needed to send the message to the xmpp server
	private XMPPTCPConnection sourceConnection;

	@BeforeEach
	void setup() throws IOException, SmackException, XMPPException, InterruptedException {

		var builder = XMPPTCPConnectionConfiguration.builder();
		builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
		builder.setHost(XmppTestContainerSupport.getXmppHost());
		builder.setPort(XmppTestContainerSupport.getXmppMappedPort());
		builder.setResource(SERVICE_NAME);
		builder.setUsernameAndPassword(JOHN_USER, USER_PW)											// Connect as user intended to send messages from
				.setXmppDomain(SERVICE_NAME);
		this.sourceConnection = new XMPPTCPConnection(builder.build());
		this.sourceConnection.connect();
		this.sourceConnection.login();

	}

	@AfterEach
	void teardown() {
		this.sourceConnection.instantShutdown();
	}

	@Test
	void testSubscriptionConfiguration() throws XmppStringprepException, SmackException.NotConnectedException, InterruptedException {

		var payload = "test";

		var stepVerifier =
				StepVerifier.create(subject.get())
						.assertNext((message) -> {

							assertThat(message.getPayload())
									.asInstanceOf(InstanceOfAssertFactories.type(String.class))
									.isEqualTo(payload);

							assertThat(message.getHeaders().containsKey(XmppHeaders.TO)).isTrue();
							assertThat(message.getHeaders().get(XmppHeaders.TO, String.class)).isEqualTo(JANE_USER + "@" + SERVICE_NAME);

						})
						.thenCancel()
						.verifyLater();

		var chatManager = ChatManager.getInstanceFor(this.sourceConnection);
		var jid = JidCreate.entityBareFrom(JANE_USER + "@" + SERVICE_NAME);
		var chat = chatManager.chatWith(jid);
		chat.send(payload);

		stepVerifier.verify(Duration.ofSeconds(10));

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(XmppSupplierConfiguration.class)
	static class XmppSupplierTestApplication { }

}
