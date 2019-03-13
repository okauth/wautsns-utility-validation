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
import java.util.function.Predicate;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.github.wautsns.utility.validation.annotation.criterion.common.VBySpEL.VBySpELValueHandlers;
import com.github.wautsns.utility.validation.annotation.helper.ACriterion;
import com.github.wautsns.utility.validation.core.criterion.Criterion.Attributes;
import com.github.wautsns.utility.validation.core.criterion.handlers.Stringifier;
import com.github.wautsns.utility.validation.core.criterion.handlers.ValueHandlers;
import com.github.wautsns.utility.validation.core.validation.VEnv;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER })
@Repeatable(VBySpELList.class)
@ACriterion(valueHandlers = VBySpELValueHandlers.class)
public @interface VBySpEL {

	String message();

	Class<?>[] groups() default {};

	String depth() default "";

	String expr();

	String stringifier() default "";

	class VBySpELValueHandlers implements ValueHandlers<Object> {

		@Override
		public Converter<Object, Object> getConverter(ResolvableType resolvableType) {
			return null;
		}

		@Override
		public Predicate<Object> getPredicate(Attributes attrs) {
			Expression expr = new SpelExpressionParser().parseExpression(attrs.get("expr"));
			return v -> expr.getValue(VEnv.SpEL_CTX, v, boolean.class);
		}

		@Override
		public Stringifier<Object> getStringifier(Attributes attrs) {
			String stringifier = attrs.get("stringifier");
			if (stringifier.isEmpty())
				return null;
			Expression expr = new SpelExpressionParser().parseExpression(stringifier);
			return v -> expr.getValue(VEnv.SpEL_CTX, v, String.class);
		}
	}

}

@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER })
@interface VBySpELList {

	VBySpEL[] value();
}
