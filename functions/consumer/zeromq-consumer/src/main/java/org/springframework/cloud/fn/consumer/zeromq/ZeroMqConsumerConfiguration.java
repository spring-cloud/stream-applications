package org.springframework.cloud.fn.consumer.zeromq;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.zeromq.outbound.ZeroMqMessageHandler;
import org.springframework.messaging.Message;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
@EnableConfigurationProperties(ZeroMqConsumerProperties.class)
public class ZeroMqConsumerConfiguration {

    @Bean
    public ZContext zContext() {
        return new ZContext();
    }

    @Bean
    public ZeroMqMessageHandler zeromqMessageHandler(ZeroMqConsumerProperties properties, ZContext zContext,
                                                     @Autowired(required = false) Consumer<ZMQ.Socket> socketConfigurer,
                                                     @Autowired(required = false) OutboundMessageMapper<byte[]> messageMapper) {
        ZeroMqMessageHandler zeroMqMessageHandler =
                new ZeroMqMessageHandler(zContext, properties.getConnectUrl(), properties.getSocketType());
        if (properties.getTopic() != null ) {
            zeroMqMessageHandler.setTopic(properties.getTopic());
        }

        if (properties.getTopicExpression() != null ) {
            zeroMqMessageHandler.setTopicExpression(properties.getTopicExpression());
        }

        if (socketConfigurer != null) {
            zeroMqMessageHandler.setSocketConfigurer(socketConfigurer);
        }

        if(messageMapper != null) {
            zeroMqMessageHandler.setMessageMapper(messageMapper);
        }

        return zeroMqMessageHandler;
    }

    @Bean
    public Function<Flux<Message<?>>, Mono<Void>> zeromqConsumer(ZeroMqMessageHandler zeromqMessageHandler) {
        return input ->
                input.flatMap(zeromqMessageHandler::handleMessage)
                        .ignoreElements();
    }

}
