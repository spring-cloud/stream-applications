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

package org.springframework.cloud.fn.consumer.twitter.friendship;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


/**
 * @author Christian Tzolov
 */
@Component
@ConfigurationProperties("twitter.friendships.update")
@Validated
public class TwitterFriendshipsConsumerProperties {

	public enum OperationType {
		/** Friendship operation types. */
		create, update, destroy
	}

	/**
	 * The screen name of the user to follow (String).
	 */
	private Expression screenName;

	/**
	 * The ID of the user to follow (Integer).
	 */
	private Expression userId;

	/**
	 * Type of Friendships request.
	 */
	private Expression type = new SpelExpressionParser().parseExpression("'create'");

	/**
	 * Additional properties for the Friendships create requests.
	 */
	private Create create = new Create();

	/**
	 * Additional properties for the Friendships update requests.
	 */
	private Update update = new Update();

	public Expression getScreenName() {
		return screenName;
	}

	public void setScreenName(Expression screenName) {
		this.screenName = screenName;
	}

	public Expression getUserId() {
		return userId;
	}

	public void setUserId(Expression userId) {
		this.userId = userId;
	}

	public Expression getType() {
		return type;
	}

	public void setType(Expression type) {
		this.type = type;
	}

	public Create getCreate() {
		return create;
	}

	public Update getUpdate() {
		return update;
	}

	@AssertTrue(message = "Either userId or screenName must be provided")
	public boolean isUserProvided() {
		return this.userId != null || this.screenName != null;
	}

	public static class Create {
		/**
		 * The ID of the user to follow (boolean).
		 */
		@NotNull
		private Expression follow = new SpelExpressionParser().parseExpression("'true'");

		public Expression getFollow() {
			return follow;
		}

		public void setFollow(Expression follow) {
			this.follow = follow;
		}
	}

	public static class Update {
		/**
		 * Enable/disable device notifications from the target user.
		 */
		@NotNull
		private Expression device = new SpelExpressionParser().parseExpression("'true'");

		/**
		 * Enable/disable Retweets from the target user.
		 */
		@NotNull
		private Expression retweets = new SpelExpressionParser().parseExpression("'true'");

		public Expression getDevice() {
			return device;
		}

		public void setDevice(Expression device) {
			this.device = device;
		}

		public Expression getRetweets() {
			return retweets;
		}

		public void setRetweets(Expression retweets) {
			this.retweets = retweets;
		}
	}
}
