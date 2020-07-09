/*
 * Copyright 2020-2020 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Christian Tzolov
 */
@ConfigurationProperties("cdc.stream")
@Validated
public class CdcSupplierProperties {

	/**
	 * Control what information to be serialized in the outbound message headers.
	 */
	private Header header = new Header();

	public Header getHeader() {
		return header;
	}

	public static class Header {

		/**
		 * Serializes the source record's offset metadata into the outbound message header under cdc.offset.
		 */
		private boolean offset = false;

		/**
		 * When true the {@link org.apache.kafka.connect.header.Header} are converted into message headers with the
		 * {@link org.apache.kafka.connect.header.Header#key()} as name and {@link org.apache.kafka.connect.header.Header#value()}.
		 */
		private boolean convertConnectHeaders = true;

		public boolean isOffset() {
			return offset;
		}

		public void setOffset(boolean offset) {
			this.offset = offset;
		}

		public boolean isConvertConnectHeaders() {
			return convertConnectHeaders;
		}

		public void setConvertConnectHeaders(boolean convertConnectHeaders) {
			this.convertConnectHeaders = convertConnectHeaders;
		}
	}
}
