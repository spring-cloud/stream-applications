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

package org.springframework.cloud.stream.app.sink.xmpp;

import java.time.Duration;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.consumer.xmpp.XmppConsumerConfiguration;
import org.springframework.cloud.fn.test.support.xmpp.XmppTestContainerSupport;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Tests for XMPP Sink.
 *
 * @author Daniel Frey
 *
 * @since 4.0.0
 */
class XmppSinkTests implements XmppTestContainerSupport {

	private XMPPTCPConnection clientConnection;

	@BeforeEach
	void prepareForTest() throws Exception {
		var builder = XMPPTCPConnectionConfiguration.builder();
		builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
		builder.setHost(XmppTestContainerSupport.getXmppHost());
		builder.setPort(XmppTestContainerSupport.getXmppMappedPort());
		builder.setResource(SERVICE_NAME);
		builder.setUsernameAndPassword(JANE_USER, USER_PW).setXmppDomain(SERVICE_NAME);
		this.clientConnection = new XMPPTCPConnection(builder.build());
		this.clientConnection.connect();
		this.clientConnection.login();
	}


	@Test
	void sinkFromConsumerFunction() {

		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(XmppSinkTestApplication.class)).run(
						"--spring.cloud.function.definition=xmppConsumer",
						"--xmpp.factory.host=" + XmppTestContainerSupport.getXmppHost(),
						"--xmpp.factory.port=" + XmppTestContainerSupport.getXmppMappedPort(),
						"--xmpp.factory.user=" + JOHN_USER,
						"--xmpp.factory.password=" + USER_PW,
						"--xmpp.factory.service-name=" + SERVICE_NAME,
						"--xmpp.factory.security-mode=disabled"
		)) {

			var inputDestination = context.getBean(InputDestination.class);

			var collector
					= this.clientConnection.createStanzaCollector(StanzaTypeFilter.MESSAGE);

			var testMessage = MessageBuilder.withPayload("test")
					.setHeader(XmppHeaders.TO, JANE_USER + "@" + SERVICE_NAME)
					.build();

			await().atMost(Duration.ofSeconds(20)).pollDelay(Duration.ofMillis(100))
					.untilAsserted(() -> {
						inputDestination.send(testMessage);
						Stanza stanza = collector.nextResult();
						assertThat(stanza.getTo().asBareJid().asUnescapedString()).isEqualTo(JANE_USER + "@" + SERVICE_NAME);
						assertThat(stanza.getFrom().asBareJid().asUnescapedString()).isEqualTo(JOHN_USER + "@" + SERVICE_NAME);
					});
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(XmppConsumerConfiguration.class)
	static class XmppSinkTestApplication {

	}

}
