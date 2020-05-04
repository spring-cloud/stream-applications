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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.SimpleDateFormat;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * The annotated String must be a valid {@link java.text.SimpleDateFormat} pattern.
 *
 * @author Eric Bottard
 * @author Soby Chacko
 */
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR,
		ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = { DateFormat.DateFormatValidator.class })
public @interface DateFormat {

	String DEFAULT_MESSAGE = "";

	String message() default DEFAULT_MESSAGE;

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };

	public static class DateFormatValidator implements ConstraintValidator<DateFormat, CharSequence> {

		private String message;

		@Override
		public void initialize(DateFormat constraintAnnotation) {
			this.message = constraintAnnotation.message();
		}

		@Override
		public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
			if (value == null) {
				return true;
			}
			try {
				new SimpleDateFormat(value.toString());
			}
			catch (IllegalArgumentException e) {
				if (DEFAULT_MESSAGE.equals(this.message)) {
					context.disableDefaultConstraintViolation();
					context.buildConstraintViolationWithTemplate(e.getMessage()).addConstraintViolation();
				}
				return false;
			}
			return true;
		}

	}

}
