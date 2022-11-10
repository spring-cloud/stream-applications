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

package org.springframework.cloud.stream.app.source.xmpp;

import java.nio.charset.StandardCharsets;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.supplier.xmpp.XmppSupplierConfiguration;
import org.springframework.cloud.fn.test.support.xmpp.XmppTestContainerSupport;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.integration.xmpp.XmppHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for XmppSource.
 *
 * @author Daniel Frey
 * @since 4.0.0
 */
public class XmppSourceTests implements XmppTestContainerSupport {

	private XMPPTCPConnection sourceConnection;

	@BeforeEach
	void prepareForTest() throws Exception {

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
	public void sourceFromSupplier() throws InterruptedException {

		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(XmppSourceTestApplication.class)).run(
				"--spring.cloud.function.definition=xmppSupplier",
				"--xmpp.factory.host=" + XmppTestContainerSupport.getXmppHost(),
				"--xmpp.factory.port=" + XmppTestContainerSupport.getXmppMappedPort(),
				"--xmpp.factory.user=" + JANE_USER,
				"--xmpp.factory.password=" + USER_PW,
				"--xmpp.factory.service-name=" + SERVICE_NAME,
				"--xmpp.factory.security-mode=disabled"
		)) {

			var outputDestination = context.getBean(OutputDestination.class);

			var payload = "test";

			var chatManager = ChatManager.getInstanceFor(this.sourceConnection);
			var jid = JidCreate.entityBareFrom(JANE_USER + "@" + SERVICE_NAME);
			var chat = chatManager.chatWith(jid);
			chat.send(payload);

			var message = outputDestination.receive(10000, "xmppSupplier-out-0");

			assertThat(message.getPayload())
					.asInstanceOf(InstanceOfAssertFactories.type(byte[].class))
					.isEqualTo(payload.getBytes(StandardCharsets.UTF_8));

			assertThat(message.getHeaders().containsKey(XmppHeaders.TO)).isTrue();
			assertThat(message.getHeaders().get(XmppHeaders.TO, String.class)).isEqualTo(JANE_USER + "@" + SERVICE_NAME);

		}
		catch (SmackException.NotConnectedException | XmppStringprepException e) {
			throw new RuntimeException(e);
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import({XmppSupplierConfiguration.class, TestChannelBinderConfiguration.class, BindingServiceConfiguration.class})
	public static class XmppSourceTestApplication { }

}
