/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.xmpp;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Daniel Frey
 * @since 4.0.0
 */
@ConfigurationProperties("xmpp.consumer")
@Validated
public class XmppConsumerProperties {

	/**
	 * XMPP handle to send message to.
	 */
	private String chatTo;

	/**
	 * @param chatTo the XMPP handle.
	 *
	 */
	public void setChatTo(String chatTo) {
		this.chatTo = chatTo;
	}

	@NotEmpty(message = "chatTo is required")
	public String getChatTo() {
		return chatTo;
	}

}
