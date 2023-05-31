/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.cloud.fn.common.debezium;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("debezium")
public class DebeziumProperties {

	public enum DebeziumFormat {
		/**
		 * JSON change event format.
		 */
		JSON("application/json"),
		/**
		 * AVRO change event format.
		 */
		AVRO("application/avro"),
		/**
		 * ProtoBuf change event format.
		 */
		PROTOBUF("application/x-protobuf"),;

		private final String contentType;

		DebeziumFormat(String contentType) {
			this.contentType = contentType;
		}

		public final String contentType() {
			return contentType;
		}
	};

	/**
	 * Spring pass-trough wrapper for debezium configuration properties. All properties with a 'debezium.properties.*'
	 * prefix are native Debezium properties.
	 */
	private Map<String, String> properties = new HashMap<>();

	/**
	 * {@link ChangeEvent} Key and Payload formats. Defaults to 'JSON'.
	 */
	private DebeziumFormat payloadFormat = DebeziumFormat.JSON;

	/**
	 * {@link ChangeEvent} header format. Defaults to 'JSON'.
	 */
	private DebeziumFormat headerFormat = DebeziumFormat.JSON;

	/**
	 * Copy Change Event headers into Message headers.
	 */
	private boolean copyHeaders = true;

	/**
	 * The policy that defines when the offsets should be committed to offset storage.
	 */
	private DebeziumOffsetCommitPolicy offsetCommitPolicy = DebeziumOffsetCommitPolicy.DEFAULT;

	public Map<String, String> getProperties() {
		return properties;
	}

	public DebeziumFormat getPayloadFormat() {
		return payloadFormat;
	}

	public void setPayloadFormat(DebeziumFormat format) {
		this.payloadFormat = format;
	}

	public DebeziumFormat getHeaderFormat() {
		return headerFormat;
	}

	public void setHeaderFormat(DebeziumFormat headerFormat) {
		this.headerFormat = headerFormat;
	}

	public boolean isCopyHeaders() {
		return copyHeaders;
	}

	public void setCopyHeaders(boolean copyHeaders) {
		this.copyHeaders = copyHeaders;
	}

	public enum DebeziumOffsetCommitPolicy {
		/**
		 * Commits offsets as frequently as possible. This may result in reduced performance, but it has the least
		 * potential for seeing source records more than once upon restart.
		 */
		ALWAYS,
		/**
		 * Commits offsets no more than the specified time period. If the specified time is less than {@code 0} then the
		 * policy will behave as ALWAYS policy. Requires the 'debezium.properties.offset.flush.interval.ms' native
		 * property to be set.
		 */
		PERIODIC,
		/**
		 * Uses the default Debezium engine policy (PERIODIC).
		 */
		DEFAULT;
	}

	public DebeziumOffsetCommitPolicy getOffsetCommitPolicy() {
		return offsetCommitPolicy;
	}

	public void setOffsetCommitPolicy(DebeziumOffsetCommitPolicy offsetCommitPolicy) {
		this.offsetCommitPolicy = offsetCommitPolicy;
	}

	/**
	 * Converts the Spring Framework "debezium.properties.*" properties into native Debezium configuration.
	 */
	public Properties getDebeziumNativeConfiguration() {
		Properties outProps = new java.util.Properties();
		outProps.putAll(this.getProperties());
		return outProps;
	}
}
