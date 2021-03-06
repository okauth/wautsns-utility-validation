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
import java.lang.annotation.Repeatable;
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

	private Attributes attrs;

	private Converter converter;
	private Predicate predicate;
	private Stringifier stringifier;
	private CriterionViolation.Template template;

	public String getDepth() {
		return attrs.depth;
	}

	public Class<?>[] getGroups() {
		return attrs.groups;
	}

	public int getOrder() {
		return attrs.order;
	}

	public CriterionViolation test(Object target) {
		Object value = (converter == null) ? target : converter.convert(target);
		return predicate.test(value)
			? null
			: template.generate(
				(stringifier == null) ? Stringifier.simple(value) : stringifier.stringify(value));
	}

	@Override
	public String toString() {
		StringBuilder bder = new StringBuilder("\n");
		bder.append("type = ").append(type.getSimpleName());
		if (attrs.rootOwner != type)
			bder.append(" (root owner: ").append(attrs.rootOwner.getSimpleName()).append(')');
		bder.append('\n').append("position = ").append(position)
			.append(" (depth: ").append(Stringifier.simple(attrs.depth)).append(')');
		bder.append('\n').append("groups = ").append(Stringifier.simple(attrs.groups));
		bder.append('\n').append("order = ").append(attrs.order);
		bder.append('\n').append("data = {");
		attrs.data.forEach((name, value) -> {
			bder.append(name).append(": ").append(Stringifier.simple(value)).append(", ");
		});
		if (attrs.data.size() != 0) bder.delete(bder.length() - 2, bder.length());
		bder.append('}');
		return bder.toString();
	}

	public static class Attributes {

		private Class<?> rootOwner;
		private String depth;
		private Class<?>[] groups;
		private int order;
		private HashMap<String, Object> data;

		public <T> T get(String name) {
			return (T) data.get(name);
		}

		private static final HashMap<String, Object> _EMPTY = new HashMap<>(0);

		private String _minimizeAndReturnMessage() {
			order = (Integer) data.remove("order");
			String message = (String) data.remove("message");
			if (data.size() == 0)
				data = _EMPTY;
			else {
				HashMap<String, Object> temp = new HashMap<>(data.size(), 1f);
				temp.putAll(data);
				data = temp;
			}
			return message;
		}

		private static Attributes _of(Attributes rootAttrs, MetaData.MetaAttrs metaAttrs, Annotation anno) {
			boolean isRoot = rootAttrs == null;
			Attributes attrs = new Attributes();
			attrs.rootOwner = isRoot ? metaAttrs.owner : rootAttrs.rootOwner;
			attrs.data = new HashMap<>(metaAttrs.size(), 1f);
			metaAttrs.forEach((name, attr) -> attrs.data.put(name, attr.getValue(anno)));
			_adjustGroups(attrs);
			_checkAndAdjustDepth(isRoot ? null : rootAttrs.depth, metaAttrs, attrs);
			return attrs;
		}

		private static void _adjustGroups(Attributes attrs) {
			LinkedList<Class<?>> groups = Arrays.stream((Class<?>[]) attrs.data.remove("groups"))
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
			attrs.groups = groups.isEmpty()
				? VGroups.DEFAULT_GROUPS
				: groups.toArray(new Class<?>[groups.size()]);
		}

		private static void _checkAndAdjustDepth(String rootDepth, MetaData.MetaAttrs metaAttrs, Attributes attrs) {
			attrs.depth = (String) attrs.data.remove("depth");
			attrs.depth = attrs.depth.toLowerCase();
			if (!attrs.depth.matches("[ekvc]*"))
				throw new InitializationException("depth 表达式只能由 ekvc 组成");
			if (rootDepth != null) attrs.depth = rootDepth + attrs.depth;
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

			public MetaAttr(Method annoAttr) {
				this(annoAttr.getDeclaringClass(), annoAttr);
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
			return Instances.get(type, null);
		}

		private static class Instances {

			private static final HashMap<Class<?>, MetaData> INSTANCES = new HashMap<>();

			public static MetaData get(Class<?> type, List<Class<?>> chain) {
				if (!type.isAnnotation()) return null;
				MetaData instance = INSTANCES.get(type);
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
					throw new InitializationException(e, "初始化约束[%s]失败", type);
				}
				INSTANCES.put(type, instance);
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
				if (Arrays.asList("message", "groups", "depth", "order").stream().allMatch(md.attrs::containsKey))
					return md;
				throw new InitializationException("缺少对必要属性[message,groups,depth,order]的定义");
			}

			private static void _specifySelfAttrs(MetaData md, ASpecify specify) {
				for (String attr : specify.attrs()) {
					String[] nmv = _toNMV(attr);
					if (!"=".equals(nmv[1]))
						throw new InitializationException("只能通过 SpEL 表达式的方式指定自身属性,即: ${name}=${SpEL}");
					MetaAttr old = md.attrs.put(nmv[0], _analyzeSpelNMV(md.type, nmv, null));
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
						.forEach(specify -> _addSpecifiedNonRootAttrs(md, specify, chain));
					md.path.add(md.attrs);
					specifies.stream()
						.filter(specify -> specify.order() > 0)
						.forEach(specify -> _addSpecifiedNonRootAttrs(md, specify, chain));
				} else if (specifies.size() == 0) {
					throw new InitializationException("标记约束需要至少关联指定其他一个约束才有意义");
				} else {
					specifies.forEach(specify -> _addSpecifiedNonRootAttrs(md, specify, chain));
				}
			}

			private static void _addSpecifiedNonRootAttrs(MetaData md, ASpecify specify, List<Class<?>> chain) {
				MetaData ref = get(specify.type(), chain);
				if (ref == null)
					throw new InitializationException("注解[%s]并不是一个约束注解", specify.type());
				MetaAttrs attrs = new MetaAttrs(specify.type(), specify.attrs().length);
				for (String expr : specify.attrs()) {
					String[] nmv = _toNMV(expr);
					MetaAttr refAttr = ref.attrs.get(nmv[0]);
					if (refAttr == null)
						throw new InitializationException("指定了约束[%s]不存在的属性[%s]", specify, nmv[0]);
					if (">".equals(nmv[1]))
						attrs.put(nmv[0], _analyzeRefNMV(specify.type(), nmv, md.attrs.get(nmv[2])));
					else if ("".equals(nmv[1]))
						attrs.put(nmv[0], _analyzeUseDefaultValueNMV(md.type, nmv, refAttr));
					else if ("=".equals(nmv[1])) {
						if (refAttr.isConst())
							throw new InitializationException("约束属性[@%s.%s]无法被指定,因为其已被指定为: %s",
								specify.type(), nmv[0], refAttr);
						attrs.put(nmv[0], _analyzeSpelNMV(md.type, nmv, refAttr.getType()));
					}
				}
				for (String name : new String[] { "message", "groups" })
					attrs.putIfAbsent(name, md.attrs.get(name));
				attrs.putIfAbsent("depth", new MetaAttr(md.type, ""));
				if (attrs.put("order", md.attrs.get("order")) != null)
					throw new InitializationException(
						"无法指定 @%s 的 order 属性, 若需要控制关联约束的执行顺序,请使用 @Specify.order() 来实现该功能",
						specify.type());
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

			private static MetaAttr _analyzeRefNMV(Class<?> nmvOwner, String[] nmv, MetaAttr maRef) {
				if (maRef == null)
					throw new InitializationException("约束属性[@%s.%s]引用了不存在的属性[%s]", nmvOwner, nmv[0], nmv[2]);
				return "depth".equals(nmv[0]) ? new MetaAttr(maRef.owner, "") : maRef;
			}

			private static MetaAttr _analyzeUseDefaultValueNMV(Class<?> root, String[] nmv, MetaAttr target) {
				if (target.isConst()) return target;
				Object value = target.getValue();
				if (value == null)
					throw new InitializationException("指定了约束属性[@%s.%s]使用默认值,但实际上该属性并没有默认值",
						target.owner, target.getName());
				return new MetaAttr(root, value);
			}

			private static MetaAttr _analyzeSpelNMV(Class<?> root, String[] nmv, Class<?> type) {
				try {
					Expression expr = new SpelExpressionParser().parseExpression(nmv[2]);
					if (type == null) {
						if ("message".equals(nmv[0]) || "depth".equals(nmv[0]))
							type = String.class;
						else if ("groups".equals(nmv[0]))
							type = Class[].class;
					}
					Object value = (type == null)
						? expr.getValue(VEnv.SpEL_CTX)
						: expr.getValue(VEnv.SpEL_CTX, type);
					return new MetaAttr(root, value);
				} catch (RuntimeException e) {
					throw new InitializationException(e, "无法解析 SpEL 表达式: %s", nmv[2]);
				}
			}

			private static void _replaceToCompletePath(MetaData md, List<Class<?>> chain) {
				LinkedList<MetaAttrs> completePath = new LinkedList<>();
				for (MetaAttrs ref : md.path)
					if (ref.owner == md.type)
						completePath.add(ref);
					else
						for (MetaAttrs pathNode : get(ref.owner, chain).path)
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
			for (Annotation annotation : annotations) {
				try {
					criteria.addAll(_analyze(position, resolvableType, annotation));
				} catch (InitializationException e) {
					throw new InitializationException(e, "初始化位于 %s 上的注解 %s 时出现错误",
						position, annotation);
				}
			}
			return criteria;
		}

		private static LinkedList<Criterion> _analyze(
				String position, ResolvableType resolvableType, Annotation annotation) {
			LinkedList<Criterion> criteria = new LinkedList<>();
			MetaData root = MetaData.of(annotation.annotationType());
			if (root == null) {
				Class<? extends Annotation> annoType = annotation.annotationType();
				Annotation[] annotations;
				try {
					Method attr = annoType.getDeclaredMethod("value");
					Class<?> attrType = attr.getReturnType();
					if (!attrType.isArray()) return criteria;
					Class<?> attrComponentType = attrType.getComponentType();
					if (!attrComponentType.isAnnotation()) return criteria;
					Repeatable repeatable = attrComponentType.getDeclaredAnnotation(Repeatable.class);
					if (repeatable == null || repeatable.value() != annoType) return criteria;
					attr.setAccessible(true);
					annotations = (Annotation[]) attr.invoke(annotation);
				} catch (Exception e) {
					return criteria;
				}
				return analyze(position, resolvableType, annotations);
			}
			Criterion rootCriterion = _newCriterion(null, position, root.attrs, annotation, resolvableType);
			for (MetaData.MetaAttrs node : root.path)
				criteria.add(_newCriterion(rootCriterion, position, node, annotation, resolvableType));
			return criteria;
		}

		private static Criterion _newCriterion(
				Criterion rootCriterion,
				String position,
				MetaData.MetaAttrs metaAttrs, Annotation annotation,
				ResolvableType resolvableType) {
			boolean isRoot = rootCriterion == null;
			if (!isRoot && rootCriterion.type == metaAttrs.owner)
				return rootCriterion;
			Criterion criterion = new Criterion();
			criterion.type = metaAttrs.owner;
			criterion.attrs = Attributes._of(isRoot ? null : rootCriterion.attrs, metaAttrs, annotation);
			criterion.position = position;
			if (!criterion.attrs.depth.isEmpty())
				criterion.position += '.' + criterion.attrs.depth;
			String message = criterion.attrs._minimizeAndReturnMessage();
			criterion.template = (!isRoot && metaAttrs.get("message").owner == rootCriterion.type)
				? rootCriterion.template
				: CriterionViolation.Template.of(criterion.position, criterion.type, message, criterion.attrs.data);
			criterion.attrs.data.remove("message");
			MetaData md = MetaData.of(metaAttrs.owner);
			if (md.isMarker()) return criterion;
			try {
				ValueHandlers<?> vhs = md.config.valueHandlers().newInstance();
				criterion.converter = vhs.getConverter(_getDepthType(criterion.attrs.depth, resolvableType));
				criterion.predicate = vhs.getPredicate(criterion.attrs);
				criterion.stringifier = vhs.getStringifier(criterion.attrs);
				return criterion;
			} catch (Exception e) {
				throw new InitializationException(e, "初始化 ValueHandlers 失败");
			}
		}

		private static ResolvableType _getDepthType(String depth, ResolvableType resolvableType) {
			ResolvableType temp = resolvableType;
			for (int i = 0; i < depth.length(); i++) {
				char op = depth.charAt(i);
				if (op == 'e')
					temp = temp.asCollection().getGeneric(0);
				else if (op == 'k')
					temp = temp.asMap().getGeneric(0);
				else if (op == 'v')
					temp = temp.asMap().getGeneric(1);
				else if (op == 'c')
					temp = temp.getComponentType();
				if (temp == ResolvableType.NONE)
					throw new InitializationException(" depth 表达式 \"%s\" 不适用于类型 %s", depth, resolvableType);
			}
			return temp;
		}
	}

}
