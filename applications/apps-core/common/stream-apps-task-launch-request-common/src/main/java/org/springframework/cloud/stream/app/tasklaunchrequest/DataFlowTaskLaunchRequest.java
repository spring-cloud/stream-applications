/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.stream.app.tasklaunchrequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataFlowTaskLaunchRequest {
    @JsonProperty("args")
    private List<String> commandlineArguments = new ArrayList<>();

    @JsonProperty("deploymentProps")
    private Map<String, String> deploymentProperties = new HashMap<>();

    @JsonProperty("name")
    private String taskName;

    public void setCommandlineArguments(List<String> commandlineArguments) {
        this.commandlineArguments = new ArrayList<>(commandlineArguments);
    }

    public List<String> getCommandlineArguments() {
        return this.commandlineArguments;
    }

    public void setDeploymentProperties(Map<String, String> deploymentProperties) {
        this.deploymentProperties = deploymentProperties;
    }

    public Map<String, String> getDeploymentProperties() {
        return this.deploymentProperties;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskName() {
        return this.taskName;
    }

    public DataFlowTaskLaunchRequest addCommmandLineArguments(Collection<String> args) {
        this.commandlineArguments.addAll(args);
        return this;
    }
}
