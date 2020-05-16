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

package org.springframework.cloud.fn.supplier.tcp;

import org.junit.jupiter.api.Test;

import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 */
public class NotNioTests extends AbstractTcpSupplierTests {

	@Test
	public void test() {
		assertThat(this.connectionFactory).isInstanceOf(TcpNetServerConnectionFactory.class);
		assertThat(TestUtils.getPropertyValue(this.connectionFactory, "lookupHost", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.connectionFactory, "soTimeout")).isEqualTo(120000);
		assertThat(TestUtils.getPropertyValue(this.connectionFactory, "deserializer.maxMessageSize")).isEqualTo(2048);
	}
}
