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

import io.debezium.engine.format.SerializationFormat;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("cdc")
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
	 * Spring pass-trough wrapper for debezium configuration properties. All properties with a 'cdc.debezium.' prefix
	 * are native Debezium properties. The prefix is removed, converting them into Debezium
	 * io.debezium.config.Configuration.
	 */
	private Map<String, String> debezium = defaultConfig();

	/**
	 * (Experimental) Debezium message format. Defaults to 'json'.
	 */
	private DebeziumFormat format = DebeziumFormat.JSON;

	/**
	 * If set overrides the default message binding name. If not set the binding name is computed from the function
	 * definition name and the '-out-0' suffix. If the function definition name is empty, the binding name defaults to
	 * 'debezium-out-0'.
	 */
	private String bindingName = null;

	/**
	 * Copy Change Event headers into Message headers.
	 */
	private boolean convertHeaders = true;

	/**
	 * DebeziumEngine specific configurations.
	 */
	private DebeziumEngineConfiguration engine = new DebeziumEngineConfiguration();

	public DebeziumEngineConfiguration getEngine() {
		return engine;
	}

	public Map<String, String> getDebezium() {
		return debezium;
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

	public String getBindingName() {
		return bindingName;
	}

	public void setBindingName(String overrideBindingName) {
		this.bindingName = overrideBindingName;
	}

	public boolean isConvertHeaders() {
		return convertHeaders;
	}

	public void setConvertHeaders(boolean convertHeaders) {
		this.convertHeaders = convertHeaders;
	}

	public static class DebeziumEngineConfiguration {

		/**
		 * The policy that defines when the offsets should be committed to offset storage.
		 */
		public enum DebeziumOffsetCommitPolicy {
			/**
			 * Commits offsets as frequently as possible. This may result in reduced performance, but it has the least
			 * potential for seeing source records more than once upon restart.
			 */
			ALWAYS,
			/**
			 * Commits offsets no more than the specified time period. If the specified time is less than {@code 0} then
			 * the policy will behave as ALWAYS policy. Requires the 'cdc.debezium.offset.flush.interval.ms' native
			 * property to be set.
			 */
			PERIODIC,
			/**
			 * Uses the default Debezium engine policy (PERIODIC).
			 */
			DEFAULT;
		}

		private DebeziumOffsetCommitPolicy offsetCommitPolicy = DebeziumOffsetCommitPolicy.DEFAULT;

		public DebeziumOffsetCommitPolicy getOffsetCommitPolicy() {
			return offsetCommitPolicy;
		}

		public void setOffsetCommitPolicy(DebeziumOffsetCommitPolicy offsetCommitPolicy) {
			this.offsetCommitPolicy = offsetCommitPolicy;
		}
	}
}
