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

package org.springframework.cloud.fn.supplier.debezium;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.debezium.engine.format.SerializationFormat;

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
		JSON(io.debezium.engine.format.JsonByteArray.class, "application/json"),
		/**
		 * AVRO change event format.
		 */
		AVRO(io.debezium.engine.format.Avro.class, "application/avro"),
		/**
		 * ProtoBuf change event format.
		 */
		PROTOBUF(io.debezium.engine.format.Protobuf.class, "application/x-protobuf"),;

		private final Class<? extends SerializationFormat<byte[]>> serializationFormat;
		private final String contentType;

		DebeziumFormat(Class<? extends SerializationFormat<byte[]>> serializationFormat, String contentType) {
			this.serializationFormat = serializationFormat;
			this.contentType = contentType;
		}

		public Class<? extends SerializationFormat<byte[]>> serializationFormat() {
			return serializationFormat;
		}

		public final String contentType() {
			return contentType;
		}
	};

	/**
	 * Spring pass-trough wrapper for debezium configuration properties. All properties with a 'debezium.inner.*' prefix
	 * are native Debezium properties.
	 */
	private Map<String, String> inner = defaultConfig();

	/**
	 * Change Event message content format. Defaults to 'JSON'.
	 */
	private DebeziumFormat format = DebeziumFormat.JSON;

	/**
	 * Copy Change Event headers into Message headers.
	 */
	private boolean copyHeaders = true;

	/**
	 * The policy that defines when the offsets should be committed to offset storage.
	 */
	private DebeziumOffsetCommitPolicy offsetCommitPolicy = DebeziumOffsetCommitPolicy.DEFAULT;

	public Map<String, String> getInner() {
		return inner;
	}

	private Map<String, String> defaultConfig() {
		Map<String, String> defaultConfig = new HashMap<>();
		return defaultConfig;
	}

	public DebeziumFormat getFormat() {
		return format;
	}

	public void setFormat(DebeziumFormat format) {
		this.format = format;
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
		 * policy will behave as ALWAYS policy. Requires the 'debezium.inner.offset.flush.interval.ms' native property
		 * to be set.
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
	 * Converts the Spring Framework "debezium.inner.*" properties into native Debezium configuration.
	 */
	public Properties getDebeziumNativeConfiguration() {
		Properties outProps = new java.util.Properties();
		outProps.putAll(this.getInner());
		return outProps;
	}
}
