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

import java.time.Duration;
import java.util.function.Consumer;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.common.xmpp.XmppConnectionFactoryConfiguration;
import org.springframework.context.annotation.Import;
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
                "xmpp.factory.user=dmfrey",
                "xmpp.factory.password=thisisatest",
                "xmpp.factory.host=jabb3r.org",
                "xmpp.factory.service-name=jabb3r.org",
                "xmpp.consumer.chat-to=dmfrey",
                "logging.level.org.springframework.integration=DEBUG",
                "logging.level.org.springframework.cloud.fn.consumer.xmpp=DEBUG"
        }
)
@DirtiesContext
public class XmppConsumerConfigurationTests {

    private static final Logger log = LoggerFactory.getLogger(XmppConsumerConfigurationTests.class);

    private static final String FROM = "dmfrey";
    private static final String TO = "dmfrey";

    @Autowired
    Consumer<Message<?>> subject;

    @Autowired
    XMPPConnection xmppConnection;

    @Test
    void testMessageHandlerConfiguration() {
        Message<?> testMessage = MessageBuilder.withPayload("test").build();

        xmppConnection.addAsyncStanzaListener(this::assertStanza, StanzaTypeFilter.MESSAGE);

        await().atMost(Duration.ofSeconds(20)).pollDelay(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    subject.accept(testMessage);

                });
    }

    private void assertStanza( final Stanza stanza ) {

        log.debug( "Stanza Message Received: {}", stanza.toXML() );
        assertTo(stanza);
        assertFrom(stanza);

    }
    private void assertTo( final Stanza stanza ) {

        assertThat( stanza.getTo().asBareJid().asUnescapedString() ).isEqualTo(TO);
        log.debug( "Sending Message To: {}", stanza.getTo().asUnescapedString() );

    }

    private void assertFrom( final Stanza stanza ) {

        assertThat( stanza.getFrom().asBareJid().asUnescapedString() ).isEqualTo(FROM);
        log.debug( "Message Sent From: {}", stanza.getFrom().asUnescapedString() );

    }

    @SpringBootApplication
    @Import(XmppConnectionFactoryConfiguration.class)
    static class XmppConsumerTestApplication { }

}
