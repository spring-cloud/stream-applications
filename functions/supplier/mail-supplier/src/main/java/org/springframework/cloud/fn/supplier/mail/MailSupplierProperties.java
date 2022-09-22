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

package org.springframework.cloud.fn.supplier.mail;

import java.util.Properties;

import jakarta.mail.URLName;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.integration.mail.AbstractMailReceiver;
import org.springframework.validation.annotation.Validated;

/**
 * Properties for the file supplier.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Soby Chacko
 */
@ConfigurationProperties("mail.supplier")
@Validated
public class MailSupplierProperties {

	/**
	 * Mail connection URL for connection to Mail server e.g.
	 * 'imaps://username:password@imap.server.com:993/Inbox'.
	 */
	private URLName url;

	/**
	 * Set to true to mark email as read.
	 */
	private boolean markAsRead = false;

	/**
	 * Set to true to delete email after download.
	 */
	private boolean delete = false;

	/**
	 * Set to true to use IdleImap Configuration.
	 */
	private boolean idleImap = false;

	/**
	 * JavaMail properties as a new line delimited string of name-value pairs, e.g.
	 * 'foo=bar\n baz=car'.
	 */
	private Properties javaMailProperties = new Properties();

	/**
	 * Configure a SpEL expression to select messages.
	 */
	private String expression = "true";

	/**
	 * The charset for byte[] mail-to-string transformation.
	 */
	private String charset = "UTF-8";

	/**
	 * The flag to mark messages when the server does not support \Recent.
	 */
	private String userFlag = AbstractMailReceiver.DEFAULT_SI_USER_FLAG;

	/**
	 * @return the markAsRead
	 */
	public boolean isMarkAsRead() {
		return this.markAsRead;
	}

	/**
	 * @param markAsRead the markAsRead to set
	 */
	public void setMarkAsRead(boolean markAsRead) {
		this.markAsRead = markAsRead;
	}

	/**
	 * @return the delete
	 */
	public boolean isDelete() {
		return this.delete;
	}

	/**
	 * @param delete the delete to set
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	/**
	 * @return the idleImap
	 */
	public boolean isIdleImap() {
		return this.idleImap;
	}

	/**
	 * @param idleImap the idleImap to set
	 */
	public void setIdleImap(boolean idleImap) {
		this.idleImap = idleImap;
	}

	/**
	 * @return the javaMailProperties
	 */
	@NotNull
	public Properties getJavaMailProperties() {
		return this.javaMailProperties;
	}

	/**
	 * @param javaMailProperties the javaMailProperties to set
	 */
	public void setJavaMailProperties(Properties javaMailProperties) {
		this.javaMailProperties = javaMailProperties;
	}

	/**
	 * @return the url
	 */
	@NotNull
	public URLName getUrl() {
		return this.url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(URLName url) {
		this.url = url;
	}

	/**
	 * @return the expression
	 */
	@NotNull
	public String getExpression() {
		return this.expression;
	}

	/**
	 * @param expression the expression to set
	 */
	public void setExpression(String expression) {
		this.expression = expression;
	}

	@NotNull
	public String getCharset() {
		return this.charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	@NotNull
	public String getUserFlag() {
		return this.userFlag;
	}

	public void setUserFlag(String userFlag) {
		this.userFlag = userFlag;
	}
}
