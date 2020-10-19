package org.springframework.cloud.fn.consumer.zeromq;

import java.util.function.Function;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Frey
 * since 3.1.0
 */
@SpringBootTest(
        properties = {
                "zeromq.consumer.topic='test-topic'"
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

        Thread.sleep(2000);

        Message<?> testMessage = MessageBuilder.withPayload("test").setHeader("topic", "test-topic").build();
        subject.apply(Flux.just(testMessage))
                .subscribe();

        String topic = socket.recvStr();
        assertThat(topic).isEqualTo("test-topic");
        assertThat(socket.recvStr()).isEmpty();
        assertThat(socket.recvStr()).isEqualTo("test");

    }

    @SpringBootApplication
    public static class ZeroMqConsumerTestApplication { }

}
