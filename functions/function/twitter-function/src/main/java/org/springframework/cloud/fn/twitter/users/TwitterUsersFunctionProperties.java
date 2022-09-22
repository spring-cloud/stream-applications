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

package org.springframework.cloud.fn.twitter.users;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.validation.annotation.Validated;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("twitter.users")
@Validated
public class TwitterUsersFunctionProperties {

	private static final Expression DEFAULT_EXPRESSION = new SpelExpressionParser().parseExpression("payload");

	public enum UserQueryType {
		/** User retrieval types. */
		search, lookup
	}

	/**
	 * Perform search or lookup type of search.
	 */
	@NotNull
	private UserQueryType type = UserQueryType.search;

	/**
	 * Returns fully-hydrated user objects for specified by comma-separated values passed to the user_id and/or
	 * screen_name parameters.
	 */
	private final Lookup lookup = new Lookup();

	/**
	 * relevance-based search interface for querying by topical interest, full name, company name, location,
	 * or other criteria.
	 */
	private final Search search = new Search();

	public UserQueryType getType() {
		return type;
	}

	public void setType(UserQueryType type) {
		this.type = type;
	}

	public Lookup getLookup() {
		return lookup;
	}

	public Search getSearch() {
		return search;
	}

	@AssertTrue(message = "Per query type validate the required parameters")
	public boolean checkParametersPerType() {
		if (this.getType() == UserQueryType.lookup) {
			return (this.getLookup().getScreenName() != null) || (this.getLookup().getUserId() != null);
		}
		else if (this.getType() == UserQueryType.search) {
			return (this.getSearch().getQuery() != null);
		}

		return false;
	}

	public static class Lookup {
		/**
		 * A comma separated list of user IDs, up to 100 are allowed in a single request.
		 * You are strongly encouraged to use a POST for larger requests.
		 */
		private Expression userId;

		/**
		 * A comma separated list of screen names, up to 100 are allowed in a single request.
		 * You are strongly encouraged to use a POST for larger (up to 100 screen names) requests.
		 */
		private Expression screenName;

		public Expression getUserId() {
			return userId;
		}

		public void setUserId(Expression userId) {
			this.userId = userId;
		}

		public Expression getScreenName() {
			return screenName;
		}

		public void setScreenName(Expression screenName) {
			this.screenName = screenName;
		}
	}

	public static class Search {
		/**
		 * The search query to run against people search.
		 */
		private Expression query = DEFAULT_EXPRESSION;

		/**
		 * Specifies the page of results to retrieve.
		 */
		private int page = 3;

		public Expression getQuery() {
			return query;
		}

		public void setQuery(Expression query) {
			this.query = query;
		}

		public int getPage() {
			return page;
		}

		public void setPage(int page) {
			this.page = page;
		}
	}
}
