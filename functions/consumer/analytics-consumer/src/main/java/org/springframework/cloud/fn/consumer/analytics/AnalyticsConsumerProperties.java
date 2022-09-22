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

package org.springframework.cloud.fn.consumer.analytics;

import java.util.Map;

import jakarta.validation.constraints.AssertTrue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.validation.annotation.Validated;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("analytics")
@Validated
public class AnalyticsConsumerProperties {

	enum MeterType {
		/** Uses the Micrometer Counter meter type. It accumulates intermediate counts toward the point where
		 * the data is sent to the metrics backend.*/
		counter,
		/** Uses the Micrometer Gauge meter type. Gauges sample the input time series. Any intermediate values set on
		 * a gauge are lost by the time the gauge value is reported to a metrics backend.
		 * TIP: Never gauge something you can count with a Counter!*/
		gauge
	}

	/**
	 * Micrometer meter type used to report the metrics to the backend.
	 */
	private MeterType meterType = MeterType.counter;

	/**
	 * The name of the output metrics.
	 * The 'name' and 'nameExpression' are mutually exclusive. Only one of them can be set.
	 */
	private String name;

	/**
	 * A SpEL expression to compute the output metrics name from the input message.
	 * The 'name' and 'nameExpression' are mutually exclusive. Only one of them can be set.
	 */
	private Expression nameExpression;

	/**
	 * If neither `name` nor `nameExpression` is set the name of the output metrics defaults to the
	 * Spring application name.
	 */
	@Value("${spring.application.name:analytics}")
	private String defaultName;

	/**
	 * A SpEL expression to compute the output metrics value (e.g. amount).
	 * It defaults to 1.0
	 */
	private Expression amountExpression;

	/**
	 * Fixed and computed tags to be assignee with the output metric.
	 */
	private final MetricsTag tag = new MetricsTag();

	public MetricsTag getTag() {
		return tag;
	}

	public MeterType getMeterType() {
		return meterType;
	}

	public void setMeterType(MeterType meterType) {
		this.meterType = meterType;
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

	@AssertTrue(message = "exactly one of 'name' and 'nameExpression' must be set")
	public boolean isExclusiveOptions() {
		return getName() != null ^ getNameExpression() != null;
	}

	@Override
	public String toString() {
		return "AnalyticsFunctionProperties{" +
				"defaultName='" + defaultName + '\'' +
				", name=" + name +
				", tag=" + tag +
				'}';
	}

	public static class MetricsTag {

		/**
		 * DEPRECATED: Please use the analytics.tag.expression with literal SpEL expression.
		 *
		 * Custom, fixed Tags. Those tags have constant values, created once and then sent along with every
		 * published metrics. The convention to define a fixed Tags is:
		 * <code>
		 *   analytics.tag.fixed.[tag-name]=[tag-value]
		 * </code>
		 */
		@Deprecated
		private Map<String, String> fixed;

		/**
		 * Computes tags from SpEL expression.
		 * Single SpEL expression can produce an array of values, which in turn means distinct name/value tags.
		 * Every name/value tag will produce a separate meter increment.
		 * Tag expression format is: analytics.tag.expression.[tag-name]=[SpEL expression]
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
}
