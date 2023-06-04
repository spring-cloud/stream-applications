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

import org.junit.jupiter.api.Test;

import org.springframework.cloud.fn.common.debezium.DebeziumProperties.DebeziumFormat;
import org.springframework.cloud.fn.common.debezium.DebeziumProperties.DebeziumOffsetCommitPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DebeziumProperties}.
 *
 * @author Christian Tzolov
 */
public class DebeziumPropertiesTests {

	DebeziumProperties properties = new DebeziumProperties();

	@Test
	public void defaultPropertiesTest() {
		assertThat(this.properties.getPayloadFormat()).isEqualTo(DebeziumFormat.JSON);
		assertThat(this.properties.getHeaderFormat()).isEqualTo(DebeziumFormat.JSON);
		assertThat(this.properties.getOffsetCommitPolicy()).isEqualTo(DebeziumOffsetCommitPolicy.DEFAULT);
		assertThat(this.properties.getProperties()).isNotNull();
		assertThat(this.properties.getProperties()).isEmpty();
	}

	@Test
	public void debeziumFormatTest() {
		this.properties.setPayloadFormat(DebeziumFormat.AVRO);
		assertThat(this.properties.getPayloadFormat()).isEqualTo(DebeziumFormat.AVRO);
		assertThat(this.properties.getPayloadFormat().contentType()).isEqualTo("application/avro");

		this.properties.setPayloadFormat(DebeziumFormat.JSON);
		assertThat(this.properties.getPayloadFormat()).isEqualTo(DebeziumFormat.JSON);
		assertThat(this.properties.getPayloadFormat().contentType()).isEqualTo("application/json");

		this.properties.setPayloadFormat(DebeziumFormat.PROTOBUF);
		assertThat(this.properties.getPayloadFormat()).isEqualTo(DebeziumFormat.PROTOBUF);
		assertThat(this.properties.getPayloadFormat().contentType()).isEqualTo("application/x-protobuf");
	}

}
