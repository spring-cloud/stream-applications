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

package org.springframework.cloud.fn.common.tensorflow.deprecated;

import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Maps domain objects into JSON strings.
 *
 * @author Christian Tzolov
 */
public class JsonMapperFunction implements Function<Object, String> {

	private static final Log logger = LogFactory.getLog(JsonMapperFunction.class);

	@Override
	public String apply(Object o) {
		try {
			return new ObjectMapper().writeValueAsString(o);
		}
		catch (JsonProcessingException e) {
			logger.error("Failed to encode the object detections into JSON message", e);
		}
		return "ERROR";

	}
}
