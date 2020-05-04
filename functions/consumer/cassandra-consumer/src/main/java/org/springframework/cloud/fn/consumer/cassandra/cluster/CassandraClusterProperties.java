/*
 * Copyright 2015-2020 the original author or authors.
 *
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

package org.springframework.cloud.fn.consumer.cassandra.cluster;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Common properties for the cassandra modules.
 *
 * @author Artem Bilan
 * @author Thomas Risberg
 * @author Rob Hardt
 */
@ConfigurationProperties("cassandra.cluster")
public class CassandraClusterProperties {

	/**
	 * Flag to create (or not) keyspace on application startup.
	 */
	private boolean createKeyspace;

	/**
	 * Resource with CQL scripts (delimited by ';') to initialize keyspace schema.
	 */
	private Resource initScript;

	/**
	 * Flag to validate the Servers' SSL certs
	 */
	private boolean skipSslValidation;

	/**
	 * Base packages to scan for entities annotated with Table annotations.
	 */
	private String[] entityBasePackages = { };


	public void setCreateKeyspace(boolean createKeyspace) {
		this.createKeyspace = createKeyspace;
	}

	public void setInitScript(Resource initScript) {
		this.initScript = initScript;
	}

	public void setSkipSslValidation(boolean skipSslValidation) {
		this.skipSslValidation = skipSslValidation;
	}

	public boolean isCreateKeyspace() {
		return this.createKeyspace;
	}

	public Resource getInitScript() {
		return this.initScript;
	}

	public boolean isSkipSslValidation() {
		return this.skipSslValidation;
	}

	public String[] getEntityBasePackages() {
		return this.entityBasePackages;
	}

	public void setEntityBasePackages(String[] entityBasePackages) {
		this.entityBasePackages = entityBasePackages;
	}

}
