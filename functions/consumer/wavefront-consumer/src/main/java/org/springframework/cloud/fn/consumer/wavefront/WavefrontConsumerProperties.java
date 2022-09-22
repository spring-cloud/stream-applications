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

package org.springframework.cloud.fn.consumer.wavefront;

import java.util.Map;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * @author Timo Salm
 */
@ConfigurationProperties("wavefront")
@Validated
public class WavefrontConsumerProperties {

	/**
	 * The name of the metric.Defaults to the application name.
	 */
	@Value("${spring.application.name:wavefront.consumer}")
	private String metricName;

	/**
	 * Unique application, host, container, or instance that emits metrics.
	 */
	private String source;

	/**
	 * A SpEL expression that evaluates to a metric value.
	 */
	private Expression metricExpression;

	/**
	 * A SpEL expression that evaluates to a timestamp of the metric (optional).
	 */
	private Expression timestampExpression;

	/**
	 * Collection of custom metadata associated with the metric.Point tags cannot be empty.
	 * Valid characters for keys: alphanumeric, hyphen ('-'), underscore ('_'), dot ('.').
	 * For values any character is allowed, including spaces. To include a double quote, escape it with a backslash,
	 * A backslash cannot be the last character in the tag value.
	 * Maximum allowed length for a combination of a point tag key and value is 254 characters
	 * (255 including the '=' separating key and value).
	 * If the value is longer, the point is rejected and logged
	 */
	private Map<String, Expression> tagExpression;

	/**
	 * The URL of the Wavefront environment.
	 */
	private String uri;

	/**
	 * Wavefront API access token.
	 */
	private String apiToken;

	/**
	 * The URL of the Wavefront proxy.
	 */
	private String proxyUri;

	public WavefrontConsumerProperties() {
	}

	WavefrontConsumerProperties(final String metricName, final String source, final Expression metricExpression,
			final Expression timestampExpression, final Map<String, Expression> pointTagExpressions,
			final String wavefrontServerUri, final String wavefrontApiToken, final String wavefrontProxyUrl) {

		setMetricName(metricName);
		setSource(source);
		setMetricExpression(metricExpression);
		setTimestampExpression(timestampExpression);
		setTagExpression(pointTagExpressions);
		setUri(wavefrontServerUri);
		setApiToken(wavefrontApiToken);
		setProxyUri(wavefrontProxyUrl);
	}

	@NotEmpty
	@Pattern(regexp = "^[a-zA-Z0-9./_,-]+")
	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	@NotEmpty
	@Size(max = 128)
	@Pattern(regexp = "^[a-zA-Z0-9._-]+")
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@NotNull
	public Expression getMetricExpression() {
		return metricExpression;
	}

	public void setMetricExpression(Expression metricExpression) {
		this.metricExpression = metricExpression;
	}

	public Expression getTimestampExpression() {
		return timestampExpression;
	}

	public void setTimestampExpression(Expression timestampExpression) {
		this.timestampExpression = timestampExpression;
	}

	public Map<String, Expression> getTagExpression() {
		return tagExpression;
	}

	public void setTagExpression(Map<String, Expression> tagExpression) {
		this.tagExpression = tagExpression;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getApiToken() {
		return apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	public String getProxyUri() {
		return proxyUri;
	}

	public void setProxyUri(String proxyUri) {
		this.proxyUri = proxyUri;
	}

	@AssertTrue(message = "Exactly one of 'proxy-uri' or the pair of ('uri' and 'api-token') must be set!")
	public boolean isMutuallyExclusiveProxyAndDirectAccessWavefrontConfiguration() {
		return StringUtils.isEmpty(getProxyUri()) ^ (StringUtils.isEmpty(getUri()) || StringUtils.isEmpty(getApiToken()));
	}
}
