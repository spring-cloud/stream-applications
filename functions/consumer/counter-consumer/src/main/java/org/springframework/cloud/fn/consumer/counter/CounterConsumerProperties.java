/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.consumer.counter;

import java.util.Map;

import javax.validation.constraints.AssertTrue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.validation.annotation.Validated;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("counter")
@Validated
public class CounterConsumerProperties {

	/**
	 * The default name of the increment
	 */
	@Value("${spring.application.name:counts}")
	private String defaultName;

	/**
	 * The name of the counter to increment. The 'name' and 'nameExpression' are mutually exclusive.
	 * Only one can be set.
	 */
	private String name;

	/**
	 * A SpEL expression (against the incoming Message) to derive the name of the counter to increment.
	 * The 'name' and 'nameExpression' are mutually exclusive. Only one can be set.
	 */
	private Expression nameExpression;

	/**
	 * A SpEL expression (against the incoming Message) to derive the amount to add to the counter.
	 * If not set the counter is incremented by 1.0
	 */
	private Expression amountExpression;

	/**
	 * Enables counting the number of messages processed. Uses the 'message.' counter name prefix to distinct it
	 * form the expression based counter. The message counter includes the fixed tags when provided.
	 */
	private boolean messageCounterEnabled = true;

	/**
	 * Fixed and computed tags to be assignee with the counter increment measurement.
	 */
	private MetricsTag tag = new MetricsTag();

	public static class MetricsTag {

		/**
		 * Custom tags assigned to every counter increment measurements.
		 * This is a map so the property convention fixed tags is: counter.tag.fixed.[tag-name]=[tag-value]
		 */
		private Map<String, String> fixed;

		/**
		 * Computes tags from SpEL expression.
		 * Single SpEL expression can produce an array of values, which in turn means distinct name/value tags.
		 * Every name/value tag will produce a separate counter increment.
		 * Tag expression format is: counter.tag.expression.[tag-name]=[SpEL expression]
		 */
		private Map<String, Expression> expression;

		public Map<String, String> getFixed() {
			return fixed;
		}

		public void setFixed(Map<String, String> fixed) {
			this.fixed = fixed;
		}

		public Map<String, Expression> getExpression() {
			return expression;
		}

		public void setExpression(Map<String, Expression> expression) {
			this.expression = expression;
		}

		@Override
		public String toString() {
			return "MetricsTag{" +
					"fixed=" + fixed +
					", expression=" + expression +
					'}';
		}
	}

	public MetricsTag getTag() {
		return tag;
	}

	public String getName() {
		if (name == null && nameExpression == null) {
			return defaultName;
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Expression getNameExpression() {
		return nameExpression;
	}

	public void setNameExpression(Expression nameExpression) {
		this.nameExpression = nameExpression;
	}

	public Expression getAmountExpression() {
		return amountExpression;
	}

	public void setAmountExpression(Expression amountExpression) {
		this.amountExpression = amountExpression;
	}

	public Expression getComputedAmountExpression() {
		return (amountExpression != null ? amountExpression : new LiteralExpression("1.0"));
	}

	public Expression getComputedNameExpression() {
		return (nameExpression != null ? nameExpression : new LiteralExpression(getName()));
	}

	public boolean isMessageCounterEnabled() {
		return messageCounterEnabled;
	}

	public void setMessageCounterEnabled(boolean messageCounterEnabled) {
		this.messageCounterEnabled = messageCounterEnabled;
	}

	@AssertTrue(message = "exactly one of 'name' and 'nameExpression' must be set")
	public boolean isExclusiveOptions() {
		return getName() != null ^ getNameExpression() != null;
	}

	@Override
	public String toString() {
		return "CounterFunctionProperties{" +
				"defaultName='" + defaultName + '\'' +
				", name=" + name +
				", tag=" + tag +
				'}';
	}
}
