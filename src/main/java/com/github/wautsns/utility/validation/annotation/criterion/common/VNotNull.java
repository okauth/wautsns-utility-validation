/**
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.wautsns.utility.validation.annotation.criterion.common;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;

import com.github.wautsns.utility.validation.annotation.criterion.common.VNotNull.VNotNullValueHandleres;
import com.github.wautsns.utility.validation.annotation.helper.ACriterion;
import com.github.wautsns.utility.validation.annotation.helper.ASpecify;
import com.github.wautsns.utility.validation.core.criterion.Criterion.Attributes;
import com.github.wautsns.utility.validation.core.criterion.handlers.ValueHandlers;
import com.github.wautsns.utility.validation.exception.initialization.UnsupportedConversionException;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER })
@Repeatable(VNotNullList.class)
@ACriterion(valueHandlers = VNotNullValueHandleres.class)
@ASpecify(type = VNotNull.class, attrs = "order=0")
public @interface VNotNull {

	String message() default "{v.not_null}";

	Class<?>[] groups() default {};

	String depth() default "";

	class VNotNullValueHandleres implements ValueHandlers<Object> {

		private static final Predicate<Object> PREDICATE = Objects::nonNull;

		@Override
		public Converter<Object, Object> getConverter(ResolvableType resolvableType)
				throws UnsupportedConversionException {
			if (!resolvableType.resolve().isPrimitive())
				return null;
			throw new UnsupportedConversionException(resolvableType, "可空对象");
		}

		@Override
		public Predicate<Object> getPredicate(Attributes attrs) {
			return PREDICATE;
		}
	}

}

@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER })
@interface VNotNullList {

	VNotNull[] value();
}
