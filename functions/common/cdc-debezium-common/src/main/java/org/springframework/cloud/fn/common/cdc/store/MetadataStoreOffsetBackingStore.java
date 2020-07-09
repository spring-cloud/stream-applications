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

package org.springframework.cloud.fn.common.cdc.store;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.runtime.standalone.StandaloneConfig;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;

import org.springframework.integration.metadata.MetadataStore;

/**
 * @author Christian Tzolov
 */
public class MetadataStoreOffsetBackingStore extends MemoryOffsetBackingStore {

	private MetadataStore metadataStore;
	private String offsetStoreName;

	public MetadataStoreOffsetBackingStore(MetadataStore metadataStore) {
		this.metadataStore = metadataStore;
	}

	@Override
	public void configure(WorkerConfig config) {
		super.configure(config);
		this.offsetStoreName = config.getString(StandaloneConfig.OFFSET_STORAGE_FILE_FILENAME_CONFIG);
	}

	@Override
	public synchronized void start() {
		super.start();
		//log.info("Starting FileOffsetBackingStore with file {}", file);
		load();
	}

	@Override
	public synchronized void stop() {
		super.stop();
		// Nothing to do since this doesn't maintain any outstanding connections/data
		//	log.info("Stopped FileOffsetBackingStore");
	}

	private void load() {
		if (this.metadataStore.get("keys") != null) {
			String[] keys = this.metadataStore.get("keys").split(",");
			for (String keySting : keys) {
				String valueString = this.metadataStore.get(keySting);
				ByteBuffer key = ByteBuffer.wrap(keySting.getBytes());
				ByteBuffer value = ByteBuffer.wrap(valueString.getBytes());
				this.data.put(key, value);
			}
		}
	}

	@Override
	protected void save() {
		List<String> keys = new ArrayList<>();
		for (Map.Entry<ByteBuffer, ByteBuffer> mapEntry : this.data.entrySet()) {
			byte[] key = (mapEntry.getKey() != null) ? mapEntry.getKey().array() : null;
			byte[] value = (mapEntry.getValue() != null) ? mapEntry.getValue().array() : null;
			if (key != null && value != null) {
				keys.add(new String(key, StandardCharsets.UTF_8));
				this.metadataStore.put(
						new String(key, StandardCharsets.UTF_8),
						new String(value, StandardCharsets.UTF_8));
			}
		}
		this.metadataStore.put("keys", String.join(",", keys));
	}
}
