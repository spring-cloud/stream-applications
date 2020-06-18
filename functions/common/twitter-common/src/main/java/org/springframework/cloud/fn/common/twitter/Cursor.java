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

package org.springframework.cloud.fn.common.twitter;

/**
 * @author Christian Tzolov
 */
public class Cursor {
	private long cursor = -1;

	public long getCursor() {
		return cursor;
	}

	public void updateCursor(long newCursor) {
		this.cursor = (newCursor > 0) ? newCursor : -1;
	}

	@Override
	public String toString() {
		return "Cursor{cursor=" + cursor + '}';
	}
}
