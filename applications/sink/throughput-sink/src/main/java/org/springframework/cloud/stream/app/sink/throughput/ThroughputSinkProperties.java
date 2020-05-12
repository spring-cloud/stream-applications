/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.throughput;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Holds configuration options for the throughput Sink.
 *
 * @author Glenn Renfro
 */
@ConfigurationProperties("throughput")
public class ThroughputSinkProperties {

	/**
	 * how often to report.
	 */
	private int reportEveryMs = 1000;

	public int getReportEveryMs() {
		return reportEveryMs;
	}

	public void setReportEveryMs(int reportEveryMs) {
		this.reportEveryMs = reportEveryMs;
	}

}
