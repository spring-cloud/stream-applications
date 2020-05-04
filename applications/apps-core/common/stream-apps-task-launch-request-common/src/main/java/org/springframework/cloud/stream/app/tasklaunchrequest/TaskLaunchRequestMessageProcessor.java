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

import org.springframework.cloud.stream.app.tasklaunchrequest.support.CommandLineArgumentsMessageMapper;
import org.springframework.cloud.stream.app.tasklaunchrequest.support.TaskLaunchRequestSupplier;
import org.springframework.cloud.stream.app.tasklaunchrequest.support.TaskNameMessageMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

public class TaskLaunchRequestMessageProcessor implements MessagePostProcessor {

    private final TaskNameMessageMapper taskNameMessageMapper;
    private final CommandLineArgumentsMessageMapper commandLineArgumentsMessageMapper;
    private final TaskLaunchRequestSupplier taskLaunchRequestInitializer;

    public TaskLaunchRequestMessageProcessor(TaskLaunchRequestSupplier taskLaunchRequestInitializer,
                                             TaskNameMessageMapper taskNameMessageMapper,
                                             CommandLineArgumentsMessageMapper commandLIneArgumentsMessageMapper) {

        this.taskLaunchRequestInitializer = taskLaunchRequestInitializer;

        this.taskNameMessageMapper = taskNameMessageMapper;

        this.commandLineArgumentsMessageMapper = commandLIneArgumentsMessageMapper;

    }

    @Override
    public Message<DataFlowTaskLaunchRequest> postProcessMessage(Message<?> message) {
        DataFlowTaskLaunchRequest taskLaunchRequest = taskLaunchRequestInitializer.get();

        if (!StringUtils.hasText(taskLaunchRequest.getTaskName())) {
            taskLaunchRequest.setTaskName(taskNameMessageMapper.processMessage(message));
            Assert.hasText(taskLaunchRequest.getTaskName(), ()->
                    "'taskName' is required in " + DataFlowTaskLaunchRequest.class.getName());
        }

        taskLaunchRequest.addCommmandLineArguments(commandLineArgumentsMessageMapper.processMessage(message));

        MessageBuilder<DataFlowTaskLaunchRequest> builder
                = MessageBuilder.withPayload(taskLaunchRequest).copyHeaders(message.getHeaders());
        return adjustHeaders(builder).build();
    }


    private MessageBuilder<DataFlowTaskLaunchRequest> adjustHeaders(MessageBuilder<DataFlowTaskLaunchRequest> builder) {
        builder.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);
        return builder;
    }
}
