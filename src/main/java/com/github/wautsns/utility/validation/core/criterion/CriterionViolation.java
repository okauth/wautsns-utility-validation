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

	private String position;
	private String message;

	public static abstract class Template {

		protected abstract String getTemplateMessage();

		public CriterionViolation generate(String position, String value) {
			return new CriterionViolation(
				position,
				getTemplateMessage()
					.replaceAll("\\{$p}", VEnv.tryGetMessage(position))
					.replaceAll("\\{$v}", value));
		}

		public static Template of(HashMap<String, Object> data) {
			String message = (String) data.get("message");
			boolean isSimple = VEnv.MESSAGE_SOURCE == null || !message.matches("\\{.*}");
			return isSimple ? new SimpleTemplate(data) : new I18nTemplate(data);
		}

		protected static String _fillData(String message, HashMap<String, Object> data) {
			for (Entry<String, Object> entry : data.entrySet()) {
				if ("message".equals(entry.getKey()))
					continue;
				String value = Stringifier.simple(entry.getValue());
				message = message.replaceAll("\\{\\#" + entry.getKey() + '}', value);
			}
			return message;
		}

		private static class SimpleTemplate extends Template {

			private String message;

			public SimpleTemplate(HashMap<String, Object> data) {
				message = _fillData((String) data.get("message"), data);
			}

			@Override
			protected String getTemplateMessage() {
				return message;
			}
		}

		@AllArgsConstructor
		private static class I18nTemplate extends Template {

			private static final Pattern PATTERN = Pattern.compile("\\{([^}$#])}");

			private HashMap<String, Object> data;

			@Override
			protected String getTemplateMessage() {
				String temp, template = (String) data.get("message");
				do {
					temp = template;
					Matcher matcher = PATTERN.matcher(temp);
					while (matcher.find()) {
						String target = matcher.group(1);
						String replacement = VEnv.tryGetMessage(target);
						if (target != replacement)
							template = temp.replace("{" + target + '}', replacement);
					}
				} while (!template.equals(temp));
				return _fillData(template, data);
			}
		}
	}

}
