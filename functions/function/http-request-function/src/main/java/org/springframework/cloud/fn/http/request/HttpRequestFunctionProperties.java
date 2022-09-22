/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.cloud.fn.http.request;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Http Client Processor module.
 *
 * @author Waldemar Hummer
 * @author Mark Fisher
 * @author Christian Tzolov
 * @author Artem Bilan
 * @author David Turanski
 * @author Sunny Hemdev
 */
@Validated
@ConfigurationProperties("http.request")
public class HttpRequestFunctionProperties {

	private static final Class<?> DEFAULT_RESPONSE_TYPE = String.class;

	/**
	 * The type used to interpret the response.
	 */
	private Class<?> expectedResponseType = DEFAULT_RESPONSE_TYPE;

	/**
	 * Request timeout in milliseconds.
	 */
	private long timeout = 30_000;

	/**
	 * A SpEL expression against incoming message to determine the URL to use.
	 */
	private Expression urlExpression;

	/**
	 * A SpEL expression to derive the request method from the incoming message.
	 */
	private Expression httpMethodExpression = new ValueExpression(HttpMethod.GET);

	/**
	 * A SpEL expression to derive the request body from the incoming message.
	 */
	private Expression bodyExpression;

	/**
	 * A SpEL expression used to derive the http headers map to use.
	 */
	private Expression headersExpression;

	/**
	 * A SpEL expression used to compute the final result, applied against the whole http
	 * {@link org.springframework.http.ResponseEntity}.
	 */
	private Expression replyExpression = new FunctionExpression<ResponseEntity>(ResponseEntity::getBody);

	@NotNull
	public Expression getUrlExpression() {
		return urlExpression;
	}

	public void setUrlExpression(Expression urlExpression) {
		this.urlExpression = urlExpression;
	}

	public Expression getHttpMethodExpression() {
		return httpMethodExpression;
	}

	public void setHttpMethodExpression(Expression httpMethodExpression) {
		this.httpMethodExpression = httpMethodExpression;
	}

	@NotNull
	public Class<?> getExpectedResponseType() {
		return expectedResponseType;
	}

	public void setExpectedResponseType(Class<?> expectedResponseType) {
		this.expectedResponseType = expectedResponseType;
	}

	public long getTimeout() {
		return this.timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public Expression getBodyExpression() {
		return bodyExpression;
	}

	public void setBodyExpression(Expression bodyExpression) {
		this.bodyExpression = bodyExpression;
	}

	public Expression getHeadersExpression() {
		return headersExpression;
	}

	public void setHeadersExpression(Expression headersExpression) {
		this.headersExpression = headersExpression;
	}

	@NotNull
	public Expression getReplyExpression() {
		return replyExpression;
	}

	public void setReplyExpression(Expression replyExpression) {
		this.replyExpression = replyExpression;
	}

}
