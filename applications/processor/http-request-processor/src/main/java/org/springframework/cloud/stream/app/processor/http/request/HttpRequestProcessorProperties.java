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

package org.springframework.cloud.stream.app.processor.http.request;

import java.util.Map;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.fn.http.request.HttpRequestFunctionProperties;
import org.springframework.cloud.fn.http.request.HttpRequestProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.Message;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Http Request Processor module.
 *
 * @author Waldemar Hummer
 * @author Mark Fisher
 * @author Christian Tzolov
 * @author Artem Bilan
 * @author David Turanski
 */

@Validated
@ConfigurationProperties("http.request.processor")
public class HttpRequestProcessorProperties implements HttpRequestProperties {

	private static final HttpMethod DEFAULT_HTTP_METHOD = HttpMethod.GET;

	private static final Class<?> DEFAULT_RESPONSE_TYPE = String.class;

	/**
	 * The URL to issue an http request to, as a static value.
	 */
	private String url;

	/**
	 * The (static) request body; if neither this nor bodyExpression is provided, the payload
	 * will be used.
	 */
	private Object body;

	/**
	 * The kind of http method to use.
	 */
	private HttpMethod httpMethod = DEFAULT_HTTP_METHOD;

	/**
	 * The type used to interpret the response.
	 */
	private Class<?> expectedResponseType = DEFAULT_RESPONSE_TYPE;

	/**
	 * Request timeout in milliseconds.
	 */
	private long timeout = 30_000;

	/**
	 * A Map of HTTP request headers.
	 */
	private HttpHeaders headers = new HttpHeaders();

	/**
	 * A SpEL expression against incoming message to determine the URL to use.
	 */
	private Expression urlExpression;

	/**
	 * A SpEL expression to derive the request method from the incoming message.
	 */
	private Expression httpMethodExpression;

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
	private Expression replyExpression = new SpelExpressionParser().parseExpression("body");

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
		this.setHttpMethod(null);
		this.httpMethodExpression = httpMethodExpression;
	}

	@Override
	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	@NotNull
	@Override
	public Class<?> getExpectedResponseType() {
		return expectedResponseType;
	}

	public void setExpectedResponseType(Class<?> expectedResponseType) {
		this.expectedResponseType = expectedResponseType;
	}

	@Override
	public Object getBody() {
		return body;
	}

	public void setBody(Object body) {
		this.body = body;
	}

	@Override
	public long getTimeout() {
		return this.timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public HttpHeaders getHeaders() {
		return headers;
	}

	public void setHeaders(HttpHeaders headers) {
		this.headers = headers;
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

	@AssertTrue(message = "Exactly one of 'url' or 'urlExpression' is required")
	public boolean isExactlyOneUrl() {
		return getUrl() == null ^ urlExpression == null;
	}

	@AssertTrue(message = "At most one of 'body' or 'bodyExpression' is allowed")
	public boolean isAtMostOneBody() {
		return getBody() == null || bodyExpression == null;
	}

	@AssertTrue(message = "At most one of 'httpMethod' or 'httpMethodExpression' is allowed")
	public boolean isAtMostOneHttpMethod() {
		return getHttpMethod() == null || httpMethodExpression == null;
	}

	public boolean usesRequestExpressions() {
		return headersExpression != null ||
				bodyExpression != null ||
				httpMethodExpression != null ||
				urlExpression != null;
	}

	HttpRequestFunctionProperties evaluateFunctionProperties(Message<?> message) {
		HttpRequestFunctionProperties properties = new HttpRequestFunctionProperties();
		properties.setUrl(urlExpression != null ? urlExpression.getValue(message, String.class) : getUrl());
		properties.setBody(bodyExpression != null ? bodyExpression.getValue(message) : getBody());
		properties.setHttpMethod(httpMethodExpression != null ? httpMethodExpression.getValue(message, HttpMethod.class)
				: getHttpMethod());

		HttpHeaders headers = new HttpHeaders();
		headers.addAll(getHeaders());
		if (headersExpression != null) {
			Map<?, ?> headersMap = headersExpression.getValue(message, Map.class);
			for (Map.Entry<?, ?> header : headersMap.entrySet()) {
				if (header.getKey() != null && header.getValue() != null) {
					headers.add(header.getKey().toString(),
							header.getValue().toString());
				}
			}
		}
		properties.setHeaders(headers);

		properties.setTimeout(getTimeout());

		properties.setExpectedResponseType(getExpectedResponseType());

		return properties;
	}
}
