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
package com.github.wautsns.utility.validation.core.validation;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VEnv {

	// >>>>>>>>>>>>>>> BEGIN SpEL env

	/**
	 * SpEL 上下文, 默认为 {@code new StandardEvaluationContext()}
	 * 
	 * <p> {@literal wautsns-utility-validation} 所有涉及到 SpEL 解析取值的地方, 均使用该上下文
	 */
	public static StandardEvaluationContext SpEL_CTX = new StandardEvaluationContext();

	// =============== END SpEL env

	// >>>>>>>>>>>>>>> BEGIN MessageSource

	/**
	 * 消息资源,默认为 {@code null}
	 * 
	 * <p> criterion 注解中所有 message 的模板
	 */
	public static MessageSource MESSAGE_SOURCE;

	/**
	 * 尝试从 {@link #MESSAGE_SOURCE} 中获取指定文本所对应的 message
	 * 
	 * @param text 消息资源中的键文本
	 * @return 存在则返回对应 message, 否则返回自身
	 */
	public static String tryGetMessage(String text) {
		return (MESSAGE_SOURCE == null)
			? text
			: MESSAGE_SOURCE.getMessage(text, null, text, LocaleContextHolder.getLocale());
	}

	// =============== END MessageSource

}
