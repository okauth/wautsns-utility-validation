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

import com.github.wautsns.utility.validation.annotation.criterion.common.VNotNull.VNotNullValueHandleres;
import com.github.wautsns.utility.validation.core.criterion.Criterion;
import com.github.wautsns.utility.validation.exception.initialization.UnsupportedConversionException;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
public interface ValueHandlers<T> {

	/**
	 * 获取值转换器
	 * 
	 * <p> 若返回 {@code null}, 则默认不进行转换, 使用原值
	 * 
	 * <p> {@code Converter<Object,T>} 的参数能够保证不为 {@code null}
	 * 
	 * @param resolvableType 值的类型
	 * @return 值转换器, 可能为 {@code null}
	 * @throws UnsupportedConversionException 若 {@code type} 不被支持, 将抛出该异常
	 */
	Converter<Object, T> getConverter(ResolvableType resolvableType) throws UnsupportedConversionException;

	/**
	 * 获取值断言器
	 * 
	 * <p> 由于对于 {@code null} 的检查应当完全交由 {@link VNotNullValueHandleres}, 所以除了它之外,
	 * 均能够保证 {@code Predicate<T>} 的参数不为 null.
	 * 
	 * @param attrs criterion 属性
	 * @return 值断言器
	 */
	Predicate<T> getPredicate(Criterion.Attributes attrs);

	/**
	 * 获取值字符串化工具
	 * 
	 * <p> 当值未通过校验时, 使用该字符串化工具将值转化成字符串
	 * 
	 * <p> 若返回 {@code null}, 则默认使用 {@link Stringifier#simpleStringify(Object)} 进行字符串化
	 * 
	 * @return 字符串化工具, 可能为 {@code null}
	 */
	default Stringifier<T> getStringifier(Criterion.Attributes attrs) {
		return null;
	}

}
