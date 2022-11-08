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

package org.springframework.cloud.fn.supplier.twitter.friendships;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("twitter.friendships.source")
@Validated
public class TwitterFriendshipsSupplierProperties {

	public enum FriendshipsRequestType {
		/** Friendship query types. */
		followers, friends
	}

	/**
	 * Selects between followers or friends APIs.
	 */
	@NotNull
	private TwitterFriendshipsSupplierProperties.FriendshipsRequestType type = FriendshipsRequestType.followers;

	/**
	 * The screen name of the user for whom to return results.
	 */
	private String screenName;

	/**
	 * The ID of the user for whom to return results.
	 */
	private Long userId;

	/**
	 * The number of users to return per page, up to a maximum of 200. Defaults to 20.
	 */
	@Positive
	@Max(200)
	private int count = 200;

	/**
	 * When set to true, statuses will not be included in the returned user objects.
	 */
	private boolean skipStatus = false;

	/**
	 * The user object entities node will be included when set to false.
	 */
	private boolean includeUserEntities = true;

	/**
	 * API request poll interval in milliseconds. Must be aligned with used APIs rate limits (~ 1 req/ 2 min).
	 */
	private int pollInterval = 121000;

	public FriendshipsRequestType getType() {
		return type;
	}

	public void setType(FriendshipsRequestType type) {
		this.type = type;
	}

	public String getScreenName() {
		return screenName;
	}

	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public boolean isSkipStatus() {
		return skipStatus;
	}

	public void setSkipStatus(boolean skipStatus) {
		this.skipStatus = skipStatus;
	}

	public boolean isIncludeUserEntities() {
		return includeUserEntities;
	}

	public void setIncludeUserEntities(boolean includeUserEntities) {
		this.includeUserEntities = includeUserEntities;
	}

	public int getPollInterval() {
		return pollInterval;
	}

	public void setPollInterval(int pollInterval) {
		this.pollInterval = pollInterval;
	}

	@AssertTrue(message = "Either userId or screenName must be provided")
	public boolean isUserProvided() {
		return this.userId != null || this.screenName != null;
	}

	@Override
	public String toString() {
		return "TwitterFriendshipsSourceProperties{" +
				"type=" + type +
				", screenName='" + screenName + '\'' +
				", userId=" + userId +
				", count=" + count +
				", skipStatus=" + skipStatus +
				", includeUserEntities=" + includeUserEntities +
				", pollInterval=" + pollInterval +
				'}';
	}
}
