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

package org.springframework.cloud.fn.common.geode;

import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;

public class InetSocketAddressConverterConfiguration {
	@Bean
	@ConfigurationPropertiesBinding
	public Converter<String, InetSocketAddress> inetSocketAddressConverter() {
		return new InetSocketAddressConverter();
	}

	public static class InetSocketAddressConverter implements Converter<String, InetSocketAddress> {

		private static final Pattern HOST_AND_PORT_PATTERN = Pattern.compile("^\\s*(.*?):(\\d+)\\s*$");

		@Override
		public InetSocketAddress convert(String hostAddress) {
			Matcher m = HOST_AND_PORT_PATTERN.matcher(hostAddress);
			if (m.matches()) {
				String host = m.group(1);
				int port = Integer.parseInt(m.group(2));
				return new InetSocketAddress(host, port);
			}
			throw new IllegalArgumentException(String.format("%s is not a valid [host]:[port] value.", hostAddress));
		}
	}
}
