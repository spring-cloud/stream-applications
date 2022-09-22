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

package org.springframework.cloud.fn.consumer.twitter.status.update;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.validation.annotation.Validated;


/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("twitter.update")
@Validated
public class TwitterUpdateConsumerProperties {

	private static final Expression DEFAULT_EXPRESSION = new SpelExpressionParser().parseExpression("payload");

	/**
	 * (SpEL expression) The text of the text update. URL encode as necessary. t.co link wrapping will
	 * affect character counts. Defaults to message's payload
	 */
	@NotNull
	private Expression text = DEFAULT_EXPRESSION;

	/**
	 * (SpEL expression) In order for a URL to not be counted in the text body of an extended Tweet, provide a URL as a Tweet attachment.
	 * This URL must be a Tweet permalink, or Direct Message deep link. Arbitrary, non-Twitter URLs must remain in
	 * the text text. URLs passed to the attachment_url parameter not matching either a Tweet permalink or Direct
	 * Message deep link will fail at Tweet creation and cause an exception.
	 */
	private Expression attachmentUrl;

	/**
	 * (SpEL expression) A place in the world.
	 */
	private Expression placeId;

	/**
	 * (SpEL expression) The ID of an existing text that the update is in reply to. Note: This parameter will be ignored unless the
	 * author of the Tweet this parameter references is mentioned within the text text. Therefore, you must
	 * include @username, where username is the author of the referenced Tweet, within the update.
	 *
	 * When inReplyToStatusId is set the auto_populate_reply_metadata is automatically set as well. Later ensures
	 * that leading @mentions will be looked up from the original Tweet, and added to the new Tweet from there.
	 * This wil append @mentions into the metadata of an extended Tweet as a reply chain grows, until the limit
	 * on @mentions is reached. In cases where the original Tweet has been deleted,
	 * the reply will fail.
	 */
	private Expression inReplyToStatusId;

	/**
	 * (SpEL expression) Whether or not to put a pin on the exact coordinates a Tweet has been sent from.
	 */
	private Expression displayCoordinates;

	/**
	 * (SpEL expression) A comma-delimited list of media_ids to associate with the Tweet. You may include up to 4 photos or 1 animated
	 * GIF or 1 video in a Tweet. See Uploading Media for further details on uploading media.
	 */
	private Expression mediaIds;

	/**
	 * (SpEL expression) The location this Tweet refers to. Ignored if geo_enabled for the user is false!
	 */
	private final Location location = new Location();

	public Expression getText() {
		return text;
	}

	public void setText(Expression text) {
		this.text = text;
	}

	public Expression getAttachmentUrl() {
		return attachmentUrl;
	}

	public void setAttachmentUrl(Expression attachmentUrl) {
		this.attachmentUrl = attachmentUrl;
	}

	public Expression getPlaceId() {
		return placeId;
	}

	public void setPlaceId(Expression placeId) {
		this.placeId = placeId;
	}

	public Expression getInReplyToStatusId() {
		return inReplyToStatusId;
	}

	public void setInReplyToStatusId(Expression inReplyToStatusId) {
		this.inReplyToStatusId = inReplyToStatusId;
	}

	public Expression getDisplayCoordinates() {
		return displayCoordinates;
	}

	public void setDisplayCoordinates(Expression displayCoordinates) {
		this.displayCoordinates = displayCoordinates;
	}

	public Expression getMediaIds() {
		return mediaIds;
	}

	public void setMediaIds(Expression mediaIds) {
		this.mediaIds = mediaIds;
	}

	public Location getLocation() {
		return location;
	}

	@AssertTrue(message = "Lat and Long must be set together or both not being set")
	public boolean validateLatLon() {
		return (this.getLocation().getLat() != null && this.getLocation().getLon() != null)
				|| (this.getLocation().getLat() == null && this.getLocation().getLon() == null);
	}

	public static class Location {
		/**
		 * The latitude of the location this Tweet refers to. This parameter will be ignored unless it is inside the range
		 * -90.0 to +90.0 (North is positive) inclusive. It will also be ignored if there is no corresponding long parameter.
		 */
		private Expression lat;

		/**
		 * The longitude of the location this Tweet refers to. The valid ranges for longitude are -180.0 to +180.0 (East
		 * is positive) inclusive. This parameter will be ignored if outside that range, if it is not a number,
		 * if geo_enabled is disabled, or if there no corresponding lat parameter.
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
