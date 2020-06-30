/*
 * Copyright 2015-2020 the original author or authors.
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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.validation.annotation.Validated;

/**
 * @author Timo Salm
 */
@ConfigurationProperties("wavefront")
@Validated
public class WavefrontConsumerProperties {

	private String metricName;

	private String source;

	private Expression metricExpression;

	private Expression timestampExpression;

	private Map<String, Expression> pointTagsExpressionsPointValue;

	public WavefrontConsumerProperties() { }

	public WavefrontConsumerProperties(final String metricName, final String source, final Expression metricExpression,
										final Expression timestampExpression, final Map<String, Expression> pointTagsExpressionsPointValue,
										final String wavefrontDomain, final String wavefrontToken, final String wavefrontProxyUrl) {
		setMetricName(metricName);
		setSource(source);
		setMetricExpression(metricExpression);
		setTimestampExpression(timestampExpression);
		setPointTagsExpressionsPointValue(pointTagsExpressionsPointValue);
		setWavefrontDomain(wavefrontDomain);
		setWavefrontToken(wavefrontToken);
		setWavefrontProxyUrl(wavefrontProxyUrl);
	}

	private String wavefrontDomain;

	private String wavefrontToken;

	private String wavefrontProxyUrl;

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

	public Map<String, Expression> getPointTagsExpressionsPointValue() {
		return pointTagsExpressionsPointValue;
	}

	public void setPointTagsExpressionsPointValue(Map<String, Expression> pointTagsExpressionsPointValue) {
		this.pointTagsExpressionsPointValue = Optional.ofNullable(pointTagsExpressionsPointValue)
				.orElse(Collections.emptyMap());
	}

	public String getWavefrontDomain() {
		return wavefrontDomain;
	}

	public void setWavefrontDomain(String wavefrontDomain) {
		this.wavefrontDomain = wavefrontDomain;
	}

	public String getWavefrontToken() {
		return wavefrontToken;
	}

	public void setWavefrontToken(String wavefrontToken) {
		this.wavefrontToken = wavefrontToken;
	}

	public String getWavefrontProxyUrl() {
		return wavefrontProxyUrl;
	}

	public void setWavefrontProxyUrl(String wavefrontProxyUrl) {
		this.wavefrontProxyUrl = wavefrontProxyUrl;
	}
}
