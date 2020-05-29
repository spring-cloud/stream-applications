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

package org.springframework.cloud.fn.supplier.geode;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * This represents the configuration properties for the Gemfire Supplier.
 *
 * @author David Turanski
 */
@ConfigurationProperties("geode.supplier")
public class GeodeSupplierProperties {

	private static final String DEFAULT_EXPRESSION = "newValue";

	private final SpelExpressionParser parser = new SpelExpressionParser();

	/**
	 * SpEL expression to extract data from an {@link org.apache.geode.cache.EntryEvent}.
	 */
	private Expression entryEventExpression = parser.parseExpression(DEFAULT_EXPRESSION);

	public Expression getEntryEventExpression() {
		return entryEventExpression;
	}

	public void setEntryEventExpression(Expression entryEventExpression) {
		this.entryEventExpression = entryEventExpression;
	}
}
