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

package org.springframework.cloud.stream.app.sink.zeromq;

import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.consumer.zeromq.ZeroMqConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ZeroMqSink.
 *
 * @author Daniel Frey
 * @since 3.1.0
 */
public class ZeroMqSinkTests {

    private static final ZContext CONTEXT = new ZContext();

    @Test
    public void testSinkFromFunction() {

        ZMQ.Socket socket = CONTEXT.createSocket(SocketType.SUB);
        socket.setReceiveTimeOut(10_000);
        int port = socket.bindToRandomPort("tcp://*");
        socket.subscribe("test-topic");

        ZMQ.Poller poller = CONTEXT.createPoller(1);
        poller.register(socket, ZMQ.Poller.POLLIN);

        try (ConfigurableApplicationContext context =
                    new SpringApplicationBuilder(
                            TestChannelBinderConfiguration.getCompleteConfiguration(ZeroMqSourceTestApplication.class)
                    ).run(
                            "--logging.level.org.springframework.integration=DEBUG",
                            "--spring.cloud.function.definition=zeromqConsumer",
                            "--zeromq.consumer.topic='test-topic'",
                            "--zeromq.consumer.connectUrl=tcp://localhost:" + port
                    )
        ) {

            InputDestination inputDestination = context.getBean(InputDestination.class);

            Message<?> testMessage =
                    MessageBuilder.withPayload("test")
                            .setHeader("topic", "test-topic")
                            .setHeader("contentType", MimeTypeUtils.APPLICATION_OCTET_STREAM)
                            .build();
            inputDestination.send(testMessage);

            ZMsg received = null;
            while (received == null) {

                poller.poll(10000);
                if (poller.pollin(0)) {

                    received = ZMsg.recvMsg(socket);
                    assertThat(received).isNotNull();
                    assertThat(received.unwrap().getString(ZMQ.CHARSET)).isEqualTo("test-topic");
                    assertThat(received.getLast().getString(ZMQ.CHARSET)).isEqualTo("test");

                }

            }

        }
        finally {
            poller.unregister(socket);
            poller.close();
            socket.close();
        }

    }

    @SpringBootApplication
    @Import(ZeroMqConsumerConfiguration.class)
    public static class ZeroMqSourceTestApplication { }

}
