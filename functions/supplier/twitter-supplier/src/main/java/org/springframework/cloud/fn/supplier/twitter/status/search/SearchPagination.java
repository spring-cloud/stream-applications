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

import java.util.List;

import twitter4j.Status;

import org.springframework.util.Assert;

/**
 * Searched tweets are ordered from top to bottom by their IDs. The higher the ID, more recent the tweet is.
 *
 * The search goes backwards - from most recent to the oldest tweets and it uses the sinceId and the maxId to retrieve
 * only tweets with IDs in the [sinceId, maxId) range. The -1 stands for unbounded sinceId or maxId.
 *
 * The first `pageCount` number requests are performed backwards, leaving the bottom boundary (sinceId) unbounded and
 * adjusting the upper boundary (maxId) to the lowest tweet ID received. This ensures that no already processed
 * tweets are returned.
 *
 * The pageCounter is used the to count the number of pages retrieved in the pageCount range. Is starts from pageCount
 * and goes backward until 0. On 0 the pageCounter is reset back to pageCount.
 *
 * After performing pageCount number requests (e.g. pageCounter = 0), we start new iteration of searches from the top,
 * most recent tweets but now the bottom boundary (sinceId) is adjusted to the max ID received so far. That means that
 * only the newly added tweets will be processed
 *
 * Search pagination with max_id and since_id: https://developer.twitter.com/en/docs/tweets/timelines/guides/working-with-timelines.html
 *
 * @author Christian Tzolov
 */
public class SearchPagination {

	/**
	 * Wildcard used as refer as infinity.
	 */
	public static final long UNBOUNDED = -1;

	/**
	 * Number of pages to search in history before start form the top again.
	 *
	 * Note that the search goes backwards - from most recent to the oldest tweets.
	 * (eg. maxId == To max ID , sinceId == From min ID)
	 */
	private final int pageCount;

	/**
	 * Min tweet ID (e.g. from ID) to be included in the search result
	 */
	private long sinceId;

	/**
	 * Keep the max tweet ID in the last pageCount search requests.
	 */
	private long pageMaxId;

	/**
	 * Max tweet ID (e.g. To (ID - 1) )to be included in the search result
	 */
	private long maxId;

	/**
	 * Current page. Goes backwards from (pageCount-1) to 0.
	 */
	private int pageCounter;

	/**
	 * When set it.
	 */
	boolean searchBackwardsUntilEmptyResponse = false;

	public SearchPagination(int pageCount, boolean searchBackwardsUntilEmptyResponse) {

		Assert.isTrue(pageCount > 0, "At least one page needs to be set but was: " + pageCount);

		this.searchBackwardsUntilEmptyResponse = searchBackwardsUntilEmptyResponse;
		this.pageCount = pageCount;
		this.sinceId = UNBOUNDED; // == From ID
		this.pageMaxId = UNBOUNDED; // == From ID
		this.maxId = UNBOUNDED; // == To (ID - 1)
		this.pageCounter = pageCount - 1;
	}

	public long getSinceId() {
		return sinceId;
	}

	public long getMaxId() {
		return maxId;
	}

	public long getPageMaxId() {
		return pageMaxId;
	}

	public int getPageCounter() {
		return pageCounter;
	}

	public void update(List<Status> tweets) {

		tweets.stream().mapToLong(t -> t.getId()).min()
				.ifPresent(tweetsMinId -> {
					this.maxId = tweetsMinId - 1;
				});

		tweets.stream().mapToLong(t -> t.getId()).max()
				.ifPresent(tweetsMaxId -> {
					Assert.isTrue(this.sinceId <= tweetsMaxId,
							String.format("MAX_ID (%s) must be bigger then current SINCE_ID(%s)",
									tweetsMaxId, this.sinceId));
					this.pageMaxId = Math.max(this.pageMaxId, tweetsMaxId);
				});

		this.countDown(tweets.size());
	}

	private void countDown(int responseSize) {

		if (this.sinceId == UNBOUNDED) { // == first pass before reset
			if (this.pageCounter <= 0) {
				this.restartSearchFromMostRecent();
			}
		}
		else {
			if (this.searchBackwardsUntilEmptyResponse) {
				if (responseSize == 0) {
					this.restartSearchFromMostRecent();
				}
			}
			else if (this.pageCounter <= 0) {
				this.restartSearchFromMostRecent();
			}
		}

		this.pageCounter--;
	}

	private void restartSearchFromMostRecent() {
		this.pageCounter = this.pageCount;

		this.sinceId = Math.max(this.sinceId, Math.max(this.maxId + 1, this.pageMaxId));

		this.maxId = UNBOUNDED;
		this.pageMaxId = UNBOUNDED;
	}

	public String status() {
		return String.format("MaxId: %s, SinceId: %s, Page Counter# %s, pageMaxId: %s",
				this.maxId, this.sinceId, this.pageCounter, this.pageMaxId);
	}
}
