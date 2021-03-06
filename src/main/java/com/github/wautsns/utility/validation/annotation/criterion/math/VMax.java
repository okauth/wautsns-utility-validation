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
package com.github.wautsns.utility.validation.annotation.criterion.math;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Predicate;

import com.github.wautsns.utility.validation.annotation.criterion.math.VMax.VMaxValueHandlers;
import com.github.wautsns.utility.validation.annotation.helper.ACriterion;
import com.github.wautsns.utility.validation.core.criterion.Criterion.Attributes;
import com.github.wautsns.utility.validation.core.criterion.handlers.ValueHandlers4LongInteger;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER })
@Repeatable(VMaxList.class)
@ACriterion(valueHandlers = VMaxValueHandlers.class)
public @interface VMax {

	String message() default "{v.max}";

	Class<?>[] groups() default {};

	String depth() default "";

	int order() default 0;

	long value();

	class VMaxValueHandlers implements ValueHandlers4LongInteger {

		@Override
		public Predicate<Long> getPredicate(Attributes attrs) {
			long max = attrs.get("value");
			return v -> v <= max;
		}
	}

}

@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER })
@interface VMaxList {

	VMax[] value();
}
