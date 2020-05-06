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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Marius Bogoevici
 * @author Gary Russell
 */
@TestPropertySource(properties = "redis.consumer.key = foo")
public class RedisConsumerKeyTests extends AbstractRedisConsumerTests {

	@Test
	public void testWithKey() {
		//Setup
		String key = "foo";
		redisTemplate.delete(key);

		RedisList<String> redisList = new DefaultRedisList<>(key, redisTemplate);
		List<String> list = new ArrayList<>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");

		//Execute
		Message<List<String>> message = new GenericMessage<>(list);

		redisConsumer.accept(message);

		//Assert
		assertThat(redisList.size()).isEqualTo(3);
		assertThat(redisList.get(0)).isEqualTo("Manny");
		assertThat(redisList.get(1)).isEqualTo("Moe");
		assertThat(redisList.get(2)).isEqualTo("Jack");

		//Cleanup
		redisTemplate.delete(key);
	}
}
