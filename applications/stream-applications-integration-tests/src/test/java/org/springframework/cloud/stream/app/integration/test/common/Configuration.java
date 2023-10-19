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

package org.springframework.cloud.stream.app.integration.test.common;

import java.time.Duration;
import java.util.function.Supplier;

public abstract class Configuration {

	/**
	 * Version.
	 */
	public static String VERSION;

	/**
	 * Duration.
	 */
	public static final Duration DEFAULT_DURATION = Duration.ofSeconds(90);

	private static final String SPRING_CLOUD_STREAM_APPLICATIONS_VERSION = "spring.cloud.stream.applications.version";

	static {
		VERSION = System.getProperty(SPRING_CLOUD_STREAM_APPLICATIONS_VERSION, "latest");
		System.out.println("VERSION=" + VERSION);
	}

	public static class VersionSupplier implements Supplier<String> {

		@Override
		public String get() {
			return VERSION;
		}
	}

}
