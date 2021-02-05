/*
 * Copyright 2021-2021 the original author or authors.
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

package org.springframework.cloud.fn.supplier.mongo;

import java.util.Collections;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.BasicUpdate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.mongodb.inbound.MongoDbMessageSource;
import org.springframework.integration.mongodb.support.MongoHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An {@link AbstractMessageSource} extension for MongoDB updates
 * based on the query result from the {@link MongoDbMessageSource} delegate.
 *
 * @author Artem Bilan
 */
class UpdatingMongoDbMessageSource extends AbstractMessageSource<Object> {

	private final MongoDbMessageSource delegate;

	private final MongoTemplate mongoTemplate;

	@Nullable
	private final Expression updateExpression;

	private EvaluationContext evaluationContext;


	UpdatingMongoDbMessageSource(MongoDbMessageSource delegate, MongoTemplate mongoTemplate,
			@Nullable Expression updateExpression) {

		this.delegate = delegate;
		this.mongoTemplate = mongoTemplate;
		this.updateExpression = updateExpression;
	}

	@Override
	public String getComponentType() {
		return "mongo:updating-inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		TypeLocator typeLocator = this.evaluationContext.getTypeLocator();
		if (typeLocator instanceof StandardTypeLocator) {
			//Register MongoDB query API package so FQCN can be avoided in query-expression.
			((StandardTypeLocator) typeLocator).registerImport("org.springframework.data.mongodb.core.query");
		}
	}

	@Override
	protected Object doReceive() {
		final Message<Object> message = this.delegate.receive();
		if (message != null && this.updateExpression != null) {
			String collectionName = message.getHeaders().get(MongoHeaders.COLLECTION_NAME, String.class);
			Object payload = message.getPayload();
			List<?> dataToUpdate;
			if (payload instanceof List) {
				dataToUpdate = (List<?>) payload;
			}
			else {
				dataToUpdate = Collections.singletonList(payload);
			}

			for (Object data : dataToUpdate) {
				Query query = new BasicQuery((String) data);

				Object value = this.updateExpression.getValue(this.evaluationContext, data);
				Assert.notNull(value, "'updateExpression' must not evaluate to null");
				Update update;
				if (value instanceof String) {
					update = new BasicUpdate((String) value);
				}
				else if (value instanceof Update) {
					update = ((Update) value);
				}
				else {
					throw new IllegalStateException("'updateExpression' must evaluate to String " +
							"or org.springframework.data.mongodb.core.query.Update");
				}

				this.mongoTemplate.updateFirst(query, update, collectionName);
			}
		}

		return message;
	}

}
