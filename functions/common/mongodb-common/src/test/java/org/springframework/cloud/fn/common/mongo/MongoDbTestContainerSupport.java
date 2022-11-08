/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.cloud.fn.common.mongo;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Provides a static {@link MongoDBContainer} that can be shared across test classes.
 *
 * @author Chris Bono
 */
@Testcontainers(disabledWithoutDocker = true)
public interface MongoDbTestContainerSupport {

	MongoDBContainer MONGO_CONTAINER = new MongoDBContainer("mongo:5.0.9");

	@BeforeAll
	static void startContainer() {
		MONGO_CONTAINER.start();
	}

	static String mongoDbUri() {
		return "mongodb://localhost:" + MONGO_CONTAINER.getFirstMappedPort();
	}

}
