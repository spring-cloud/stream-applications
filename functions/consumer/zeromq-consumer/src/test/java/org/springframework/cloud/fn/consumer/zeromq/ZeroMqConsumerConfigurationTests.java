package org.springframework.cloud.fn.consumer.zeromq;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.zeromq.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Frey
 * since 3.1.0
 */
@SpringBootTest(
        properties = {
                "logging.level.org.springframework.cloud.stream=DEBUG",
                "logging.level.org.springframework.context=DEBUG",
                "logging.level.org.springframework.integration=DEBUG",
                "zeromq.consumer.topic=test-topic"
        }
)
@DirtiesContext
public class ZeroMqConsumerConfigurationTests {

    private static final ZContext CONTEXT = new ZContext();
    private static ZMQ.Socket socket;

    @Autowired
    Function<Flux<Message<?>>, Mono<Void>> subject;

    @BeforeAll
    static void setup() {

        socket = CONTEXT.createSocket(SocketType.SUB);
        socket.setReceiveTimeOut(10_000);
        int bindPort = socket.bindToRandomPort("tcp://*");
        socket.subscribe("test-topic");

        System.setProperty("zeromq.consumer.connectUrl", "tcp://localhost:" + bindPort);

    }

    @AfterAll
    static void tearDown() {
        socket.close();
        CONTEXT.close();
    }

    @Test
    void testMessageHandlerConfiguration() throws InterruptedException {

        Message<ZMsg> testMessage = MessageBuilder.withPayload(ZMsg.newStringMsg("test")).setHeader("topic", "test-topic").build();
        subject.apply(Flux.just(testMessage))
                .subscribe();

        Thread.sleep(2000);

        ZMsg actual = ZMsg.recvMsg(socket);
        assertThat(actual).isNotNull();
        assertThat(actual.unwrap().getString(ZMQ.CHARSET)).isEqualTo("test-topic");

    }

    @SpringBootApplication
    public static class ZeroMqConsumerTestApplication { }

}
