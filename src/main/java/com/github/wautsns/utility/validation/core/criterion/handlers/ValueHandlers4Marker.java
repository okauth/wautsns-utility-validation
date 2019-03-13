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
package com.github.wautsns.utility.validation.core.criterion.handlers;

import java.util.function.Predicate;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;

import com.github.wautsns.utility.validation.core.criterion.Criterion.Attributes;
import com.github.wautsns.utility.validation.exception.initialization.UnsupportedConversionException;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
public final class ValueHandlers4Marker implements ValueHandlers<Object> {

	@Override
	public Converter<Object, Object> getConverter(ResolvableType resolvableType) throws UnsupportedConversionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Predicate<Object> getPredicate(Attributes attrs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Stringifier<Object> getStringifier(Attributes attrs) {
		throw new UnsupportedOperationException();
	}

}
