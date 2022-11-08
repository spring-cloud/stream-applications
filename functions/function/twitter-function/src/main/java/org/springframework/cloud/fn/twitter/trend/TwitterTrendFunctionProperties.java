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

package org.springframework.cloud.fn.twitter.trend;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.validation.annotation.Validated;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("twitter.trend")
@Validated
public class TwitterTrendFunctionProperties {

	private static final Expression DEFAULT_EXPRESSION = new SpelExpressionParser().parseExpression("payload");

	enum TrendQueryType {
		/** Retrieve trending places. */
		trend,
		/** Retrieve the Locations of trending places. */
		trendLocation
	}

	private TrendQueryType trendQueryType = TrendQueryType.trend;

	public TrendQueryType getTrendQueryType() {
		return trendQueryType;
	}

	public void setTrendQueryType(TrendQueryType trendQueryType) {
		this.trendQueryType = trendQueryType;
	}

	/**
	 * The Yahoo! Where On Earth ID of the location to return trending information for.
	 * Global information is available by using 1 as the WOEID.
	 */
	@NotNull
	private Expression locationId = DEFAULT_EXPRESSION;

	public Expression getLocationId() {
		return locationId;
	}

	public void setLocationId(Expression locationId) {
		this.locationId = locationId;
	}

	/**
	 *
	 */
	private Closest closest = new Closest();

	public Closest getClosest() {
		return closest;
	}

	public static class Closest {
		/**
		 * If provided with a long parameter the available trend locations will be sorted by distance, nearest
		 * to furthest, to the co-ordinate pair.
		 * The valid ranges for longitude is -180.0 to +180.0 (West is negative, East is positive) inclusive.
		 */
		private Expression lat;

		/**
		 * If provided with a lat parameter the available trend locations will be sorted by distance, nearest to
		 * furthest, to the co-ordinate pair. The valid ranges for longitude is -180.0 to +180.0 (West is negative,
		 * East is positive) inclusive.
		 */
		private Expression lon;

		public Expression getLat() {
			return lat;
		}

		public void setLat(Expression lat) {
			this.lat = lat;
		}

		public Expression getLon() {
			return lon;
		}

		public void setLon(Expression lon) {
			this.lon = lon;
		}
	}
}
