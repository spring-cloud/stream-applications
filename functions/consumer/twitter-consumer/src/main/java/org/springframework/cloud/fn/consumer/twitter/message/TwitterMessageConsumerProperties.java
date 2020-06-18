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

package org.springframework.cloud.fn.consumer.twitter.message;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.validation.annotation.Validated;


/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("twitter.message.update")
@Validated
public class TwitterMessageConsumerProperties {

	private static final Expression DEFAULT_EXPRESSION = new SpelExpressionParser().parseExpression("payload");

	/**
	 * The direct message text. URL encode as necessary. Max length of 10,000 characters.
	 */
	private Expression text = DEFAULT_EXPRESSION;

	/**
	 * The screen name of the user to whom send the direct message.
	 */
	private Expression screenName;

	/**
	 * The user id of the user to whom send the direct message.
	 */
	private Expression userId;

	/**
	 * A media id to associate with the message. A Direct Message may only reference a single media id.
	 */
	private Expression mediaId;

	public Expression getUserId() {
		return userId;
	}

	public void setUserId(Expression userId) {
		this.userId = userId;
	}

	public void setText(Expression text) {
		this.text = text;
	}

	public Expression getText() {
		return text;
	}

	public Expression getScreenName() {
		return screenName;
	}

	public void setScreenName(Expression screenName) {
		this.screenName = screenName;
	}

	public Expression getMediaId() {
		return mediaId;
	}

	public void setMediaId(Expression mediaId) {
		this.mediaId = mediaId;
	}
}
