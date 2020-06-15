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

package org.springframework.cloud.fn.task.launch.request;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.util.Assert;

class TaskLaunchRequestSupplier implements Supplier<TaskLaunchRequest> {

	private Supplier<String> taskNameSupplier;

	private Supplier<List<String>> commandLineArgumentsSupplier;

	private Supplier<Map<String, String>> deploymentPropertiesSupplier;

	public TaskLaunchRequestSupplier taskNameSupplier(Supplier<String> taskNameSupplier) {
		this.taskNameSupplier = taskNameSupplier;
		return this;
	}

	public TaskLaunchRequestSupplier commandLineArgumentSupplier(Supplier<List<String>> commandLineArgumentsSupplier) {
		this.commandLineArgumentsSupplier = commandLineArgumentsSupplier;
		return this;
	}

	public TaskLaunchRequestSupplier deploymentPropertiesSupplier(
			Supplier<Map<String, String>> deploymentPropertiesSupplier) {
		this.deploymentPropertiesSupplier = deploymentPropertiesSupplier;
		return this;
	}

	@Override
	public TaskLaunchRequest get() {

		Assert.notNull(this.taskNameSupplier, "'taskNameSupplier' is required.");

		TaskLaunchRequest taskLaunchRequest = new TaskLaunchRequest();
		taskLaunchRequest.setTaskName(this.taskNameSupplier.get());

		if (this.commandLineArgumentsSupplier != null) {
			taskLaunchRequest.setCommandlineArguments(this.commandLineArgumentsSupplier.get());
		}

		if (this.deploymentPropertiesSupplier != null) {
			taskLaunchRequest.setDeploymentProperties(this.deploymentPropertiesSupplier.get());
		}

		return taskLaunchRequest;
	}
}
