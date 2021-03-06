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
package com.github.wautsns.utility.validation.annotation.helper;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
@Documented
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
@Repeatable(ASpecifyList.class)
public @interface ASpecify {

	Class<? extends Annotation> type();

	int order() default 0;

	String[] attrs() default {};

}

@Documented
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
@interface ASpecifyList {

	ASpecify[] value();
}
