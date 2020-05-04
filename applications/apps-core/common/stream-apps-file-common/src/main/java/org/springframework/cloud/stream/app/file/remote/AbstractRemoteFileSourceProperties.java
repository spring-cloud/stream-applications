/*
 * Copyright 2015-2017 the original author or authors.
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
package org.springframework.cloud.stream.app.file.remote;

import java.io.File;
import java.util.regex.Pattern;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

/**
 * Common properties for remote file sources (e.g. (S)FTP).
 *
 * @deprecated - properties are flattened.
 *
 * @author David Turanski
 * @author Gary Russell
 *
 */
@Deprecated
public abstract class AbstractRemoteFileSourceProperties extends AbstractRemoteFileProperties {

	/**
	 * Set to true to delete remote files after successful transfer.
	 */
	private boolean deleteRemoteFiles = false;

	/**
	 * The local directory to use for file transfers.
	 */
	private File localDir = new File(System.getProperty("java.io.tmpdir") + "/xd/ftp");

	/**
	 * Set to true to create the local directory if it does not exist.
	 */
	private boolean autoCreateLocalDir = true;

	/**
	 * A filter pattern to match the names of files to transfer.
	 */
	private String filenamePattern;

	/**
	 * A filter regex pattern to match the names of files to transfer.
	 */
	private Pattern filenameRegex;

	/**
	 * Set to true to preserve the original timestamp.
	 */
	private boolean preserveTimestamp = true;

	public boolean isAutoCreateLocalDir() {
		return autoCreateLocalDir;
	}

	public void setAutoCreateLocalDir(boolean autoCreateLocalDir) {
		this.autoCreateLocalDir = autoCreateLocalDir;
	}

	public boolean isDeleteRemoteFiles() {
		return deleteRemoteFiles;
	}

	public void setDeleteRemoteFiles(boolean deleteRemoteFiles) {
		this.deleteRemoteFiles = deleteRemoteFiles;
	}

	@NotNull
	public File getLocalDir() {
		return localDir;
	}

	public final void setLocalDir(File localDir) {
		this.localDir = localDir;
	}

	public String getFilenamePattern() {
		return filenamePattern;
	}

	public void setFilenamePattern(String filenamePattern) {
		this.filenamePattern = filenamePattern;
	}

	public Pattern getFilenameRegex() {
		return filenameRegex;
	}

	public void setFilenameRegex(Pattern filenameRegex) {
		this.filenameRegex = filenameRegex;
	}

	public boolean isPreserveTimestamp() {
		return preserveTimestamp;
	}

	public void setPreserveTimestamp(boolean preserveTimestamp) {
		this.preserveTimestamp = preserveTimestamp;
	}

	@AssertTrue(message = "filenamePattern and filenameRegex are mutually exclusive")
	public boolean isExclusivePatterns() {
		return !(this.filenamePattern != null && this.filenameRegex != null);
	}

}
