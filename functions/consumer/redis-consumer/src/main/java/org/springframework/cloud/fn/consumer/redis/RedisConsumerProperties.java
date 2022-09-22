/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.redis;

import java.util.Arrays;
import java.util.Collections;

import jakarta.validation.constraints.AssertTrue;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Used to configure those Redis Sink module options that are not related to connecting to Redis.
 *
 * @author Eric Bottard
 * @author Mark Pollack
 * @author Artem Bilan
 * @author Soby Chacko
 */
@ConfigurationProperties("redis.consumer")
@Validated
public class RedisConsumerProperties {

	private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	/**
	 * A SpEL expression to use for topic.
	 */
	private String topicExpression;

	/**
	 * A SpEL expression to use for queue.
	 */
	private String queueExpression;

	/**
	 * A SpEL expression to use for storing to a key.
	 */
	private String keyExpression;

	/**
	 * A literal key name to use when storing to a key.
	 */
	private String key;

	/**
	 * A literal queue name to use when storing in a queue.
	 */
	private String queue;

	/**
	 * A literal topic name to use when publishing to a topic.
	 */
	private String topic;

	public Expression keyExpression() {
		return key != null ? new LiteralExpression(key) : EXPRESSION_PARSER.parseExpression(keyExpression);
	}

	public Expression queueExpression() {
		return queue != null ? new LiteralExpression(queue) : EXPRESSION_PARSER.parseExpression(queueExpression);
	}

	public Expression topicExpression() {
		return topic != null ? new LiteralExpression(topic) : EXPRESSION_PARSER.parseExpression(topicExpression);
	}

	boolean isKeyPresent() {
		return StringUtils.hasText(key) || keyExpression != null;
	}

	boolean isQueuePresent() {
		return StringUtils.hasText(queue) || queueExpression != null;
	}

	boolean isTopicPresent() {
		return StringUtils.hasText(topic) || topicExpression != null;
	}

	public String getTopicExpression() {
		return topicExpression;
	}

	public void setTopicExpression(String topicExpression) {
		this.topicExpression = topicExpression;
	}

	public String getQueueExpression() {
		return queueExpression;
	}

	public void setQueueExpression(String queueExpression) {
		this.queueExpression = queueExpression;
	}

	public String getKeyExpression() {
		return keyExpression;
	}

	public void setKeyExpression(String keyExpression) {
		this.keyExpression = keyExpression;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getQueue() {
		return queue;
	}

	public void setQueue(String queue) {
		this.queue = queue;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	// The javabean property name is what will be reported in case of violation. Make it meaningful
	@AssertTrue(message = "Exactly one of 'queue', 'queueExpression', 'key', 'keyExpression', "
			+ "'topic' and 'topicExpression' must be set")
	public boolean isMutuallyExclusive() {
		Object[] props = new Object[] { queue, queueExpression, key, keyExpression, topic, topicExpression };
		return (props.length - 1) == Collections.frequency(Arrays.asList(props), null);
	}

}
