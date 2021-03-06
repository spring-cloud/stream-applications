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

package org.springframework.cloud.stream.app.integration.test.source.time;

import org.testcontainers.junit.jupiter.Container;

import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.RabbitMQStreamAppTest;
import org.springframework.cloud.stream.app.test.integration.rabbitmq.RabbitMQStreamAppContainer;

import static org.springframework.cloud.stream.app.integration.test.common.Configuration.VERSION;

@RabbitMQStreamAppTest

class RabbitMQTimeSourceTests extends TimeSourceTests {

	@Container
	static StreamAppContainer source = new RabbitMQStreamAppContainer(StreamAppContainerTestUtils
			.imageName(StreamAppContainerTestUtils.SPRINGCLOUDSTREAM_REPOSITOTRY, "time-source-rabbit", VERSION));

}
