/*
 * Copyright 2017 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.consumer.cassandra.query;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * @author Akos Ratku
 * @author Artem Bilan
 */
public class UpdateQueryColumnNameExtractor implements ColumnNameExtractor {

	private static final Pattern PATTERN = Pattern.compile("(?i)(?<=set)(.*)(?=where)where(.*)");

	@Override
	public List<String> extract(String query) {
		List<String> extractedColumns = new LinkedList<>();
		Matcher matcher = PATTERN.matcher(query);
		if (matcher.find()) {
			String[] settings = StringUtils.delimitedListToStringArray(matcher.group(1), ",", " ");
			String[] where = StringUtils.delimitedListToStringArray(matcher.group(2), ",", " ");
			readPairs(extractedColumns, settings);
			readPairs(extractedColumns, where);
		}
		else {
			throw new IllegalArgumentException("Invalid CQL update query syntax: " + query);
		}
		return extractedColumns;
	}

	protected void readPairs(List<String> extractedColumns, String[] settings) {
		for (String setting : settings) {
			String[] columnValuePair = StringUtils.delimitedListToStringArray(setting, "=", " ");
			if (columnValuePair[1].startsWith(":") || columnValuePair[1].equals("?")) {
				extractedColumns.add(columnValuePair[0]);
			}
		}
	}

}
