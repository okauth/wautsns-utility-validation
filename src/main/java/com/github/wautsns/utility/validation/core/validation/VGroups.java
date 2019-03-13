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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VGroups {

	/** 默认组, 该值内容不可被修改 */
	public static final Class<?>[] DEFAULT_GROUPS = { VGroups.Default.class };

	public interface Default {}

}
