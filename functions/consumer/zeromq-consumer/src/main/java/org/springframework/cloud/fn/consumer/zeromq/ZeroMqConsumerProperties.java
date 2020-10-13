package org.springframework.cloud.fn.consumer.zeromq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.validation.annotation.Validated;
import org.zeromq.SocketType;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Daniel Frey
 * @since 3.1.0
 */
@ConfigurationProperties("zeromq.consumer")
@Validated
public class ZeroMqConsumerProperties {

    /**
     * The Socket Type the connection should establish.
     */
    private SocketType socketType = SocketType.PUB;

    /**
     * Connection URL for to the ZeroMQ Socket.
     */
    private String connectUrl;

    /**
     * The Topic to expose.
     */
    private String topic;

    /**
     * A Topic SpEL expression to evaluate a topic before sending
     * messages to subscribers
     */
    private Expression topicExpression;

    @NotNull(message = "'socketType' is required")
    public SocketType getSocketType() {
        return socketType;
    }

    /**
     * @param socketType the {@link SocketType} to establish.
     */
    public void setSocketType(SocketType socketType) {
        this.socketType = socketType;
    }

    @NotEmpty(message = "connectUrl is required like protocol://server:port")
    public String getConnectUrl() {
        return connectUrl;
    }

    /**
     * @param connectUrl The ZeroMQ socket to expose
     */
    public void setConnectUrl(String connectUrl) {
        this.connectUrl = connectUrl;
    }

    public String getTopic() {
        return topic;
    }

    /**
     * @param topic the ZeroMq Topic to publish messages on
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Expression getTopicExpression() {
        return topicExpression;
    }

    /**
     * @param topicExpression the Topic Expression to evaluate topics before publishing messages
     */
    public void setTopicExpression(Expression topicExpression) {
        this.topicExpression = topicExpression;
    }
}
