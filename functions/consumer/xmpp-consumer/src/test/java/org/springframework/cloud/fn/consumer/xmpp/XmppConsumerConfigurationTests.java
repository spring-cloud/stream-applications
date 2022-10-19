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

package org.springframework.cloud.fn.consumer.xmpp;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jxmpp.stringprep.XmppStringprepException;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Daniel Frey
 *
 * @since 4.0.0
 */
@SpringBootTest(
		properties = {
				"xmpp.factory.user=john",
				"xmpp.factory.password=secret",
				"xmpp.factory.host=localhost",
				"xmpp.factory.service-name=localhost",
				"xmpp.factory.security-mode=disabled"
		}
)
@DirtiesContext
@Testcontainers
public class XmppConsumerConfigurationTests {

	private static final String XMPP_HOST = "localhost";
	private static final int XMPP_PORT = 5222;
	private static final String FROM = "john";
	private static final String TO = "jane";
	private static final String TO_PW = "secret";
	private static final String SERVICE_NAME = "localhost";

	@Container
	private static final DockerComposeContainer XMPP_CONTAINER =
			new DockerComposeContainer(new File("src/test/docker-compose/xmpp/docker-compose.yml"))
					.withExposedService("Openfire", XMPP_PORT);

	@Autowired
	private Consumer<Message<?>> xmppConsumer;

	// A client connection is needed to receive the message from the xmpp server
	//   to verify it was received successfully
	private XMPPTCPConnection clientConnection;

	@BeforeEach
	void setup() throws IOException, SmackException, XMPPException, InterruptedException {

		XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
		builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
		builder.setHost(XMPP_HOST);
		builder.setPort(XMPP_PORT);
		builder.setResource(SERVICE_NAME);
		builder.setUsernameAndPassword(TO, TO_PW)
				.setXmppDomain(SERVICE_NAME);

		this.clientConnection = new XMPPTCPConnection(builder.build());
		this.clientConnection.connect();
		this.clientConnection.login();

	}

	@AfterEach
	void teardown() {

		this.clientConnection.instantShutdown();

	}

	@Test
	void messageHandlerConfiguration() {

		StanzaCollector collector
				= this.clientConnection.createStanzaCollector(StanzaTypeFilter.MESSAGE);

		Message<?> testMessage =
				MessageBuilder.withPayload("test")
						.setHeader(XmppHeaders.TO, TO + "@" + SERVICE_NAME)
						.build();

		await().atMost(Duration.ofSeconds(20)).pollDelay(Duration.ofMillis(100))
				.untilAsserted(() -> {

					xmppConsumer.accept(testMessage);

					Stanza stanza = collector.nextResult();
					assertStanza(stanza);

				});

	}

	@Test
	void xmppMessageHandlerConfiguration() throws XmppStringprepException {

		StanzaCollector collector
				= this.clientConnection.createStanzaCollector(StanzaTypeFilter.MESSAGE);

		Message<?> testMessage =
				MessageBuilder.withPayload(org.jivesoftware.smack.packet.MessageBuilder.buildMessage().addBody("en_us", "test").to(TO + "@" + SERVICE_NAME).build())
						.build();

		await().atMost(Duration.ofSeconds(20)).pollDelay(Duration.ofMillis(100))
				.untilAsserted(() -> {

					xmppConsumer.accept(testMessage);

					Stanza stanza = collector.nextResult();
					assertStanza(stanza);

				});

	}

	private void assertStanza(Stanza stanza) {
		assertTo(stanza);
		assertFrom(stanza);
	}

	private void assertTo(Stanza stanza) {

		assertThat(stanza.getTo().asBareJid().asUnescapedString()).isEqualTo(TO + "@" + SERVICE_NAME);

	}

	private void assertFrom(Stanza stanza) {

		assertThat(stanza.getFrom().asBareJid().asUnescapedString()).isEqualTo(FROM + "@" + SERVICE_NAME);

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(XmppConsumerConfiguration.class)
	static class XmppConsumerTestApplication { }

}
