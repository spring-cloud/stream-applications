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

package org.springframework.cloud.fn.twitter.geo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.validation.annotation.Validated;


/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("twitter.geo")
@Validated
public class TwitterGeoFunctionProperties {

	private static final Expression DEFAULT_EXPRESSION = new SpelExpressionParser().parseExpression("payload");

	public enum GeoType {
		/** Geo retrieval type. */
		reverse, search
	}

	/**
	 * Geo search API type: reverse or search.
	 */
	@NotNull
	private GeoType type = GeoType.search;

	/**
	 * Search geo type filter parameters.
	 */
	private Search search = new Search();

	/**
	 *
	 */
	private Location location = new Location();

	/**
	 * Hints for the number of results to return. This does not guarantee that the number of results
	 * returned will equal max_results, but instead informs how many 'nearby' results to return.
	 */
	private int maxResults = -1;

	/**
	 * Sets a hint on the 'region' in which to search. If a number, then this is a radius in meters, but it
	 * can also take a string that is suffixed with ft to specify feet. If this is not passed in, then it is
	 * assumed to be 0m. If coming from a device, in practice, this value is whatever accuracy the device
	 * has measuring its location (whether it be coming from a GPS, WiFi triangulation, etc.).
	 */
	private String accuracy = null;

	/**
	 * Minimal granularity of data to return. If this is not passed in, then neighborhood is assumed.
	 * City can also be passed.
	 */
	private String granularity = null;

	public Search getSearch() {
		return search;
	}

	public void setSearch(Search search) {
		this.search = search;
	}

	public GeoType getType() {
		return type;
	}

	public void setType(GeoType type) {
		this.type = type;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	public String getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(String accuracy) {
		this.accuracy = accuracy;
	}

	public String getGranularity() {
		return granularity;
	}

	public void setGranularity(String granularity) {
		this.granularity = granularity;
	}

	@AssertTrue(message = "Either the IP or the Location must be set")
	public boolean isAtLeastOne() {
		return this.getSearch().getIp() == null ^ (this.getLocation().getLat() == null && this.getLocation().getLon() == null);
	}

	@AssertTrue(message = "The IP parameter is applicable only for 'Search' GeoType")
	public boolean isIpUsedWithSearchGeoType() {
		if (this.getSearch().getIp() != null) {
			return this.type == GeoType.search;
		}
		return true;
	}

	public static class Search {
		/**
		 * An IP address. Used when attempting to fix geolocation based off of the user's IP address.
		 * Applicable only for 'search' geo type.
		 */
		private Expression ip = null;

		/**
		 * Query expression to filter Places in search results.
		 */
		private Expression query = null;

		public Expression getIp() {
			return ip;
		}

		public void setIp(Expression ip) {
			this.ip = ip;
		}

		public Expression getQuery() {
			return query;
		}

		public void setQuery(Expression query) {
			this.query = query;
		}
	}

	public static class Location {

		/**
		 * User's lat.
		 */
		private Expression lat;

		/**
		 * User's lon.
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
