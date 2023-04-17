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

package org.springframework.cloud.fn.supplier.cdc;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Christian Tzolov
 */
@ConfigurationProperties("cdc")
@Validated
public class CdcProperties {

	enum DebeziumFormat {
		json, avro
	};

	/**
	 * Spring pass-trough wrapper for debezium configuration properties. All properties with a 'cdc.debezium.' prefix
	 * are native Debezium properties. The prefix is removed, converting them into Debezium
	 * io.debezium.config.Configuration.
	 */
	private Map<String, String> debezium = defaultConfig();

	/**
	 * When set to 'true', it allows replace the default Consumer or ChangeEvent implementation. Do not change unless
	 * you know what you are doing.
	 */
	private boolean disableDefaultConsumer = false;

	/**
	 * (Experimental) Debezium message format. Defaults to 'json'.
	 */
	private DebeziumFormat format = DebeziumFormat.json;

	/**
	 * If set overrides the default message binding name. If not set the binding name is computed from the function
	 * definition name and the '-out-0' suffix. If the function definition name is empty, the binding name defaults to
	 * 'cdcSupplier-out-0'.
	 */
	private String overrideBindingName = null;

	public boolean isDisableDefaultConsumer() {
		return disableDefaultConsumer;
	}

	public void setDisableDefaultConsumer(boolean disableDefaultConsumer) {
		this.disableDefaultConsumer = disableDefaultConsumer;
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

	public String getOverrideBindingName() {
		return overrideBindingName;
	}

	public void setOverrideBindingName(String overrideBindingName) {
		this.overrideBindingName = overrideBindingName;
	}
}
