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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Place;
import twitter4j.RateLimitStatus;
import twitter4j.Scopes;
import twitter4j.Status;
import twitter4j.SymbolEntity;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.fn.supplier.twitter.status.search.SearchPagination.UNBOUNDED;

/**
 * @author Christian Tzolov
 */
public class SearchPaginationTests {

	@Test
	public void tests1() {

		int maxCountPerRequest = 5;

		int count = 10;

		int pageCount = count / maxCountPerRequest;

		assertThat(pageCount).isEqualTo(2);

		SearchPagination pagination = new SearchPagination(pageCount, false);

		assertThat(pagination.getSinceId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageCounter()).isEqualTo(pageCount - 1);

		pagination.update(tweets(10, 8, 1));

		assertThat(pagination.getSinceId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getMaxId()).isEqualTo(8L - 1); // = min - 1
		assertThat(pagination.getPageMaxId()).isEqualTo(10L);
		assertThat(pagination.getPageCounter()).isEqualTo(pageCount - 2);

		pagination.update(tweets(7, 4, 1));

		assertThat(pagination.getSinceId()).isEqualTo(10L);
		assertThat(pagination.getMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageCounter()).isEqualTo(pageCount - 1);

		pagination.update(tweets(20, 14, 1));

		assertThat(pagination.getSinceId()).isEqualTo(10L);
		assertThat(pagination.getMaxId()).isEqualTo(14L - 1);
		assertThat(pagination.getPageMaxId()).isEqualTo(20L);
		assertThat(pagination.getPageCounter()).isEqualTo(pageCount - 2);

		pagination.update(tweets(13, 8, 1));

		assertThat(pagination.getSinceId()).isEqualTo(20L);
		assertThat(pagination.getMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageCounter()).isEqualTo(pageCount - 1);
	}

	@Test
	public void tests2() {

		int maxCountPerRequest = 5;

		int count = 10;

		int pageCount = count / maxCountPerRequest;

		assertThat(pageCount).isEqualTo(2);

		SearchPagination pagination = new SearchPagination(pageCount, true);

		assertThat(pagination.getSinceId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageCounter()).isEqualTo(pageCount - 1);

		pagination.update(tweets(10, 8, 1));

		assertThat(pagination.getSinceId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getMaxId()).isEqualTo(8L - 1); // = min - 1
		assertThat(pagination.getPageMaxId()).isEqualTo(10L);
		assertThat(pagination.getPageCounter()).isEqualTo(pageCount - 2);

		pagination.update(tweets(7, 4, 1));

		// Restart from Most Recent due to pageCounter == 0 while no reset have been performed so ar
		assertThat(pagination.getSinceId()).isEqualTo(10L);
		assertThat(pagination.getMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageCounter()).isEqualTo(pageCount - 1);

		pagination.update(tweets(20, 14, 1));

		assertThat(pagination.getSinceId()).isEqualTo(10L);
		assertThat(pagination.getMaxId()).isEqualTo(14L - 1);
		assertThat(pagination.getPageMaxId()).isEqualTo(20L);
		assertThat(pagination.getPageCounter()).isEqualTo(pageCount - 2);

		pagination.update(tweets(13, 8, 1));

		assertThat(pagination.getSinceId()).isEqualTo(10L);
		assertThat(pagination.getMaxId()).isEqualTo(8L - 1);
		assertThat(pagination.getPageMaxId()).isEqualTo(20L);
		assertThat(pagination.getPageCounter()).isEqualTo(pageCount - 3);

		pagination.update(new ArrayList<>()); // EMPTY

		// Restart from Most Recent on Empty Response
		assertThat(pagination.getSinceId()).isEqualTo(20L);
		assertThat(pagination.getMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageMaxId()).isEqualTo(UNBOUNDED);
		assertThat(pagination.getPageCounter()).isEqualTo(pageCount - 1);
	}

	public List<Status> tweets(int to, int from, int step) {
		List<Status> tweets = new ArrayList<>(to - from);
		for (int i = to; i >= from; i = i - step) {
			tweets.add(new MyStatus(i));
		}

		return tweets;
	}

	public static class MyStatus implements Status {

		private long id;

		public MyStatus(long id) {
			this.id = id;
		}

		@Override
		public Date getCreatedAt() {
			return null;
		}

		@Override
		public long getId() {
			return this.id;
		}

		@Override
		public String getText() {
			return null;
		}

		@Override
		public int getDisplayTextRangeStart() {
			return 0;
		}

		@Override
		public int getDisplayTextRangeEnd() {
			return 0;
		}

		@Override
		public String getSource() {
			return null;
		}

		@Override
		public boolean isTruncated() {
			return false;
		}

		@Override
		public long getInReplyToStatusId() {
			return 0;
		}

		@Override
		public long getInReplyToUserId() {
			return 0;
		}

		@Override
		public String getInReplyToScreenName() {
			return null;
		}

		@Override
		public GeoLocation getGeoLocation() {
			return null;
		}

		@Override
		public Place getPlace() {
			return null;
		}

		@Override
		public boolean isFavorited() {
			return false;
		}

		@Override
		public boolean isRetweeted() {
			return false;
		}

		@Override
		public int getFavoriteCount() {
			return 0;
		}

		@Override
		public User getUser() {
			return null;
		}

		@Override
		public boolean isRetweet() {
			return false;
		}

		@Override
		public Status getRetweetedStatus() {
			return null;
		}

		@Override
		public long[] getContributors() {
			return new long[0];
		}

		@Override
		public int getRetweetCount() {
			return 0;
		}

		@Override
		public boolean isRetweetedByMe() {
			return false;
		}

		@Override
		public long getCurrentUserRetweetId() {
			return 0;
		}

		@Override
		public boolean isPossiblySensitive() {
			return false;
		}

		@Override
		public String getLang() {
			return null;
		}

		@Override
		public Scopes getScopes() {
			return null;
		}

		@Override
		public String[] getWithheldInCountries() {
			return new String[0];
		}

		@Override
		public long getQuotedStatusId() {
			return 0;
		}

		@Override
		public Status getQuotedStatus() {
			return null;
		}

		@Override
		public URLEntity getQuotedStatusPermalink() {
			return null;
		}

		@Override
		public int compareTo(Status o) {
			return 0;
		}

		@Override
		public UserMentionEntity[] getUserMentionEntities() {
			return new UserMentionEntity[0];
		}

		@Override
		public URLEntity[] getURLEntities() {
			return new URLEntity[0];
		}

		@Override
		public HashtagEntity[] getHashtagEntities() {
			return new HashtagEntity[0];
		}

		@Override
		public MediaEntity[] getMediaEntities() {
			return new MediaEntity[0];
		}

		@Override
		public SymbolEntity[] getSymbolEntities() {
			return new SymbolEntity[0];
		}

		@Override
		public RateLimitStatus getRateLimitStatus() {
			return null;
		}

		@Override
		public int getAccessLevel() {
			return 0;
		}
	}
}
