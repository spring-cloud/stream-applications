/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.stream.app.jdbc.source;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 */
@TestPropertySource(properties = {
		"jdbc.supplier.query=select id, name, tag from test where tag is NULL order by id",
		"jdbc.supplier.split=false",
		"jdbc.supplier.maxRows=2",
		"jdbc.supplier.update=update test set tag='1' where id in (:id)" })
public class Select2PerPollNoSplitWithUpdateTests extends JdbcSourceIntegrationTests {

	@Test
	public void testExtraction() throws Exception {
		Message<?> received = this.messageCollector.forChannel(this.output).poll(10, TimeUnit.SECONDS);
		assertNotNull(received);
		assertThat(received.getPayload().getClass()).isEqualTo(String.class);

		CollectionLikeType valueType = TypeFactory.defaultInstance()
				.constructCollectionLikeType(List.class, Map.class);

		List<Map<?, ?>> payload = this.objectMapper.readValue((String) received.getPayload(), valueType);

		assertEquals(2, payload.size());
		assertEquals(1, payload.get(0).get("ID"));
		assertEquals(2, payload.get(1).get("ID"));
		received = this.messageCollector.forChannel(this.output).poll(10, TimeUnit.SECONDS);
		assertNotNull(received);
		payload = this.objectMapper.readValue((String) received.getPayload(), valueType);
		assertEquals(1, payload.size());
		assertEquals(3, payload.get(0).get("ID"));
	}

}
