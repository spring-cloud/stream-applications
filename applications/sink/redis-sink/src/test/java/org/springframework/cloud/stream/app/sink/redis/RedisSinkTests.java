/*
 * Copyright 2020-2024 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 * @author Chris Bono
 */
@Testcontainers(disabledWithoutDocker = true)
public class RedisSinkTests {
	static GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:7").withExposedPorts(6379)
			.withStartupTimeout(Duration.ofSeconds(120))
			.withStartupAttempts(3);

	@BeforeAll
	static void startContainer() {
		REDIS_CONTAINER.start();
	}
	static String getUri() {
		return "redis://localhost:" + REDIS_CONTAINER.getFirstMappedPort();
	}
	@Test
	public void testRedisSink() {
		String key = "foo";
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(RedisSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=redisConsumer",
						"--spring.cloud.stream.bindings.redisConsumer-in-0.consumer.use-native-decoding=true",
						"--spring.data.redis.url=" + getUri(),
						"--redis.consumer.key=" + key)) {

			StringRedisTemplate redisTemplate = context.getBean(StringRedisTemplate.class);
			redisTemplate.delete(key);

			List<String> list = new ArrayList<>();
			list.add("Manny");
			list.add("Moe");
			list.add("Jack");

			Message<List<String>> message = new GenericMessage<>(list);

			InputDestination source = context.getBean(InputDestination.class);
			source.send(message);

			RedisList<String> redisList = new DefaultRedisList<>(key, redisTemplate);
			assertThat(redisList.size()).isEqualTo(3);
			assertThat(redisList.get(0)).isEqualTo("Manny");
			assertThat(redisList.get(1)).isEqualTo("Moe");
			assertThat(redisList.get(2)).isEqualTo("Jack");
		}
	}

	@SpringBootApplication
	public static class RedisSinkTestApplication {
	}
}
