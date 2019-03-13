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

import java.util.Arrays;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;

import com.github.wautsns.utility.validation.exception.initialization.UnsupportedConversionException;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
public interface ValueHandlers4LongInteger extends ValueHandlers<Long> {

	Converter<Object, Long> OF_LONG = v -> (Long) v;
	Converter<Object, Long> OF_NUMBER = v -> ((Number) v).longValue();

	@Override
	default Converter<Object, Long> getConverter(ResolvableType resolvableType) throws UnsupportedConversionException {
		Class<?> type = resolvableType.resolve();
		if (Arrays.asList(Long.class, long.class, int.class, short.class, byte.class).contains(type))
			return OF_LONG;
		if (Number.class.isAssignableFrom(type))
			return OF_NUMBER;
		throw new UnsupportedConversionException(resolvableType, "长整数");
	}

}
