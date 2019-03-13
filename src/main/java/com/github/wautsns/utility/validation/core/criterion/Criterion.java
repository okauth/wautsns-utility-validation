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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.github.wautsns.utility.validation.annotation.helper.ACriterion;
import com.github.wautsns.utility.validation.annotation.helper.ASpecify;
import com.github.wautsns.utility.validation.core.criterion.handlers.Stringifier;
import com.github.wautsns.utility.validation.core.criterion.handlers.ValueHandlers;
import com.github.wautsns.utility.validation.core.criterion.handlers.ValueHandlers4Marker;
import com.github.wautsns.utility.validation.core.validation.VEnv;
import com.github.wautsns.utility.validation.core.validation.VGroups;
import com.github.wautsns.utility.validation.exception.initialization.InitializationException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 *
 * @author wautsns
 * @version 0.1.0 Mar 12, 2019
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Criterion {

	private Class<?> type;
	private String position;

	private Class<?> root;
	private String indepth;
	private Class<?>[] groups;

	private Converter converter;
	private Predicate predicate;
	private Stringifier stringifier;
	private CriterionViolation.Template template;

	public CriterionViolation test(Object source) {
		Object value = (converter == null) ? source : converter.convert(source);
		if (predicate.test(value)) return null;
		String temp = (stringifier == null) ? Stringifier.simple(value) : stringifier.stringify(value);
		return predicate.test(value) ? null : template.generate(position, temp);
	}

	@Override
	public String toString() {
		StringBuilder bder = new StringBuilder();
		Arrays.stream(groups).forEach(group -> bder.append(group.getSimpleName()).append(", "));
		if (bder.length() != 0)
			bder.delete(bder.length() - 2, bder.length());
		return String.format("{type = %s, root = %s, position = %s, indepth = %s, groups = %s}",
			type.getSimpleName(), root.getSimpleName(), position, indepth, bder);
	}

	public static class Attributes {

		private HashMap<String, Object> data;

		public <T> T get(String name) {
			return (T) data.get(name);
		}

		private static Attributes of(MetaData.MetaAttrs metaAttrs, Annotation annotation) {
			Attributes attrs = new Attributes();
			attrs.data = new HashMap<>(metaAttrs.size(), 1f);
			metaAttrs.forEach((name, attr) -> attrs.data.put(name, attr.getValue(annotation)));
			_adjustGroups(attrs);
			_checkAndAdjustIndepth(attrs);
			return attrs;
		}

		private static void _adjustGroups(Attributes attrs) {
			LinkedList<Class<?>> groups = Arrays.stream((Class<?>[]) attrs.get("groups"))
				.distinct().collect(Collectors.toCollection(LinkedList::new));
			for (int i = 0; i < groups.size();) {
				Class<?> group = groups.get(i);
				Class<?>[] interfaces = group.getInterfaces();
				if (interfaces.length == 0)
					i++;
				else {
					groups.remove(i);
					for (Class<?> curr : interfaces)
						if (!groups.contains(curr))
							groups.add(curr);
				}
			}
			attrs.data.put("groups", groups.isEmpty()
				? VGroups.DEFAULT_GROUPS
				: groups.toArray(new Class<?>[groups.size()]));
		}

		private static void _checkAndAdjustIndepth(Attributes attrs) {
			String indepth = attrs.get("indepth");
			indepth = indepth.toLowerCase();
			if (!indepth.matches("[ekvc]*"))
				throw new InitializationException("indepth 表达式只能由 ekvc 组成");
			attrs.data.put("indepth", indepth);
		}
	}

	public static class MetaData {

		private Class<?> type;
		private ACriterion config;
		private MetaAttrs attrs;
		private LinkedList<MetaAttrs> path;

		public boolean isMarker() {
			return config.valueHandlers() == ValueHandlers4Marker.class;
		}

		@Override
		public String toString() {
			StringBuilder bder = new StringBuilder();
			bder.append("root: ").append(type.getSimpleName()).append('\n');
			bder.append("path: ");
			path.forEach(attr -> bder.append(attr.owner.getSimpleName()).append(" -> "));
			bder.delete(bder.length() - 4, bder.length()).append('\n');
			bder.append("self: \n").append(attrs);
			bder.append("details:\n");
			path.forEach(bder::append);
			return bder.toString();
		}

		@AllArgsConstructor
		private static class MetaAttr {

			/** 最原始的元属性拥有者 */
			private Class<?> owner;
			private Object _data;

			public MetaAttr(Method method) {
				this(method.getDeclaringClass(), method);
			}

			public boolean isConst() {
				return !(_data instanceof Method);
			}

			public String getName() {
				return isConst() ? null : ((Method) _data).getName();
			}

			public Class<?> getType() {
				return (_data == null) ? null : isConst() ? _data.getClass() : ((Method) _data).getReturnType();
			}

			public Object getValue() {
				return isConst() ? _data : ((Method) _data).getDefaultValue();
			}

			public Object getValue(Annotation ownerAnno) {
				if (ownerAnno == null) return getValue();
				if (isConst()) return _data;
				try {
					return ((Method) _data).invoke(ownerAnno);
				} catch (Exception e) {
					throw new RuntimeException("unreachable");
				}
			}

			@Override
			public String toString() {
				StringBuilder bder = new StringBuilder();
				String value = Stringifier.simple(getValue());
				if (isConst())
					bder.append(value).append(" defined in ").append(owner.getSimpleName());
				else
					bder.append(owner.getSimpleName()).append('.').append(getName())
						.append("() default ").append(value);
				return bder.toString();
			}
		}

		private static class MetaAttrs extends HashMap<String, MetaAttr> {

			private static final long serialVersionUID = 1L;

			private Class<?> owner;

			public MetaAttrs(Class<?> owner, int capacity) {
				super(capacity <= 4 ? 4 : capacity, 1f);
				this.owner = owner;
			}

			@Override
			public String toString() {
				StringBuilder bder = new StringBuilder();
				bder.append("  ").append(owner.getSimpleName()).append('\n');
				forEach((name, attr) -> {
					bder.append("    ").append(name).append(" = ").append(attr).append('\n');
				});
				return bder.toString();
			}
		}

		public static MetaData of(Class<?> type) {
			return InstanceHelper.getInstance(type, null);
		}

		private static class InstanceHelper {

			private static final HashMap<Class<?>, MetaData> CACHE = new HashMap<>();

			public static MetaData getInstance(Class<?> type, List<Class<?>> chain) {
				if (!type.isAnnotation()) return null;
				MetaData instance = CACHE.get(type);
				if (instance != null) return instance;
				ACriterion config = type.getDeclaredAnnotation(ACriterion.class);
				if (config == null) return null;
				if (chain == null) chain = new LinkedList<>();
				_checkCircularChain(type, chain);
				try {
					instance = _newSimpleMetaData(type, config);
					_initDirectPath(instance, chain);
					_replaceToCompletePath(instance, chain);
				} catch (InitializationException e) {
					throw new InitializationException(e, "\n\t\t初始化约束[%s]失败,原因: %s", type, e.getMessage());
				}
				CACHE.put(type, instance);
				return instance;
			}

			private static void _checkCircularChain(Class<?> criterionAnnoType, List<Class<?>> chain) {
				if (!chain.contains(criterionAnnoType))
					chain.add(criterionAnnoType);
				else {
					StringBuilder bder = new StringBuilder();
					for (Class<?> node : chain) {
						if (node == criterionAnnoType)
							bder.append("((( ");
						bder.append(node.getSimpleName()).append(" -> ");
					}
					bder.append(criterionAnnoType.getSimpleName()).append(" )))");
					throw new InitializationException("出现循环初始化链: %s", bder);
				}
			}

			private static MetaData _newSimpleMetaData(Class<?> criterionAnnoType, ACriterion config) {
				MetaData md = new MetaData();
				md.type = criterionAnnoType;
				md.config = config;
				Method[] annoAttrs = criterionAnnoType.getDeclaredMethods();
				md.attrs = new MetaAttrs(criterionAnnoType, annoAttrs.length);
				for (Method annoAttr : annoAttrs)
					md.attrs.put(annoAttr.getName(), new MetaAttr(annoAttr));
				ASpecify[] specifies = criterionAnnoType.getDeclaredAnnotationsByType(ASpecify.class);
				if (specifies.length != 0)
					Arrays.stream(specifies)
						.filter(specify -> md.type == specify.type())
						.forEach(specify -> _specifySelfAttrs(md, specify));
				if (Arrays.asList("message", "groups", "indepth").stream().allMatch(md.attrs::containsKey))
					return md;
				throw new InitializationException("缺少对必要属性[message,groups,indepth]的定义");
			}

			private static void _specifySelfAttrs(MetaData md, ASpecify specify) {
				for (String attr : specify.attrs()) {
					String[] nmv = _toNMV(attr);
					if (!"=".equals(nmv[1]))
						throw new InitializationException("只能通过 SpEL 表达式的方式指定自身属性,即: ${name}=${SpEL}");
					MetaAttr old = md.attrs.put(nmv[0], _analysisSpelNMV(md.type, nmv, null));
					if (old != null)
						throw new InitializationException("属性[%s]被多次定义", nmv[0]);
				}
			}

			private static void _initDirectPath(MetaData md, List<Class<?>> chain) {
				List<ASpecify> specifies = Arrays.stream(md.type.getDeclaredAnnotationsByType(ASpecify.class))
					.filter(specify -> md.type != specify.type())
					.sorted(Comparator.comparingInt(ASpecify::order))
					.collect(Collectors.toList());
				md.path = new LinkedList<>();
				if (!md.isMarker()) {
					specifies.stream()
						.filter(specify -> specify.order() <= 0)
						.forEach(specify -> _addSpecifiedAttrs(md, specify, chain));
					md.path.add(md.attrs);
					specifies.stream()
						.filter(specify -> specify.order() > 0)
						.forEach(specify -> _addSpecifiedAttrs(md, specify, chain));
				} else if (specifies.size() == 0) {
					throw new InitializationException("标记约束需要至少关联指定其他一个约束才有意义");
				} else {
					specifies.forEach(specify -> _addSpecifiedAttrs(md, specify, chain));
				}
			}

			private static void _addSpecifiedAttrs(MetaData md, ASpecify specify, List<Class<?>> chain) {
				MetaData ref = getInstance(specify.type(), chain);
				if (ref == null)
					throw new InitializationException("注解[%s]并不是一个约束注解", specify.type());
				MetaAttrs attrs = new MetaAttrs(specify.type(), specify.attrs().length);
				for (String expr : specify.attrs()) {
					String[] nmv = _toNMV(expr);
					MetaAttr refAttr = ref.attrs.get(nmv[0]);
					if (refAttr == null)
						throw new InitializationException("指定了约束[%s]不存在的属性[%s]", specify, nmv[0]);
					if (">".equals(nmv[1]))
						attrs.put(nmv[0], _analysisRefNMV(specify.type(), nmv, md.attrs.get(nmv[2])));
					else if ("".equals(nmv[1]))
						attrs.put(nmv[0], _analysisUseDefaultValueNMV(md.type, nmv, refAttr));
					else if ("=".equals(nmv[1])) {
						if (refAttr.isConst())
							throw new InitializationException("约束属性[@%s.%s]无法被指定,因为其已被指定为: %s",
								specify.type(), nmv[0], refAttr);
						attrs.put(nmv[0], _analysisSpelNMV(md.type, nmv, refAttr.getType()));
					}
				}
				for (String name : new String[] { "message", "groups", "indepth" })
					attrs.putIfAbsent(name, md.attrs.get(name));
				for (Entry<String, MetaAttr> entry : ref.attrs.entrySet())
					if (!attrs.containsKey(entry.getKey()))
						throw new InitializationException("缺少约束属性[@%s.%s]的指定", specify.type(), entry.getKey());
				md.path.add(attrs);
			}

			private static String[] _toNMV(String attr) {
				Pattern pattern = Pattern.compile("^([^=>]+)([=>]?)(.*)$");
				Matcher matcher = pattern.matcher(attr);
				if (!matcher.find())
					throw new InitializationException("无效的属性表达式: %s", attr);
				String[] nmv = new String[3]; // name mode value
				for (int i = 0; i < nmv.length; i++)
					nmv[i] = matcher.group(i + 1).trim();
				return nmv;
			}

			private static MetaAttr _analysisRefNMV(Class<?> nmvOwner, String[] nmv, MetaAttr maRef) {
				if (maRef != null) return maRef;
				throw new InitializationException("约束属性[@%s.%s]引用了不存在的属性[%s]", nmvOwner, nmv[0], nmv[2]);
			}

			private static MetaAttr _analysisUseDefaultValueNMV(Class<?> root, String[] nmv, MetaAttr target) {
				if (target.isConst()) return target;
				Object value = target.getValue();
				if (value == null)
					throw new InitializationException("指定了约束属性[@%s.%s]使用默认值,但实际上该属性并没有默认值",
						target.owner, target.getName());
				return new MetaAttr(root, value);
			}

			private static MetaAttr _analysisSpelNMV(Class<?> root, String[] nmv, Class<?> type) {
				try {
					Expression expr = new SpelExpressionParser().parseExpression(nmv[2]);
					if (type == null) {
						if ("message".equals(nmv[0]) || "indepth".equals(nmv[0]))
							type = String.class;
						else if ("groups".equals(nmv[0]))
							type = Class[].class;
					}
					Object value = (type == null)
						? expr.getValue(VEnv.SpEL_CTX)
						: expr.getValue(VEnv.SpEL_CTX, type);
					return new MetaAttr(root, value);
				} catch (RuntimeException e) {
					throw new InitializationException("无法解析 SpEL 表达式: %s, 原因: %s", nmv[2], e.getMessage());
				}
			}

			private static void _replaceToCompletePath(MetaData md, List<Class<?>> chain) {
				LinkedList<MetaAttrs> completePath = new LinkedList<>();
				for (MetaAttrs ref : md.path)
					if (ref.owner == md.type)
						completePath.add(ref);
					else
						for (MetaAttrs pathNode : getInstance(ref.owner, chain).path)
							if (pathNode.owner == ref.owner)
								completePath.add(ref);
							else {
								MetaAttrs temp = new MetaAttrs(pathNode.owner, pathNode.size());
								pathNode.forEach((name, attr) -> {
									temp.put(name, attr.isConst() ? attr : ref.get(attr.getName()));
								});
								completePath.add(temp);
							}
				md.path = completePath;
			}
		}
	}

	public static class Analyzer {

		public static LinkedList<Criterion> analyzeAnnosOn(Class<?> clazz) {
			return analyze(
				clazz.getSimpleName(),
				ResolvableType.forClass(clazz),
				clazz.getDeclaredAnnotations());
		}

		public static LinkedList<Criterion> analyzeAnnosOn(Field field) {
			return analyze(
				field.getDeclaringClass().getSimpleName() + '.' + field.getName(),
				ResolvableType.forField(field),
				field.getDeclaredAnnotations());
		}

		public static LinkedList<Criterion> analyzeAnnosOn(Method method) {
			if (method.getReturnType() == void.class || method.getParameterCount() != 0)
				return null;
			String name = method.getName();
			if (name.matches("get[A-Z]"))
				name = name.substring(3);
			else if (name.matches("is[A-Z]*"))
				name = name.substring(2);
			char[] chars = name.toCharArray();
			chars[0] = Character.toLowerCase(chars[0]);
			name = new String(chars);
			return analyze(
				method.getDeclaringClass().getSimpleName() + '.' + name,
				ResolvableType.forMethodReturnType(method),
				method.getDeclaredAnnotations());
		}

		public static LinkedList<Criterion> analyze(
				String position, ResolvableType resolvableType, Annotation[] annotations) {
			LinkedList<Criterion> criteria = new LinkedList<>();
			if (annotations.length == 0) return criteria;
			for (Annotation annotation : annotations)
				criteria.addAll(_analyze(position, resolvableType, annotation));
			if (!criteria.isEmpty()) _optimize(criteria);
			return criteria;
		}

		private static LinkedList<Criterion> _analyze(
				String position, ResolvableType resolvableType, Annotation annotation) {
			LinkedList<Criterion> criteria = new LinkedList<>();
			MetaData root = MetaData.of(annotation.annotationType());
			if (root == null) {
				Class<? extends Annotation> annoType = annotation.annotationType();
				try {
					Method attr = annoType.getDeclaredMethod("value");
					Class<?> attrType = attr.getReturnType();
					if (!attrType.isArray() || !attrType.getComponentType().isAnnotation())
						return criteria;
					attr.setAccessible(true);
					Annotation[] annotations = (Annotation[]) attr.invoke(annotation);
					return analyze(position, resolvableType, annotations);
				} catch (Exception e) {
					return criteria;
				}
			}
			Criterion rootCriterion = _newCriterion(null, position, root.attrs, annotation, resolvableType);
			for (MetaData.MetaAttrs node : root.path)
				criteria.add(_newCriterion(rootCriterion, position, node, annotation, resolvableType));
			return criteria;
		}

		private static void _optimize(LinkedList<Criterion> criteria) {
			Class<?>[] groups = criteria.getFirst().groups;
			// TODO 优化 criteria
		}

		private static Criterion _newCriterion(
				Criterion rootCriterion,
				String position,
				MetaData.MetaAttrs metaAttrs, Annotation annotation,
				ResolvableType resolvableType) {
			if (rootCriterion != null && rootCriterion.type == metaAttrs.owner)
				return rootCriterion;
			Criterion criterion = new Criterion();
			criterion.root = (rootCriterion == null) ? metaAttrs.owner : rootCriterion.type;
			criterion.type = metaAttrs.owner;
			criterion.position = position;
			Attributes attrs = Attributes.of(metaAttrs, annotation);
			criterion.groups = attrs.get("groups");
			criterion.indepth = attrs.get("indepth");
			criterion.template = (rootCriterion != null && metaAttrs.get("message").owner == rootCriterion.type)
				? rootCriterion.template : CriterionViolation.Template.of(attrs.data);
			MetaData md = MetaData.of(metaAttrs.owner);
			if (md.isMarker()) return criterion;
			try {
				ValueHandlers<?> vhs = md.config.valueHandlers().newInstance();
				criterion.converter = vhs.getConverter(_getIndepthType(criterion.indepth, resolvableType));
				criterion.predicate = vhs.getPredicate(attrs);
				criterion.stringifier = vhs.getStringifier(attrs);
				return criterion;
			} catch (Exception e) {
				throw new InitializationException(e, "初始化 ValueHandlers 失败,原因: %s", e.getMessage());
			}
		}

		private static ResolvableType _getIndepthType(String indepth, ResolvableType resolvableType) {
			ResolvableType temp = resolvableType;
			for (int i = 0; i < indepth.length(); i++) {
				char op = indepth.charAt(i);
				if (op == 'e')
					temp = temp.asCollection().getGeneric(0);
				else if (op == 'k')
					temp = temp.asMap().getGeneric(0);
				else if (op == 'v')
					temp = temp.asMap().getGeneric(1);
				else if (op == 'c')
					temp = temp.getComponentType();
				if (temp == ResolvableType.NONE)
					throw new InitializationException(" indepth 表达式 \"%s\" 不适用于类型 %s", indepth, resolvableType);
			}
			return temp;
		}
	}

}
