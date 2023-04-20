/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.cloud.fn.supplier.debezium.custom;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.debezium.engine.DebeziumEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;

/**
 * @author Christian Tzolov
 */
public class EmbeddedEngineExecutorService implements SmartLifecycle, AutoCloseable {

	private static final Log logger = LogFactory.getLog(EmbeddedEngineExecutorService.class);

	private final DebeziumEngine<?> engine;
	private final ExecutorService executor;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public EmbeddedEngineExecutorService(DebeziumEngine<?> engine) {
		this.engine = engine;
		this.executor = Executors.newSingleThreadExecutor();
	}

	@Override
	public void start() {
		logger.info("Start Embedded Engine");
		this.executor.execute(this.engine);
		this.running.set(true);
	}

	@Override
	public void stop() {
		this.close();
	}

	@Override
	public void close() {
		logger.info("Stop Embedded Engine");
		try {
			this.engine.close();
			this.running.set(false);
		}
		catch (IOException e) {
			logger.warn("Failed to close the Debezium Engine:", e);
		}
		this.executor.shutdown();
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}
}
