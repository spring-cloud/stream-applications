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

package org.springframework.cloud.stream.app.source.jdbc;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 * @author Chris Bono
 */
@TestPropertySource(properties = {
		"jdbc.supplier.query=select id, name, tag from test where tag is NULL order by id",
		"jdbc.supplier.split=false"
})
public class SelectAllNoSplitTests extends JdbcSourceIntegrationTests {

	@Test
	public void testExtraction() {
		CollectionLikeType valueType = TypeFactory.defaultInstance()
				.constructCollectionLikeType(List.class, Map.class);
		Message<?> received = receiveMessage(10_000);
		List<Map<?, ?>> payload = extractPayload(received, valueType);
		assertThat(payload.size()).isEqualTo(3);
		assertThat(payload.get(0).get("ID")).isEqualTo(1);
		assertThat(payload.get(2).get("NAME")).isEqualTo("John");
	}

}
