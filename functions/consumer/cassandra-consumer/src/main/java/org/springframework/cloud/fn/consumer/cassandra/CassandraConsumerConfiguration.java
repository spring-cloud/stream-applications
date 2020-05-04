/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.consumer.cassandra;

import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.consumer.cassandra.query.InsertQueryColumnNameExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.data.cassandra.core.UpdateOptions;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.data.cassandra.core.cql.WriteOptions;
import org.springframework.integration.cassandra.outbound.CassandraMessageHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.springframework.cloud.fn.consumer.cassandra.query.ColumnNameExtractor;
import org.springframework.cloud.fn.consumer.cassandra.query.UpdateQueryColumnNameExtractor;
import reactor.core.publisher.Mono;

/**
 * @author Artem Bilan
 * @author Thomas Risberg
 * @author Ashu Gairola
 * @author Akos Ratku
 */
@Configuration
@EnableConfigurationProperties(CassandraConsumerProperties.class)
public class CassandraConsumerConfiguration {

	@Autowired
	private CassandraConsumerProperties cassandraSinkProperties;

	@Bean
	public IntegrationFlow cassandraConsumerFlow(MessageHandler cassandraSinkMessageHandler,
			ObjectMapper objectMapper) {
		IntegrationFlowBuilder integrationFlowBuilder =
				IntegrationFlows.from(CassandraConsumerFunction.class);
		if (StringUtils.hasText(this.cassandraSinkProperties.getIngestQuery())) {
			integrationFlowBuilder.transform(
					new PayloadToMatrixTransformer(objectMapper, this.cassandraSinkProperties.getIngestQuery(),
							CassandraMessageHandler.Type.UPDATE == this.cassandraSinkProperties.getQueryType()
									? new UpdateQueryColumnNameExtractor()
									: new InsertQueryColumnNameExtractor()));
		}
		return integrationFlowBuilder
				.handle(cassandraSinkMessageHandler)
				.get();
	}

	@Bean
	public MessageHandler cassandraSinkMessageHandler(ReactiveCassandraOperations cassandraOperations) {
		CassandraMessageHandler cassandraMessageHandler =
				this.cassandraSinkProperties.getQueryType() != null
						? new CassandraMessageHandler(cassandraOperations, this.cassandraSinkProperties.getQueryType())
						: new CassandraMessageHandler(cassandraOperations);
		cassandraMessageHandler.setProducesReply(true);
		cassandraMessageHandler.setAsync(true);
		if (this.cassandraSinkProperties.getConsistencyLevel() != null
				|| this.cassandraSinkProperties.getTtl() > 0) {

			WriteOptions.WriteOptionsBuilder writeOptionsBuilder = WriteOptions.builder();

			switch (this.cassandraSinkProperties.getQueryType()) {

				case INSERT:
					writeOptionsBuilder = InsertOptions.builder();
					break;
				case UPDATE:
					writeOptionsBuilder = UpdateOptions.builder();
					break;
			}

			if (this.cassandraSinkProperties.getConsistencyLevel() != null) {
				writeOptionsBuilder.consistencyLevel(this.cassandraSinkProperties.getConsistencyLevel());
			}

			if (this.cassandraSinkProperties.getTtl() > 0) {
				writeOptionsBuilder.ttl(this.cassandraSinkProperties.getTtl());
			}

			cassandraMessageHandler.setWriteOptions(writeOptionsBuilder.build());
		}
		if (StringUtils.hasText(this.cassandraSinkProperties.getIngestQuery())) {
			cassandraMessageHandler.setIngestQuery(this.cassandraSinkProperties.getIngestQuery());
		}
		else if (this.cassandraSinkProperties.getStatementExpression() != null) {
			cassandraMessageHandler.setStatementExpression(this.cassandraSinkProperties.getStatementExpression());
		}
		return cassandraMessageHandler;
	}

	private static boolean isUuid(String uuid) {
		if (uuid.length() == 36) {
			String[] parts = uuid.split("-");
			if (parts.length == 5) {
				return (parts[0].length() == 8) && (parts[1].length() == 4) &&
						(parts[2].length() == 4) && (parts[3].length() == 4) &&
						(parts[4].length() == 12);
			}
		}
		return false;
	}


	private static class PayloadToMatrixTransformer extends AbstractPayloadTransformer<Object, List<List<Object>>> {

		private final Jackson2JsonObjectMapper jsonObjectMapper;

		private final List<String> columns = new LinkedList<>();

		private final ISO8601StdDateFormat dateFormat = new ISO8601StdDateFormat();

		PayloadToMatrixTransformer(ObjectMapper objectMapper, String query, ColumnNameExtractor columnNameExtractor) {
			this.jsonObjectMapper = new Jackson2JsonObjectMapper(objectMapper);
			this.columns.addAll(columnNameExtractor.extract(query));
			this.jsonObjectMapper.getObjectMapper()
					.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected List<List<Object>> transformPayload(Object payload) {
			if (payload instanceof List) {
				return (List<List<Object>>) payload;
			}
			else {
				try {
					List<Map<String, Object>> model = this.jsonObjectMapper.fromJson(payload, List.class);
					List<List<Object>> data = new ArrayList<>(model.size());
					for (Map<String, Object> entity : model) {
						List<Object> row = new ArrayList<>(this.columns.size());
						for (String column : this.columns) {
							Object value = entity.get(column);
							if (value instanceof String) {
								String string = (String) value;
								if (this.dateFormat.looksLikeISO8601(string)) {
									synchronized (this.dateFormat) {
										value = new Date(this.dateFormat.parse(string).getTime()).toLocalDate();
									}
								}
								if (isUuid(string)) {
									value = UUID.fromString(string);
								}
							}
							row.add(value);
						}
						data.add(row);
					}
					return data;
				}
				catch (Exception ex) {
					throw new IllegalArgumentException("Cannot parse json into matrix", ex);
				}
			}
		}

	}

	/*
	 * We need this to provide visibility to the protected method.
	 */
	@SuppressWarnings("serial")
	private static class ISO8601StdDateFormat extends StdDateFormat {

		@Override
		protected boolean looksLikeISO8601(String dateStr) {
			return super.looksLikeISO8601(dateStr);
		}

	}

	interface CassandraConsumerFunction extends Function<Object, Mono<? extends WriteResult>> {

	}

}
