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
package com.github.wautsns.utility.validation.core.criterion;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.wautsns.utility.validation.core.criterion.handlers.Stringifier;
import com.github.wautsns.utility.validation.core.validation.VEnv;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CriterionViolation {

	private Class<?> cause;
	private String position;
	private String message;

	@Override
	public String toString() {
		return String.format("{cause: %s, position: %s, message: %s}", cause.getSimpleName(), position, message);
	}

	@AllArgsConstructor(access = AccessLevel.PROTECTED)
	public static abstract class Template {

		private String position;
		private Class<?> cause;

		protected abstract String getMessageTemplate();

		public CriterionViolation generate(String value) {
			return new CriterionViolation(
				cause,
				position,
				getMessageTemplate()
					.replaceAll("\\{\\$p}", VEnv.tryGetI18nMessage(position))
					.replaceAll("\\{\\$v}", value));
		}

		public static Template of(String position, Class<?> cause, String message, HashMap<String, Object> data) {
			boolean isSimple = VEnv.MESSAGE_SOURCE == null || !message.matches("\\{[^$#]*}");
			return isSimple
				? new SimpleTemplate(position, cause, message, data)
				: new I18nTemplate(position, cause, message, data);
		}

		protected static String _fillData(String message, HashMap<String, Object> data) {
			// 需确保 data 已移除 message
			for (Entry<String, Object> entry : data.entrySet()) {
				String value = Stringifier.simple(entry.getValue());
				message = message.replaceAll("\\{\\#" + entry.getKey() + '}', value);
			}
			return message;
		}

		public static class SimpleTemplate extends Template {

			private String message;

			public SimpleTemplate(String position, Class<?> cause, String message, HashMap<String, Object> data) {
				super(position, cause);
				this.message = _fillData(message, data);
			}

			@Override
			protected String getMessageTemplate() {
				return message;
			}
		}

		public static class I18nTemplate extends Template {

			private String text;
			private HashMap<String, Object> data;

			public I18nTemplate(String position, Class<?> cause, String message, HashMap<String, Object> data) {
				super(position, cause);
				text = message;
				this.data = data;
			}

			private static final Pattern PATTERN = Pattern.compile("\\{(?![$#])([^{}]*)}");

			@Override
			protected String getMessageTemplate() {
				String temp, message = VEnv.tryGetI18nMessage(text);
				do {
					temp = message;
					Matcher matcher = PATTERN.matcher(temp);
					while (matcher.find()) {
						String target = matcher.group(1);
						String replacement = VEnv.tryGetI18nMessage(target);
						if (target != replacement)
							message = temp.replace("{" + target + '}', replacement);
					}
				} while (!message.equals(temp));
				return _fillData(message, data);
			}
		}
	}

}
