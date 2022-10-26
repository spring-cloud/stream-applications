/*
 * Copyright 2017-2018 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Thomas Risberg
 * @author Artem Bilan
 */
public class PgcopySinkPropertiesTests {

	private AnnotationConfigApplicationContext context;

	@BeforeEach
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@AfterEach
	public void tearDown() {
		this.context.close();
	}

	@Test
	public void tableNameIsRequired() {
		this.context.register(Conf.class);
		assertThatThrownBy(() -> this.context.refresh())
				.isInstanceOf(BeanCreationException.class)
				.cause()
				.hasMessageContaining("Failed to bind properties under 'pgcopy' to org.springframework.cloud.stream.app.pgcopy.sink.PgcopySinkProperties");
	}

	@Test
	public void tableNameCanBeCustomized() {
		String table = "TEST_DATA";
		TestPropertyValues.of("pgcopy.table-name:" + table)
				.applyTo(this.context);
		this.context.register(Conf.class);
		this.context.refresh();
		PgcopySinkProperties properties = this.context.getBean(PgcopySinkProperties.class);
		assertThat(properties.getTableName(), equalTo(table));
	}

	@Test
	public void formatDefaultsToText() {
		TestPropertyValues.of("pgcopy.table-name: test")
				.applyTo(this.context);
		this.context.register(Conf.class);
		this.context.refresh();
		PgcopySinkProperties properties = this.context.getBean(PgcopySinkProperties.class);
		assertThat(properties.getFormat().toString(), equalTo("TEXT"));
	}

	@Test
	public void formatCanBeCustomized() {
		String format = "CSV";
		TestPropertyValues.of("pgcopy.table-name: test", "pgcopy.format:" + format)
				.applyTo(this.context);
		this.context.register(Conf.class);
		this.context.refresh();
		PgcopySinkProperties properties = this.context.getBean(PgcopySinkProperties.class);
		assertThat(properties.getFormat().toString(), equalTo(format));
	}

	@Test
	public void nullCanBeCustomized() {
		String nullString = "@#$";
		TestPropertyValues.of("pgcopy.table-name: test",
				"pgcopy.format: CSV",
				"pgcopy.null-string: " + nullString)
				.applyTo(this.context);
		this.context.register(Conf.class);
		this.context.refresh();
		PgcopySinkProperties properties = this.context.getBean(PgcopySinkProperties.class);
		assertThat(properties.getNullString(), equalTo(nullString));
	}

	@Test
	public void delimiterCanBeCustomized() {
		String delimiter = "|";
		TestPropertyValues.of("pgcopy.table-name: test",
				"pgcopy.format: CSV",
				"pgcopy.delimiter: " + delimiter)
				.applyTo(this.context);
		this.context.register(Conf.class);
		this.context.refresh();
		PgcopySinkProperties properties = this.context.getBean(PgcopySinkProperties.class);
		assertThat(properties.getDelimiter(), equalTo(delimiter));
	}

	@Test
	public void quoteCanBeCustomized() {
		String quote = "'";
		TestPropertyValues.of("pgcopy.table-name: test",
				"pgcopy.format: CSV",
				"pgcopy.quote: " + quote)
				.applyTo(this.context);
		this.context.register(Conf.class);
		this.context.refresh();
		PgcopySinkProperties properties = this.context.getBean(PgcopySinkProperties.class);
		assertThat(String.valueOf(properties.getQuote()), equalTo(quote));
	}

	@Test
	public void escapeCanBeCustomized() {
		String escape = "~";
		TestPropertyValues.of("pgcopy.table-name: test",
				"pgcopy.format: CSV",
				"pgcopy.escape: " + escape)
				.applyTo(this.context);
		this.context.register(Conf.class);
		this.context.refresh();
		PgcopySinkProperties properties = this.context.getBean(PgcopySinkProperties.class);
		assertThat(String.valueOf(properties.getEscape()), equalTo(escape));
	}

	@Configuration
	@EnableConfigurationProperties(PgcopySinkProperties.class)
	static class Conf {

	}

}
