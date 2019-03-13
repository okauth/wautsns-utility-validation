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

import java.lang.reflect.Array;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
public interface Stringifier<T> {

	String stringify(T value);

	static String simple(Object value) {
		if (value == null) return "null";
		if (value.getClass().isArray()) {
			int len = Array.getLength(value);
			if (len == 0)
				return "[]";
			StringBuilder bder = new StringBuilder();
			bder.append('[');
			for (int i = 0; i < len; i++)
				bder.append(Stringifier.simple(Array.get(value, i))).append(", ");
			bder.delete(bder.length() - 2, bder.length()).append(']');
			return bder.toString();
		} else if (value instanceof CharSequence)
			return '"' + value.toString() + '"';
		else if (value instanceof Class)
			return ((Class<?>) value).getSimpleName() + ".class";
		else if (value instanceof Character)
			return '\'' + value.toString() + '\'';
		return value.toString();
	}

}
