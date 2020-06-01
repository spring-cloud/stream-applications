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

package org.springframework.cloud.fn.supplier.geode.cq;

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.validation.annotation.Validated;

/**
 * This represents the configuration properties for the Gemfire CQ Source.
 *
 * @author David Turanski
 */
@ConfigurationProperties("geode.cq")
@Validated
public class GeodeCqSupplierProperties {

	private static final String DEFAULT_EXPRESSION = "newValue";

	/**
	 * SpEL expression to use to extract data from a cq event.
	 */
	private Expression eventExpression = new SpelExpressionParser().parseExpression
			(DEFAULT_EXPRESSION);

	/**
	 * The OQL query
	 */
	private String query;

	@NotEmpty(message = "A valid query string is required")
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Expression getEventExpression() {
		return eventExpression;
	}

	public void setEventExpression(Expression eventExpression) {
		this.eventExpression = eventExpression;
	}
}
