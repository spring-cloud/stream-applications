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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

/**
 * Configuration for a {@link TaskLaunchRequestFunction}, provided as a common function that can be composed with other Suppliers or
 * Functions to transform any {@link Message} to a {@link TaskLaunchRequest} which may be used as input to the {@code TaskLauncherFunction} to launch a task.
 *
 * Command line arguments used by the task, as well as the task name itself may be statically configured or extracted from
 * the message contents, using SpEL. See {@link TaskLaunchRequestFunctionProperties} for details.
 *
 * It is also possible to provide your own implementations of {@link CommandLineArgumentsMessageMapper} and {@link TaskNameMessageMapper}.
 *
 * @author David Turanski
 **/
@Configuration
@EnableConfigurationProperties(TaskLaunchRequestFunctionProperties.class)
public class TaskLaunchRequestFunctionConfiguration {

	/**
	 * The function name.
	 */
	public final static String TASK_LAUNCH_REQUEST_FUNCTION_NAME = "taskLaunchRequestFunction";

	/**
	 * A {@link java.util.function.Function} to transform a {@link Message} payload to a
	 * {@link TaskLaunchRequest}.
	 *
	 * @param taskLaunchRequestMessageProcessor a {@link TaskLaunchRequestMessageProcessor}.
	 *
	 * @return a {@code TaskLaunchRequest} Message.
	 */
	@Bean(name = TASK_LAUNCH_REQUEST_FUNCTION_NAME)
	public TaskLaunchRequestFunction taskLaunchRequest(
			TaskLaunchRequestMessageProcessor taskLaunchRequestMessageProcessor) {
		return message -> taskLaunchRequestMessageProcessor.postProcessMessage(message);
	}

	@Bean
	public TaskLaunchRequestSupplier taskLaunchRequestInitializer(
			TaskLaunchRequestFunctionProperties taskLaunchRequestProperties) {
		return new TaskLaunchRequestPropertiesInitializer(taskLaunchRequestProperties);
	}

	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	@Bean
	public TaskLaunchRequestMessageProcessor taskLaunchRequestMessageProcessor(
			TaskLaunchRequestSupplier taskLaunchRequestInitializer,
			TaskLaunchRequestFunctionProperties properties,
			EvaluationContext evaluationContext,
			@Nullable TaskNameMessageMapper taskNameMessageMapper,
			@Nullable CommandLineArgumentsMessageMapper commandLineArgumentsMessageMapper) {

		if (taskNameMessageMapper == null) {
			taskNameMessageMapper = taskNameMessageMapper(properties, evaluationContext);
		}

		if (commandLineArgumentsMessageMapper == null) {
			commandLineArgumentsMessageMapper = commandLineArgumentsMessageMapper(properties, evaluationContext);
		}

		return new TaskLaunchRequestMessageProcessor(taskLaunchRequestInitializer,
				taskNameMessageMapper,
				commandLineArgumentsMessageMapper);
	}

	@Bean
	public EvaluationContext evaluationContext(BeanFactory beanFactory) {
		return ExpressionUtils.createStandardEvaluationContext(beanFactory);
	}

	private TaskNameMessageMapper taskNameMessageMapper(TaskLaunchRequestFunctionProperties taskLaunchRequestProperties,
														EvaluationContext evaluationContext) {
		if (StringUtils.hasText(taskLaunchRequestProperties.getTaskNameExpression())) {
			SpelExpressionParser expressionParser = new SpelExpressionParser();
			Expression taskNameExpression = expressionParser
					.parseExpression(taskLaunchRequestProperties.getTaskNameExpression());
			return new ExpressionEvaluatingTaskNameMessageMapper(taskNameExpression, evaluationContext);
		}

		return message -> taskLaunchRequestProperties.getTaskName();
	}

	private CommandLineArgumentsMessageMapper commandLineArgumentsMessageMapper(
			TaskLaunchRequestFunctionProperties taskLaunchRequestFunctionProperties,
			EvaluationContext evaluationContext) {
		return new ExpressionEvaluatingCommandLineArgsMapper(taskLaunchRequestFunctionProperties.getArgExpressions(),
				evaluationContext);
	}

	private static class TaskLaunchRequestPropertiesInitializer extends TaskLaunchRequestSupplier {
		TaskLaunchRequestPropertiesInitializer(
				TaskLaunchRequestFunctionProperties taskLaunchRequestProperties) {

			this.commandLineArgumentSupplier(
					() -> new ArrayList<>(taskLaunchRequestProperties.getArgs()));

			this.deploymentPropertiesSupplier(
					() -> KeyValueListParser.parseCommaDelimitedKeyValuePairs(
							taskLaunchRequestProperties.getDeploymentProperties()));

			this.taskNameSupplier(() -> taskLaunchRequestProperties.getTaskName());
		}
	}

	private static class ExpressionEvaluatingTaskNameMessageMapper implements TaskNameMessageMapper {

		private final Expression expression;
		private final EvaluationContext evaluationContext;

		ExpressionEvaluatingTaskNameMessageMapper(Expression expression, EvaluationContext evaluationContext) {
			this.evaluationContext = evaluationContext;
			this.expression = expression;
		}

		@Override
		public String processMessage(Message<?> message) {
			return expression.getValue(evaluationContext, message).toString();
		}
	}

	private static class ExpressionEvaluatingCommandLineArgsMapper implements CommandLineArgumentsMessageMapper {
		private final Map<String, Expression> argExpressionsMap;

		private final EvaluationContext evaluationContext;

		ExpressionEvaluatingCommandLineArgsMapper(String argExpressions, EvaluationContext evaluationContext) {
			this.evaluationContext = evaluationContext;
			this.argExpressionsMap = new HashMap<>();
			if (StringUtils.hasText(argExpressions)) {
				SpelExpressionParser expressionParser = new SpelExpressionParser();

				KeyValueListParser.parseCommaDelimitedKeyValuePairs(argExpressions).forEach(
						(k, v) -> argExpressionsMap.put(k, expressionParser.parseExpression(v)));
			}
		}

		@Override
		public Collection<String> processMessage(Message<?> message) {
			return evaluateArgExpressions(message);
		}

		private Collection<String> evaluateArgExpressions(Message<?> message) {
			List<String> results = new LinkedList<>();
			this.argExpressionsMap.forEach((k, expression) -> results
					.add(String.format("%s=%s", k, expression.getValue(this.evaluationContext, message))));
			return results;
		}
	}

}
