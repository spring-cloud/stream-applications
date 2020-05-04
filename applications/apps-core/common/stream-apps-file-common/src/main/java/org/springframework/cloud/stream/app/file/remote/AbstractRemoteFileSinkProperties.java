/*
 * Copyright 2015-2016 the original author or authors.
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

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.expression.Expression;
import org.springframework.integration.file.support.FileExistsMode;

/**
 * @deprecated - properties are flattened.
 *
 * @author Gary Russell
 *
 */
@Deprecated
public abstract class AbstractRemoteFileSinkProperties extends AbstractRemoteFileProperties {

	/**
	 * A temporary directory where the file will be written if {@link #isUseTemporaryFilename()}
	 * is true.
	 */
	private String temporaryRemoteDir = "/";

	/**
	 * Whether or not to create the remote directory.
	 */
	private boolean autoCreateDir = true;

	/**
	 * Action to take if the remote file already exists.
	 */
	private FileExistsMode mode = FileExistsMode.REPLACE;

	/**
	 * Whether or not to write to a temporary file and rename.
	 */
	private boolean useTemporaryFilename = true;

	/**
	 * A SpEL expression to generate the remote file name.
	 */
	private Expression filenameExpression;

	@NotBlank
	public String getTemporaryRemoteDir() {
		return this.temporaryRemoteDir;
	}

	public void setTemporaryRemoteDir(String temporaryRemoteDir) {
		this.temporaryRemoteDir = temporaryRemoteDir;
	}

	public boolean isAutoCreateDir() {
		return this.autoCreateDir;
	}

	public void setAutoCreateDir(boolean autoCreateDir) {
		this.autoCreateDir = autoCreateDir;
	}

	@NotNull
	public FileExistsMode getMode() {
		return this.mode;
	}

	public void setMode(FileExistsMode mode) {
		this.mode = mode;
	}

	public boolean isUseTemporaryFilename() {
		return this.useTemporaryFilename;
	}

	public void setUseTemporaryFilename(boolean useTemporaryFilename) {
		this.useTemporaryFilename = useTemporaryFilename;
	}

	public Expression getFilenameExpression() {
		return this.filenameExpression;
	}

	public void setFilenameExpression(Expression filenameExpression) {
		this.filenameExpression = filenameExpression;
	}

}
