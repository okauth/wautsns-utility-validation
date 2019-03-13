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
package com.github.wautsns.utility.validation.exception;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
public class ValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ValidationException(String message, Object... args) {
		this(null, message, args);
	}

	public ValidationException(Throwable cause, String message, Object... args) {
		super("\n\t\t" + format(message, args), cause);
	}

	private static String format(String template, Object[] args) {
		if (args.length == 0)
			return template;
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof Class)
				args[i] = shortenTypeName((Class<?>) args[i]);
			else if (args[i] instanceof Method)
				args[i] = shortenMethodName((Method) args[i]);
			else if (args[i] instanceof Field)
				args[i] = shorternFieldName((Field) args[i]);
		}
		return String.format(template, args);
	}

	private static String shortenTypeName(Class<?> type) {
		return type.getName().replaceAll("((?:^|.).)[^.]*(?=[.])", "$1");
	}

	private static String shortenMethodName(Method method) {
		return String.format("%s.%s(%s)",
			shortenTypeName(method.getDeclaringClass()),
			method.getName(),
			method.getParameterCount() == 0 ? "" : "...");
	}

	private static String shorternFieldName(Field field) {
		return String.format("%s.%s",
			shortenTypeName(field.getDeclaringClass()),
			field.getName());
	}

}
