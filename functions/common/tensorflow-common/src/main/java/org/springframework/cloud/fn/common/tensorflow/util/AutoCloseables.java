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

package org.springframework.cloud.fn.common.tensorflow.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for AutoCloseable classes.
 * Based on the Apache Drill AutoCloseables implementation.
 */
public final class AutoCloseables {

	private AutoCloseables() {

	}

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCloseables.class);

	public static AutoCloseable all(final Collection<? extends AutoCloseable> autoCloseables) {
		return () -> close(autoCloseables);
	}

	public static AutoCloseable all(final Map<?, ? extends AutoCloseable>... autoCloseables) {
		return () -> close(autoCloseables);
	}

	/**
	 * Closes all autoCloseables if not null and suppresses exceptions by adding them to t.
	 * @param t the throwable to add suppressed exception to
	 * @param autoCloseables the closeables to close
	 */
	public static void close(Throwable t, AutoCloseable... autoCloseables) {
		close(t, Arrays.asList(autoCloseables));
	}

	/**
	 * Closes all autoCloseables if not null and suppresses exceptions by adding them to t.
	 * @param t the throwable to add suppressed exception to
	 * @param autoCloseables the closeables to close
	 */
	public static void close(Throwable t, Collection<? extends AutoCloseable> autoCloseables) {
		try {
			close(autoCloseables);
		}
		catch (Exception e) {
			t.addSuppressed(e);
		}
	}

	/**
	 * Closes all autoCloseables if not null and suppresses subsequent exceptions if more than one.
	 * @param autoCloseables the closeables to close
	 */
	public static void close(AutoCloseable... autoCloseables) throws Exception {
		close(Arrays.asList(autoCloseables));
	}

	/**
	 * Closes all autoCloseables if not null and suppresses subsequent exceptions if more than one.
	 * @param autoCloseables the closeables to close
	 */
	public static void close(Iterable<? extends AutoCloseable> autoCloseables) throws Exception {
		Exception topLevelException = null;
		for (AutoCloseable closeable : autoCloseables) {
			try {
				if (closeable != null) {
					closeable.close();
				}
			}
			catch (Exception e) {
				if (topLevelException == null) {
					topLevelException = e;
				}
				else {
					topLevelException.addSuppressed(e);
				}
			}
		}
		if (topLevelException != null) {
			throw topLevelException;
		}
	}

	/**
	 * Closes all autoCloseables entry values if not null and suppresses subsequent exceptions if more than one.
	 * @param closableMaps the closeables to close
	 */
	public static void close(Map<?, ? extends AutoCloseable>... closableMaps) throws Exception {
		Exception topLevelException = null;
		for (Map<?, ? extends AutoCloseable> closableMap : closableMaps) {

			for (Object key : closableMap.keySet()) {
				AutoCloseable closeable = closableMap.get(key);
				try {
					if (closeable != null) {
						closeable.close();
					}
				}
				catch (Exception e) {
					if (topLevelException == null) {
						topLevelException = e;
					}
					else {
						topLevelException.addSuppressed(e);
					}
				}
			}

			closableMap.keySet();
		}
		if (topLevelException != null) {
			throw topLevelException;
		}

	}

	/**
	 * Close all without caring about thrown exceptions.
	 * @param closeables - array containing auto closeables
	 */
	public static void closeSilently(AutoCloseable... closeables) {
		Arrays.stream(closeables).filter(Objects::nonNull)
				.forEach(target -> {
					try {
						target.close();
					}
					catch (Exception e) {
						LOGGER.warn(String.format("Exception was thrown while closing auto closeable: %s", target), e);
					}
				});
	}

}
