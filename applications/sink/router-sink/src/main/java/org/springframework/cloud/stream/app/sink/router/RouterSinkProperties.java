/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.router;

import java.util.Properties;
import java.util.function.Function;

import jakarta.validation.constraints.AssertTrue;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.messaging.Message;

/**
 * Properties for the Router Sink; the router can use an expression
 * or groovy script to return either a channel name, or a key to
 * the channel mappings map.
 *
 * @author Gary Russell
 * @author Artem Bilan
 */
@ConfigurationProperties("router")
public class RouterSinkProperties {

	/**
	 * Default SpEL expression.
	 */
	public static final Expression DEFAULT_EXPRESSION =
			new FunctionExpression<>((Function<Message<?>, Object>) message -> message.getHeaders().get("routeTo"));

	/**
	 * Variable bindings as a new line delimited string of name-value pairs, e.g. 'foo=bar\n baz=car'.
	 */
	private Properties variables;

	/**
	 * The location of a properties file containing custom script variable bindings.
	 */
	private Resource variablesLocation;

	/**
	 * The expression to be applied to the message to determine the channel(s) to route to.
	 * Note that the payload wire format for content types such as text, json or xml is byte[] not String!.
	 * Consult the documentation for how to handle byte array payload content.
	 */
	private Expression expression = DEFAULT_EXPRESSION;

	/**
	 * The location of a groovy script that returns channels or channel mapping
	 * resolution keys.
	 */
	private Resource script;

	/**
	 * How often to check for script changes in ms (if present); < 0 means don't refresh.
	 */
	private int refreshDelay = 60000;

	/**
	 * Where to send un-routable messages.
	 */
	private String defaultOutputBinding;

	/**
	 * Whether channel resolution is required.
	 */
	private boolean resolutionRequired = false;

	/**
	 * Destination mappings as a new line delimited string of name-value pairs, e.g. 'foo=bar\n baz=car'.
	 */
	private Properties destinationMappings;

	public Properties getVariables() {
		return variables;
	}

	public void setVariables(Properties variables) {
		this.variables = variables;
	}

	public Resource getVariablesLocation() {
		return variablesLocation;
	}

	public void setVariablesLocation(Resource variablesLocation) {
		this.variablesLocation = variablesLocation;
	}

	public Expression getExpression() {
		return this.expression;
	}

	public void setExpression(Expression expression) {
		this.expression = expression;
	}

	public Resource getScript() {
		return script;
	}

	public void setScript(Resource script) {
		this.script = script;
	}

	public String getDefaultOutputBinding() {
		return this.defaultOutputBinding;
	}

	public void setDefaultOutputBinding(String defaultOutputBinding) {
		this.defaultOutputBinding = defaultOutputBinding;
	}

	public int getRefreshDelay() {
		return refreshDelay;
	}

	public void setRefreshDelay(int refreshDelay) {
		this.refreshDelay = refreshDelay;
	}

	public boolean isResolutionRequired() {
		return this.resolutionRequired;
	}

	public void setResolutionRequired(boolean resolutionRequired) {
		this.resolutionRequired = resolutionRequired;
	}

	public Properties getDestinationMappings() {
		return destinationMappings;
	}

	public void setDestinationMappings(Properties destinationMappings) {
		this.destinationMappings = destinationMappings;
	}

	@AssertTrue(message = "'expression' and 'script' are mutually exclusive")
	public boolean isExpressionOrScriptValid() {
		return this.script == null || this.expression == DEFAULT_EXPRESSION;
	}

}
