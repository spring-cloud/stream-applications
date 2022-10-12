package org.springframework.cloud.fn.consumer.xmpp;

import java.time.Duration;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.common.xmpp.XmppConnectionFactoryConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

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
                "logging.level.org.springframework.integration=DEBUG"
        }
)
@DirtiesContext
public class XmppConsumerConfigurationTests {

    @Autowired
    Consumer<Message<?>> subject;

    @Test
    void testMessageHandlerConfiguration() {
        Message<?> testMessage = MessageBuilder.withPayload("test").build();

        await().atMost(Duration.ofSeconds(20)).pollDelay(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    subject.accept(testMessage);

                });
    }

    @SpringBootApplication
    @Import(XmppConnectionFactoryConfiguration.class)
    static class XmppConsumerTestApplication {

    }

}
