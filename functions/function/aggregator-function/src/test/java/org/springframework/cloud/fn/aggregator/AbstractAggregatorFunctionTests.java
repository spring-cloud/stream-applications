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

package org.springframework.cloud.fn.aggregator;

import java.util.function.Function;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

/**
 * @author Artem Bilan
 * @author Corneil du Plessis
 */
@SpringBootTest
@DirtiesContext
public abstract class AbstractAggregatorFunctionTests {

	@Autowired
	protected Function<Flux<Message<?>>, Flux<Message<?>>> aggregatorFunction;

	@Autowired(required = false)
	protected MessageGroupStore messageGroupStore;

	@Autowired
	protected AggregatingMessageHandler aggregatingMessageHandler;

	@SpringBootApplication
	static class AggregatorFunctionTestApplication {

	}

}
