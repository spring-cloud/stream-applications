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

package org.springframework.cloud.fn.supplier.twitter.status.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import twitter4j.Query;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("twitter.search")
@Validated
public class TwitterSearchSupplierProperties {

	/**
	 * Search tweets by search query string.
	 */
	@NotNull
	@NotEmpty
	private String query;

	/**
	 * Number of pages (e.g. requests) to search backwards (from most recent to the oldest tweets) before start
	 * the search from the most recent tweets again.
	 * The total amount of tweets searched backwards is (page * count)
	 */
	@Positive
	private int page = 3;

	/**
	 * Number of tweets to return per page (e.g. per single request), up to a max of 100.
	 */
	@Positive
	@Max(100)
	private int count = 100;

	/**
	 * Restricts searched tweets to the given language, given by an http://en.wikipedia.org/wiki/ISO_639-1 .
	 */
	private String lang = null;

	/**
	 * If specified, returns tweets with since the given date. Date should be formatted as YYYY-MM-DD.
	 */
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
	private String since = null;

	/**
	 * If specified, returns tweets by users located within a given radius (in Km) of the given latitude/longitude,
	 * where the user's location is taken from their Twitter profile.
	 * Should be formatted as
	 */
	private Geocode geocode = new Geocode();

	/**
	 *  Specifies what type of search results you would prefer to receive.
	 *  The current default is "mixed." Valid values include:
	 *   mixed : Include both popular and real time results in the response.
	 *   recent : return only the most recent results in the response
	 *   popular : return only the most popular results in the response
	 */
	@NotNull
	private Query.ResultType resultType = Query.ResultType.mixed;

	/**
	 * Restart search from the most recent tweets on empty response.
	 * Applied only after the first restart (e.g. when since_id != UNBOUNDED)
	 */
	private boolean restartFromMostRecentOnEmptyResponse = false;

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getSince() {
		return since;
	}

	public void setSince(String since) {
		this.since = since;
	}

	public Geocode getGeocode() {
		return geocode;
	}

	public Query.ResultType getResultType() {
		return resultType;
	}

	public void setResultType(Query.ResultType resultType) {
		this.resultType = resultType;
	}

	public boolean isRestartFromMostRecentOnEmptyResponse() {
		return restartFromMostRecentOnEmptyResponse;
	}

	public void setRestartFromMostRecentOnEmptyResponse(boolean restartFromMostRecentOnEmptyResponse) {
		this.restartFromMostRecentOnEmptyResponse = restartFromMostRecentOnEmptyResponse;
	}

	public static class Geocode {

		/**
		 * User's latitude.
		 */
		private double latitude = -1;

		/**
		 * User's longitude.
		 */
		private double longitude = -1;

		/**
		 * Radius (in kilometers) around the (latitude, longitude) point.
		 */
		private double radius = -1;

		public double getLatitude() {
			return latitude;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		public double getRadius() {
			return radius;
		}

		public void setRadius(double radius) {
			this.radius = radius;
		}

		public boolean isValid() {
			return this.radius > 0;
		}
	}
}
