/*
 * Copyright 2020-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.consumer.redis.RedisConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 */
public class RedisSinkTests {

	@Test
	public void testRedisSink() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(RedisSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=redisConsumer",
						"--redis.consumer.key=foo")) {

			//Setup
			String key = "foo";

			final StringRedisTemplate redisTemplate = context.getBean(StringRedisTemplate.class);
			redisTemplate.delete(key);

			RedisList<String> redisList = new DefaultRedisList<>(key, redisTemplate);
			List<String> list = new ArrayList<>();
			list.add("Manny");
			list.add("Moe");
			list.add("Jack");

			//Execute
			Message<List<String>> message = new GenericMessage<>(list);

			InputDestination source = context.getBean(InputDestination.class);
			source.send(message);

			assertThat(redisList.size()).isEqualTo(3);
			assertThat(redisList.get(0)).isEqualTo("Manny");
			assertThat(redisList.get(1)).isEqualTo("Moe");
			assertThat(redisList.get(2)).isEqualTo("Jack");
		}
	}

	@SpringBootApplication
	@Import(RedisConsumerConfiguration.class)
	public static class RedisSinkTestApplication {
	}
}
