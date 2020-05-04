/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.file;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * TODO: This will be used in other apps like (S)FTP and S3. Therefore, it might be moved to a common place.
 *
 * @author David Turanski
 * @author Artem Bilan
 */
@ConfigurationProperties("file.consumer")
@Validated
public class FileConsumerProperties {

	/**
	 * The FileReadingMode to use for file reading sources.
	 * Values are 'ref' - The File object,
	 * 'lines' - a message per line, or
	 * 'contents' - the contents as bytes.
	 */
	private FileReadingMode mode = FileReadingMode.contents;

	/**
	 * 	Set to true to emit start of file/end of file marker messages before/after the data.
	 * 	Only valid with FileReadingMode 'lines'.
	 */
	private Boolean withMarkers = null;

	/**
	 * When 'fileMarkers == true', specify if they should be produced
	 * as FileSplitter.FileMarker objects or JSON.
	 */
	private boolean markersJson = true;

	@NotNull
	public FileReadingMode getMode() {
		return this.mode;
	}

	public void setMode(FileReadingMode mode) {
		this.mode = mode;
	}

	public Boolean getWithMarkers() {
		return this.withMarkers;
	}

	public void setWithMarkers(Boolean withMarkers) {
		this.withMarkers = withMarkers;
	}

	public boolean getMarkersJson() {
		return this.markersJson;
	}

	public void setMarkersJson(boolean markersJson) {
		this.markersJson = markersJson;
	}

	@AssertTrue(message = "withMarkers can only be supplied when FileReadingMode is 'lines'")
	public boolean isWithMarkersValid() {
		return this.withMarkers == null || FileReadingMode.lines == this.mode;
	}
}
