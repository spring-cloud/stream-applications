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

package org.springframework.cloud.stream.app.sink.throughput;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

@Configuration
@EnableConfigurationProperties({ThroughputSinkProperties.class})
public class ThroughputSinkConfiguration {

	private final Log logger = LogFactory.getLog(getClass());

	private final AtomicLong counter = new AtomicLong();

	private final AtomicLong start = new AtomicLong(-1);

	private final AtomicLong bytes = new AtomicLong(-1);

	private final AtomicLong intermediateCounter = new AtomicLong();

	private final AtomicLong intermediateBytes = new AtomicLong();

	private final TimeUnit timeUnit = TimeUnit.SECONDS;

	private final ExecutorService executorService = Executors.newFixedThreadPool(1);

	private volatile boolean reportBytes = false;

	@Autowired
	private volatile ThroughputSinkProperties properties;

	@Bean
	public Consumer<Message<?>> throughputConsumer() {
		return message -> {
			if (start.get() == -1L) {
				synchronized (start) {
					if (start.get() == -1L) {
						// assume a homogeneous message structure - this is intended for
						// performance tests so we can assume that the messages are similar;
						// therefore we'll do our reporting based on the first message
						Object payload = message.getPayload();
						if (payload instanceof byte[] || payload instanceof String) {
							reportBytes = true;
						}
						start.set(System.currentTimeMillis());
						executorService.execute(new ReportStats());
					}
				}
			}
			intermediateCounter.incrementAndGet();
			if (reportBytes) {
				Object payload = message.getPayload();
				if (payload instanceof byte[]) {
					intermediateBytes.addAndGet(((byte[]) payload).length);
				}
				else if (payload instanceof String) {
					intermediateBytes.addAndGet((((String) payload).getBytes()).length);
				}
			}
		};
	}

	private class ReportStats implements Runnable {
		@Override
		public void run() {
			int reportEveryMs = properties.getReportEveryMs();
			long intervalStart = System.currentTimeMillis();
			try {
				Thread.sleep(reportEveryMs);
				long timeNow = System.currentTimeMillis();
				long currentCounter = intermediateCounter.getAndSet(0L);
				long currentBytes = intermediateBytes.getAndSet(0L);
				long totalCounter = counter.addAndGet(currentCounter);
				long totalBytes = bytes.addAndGet(currentBytes);

				logger.info(
						String.format("Messages: %10d in %5.2f%s = %11.2f/s",
								currentCounter,
								(timeNow - intervalStart) / 1000.0, timeUnit, ((double) currentCounter * 1000 / reportEveryMs)));
				logger.info(
						String.format("Messages: %10d in %5.2f%s = %11.2f/s",
								totalCounter, (timeNow - start.get()) / 1000.0, timeUnit,
								((double) totalCounter * 1000 / (timeNow - start.get()))));
				if (reportBytes) {
					logger.info(
							String.format("Throughput: %12d in %5.2f%s = %11.2fMB/s, ",
									currentBytes,
									(timeNow - intervalStart) / 1000.0, timeUnit,
									((currentBytes / (1024.0 * 1024)) * 1000 / reportEveryMs)));
					logger.info(
							String.format("Throughput: %12d in %5.2f%s = %11.2fMB/s",
									totalBytes, (timeNow - start.get()) / 1000.0, timeUnit,
									((totalBytes / (1024.0 * 1024)) * 1000 / (timeNow - start.get()))));
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.warn("Thread interrupted", e);
			}
		}
	}
}
