/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.pgcopy.sink;

import java.nio.charset.Charset;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.ByteArrayResource;

/**
 * An in-memory script crafted for dropping-creating the table we're working with.
 * All columns are created as VARCHAR(2000).
 *
 * @author Eric Bottard
 * @author Thomas Risberg
 */
public class DefaultInitializationScriptResource extends ByteArrayResource {

	private static final Log logger = LogFactory.getLog(DefaultInitializationScriptResource.class);

	public DefaultInitializationScriptResource(String tableName, Collection<String> columns) {
		super(scriptFor(tableName, columns).getBytes(Charset.forName("UTF-8")));
	}

	private static String scriptFor(String tableName, Collection<String> columns) {
		StringBuilder result = new StringBuilder("DROP TABLE ");
		result.append(tableName).append(";\n\n");

		result.append("CREATE TABLE ").append(tableName).append('(');
		int i = 0;
		for (String column : columns) {
			if (i++ > 0) {
				result.append(", ");
			}
			result.append(column).append(" VARCHAR(2000)");
		}
		result.append(");\n");
		logger.debug(String.format("Generated the following initializing script for table %s:\n%s", tableName,
				result.toString()));
		return result.toString();
	}
}
