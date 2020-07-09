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

package org.springframework.cloud.fn.common.tensorflow.util;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * Extends the {@link ModelExtractor} to allow keeping a local copy (cache) of the loaded model (protobuf) files.
 * @author Christian Tzolov
 */
public class CachedModelExtractor extends ModelExtractor {

	private static final Log logger = LogFactory.getLog(CachedModelExtractor.class);

	/**
	 * Parent folder under which the model files are cached.
	 */
	public String cacheRootDirectory = new File(System.getProperty("java.io.tmpdir"), "mind-model").getAbsolutePath();

	public String getCacheRootDirectory() {
		return cacheRootDirectory;
	}

	public void setCacheRootDirectory(String cacheRootDirectory) {
		this.cacheRootDirectory = cacheRootDirectory;
	}

	public CachedModelExtractor() {
		super();
	}

	public CachedModelExtractor(String frozenGraphFileExtension) {
		super(frozenGraphFileExtension);
	}

	@Override
	public byte[] getModel(String modelUri) {
		return this.getModel(new DefaultResourceLoader().getResource(modelUri));
	}

	@Override
	public byte[] getModel(Resource modelResource) {
		try {
			File rootFolder = new File(this.cacheRootDirectory);
			if (!rootFolder.exists()) {
				logger.info("Create Model Cache root folder: " + rootFolder.getAbsolutePath());
				rootFolder.mkdirs();
			}

			Validate.isTrue(rootFolder.isDirectory(), "The cache root folder must be a Directory");

			String fileName = modelResource.getFilename();
			String fragment = modelResource.getURI().getFragment();
			File cachedFile = StringUtils.isEmpty(fragment) ? new File(rootFolder, fileName) :
					new File(rootFolder, fileName + "_" + fragment);
			if (cachedFile.exists()) {
				logger.info("Load model " + modelResource.toString() + " from cache: " + cacheRootDirectory);
				return IOUtils.toByteArray(new FileInputStream(cachedFile));
			}

			byte[] model = super.getModel(modelResource);

			// cache the file
			FileUtils.writeByteArrayToFile(cachedFile, model);
			logger.info("Caching the " + modelResource.toString() + " model at: " + cachedFile);

			return model;
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to extract a model from: " + modelResource.getDescription(), e);
		}
	}

	public void emptyModelCache() {
		File rootFolder = new File(this.cacheRootDirectory);
		if (rootFolder.exists()) {
			logger.info("Empty Model Cache at:" + rootFolder.getAbsolutePath());
			rootFolder.delete();
			rootFolder.mkdirs();
		}
	}
}
