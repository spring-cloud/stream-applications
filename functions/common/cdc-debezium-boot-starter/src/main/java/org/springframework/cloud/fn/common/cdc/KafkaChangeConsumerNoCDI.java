/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.springframework.cloud.fn.common.cdc;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import io.debezium.DebeziumException;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.DebeziumEngine.ChangeConsumer;
import io.debezium.engine.DebeziumEngine.RecordCommitter;
import io.debezium.server.CustomConsumerBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.connect.source.SourceRecord;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of the {@link DebeziumEngine.ChangeConsumer} interface that publishes change event messages to Kafka.
 */
@Named("kafka")
@Dependent
//DebeziumEngine.ChangeConsumer<SourceRecord>
public class KafkaChangeConsumerNoCDI extends BaseChangeConsumerNoCDI implements DebeziumEngine.ChangeConsumer<SourceRecord> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaChangeConsumerNoCDI.class);

    private static final String PROP_PREFIX = "debezium.sink.kafka.";
    private static final String PROP_PREFIX_PRODUCER = PROP_PREFIX + "producer.";

	@Autowired
	Function<SourceRecord, byte[]> valueSerializer;

	@Autowired
	Function<SourceRecord, byte[]> keySerializer;

	private KafkaProducer<Object, Object> producer;

	private int count;

//    @Inject
//    @CustomConsumerBuilder
//    Instance<KafkaProducer<Object, Object>> customKafkaProducer;

    @PostConstruct
    void start() {
//        if (customKafkaProducer.isResolvable()) {
//            producer = customKafkaProducer.get();
//            LOGGER.info("Obtained custom configured KafkaProducer '{}'", producer);
//            return;
//        }

        final Config config = ConfigProvider.getConfig();
        producer = new KafkaProducer<>(getConfigSubset(config, PROP_PREFIX_PRODUCER));
        LOGGER.info("consumer started...");
    }

    @PreDestroy
    void stop() {
        LOGGER.info("consumer destroyed...");
        if (producer != null) {
            try {
                producer.close(Duration.ofSeconds(5));
            }
            catch (Throwable t) {
                LOGGER.warn("Could not close producer {}", t);
            }
        }
    }

    @Override
	public void handleBatch(List<SourceRecord> records, RecordCommitter<SourceRecord> committer) throws InterruptedException {
    //public void handleBatch(final List<ChangeEvent<Object, Object>> records, final RecordCommitter<ChangeEvent<Object, Object>> committer) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(records.size());
        for (SourceRecord record : records) {
            try {
                LOGGER.info("Received event '{}'", record);
				Object cdcJsonPayload = valueSerializer.apply(record);
				byte[] key = keySerializer.apply(record);
                producer.send(new ProducerRecord<>(record.topic(), key, cdcJsonPayload), (metadata, exception) -> {
                    if (exception != null) {
                        LOGGER.error("Failed to send record to {}:", record.topic(), exception);
                        throw new DebeziumException(exception);
                    }
                    else {
                        LOGGER.trace("Sent message with offset: {}", metadata.offset());
                        latch.countDown();
						count++;
                    }
                });
                committer.markProcessed(record);
            }
            catch (Exception e) {
                throw new DebeziumException(e);
            }
        }

        latch.await();
        committer.markBatchFinished();
    }

	public int getCount() {
		return count;
	}
}
