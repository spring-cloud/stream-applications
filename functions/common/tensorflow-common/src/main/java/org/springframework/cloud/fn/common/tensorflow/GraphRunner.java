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

package org.springframework.cloud.fn.common.tensorflow;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.op.Ops;


/**
 * @author Christian Tzolov
 */
public class GraphRunner extends AbstractGraphRunner implements AutoCloseable {

	private SavedModelBundle savedModelBundle;
	private AutoCloseableSession autoCloseableSession;

	public GraphRunner(List<String> feedNames, String fetchedName) {
		super(feedNames, Arrays.asList(fetchedName));
	}

	public GraphRunner(String feedName, List<String> fetchedNames) {
		super(Arrays.asList(feedName), fetchedNames);
	}

	public GraphRunner(String feedName, String fetchedName) {
		super(feedName, fetchedName);
	}

	public GraphRunner(List<String> feedNames, List<String> fetchedNames) {
		super(feedNames, fetchedNames);
	}


	@Override
	public Session doGetSession() {

		if (this.autoCloseableSession != null && this.savedModelBundle != null) {
			throw new IllegalStateException("Either SavedModel or GraphDefinition can be set! But both are set!");
		}

		if (this.autoCloseableSession != null) {
			return this.autoCloseableSession.getSession();
		}

		if (this.savedModelBundle != null) {
			return this.savedModelBundle.session();
		}

		throw new IllegalStateException("Either SavedModel or GraphDefinition can be set! None found");
	}

	public GraphRunner withGraphDefinition(GraphDefinition graphDefinition) {
		Validate.isTrue(this.savedModelBundle == null, "Either SavedModel or GraphDefinition can be set! " +
				"SavedModelBundle is found: " + this.savedModelBundle);

		this.autoCloseableSession = new AutoCloseableSession() {
			@Override
			protected void doGraphDefinition(Ops tf) {
				graphDefinition.defineGraph(tf);
			}
		};

		return this;
	}

	public GraphRunner withSavedModel(String savedModelDir, String... tags) {
		Validate.isTrue(this.autoCloseableSession == null, "Either SavedModel or GraphDefinition can be set! " +
				"AutoCloseableSession is found: " + this.autoCloseableSession);
		this.savedModelBundle = SavedModelBundle.load(savedModelDir, tags);
		return this;
	}

	@Override
	public String toString() {
		return String.format("(%s) -> (%s)", String.join(",", this.getFeedNames()),
				String.join(",", this.getFetchNames()));
	}

	@Override
	public void close() {
		if (this.savedModelBundle != null) {
			this.savedModelBundle.close();
		}
		if (this.autoCloseableSession != null) {
			this.autoCloseableSession.close();
		}
	}
}
