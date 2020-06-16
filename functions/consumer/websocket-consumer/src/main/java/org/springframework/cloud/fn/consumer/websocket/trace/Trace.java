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

package org.springframework.cloud.fn.consumer.websocket.trace;

import java.util.Date;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * A value object representing a trace event: at a particular time with a simple (map)
 * information. Can be used for analyzing contextual information such as HTTP headers.
 *
 * <p> It is a copy of {@code InMemoryTraceRepository} from Spring Boot 1.5.x.
 * Since Spring Boot 2.0 traces are only available for HTTP.
 *
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class Trace {

	private final Date timestamp;

	private final Map<String, Object> info;

	public Trace(Date timestamp, Map<String, Object> info) {
		Assert.notNull(timestamp, "Timestamp must not be null");
		Assert.notNull(info, "Info must not be null");
		this.timestamp = timestamp;
		this.info = info;
	}

	public Date getTimestamp() {
		return this.timestamp;
	}

	public Map<String, Object> getInfo() {
		return this.info;
	}
}
