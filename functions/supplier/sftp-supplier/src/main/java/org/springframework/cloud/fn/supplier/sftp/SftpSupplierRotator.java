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

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.remote.aop.RotatingServerAdvice;
import org.springframework.integration.file.remote.aop.StandardRotationPolicy;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * An {@link RotatingServerAdvice} for listing files on multiple directories/servers.
 *
 * @author Gary Russell
 * @author David Turanski
 * @since 2.0
 */
public class SftpSupplierRotator extends RotatingServerAdvice {

	private static String SFTP_SELECTED_SERVER_PROPERTY_KEY = "sftp_selectedServer";

	private final SftpSupplierProperties properties;

	private final StandardRotationPolicy rotationPolicy;

	public SftpSupplierRotator(SftpSupplierProperties properties, StandardRotationPolicy rotationPolicy) {
		super(rotationPolicy);
		this.properties = properties;
		this.rotationPolicy = rotationPolicy;
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
}
