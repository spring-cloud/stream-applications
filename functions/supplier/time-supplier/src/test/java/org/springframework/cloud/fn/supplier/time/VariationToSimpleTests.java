/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 */
@SpringBootTest({ "time.dateFormat=MMddyyyy HH:mm:ss" })
public class VariationToSimpleTests extends TimeSupplierApplicationTests {

	@Test
	public void testTimeSupplier() {
		final String time = timeSupplier.get();
		SimpleDateFormat dateFormat = new SimpleDateFormat(timeProperties.getDateFormat());
		assertThatCode(() -> {
			Date date = dateFormat.parse(time);
			assertThat(date).isNotNull();
		}).doesNotThrowAnyException();
	}

	@Test
	public void testInvalidDateFormat() {
		TimeProperties timeProperties = new TimeProperties();
		timeProperties.setDateFormat("AA/dd/yyyy HH:mm:ss");
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleDateFormat(timeProperties.getDateFormat()));
	}

}
