/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.cloud.fn.supplier.debezium.it.supplier;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
		"spring.cloud.function.definition=debeziumSupplier",

		// https://debezium.io/documentation/reference/2.3/transformations/event-flattening.html
		"debezium.properties.transforms=unwrap",
		"debezium.properties.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState",
		"debezium.properties.transforms.unwrap.drop.tombstones=true",
		"debezium.properties.transforms.unwrap.delete.handling.mode=rewrite",
		"debezium.properties.transforms.unwrap.add.fields=name,db,op,table",

		"debezium.properties.schema.history.internal=io.debezium.relational.history.MemorySchemaHistory",
		"debezium.properties.offset.storage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore",

		// Drop schema from the message payload.
		"debezium.properties.key.converter.schemas.enable=false",
		"debezium.properties.value.converter.schemas.enable=false",

		"debezium.properties.topic.prefix=my-topic",
		"debezium.properties.name=my-connector",
		"debezium.properties.database.server.id=85744",
		"debezium.properties.connector.class=io.debezium.connector.mysql.MySqlConnector",
		"debezium.properties.database.user=debezium",
		"debezium.properties.database.password=dbz",
		"debezium.properties.database.hostname=localhost",

		"debezium.properties.table.include.list=inventory.customers, inventory.addresses",

		// JdbcTemplate config.
		"app.datasource.username=root",
		"app.datasource.password=debezium",
		"app.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
		"app.datasource.type=com.zaxxer.hikari.HikariDataSource"
})
@Testcontainers
public class DebeziumSupplierIntegrationTest {

	public static final String IMAGE_TAG = "2.3.0.Final";
	public static final String DEBEZIUM_EXAMPLE_MYSQL_IMAGE = "debezium/example-mysql:" + IMAGE_TAG;

	@Container
	static GenericContainer<?> debeziumMySQL = new GenericContainer<>(DEBEZIUM_EXAMPLE_MYSQL_IMAGE)
			.withEnv("MYSQL_ROOT_PASSWORD", "debezium")
			.withEnv("MYSQL_USER", "mysqluser")
			.withEnv("MYSQL_PASSWORD", "mysqlpw")
			.withExposedPorts(3306);

	@DynamicPropertySource
	static void mysqlDbProperties(DynamicPropertyRegistry registry) {
		registry.add("debezium.properties.database.port", () -> debeziumMySQL.getMappedPort(3306));
		registry.add("app.datasource.url",
				() -> String.format("jdbc:mysql://localhost:%d/%s?enabledTLSProtocols=TLSv1.2",
						debeziumMySQL.getMappedPort(3306), "inventory")); // JdbcTemplate config.
	}

	@Autowired
	private Supplier<Flux<Message<?>>> debeziumSupplier;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void testDebeziumSupplier() {

		jdbcTemplate.update(
				"INSERT INTO `customers`(`first_name`,`last_name`,`email`) VALUES('Test666', 'Test666', 'Test666@spring.org')");

		Flux<Message<?>> messageFlux = this.debeziumSupplier.get();

		// Message size should correspond to the number of insert statements in:
		// https://github.com/debezium/container-images/blob/main/examples/mysql/2.3/inventory.sql
		// filtered by Customers and Addresses table.
		StepVerifier.create(messageFlux)
				.expectNextCount(16) // Skip the DDL transaction logs.

				// Customers table
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":1001,\"first_name\":\"Sally\",\"last_name\":\"Thomas\",\"email\":\"sally.thomas@acme.com\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"customers\",\"__deleted\":\"false\"}"))
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":1002,\"first_name\":\"George\",\"last_name\":\"Bailey\",\"email\":\"gbailey@foobar.com\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"customers\",\"__deleted\":\"false\"}"))
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":1003,\"first_name\":\"Edward\",\"last_name\":\"Walker\",\"email\":\"ed@walker.com\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"customers\",\"__deleted\":\"false\"}"))
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":1004,\"first_name\":\"Anne\",\"last_name\":\"Kretchmar\",\"email\":\"annek@noanswer.org\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"customers\",\"__deleted\":\"false\"}"))

				// NEW Customer Insert
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":1005,\"first_name\":\"Test666\",\"last_name\":\"Test666\",\"email\":\"Test666@spring.org\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"customers\",\"__deleted\":\"false\"}"))

				// Addresses table
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":10,\"customer_id\":1001,\"street\":\"3183 Moore Avenue\",\"city\":\"Euless\",\"state\":\"Texas\",\"zip\":\"76036\",\"type\":\"SHIPPING\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"addresses\",\"__deleted\":\"false\"}"))
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":11,\"customer_id\":1001,\"street\":\"2389 Hidden Valley Road\",\"city\":\"Harrisburg\",\"state\":\"Pennsylvania\",\"zip\":\"17116\",\"type\":\"BILLING\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"addresses\",\"__deleted\":\"false\"}"))
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":12,\"customer_id\":1002,\"street\":\"281 Riverside Drive\",\"city\":\"Augusta\",\"state\":\"Georgia\",\"zip\":\"30901\",\"type\":\"BILLING\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"addresses\",\"__deleted\":\"false\"}"))
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":13,\"customer_id\":1003,\"street\":\"3787 Brownton Road\",\"city\":\"Columbus\",\"state\":\"Mississippi\",\"zip\":\"39701\",\"type\":\"SHIPPING\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"addresses\",\"__deleted\":\"false\"}"))
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":14,\"customer_id\":1003,\"street\":\"2458 Lost Creek Road\",\"city\":\"Bethlehem\",\"state\":\"Pennsylvania\",\"zip\":\"18018\",\"type\":\"SHIPPING\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"addresses\",\"__deleted\":\"false\"}"))
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":15,\"customer_id\":1003,\"street\":\"4800 Simpson Square\",\"city\":\"Hillsdale\",\"state\":\"Oklahoma\",\"zip\":\"73743\",\"type\":\"BILLING\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"addresses\",\"__deleted\":\"false\"}"))
				.assertNext((message) -> assertThat(payloadString(message))
						.isEqualTo(
								"{\"id\":16,\"customer_id\":1004,\"street\":\"1289 University Hill Road\",\"city\":\"Canehill\",\"state\":\"Arkansas\",\"zip\":\"72717\",\"type\":\"LIVING\",\"__name\":\"my-topic\",\"__db\":\"inventory\",\"__op\":\"r\",\"__table\":\"addresses\",\"__deleted\":\"false\"}"))
				.thenCancel()
				.verify();

	}

	private String payloadString(Message<?> message) {
		return new String((byte[]) message.getPayload());
	}

	@SpringBootApplication(exclude = { MongoAutoConfiguration.class })
	@Import({TestJdbcTemplateConfiguration.class})
	static class DebeziumSupplierTestApplication {
	}

}
