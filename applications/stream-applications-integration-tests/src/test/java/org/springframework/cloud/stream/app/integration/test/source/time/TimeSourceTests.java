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

package org.springframework.cloud.stream.app.integration.test.source.time;

import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.OutputMatcher;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;
@Tag("integration")
abstract class TimeSourceTests {

	// "MM/dd/yy HH:mm:ss";
	private final static Pattern pattern = Pattern.compile(".*\\d{2}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}");

	@Autowired
	private OutputMatcher outputMatcher;

	@Test
	void test() {
		await().atMost(DEFAULT_DURATION)
				.until(outputMatcher.payloadMatches((String s) -> pattern.matcher(s).matches()));
	}

	@AfterEach
	void cleanUp() {
		outputMatcher.clearMessageMatchers();
	}
}
