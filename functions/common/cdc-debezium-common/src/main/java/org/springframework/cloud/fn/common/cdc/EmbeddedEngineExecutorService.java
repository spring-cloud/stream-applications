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

package org.springframework.cloud.fn.common.cdc;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Christian Tzolov
 */
public class EmbeddedEngineExecutorService implements Closeable {

	private static final Log logger = LogFactory.getLog(EmbeddedEngineExecutorService.class);

	private final EmbeddedEngine engine;
	private final ExecutorService executor;

	public EmbeddedEngineExecutorService(EmbeddedEngine engine) {
		this.engine = engine;
		this.executor = Executors.newSingleThreadExecutor();
	}

	public void start() {
		logger.info("Start Embedded Engine");
		this.executor.execute(this.engine);
	}

	@Override
	public void close() {
		logger.info("Stop Embedded Engine");
		this.engine.stop();
		this.executor.shutdown();
	}
}
