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

package org.springframework.cloud.stream.app.test.integration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

/**
 * The base class for
 * {@link org.springframework.cloud.stream.app.test.integration.TestTopicListener}s.
 * Registers and tests {@link MessageMatcher}s on incoming messages. Subclasses delegate
 * to this listener.
 * @author David Turanski
 */
public abstract class AbstractTestTopicListener implements TestTopicListener {

	protected final Map<String, List<MessageMatcher>> messageMatchers = new ConcurrentHashMap<>();

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public AtomicBoolean allMatch(String topic) {
		AtomicBoolean all = new AtomicBoolean(true);
		if (messageMatchers.containsKey(topic)) {
			return allMatch(messageMatchers.get(topic));
		}
		else {
			all.set(false);
		}

		logger.trace("All MessageMappers matched on topic {} is {}", topic, all.get());
		return all;
	}

	private AtomicBoolean allMatch(@Nullable List<MessageMatcher> messageMatchers) {
		if (CollectionUtils.isEmpty(messageMatchers)) {
			return new AtomicBoolean(false);
		}
		AtomicBoolean all = new AtomicBoolean(true);
		messageMatchers.forEach(mm -> all.compareAndSet(true, mm.isSatisfied()));
		return all;
	}

	@Override
	public AtomicBoolean matches(String topic, Predicate<?>... predicates) {
		AtomicBoolean matched = new AtomicBoolean(true);
		for (Predicate<?> predicate : predicates) {
			Optional<MessageMatcher> optionalMessageMatcher = messageMatcher(topic, predicate);
			if (optionalMessageMatcher.isPresent()) {
				MessageMatcher matcher = optionalMessageMatcher.get();
				matched.compareAndSet(true, matcher.isSatisfied());
			}
			else {
				matched.set(false);
			}
		}
		return matched;
	}

	protected Optional<MessageMatcher> messageMatcher(String topic, Predicate<?> predicate) {
		if (messageMatchers.containsKey(topic)) {
			return messageMatchers.get(topic).stream().filter(mm -> mm.getPredicate().equals(predicate))
					.findFirst();
		}
		return Optional.empty();
	}

	@Override
	public boolean addMessageMatcher(String topic, MessageMatcher messageMatcher) {
		AtomicBoolean isNewTopic = new AtomicBoolean(!messageMatchers.containsKey(topic));

		if (isRegisteredMessageMatcher(topic, messageMatcher)) {
			return false;
		}

		if (isNewTopic.get()) {
			logger.trace("Listener is consuming from topic {}", topic);
		}

		logger.trace("Setting new MessageMatchers on topic {}", topic);
		messageMatchers.putIfAbsent(topic, new LinkedList<>());
		messageMatchers.get(topic).add(new MessageMatcher(messageMatcher));
		logger.trace("There are {} MessageMatchers on topic {}", messageMatchers.get(topic).size(),
				topic);
		return true;
	}

	@Override
	public boolean addMessageMatcher(MessageMatcher messageMatcher) {
		return addMessageMatcher(STREAM_APPLICATIONS_TEST_TOPIC, messageMatcher);
	}

	@Override
	public void clearMessageMatchers() {
		messageMatchers.clear();
	}

	@Override
	public void resetMessageMatchers() {
		messageMatchers.values().forEach((List<MessageMatcher> l) -> l.forEach(v -> v.setSatisfied(false)));
	}

	private boolean isRegisteredMessageMatcher(String topic, Predicate<?> newMessageMatcher) {
		if (!messageMatchers.containsKey(topic)) {
			return false;
		}
		AtomicBoolean registered = new AtomicBoolean(false);
		messageMatchers.get(topic).forEach(verifier -> {
			if (newMessageMatcher.equals(verifier)) {
				logger.debug("This verifier is already registered on topic {}", topic);
				registered.set(true);
				return;
			}
		});
		return registered.get();
	}

	protected abstract Function<Message<?>, String> topicForMessage();

	@Override
	public void listen(Message<?> message) {
		String topic = topicForMessage().apply(message);
		logger.debug("Received message: {} on topic {}", message, topic);

		if (!messageMatchers.containsKey(topic)) {
			return;
		}

		logger.trace("Testing message: {} on topic {}", message, topic);
		AtomicBoolean any = new AtomicBoolean(false);
		messageMatchers.get(topic).forEach(mm -> any.compareAndSet(false, mm.test(message)));
		if (any.get()) {
			logger.debug("Matched message: {} on topic {}", message, topic);
		}
	}

}
