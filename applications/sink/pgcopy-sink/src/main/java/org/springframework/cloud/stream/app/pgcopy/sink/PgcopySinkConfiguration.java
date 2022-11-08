/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.stream.app.pgcopy.sink;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import javax.sql.DataSource;

import jakarta.annotation.PreDestroy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.copy.CopyIn;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binding.InputBindingLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ExpressionEvaluatingCorrelationStrategy;
import org.springframework.integration.aggregator.MessageCountReleaseStrategy;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.AggregatorFactoryBean;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageGroupStoreReaper;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Configuration class for the PostgreSQL CopyManager.
 *
 * @author Thomas Risberg
 * @author Janne Valkealahti
 * @author Chris Bono
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(PgcopySinkProperties.class)
public class PgcopySinkConfiguration {

	private static final Log logger = LogFactory.getLog(PgcopySinkConfiguration.class);

	@Autowired
	private PgcopySinkProperties properties;

	@Bean
	public Consumer<Message<?>> pgcopyConsumer(MessageHandler aggregatingMessageHandler) {
		return aggregatingMessageHandler::handleMessage;
	}

	@Bean
	@Primary
	FactoryBean<MessageHandler> aggregatorFactoryBean(MessageChannel toSink, MessageGroupStore messageGroupStore) {
		AggregatorFactoryBean aggregatorFactoryBean = new AggregatorFactoryBean();
		aggregatorFactoryBean.setCorrelationStrategy(
				new ExpressionEvaluatingCorrelationStrategy("payload.getClass().name"));
		aggregatorFactoryBean.setReleaseStrategy(new MessageCountReleaseStrategy(properties.getBatchSize()));
		aggregatorFactoryBean.setMessageStore(messageGroupStore);
		aggregatorFactoryBean.setProcessorBean(new DefaultAggregatingMessageGroupProcessor());
		aggregatorFactoryBean.setExpireGroupsUponCompletion(true);
		aggregatorFactoryBean.setSendPartialResultOnExpiry(true);
		aggregatorFactoryBean.setOutputChannel(toSink);
		return aggregatorFactoryBean;
	}

	@Bean
	public MessageChannel toSink() {
		return new DirectChannel();
	}

	@Bean
	@ServiceActivator(inputChannel = "toSink")
	public MessageHandler datasetSinkMessageHandler(final JdbcTemplate jdbcTemplate,
													final PlatformTransactionManager platformTransactionManager) {

		final TransactionTemplate txTemplate = new TransactionTemplate(platformTransactionManager);

		if (StringUtils.hasText(properties.getErrorTable())) {
			verifyErrorTable(jdbcTemplate, txTemplate);
		}

		StringBuilder columns = new StringBuilder();
		for (String col : properties.getColumns()) {
			if (columns.length() > 0) {
				columns.append(",");
			}
			columns.append(col);
		}
		// the copy command
		final StringBuilder sql = new StringBuilder("COPY " + properties.getTableName());
		if (columns.length() > 0) {
			sql.append(" (" + columns + ")");
		}
		sql.append(" FROM STDIN");

		StringBuilder options = new StringBuilder();
		if (properties.getFormat() == PgcopySinkProperties.Format.CSV) {
			options.append("CSV");
		}
		if (properties.getDelimiter() != null) {
			options.append(escapedOptionCharacterValue(options.length(), "DELIMITER", properties.getDelimiter()));
		}
		if (properties.getNullString() != null) {
			options.append((options.length() > 0 ? " " : "") + "NULL '" + properties.getNullString() + "'");
		}
		if (properties.getQuote() != null) {
			options.append(quotedOptionCharacterValue(options.length(), "QUOTE", properties.getQuote()));
		}
		if (properties.getEscape() != null) {
			options.append(quotedOptionCharacterValue(options.length(), "ESCAPE", properties.getEscape()));
		}
		if (options.length() > 0) {
			sql.append(" WITH " + options.toString());
		}

		return new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				Object payload = message.getPayload();
				if (payload instanceof Collection<?>) {
					final Collection<?> payloads = (Collection<?>) payload;
					if (logger.isDebugEnabled()) {
						logger.debug("Executing batch of size " + payloads.size() + " for " + sql);
					}
					try {
						long rows = doCopy(payloads, txTemplate);
						if (logger.isDebugEnabled()) {
							logger.debug("Wrote " + rows + " rows");
						}
					}
					catch (DataAccessException e) {
						logger.error("Error while copying batch of data: " + e.getMessage());
						logger.error("Switching to single row copy for current batch");
						long rows = 0;
						for (Object singlePayload : payloads) {
							try {
								rows = rows + doCopy(Collections.singletonList(singlePayload), txTemplate);
							}
							catch (DataAccessException e2) {
								logger.error("Copy for single row caused error: " + e2.getMessage());
								logger.error("Bad Data: \n" + singlePayload);
								if (StringUtils.hasText(properties.getErrorTable())) {
									writeError(e2, singlePayload);
								}
							}
						}
						if (logger.isDebugEnabled()) {
							logger.debug("Re-tried batch and wrote " + rows + " rows");
						}
					}
				}
				else {
					throw new IllegalStateException("Expected a collection of strings but received " +
							message.getPayload().getClass().getName());
				}
			}

			private void writeError(final DataAccessException exception, final Object payload) {
				final String message;
				if (exception.getCause() != null) {
					message = exception.getCause().getMessage();
				}
				else {
					message = exception.getMessage();
				}
				try {
					txTemplate.execute(new TransactionCallback<Long>() {
						@Override
						public Long doInTransaction(TransactionStatus transactionStatus) {
							jdbcTemplate.update(
									"insert into " + properties.getErrorTable() + " (table_name, error_message, payload) values (?, ?, ?)",
									new Object[]{properties.getTableName(), message, payload});
							return null;
						}
					});
				}
				catch (DataAccessException e) {
					logger.error("Writing to error table failed: " + e.getMessage());
				}
			}

			private long doCopy(final Collection<?> payloads, TransactionTemplate txTemplate) {
				Long rows = txTemplate.execute(transactionStatus -> jdbcTemplate.execute(
						new ConnectionCallback<Long>() {
							@Override
							public Long doInConnection(Connection connection) throws SQLException, DataAccessException {
								CopyManager cm = connection.unwrap(BaseConnection.class).getCopyAPI();
								CopyIn ci = cm.copyIn(sql.toString());
								for (Object payloadData : payloads) {
									String textPayload = (payloadData instanceof byte[]) ?
											new String((byte[]) payloadData) : (String) payloadData;
									byte[] data = (textPayload + "\n").getBytes();
									ci.writeToCopy(data, 0, data.length);
								}
								return Long.valueOf(ci.endCopy());
							}
						}
				));
				return rows;
			}
		};
	}

	@ConditionalOnProperty("pgcopy.initialize")
	@Bean
	public DataSourceInitializer nonBootDataSourceInitializer(DataSource dataSource, ResourceLoader resourceLoader) {
		DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
		dataSourceInitializer.setDataSource(dataSource);
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.setIgnoreFailedDrops(true);
		dataSourceInitializer.setDatabasePopulator(databasePopulator);
		if ("true".equals(properties.getInitialize())) {
			databasePopulator.addScript(new DefaultInitializationScriptResource(properties.getTableName(),
					properties.getColumns()));
		}
		else {
			databasePopulator.addScript(resourceLoader.getResource(properties.getInitialize()));
		}
		return dataSourceInitializer;
	}

	@Bean
	MessageGroupStore messageGroupStore() {
		SimpleMessageStore messageGroupStore = new SimpleMessageStore();
		messageGroupStore.setTimeoutOnIdle(true);
		messageGroupStore.setCopyOnGet(false);
		return messageGroupStore;
	}

	@Bean
	MessageGroupStoreReaper messageGroupStoreReaper(MessageGroupStore messageStore,
													InputBindingLifecycle inputBindingLifecycle) {
		MessageGroupStoreReaper messageGroupStoreReaper = new MessageGroupStoreReaper(messageStore);
		messageGroupStoreReaper.setPhase(inputBindingLifecycle.getPhase() - 1);
		messageGroupStoreReaper.setTimeout(properties.getIdleTimeout());
		messageGroupStoreReaper.setAutoStartup(true);
		messageGroupStoreReaper.setExpireOnDestroy(true);
		return messageGroupStoreReaper;
	}

	@Bean
	ReaperTask reaperTask() {
		return new ReaperTask();
	}

	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		JdbcTemplate jt = new JdbcTemplate(dataSource);
		return jt;
	}

	private String quotedOptionCharacterValue(int length, String option, char value) {
		return (length > 0 ? " " : "") + option + " '" + (value == '\'' ? "''" : value) + "'";
	}

	private String escapedOptionCharacterValue(int length, String option, String value) {
		return (length > 0 ? " " : "") + option + " " + (value.startsWith("\\") ? "E'" + value : "'" + value) + "'";
	}

	private void verifyErrorTable(final JdbcTemplate jdbcTemplate, final TransactionTemplate txTemplate) {
		try {
			txTemplate.execute(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus transactionStatus) {
					jdbcTemplate.update(
							"insert into " + properties.getErrorTable() + " (table_name, error_message, payload) values (?, ?, ?)",
							properties.getErrorTable(), "message", "payload");
					transactionStatus.setRollbackOnly();
					return null;
				}
			});
		}
		catch (DataAccessException e) {
			throw new IllegalStateException("Invalid error table specified", e);
		}
	}

	public static class ReaperTask {

		@Autowired
		MessageGroupStoreReaper messageGroupStoreReaper;

		@Scheduled(fixedRate = 1000)
		public void reap() {
			messageGroupStoreReaper.run();
		}

		@PreDestroy
		public void beforeDestroy() {
			reap();
		}

	}
}
