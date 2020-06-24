/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.sftp;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.expression.ExpressionEvalMap;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.remote.aop.RotatingServerAdvice;
import org.springframework.integration.file.remote.aop.StandardRotationPolicy;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.springframework.cloud.fn.supplier.sftp.SftpHeaders.SFTP_HOST_PROPERTY_KEY;
import static org.springframework.cloud.fn.supplier.sftp.SftpHeaders.SFTP_PASSWORD_PROPERTY_KEY;
import static org.springframework.cloud.fn.supplier.sftp.SftpHeaders.SFTP_PORT_PROPERTY_KEY;
import static org.springframework.cloud.fn.supplier.sftp.SftpHeaders.SFTP_SELECTED_SERVER_PROPERTY_KEY;
import static org.springframework.cloud.fn.supplier.sftp.SftpHeaders.SFTP_USERNAME_PROPERTY_KEY;

/**
 * An {@link RotatingServerAdvice} for listing files on multiple directories/servers.
 *
 * @author Gary Russell
 * @author David Turanski
 * @since 2.0
 */
public class SftpSupplierRotator extends RotatingServerAdvice {

	private final SftpSupplierProperties properties;

	private final StandardRotationPolicy rotationPolicy;

	public SftpSupplierRotator(SftpSupplierProperties properties, StandardRotationPolicy rotationPolicy) {
		super(rotationPolicy);
		this.properties = properties;
		this.rotationPolicy = rotationPolicy;
	}

	/**
	 * Build a {@code Map<String,Expression>} whose values are obtained by dynamically
	 * evaluating the expressions. The values are dependent on the selected session factory in
	 * the rotation.
	 * @return the map as {@code Map<String, Object> } to use as an argument for
	 * {@code IntegrationFlowBuilder.enrichHeaders()}.
	 */
	public Map<String, Object> headers() {
		Supplier<SftpSupplierProperties.Factory> factory = () -> {
			SftpSupplierProperties.Factory selected = this.properties.getFactories().get(this.getCurrentKey());
			if (selected == null) {
				// missing key used default factory
				selected = this.properties.getFactory();
			}
			return selected;
		};

		Map<String, Object> map = new HashMap<>();
		map.put(SFTP_SELECTED_SERVER_PROPERTY_KEY, new FunctionExpression<>(m -> this.getCurrentKey()));
		map.put(SFTP_HOST_PROPERTY_KEY, new FunctionExpression<>(m -> factory.get().getHost()));
		map.put(SFTP_PORT_PROPERTY_KEY, new FunctionExpression<>(m -> factory.get().getPort()));
		map.put(SFTP_USERNAME_PROPERTY_KEY, new FunctionExpression<>(m -> factory.get().getUsername()));
		map.put(SFTP_PASSWORD_PROPERTY_KEY, new FunctionExpression<>(m -> factory.get().getPassword()));
		return map;
	}

	public String getCurrentKey() {
		return this.rotationPolicy.getCurrent().getKey().toString();
	}

	public String getCurrentDirectory() {
		return this.rotationPolicy.getCurrent().getDirectory();
	}

	@Override
	public Message<?> afterReceive(Message<?> result, MessageSource<?> source) {
		if (result != null) {
			result = MessageBuilder.fromMessage(result)
					.setHeader(SFTP_SELECTED_SERVER_PROPERTY_KEY, this.getCurrentKey()).build();
		}
		this.rotationPolicy.afterReceive(result != null, source);
		return result;
	}

	/**
	 * Evaluate the headers.
	 * @return a {@code Map<String,Object>}
	 */
	public Map<String, Object> evaluateHeaders() {
		return ExpressionEvalMap.from(headers())
				.usingSimpleCallback()
				.build();
	}
}
