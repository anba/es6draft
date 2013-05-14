/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;

/**
 * Utility class to set-up initial properties for objects
 */
public final class Properties {
    private Properties() {
    }

    /**
     * Built-in prototype
     */
    @Documented
    @Target({ ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Prototype {
    }

    /**
     * Built-in function property
     */
    @Documented
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Function {
        /**
         * Function name
         */
        String name();

        /**
         * Function symbol
         */
        BuiltinSymbol symbol() default BuiltinSymbol.NONE;

        /**
         * Function arity
         */
        int arity();

        /**
         * Function attributes, default to <code>{[[Writable]]: true, [[Enumerable]]:
         * false, [[Configurable]]: true}</code>
         */
        Attributes attributes() default @Attributes(writable = true, enumerable = false,
                configurable = true);
    }

    /**
     * Built-in value property
     */
    @Documented
    @Target({ ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Value {
        /**
         * Value name
         */
        String name();

        /**
         * Value symbol
         */
        BuiltinSymbol symbol() default BuiltinSymbol.NONE;

        /**
         * Value attributes, default to <code>{[[Writable]]: true, [[Enumerable]]:
         * false, [[Configurable]]: true}</code>
         */
        Attributes attributes() default @Attributes(writable = true, enumerable = false,
                configurable = true);
    }

    /**
     * Built-in accessor property
     */
    @Documented
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Accessor {
        /**
         * Accessor name
         */
        String name();

        /**
         * Accessor symbol
         */
        BuiltinSymbol symbol() default BuiltinSymbol.NONE;

        enum Type {
            Getter, Setter
        }

        Type type();

        /**
         * Accessor attributes, default to <code>{[[Enumerable]]:
         * false, [[Configurable]]: true}</code>
         */
        Attributes attributes() default @Attributes(writable = false /*unused*/,
                enumerable = false, configurable = true);
    }

    @Documented
    @Target({ ElementType.ANNOTATION_TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Attributes {
        boolean writable();

        boolean enumerable();

        boolean configurable();
    }

    @Documented
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Optional {
        Default value() default Default.Undefined;

        boolean booleanValue() default false;

        String stringValue() default "";

        double numberValue() default Double.NaN;

        enum Default {
            NONE, Undefined, Null, Boolean, String, Number;

            static Object defaultValue(Optional optional) {
                switch (optional.value()) {
                case Undefined:
                    return UNDEFINED;
                case Null:
                    return NULL;
                case Boolean:
                    return optional.booleanValue();
                case Number:
                    return optional.numberValue();
                case String:
                    return optional.stringValue();
                case NONE:
                default:
                    return null;
                }
            }
        }
    }

    private static ClassValue<ObjectLayout> internalLayouts = new ClassValue<ObjectLayout>() {
        @Override
        protected ObjectLayout computeValue(Class<?> type) {
            return createInternalObjectLayout(type);
        }
    };

    private static ClassValue<ObjectLayout> externalLayouts = new ClassValue<ObjectLayout>() {
        @Override
        protected ObjectLayout computeValue(Class<?> type) {
            return createExternalObjectLayout(type);
        }
    };

    private static class ObjectLayout {
        Prototype proto = null;
        Object protoValue = null;
        Map<Value, Object> values = null;
        Map<Function, MethodHandle> functions = null;
        Map<Accessor, MethodHandle> accessors = null;
    }

    /**
     * Sets the {@link Prototype} and creates own properties for {@link Value}, {@link Function} and
     * {@link Accessor} fields
     */
    public static void createProperties(ScriptObject owner, ExecutionContext cx, Class<?> holder) {
        if (holder.getPackage().getName().startsWith(INTERNAL_PACKAGE)) {
            createInternalProperties(owner, cx.getRealm(), holder);
        } else {
            createExternalProperties(owner, cx.getRealm(), holder);
        }
    }

    private static final String INTERNAL_PACKAGE = "com.github.anba.es6draft.runtime.";

    private static class Converter {
        final MethodHandle ToBooleanMH;
        final MethodHandle ToStringMH;
        final MethodHandle ToFlatStringMH;
        final MethodHandle ToNumberMH;
        final MethodHandle ToObjectMH;

        Converter(ExecutionContext cx) {
            ToBooleanMH = _ToBooleanMH;
            ToStringMH = MethodHandles.insertArguments(_ToStringMH, 0, cx);
            ToFlatStringMH = MethodHandles.insertArguments(_ToFlatStringMH, 0, cx);
            ToNumberMH = MethodHandles.insertArguments(_ToNumberMH, 0, cx);
            ToObjectMH = MethodHandles.insertArguments(_ToObjectMH, 0, cx);
        }

        private static final MethodHandle _ToBooleanMH;
        private static final MethodHandle _ToStringMH;
        private static final MethodHandle _ToFlatStringMH;
        private static final MethodHandle _ToNumberMH;
        private static final MethodHandle _ToObjectMH;
        static {
            Lookup lookup = MethodHandles.publicLookup();
            try {
                _ToStringMH = lookup.findStatic(AbstractOperations.class, "ToString", MethodType
                        .methodType(CharSequence.class, ExecutionContext.class, Object.class));
                _ToFlatStringMH = lookup.findStatic(AbstractOperations.class, "ToFlatString",
                        MethodType.methodType(String.class, ExecutionContext.class, Object.class));
                _ToNumberMH = lookup.findStatic(AbstractOperations.class, "ToNumber",
                        MethodType.methodType(Double.TYPE, ExecutionContext.class, Object.class));
                _ToBooleanMH = lookup.findStatic(AbstractOperations.class, "ToBoolean",
                        MethodType.methodType(Boolean.TYPE, Object.class));
                _ToObjectMH = lookup.findStatic(AbstractOperations.class, "ToObject", MethodType
                        .methodType(ScriptObject.class, ExecutionContext.class, Object.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static void createExternalProperties(ScriptObject owner, Realm realm, Class<?> holder) {
        ObjectLayout layout = externalLayouts.get(holder);
        if (layout.functions != null) {
            ExecutionContext cx = realm.defaultContext();
            Converter converter = new Converter(cx);
            for (Entry<Function, MethodHandle> entry : layout.functions.entrySet()) {
                Function function = entry.getKey();
                MethodHandle unreflect = entry.getValue();

                MethodHandle handle = getInstanceMethodHandle(converter, unreflect, owner);
                String name = function.name();
                int arity = function.arity();
                Attributes attrs = function.attributes();
                NativeFunction fun = new NativeFunction(realm, name, arity, handle);
                owner.defineOwnProperty(cx, name, propertyDescriptor(fun, attrs));
            }
        }
    }

    private static void createInternalProperties(ScriptObject owner, Realm realm, Class<?> holder) {
        ObjectLayout layout = internalLayouts.get(holder);
        if (layout.proto != null) {
            createPrototype(owner, realm, layout.proto, layout.protoValue);
        }
        if (layout.values != null) {
            for (Entry<Value, Object> entry : layout.values.entrySet()) {
                createValue(owner, realm, entry.getKey(), entry.getValue());
            }
        }
        if (layout.functions != null) {
            for (Entry<Function, MethodHandle> entry : layout.functions.entrySet()) {
                createFunction(owner, realm, entry.getKey(), entry.getValue());
            }
        }
        if (layout.accessors != null) {
            Map<String, PropertyDescriptor> accessors1 = new LinkedHashMap<>();
            Map<BuiltinSymbol, PropertyDescriptor> accessors2 = new EnumMap<>(BuiltinSymbol.class);
            for (Entry<Accessor, MethodHandle> entry : layout.accessors.entrySet()) {
                createAccessor(owner, realm, entry.getKey(), entry.getValue(), accessors1,
                        accessors2);
            }
            completeAccessors(owner, realm, accessors1, accessors2);
        }
    }

    private static ObjectLayout createExternalObjectLayout(Class<?> holder) {
        try {
            ObjectLayout layout = new ObjectLayout();
            Lookup lookup = MethodHandles.publicLookup();
            for (Method method : holder.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()))
                    continue;
                Function function = method.getAnnotation(Function.class);
                if (function != null) {
                    if (layout.functions == null) {
                        layout.functions = new LinkedHashMap<>();
                    }
                    layout.functions.put(function, lookup.unreflect(method));
                }
            }
            return layout;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static ObjectLayout createInternalObjectLayout(Class<?> holder) {
        try {
            ObjectLayout layout = new ObjectLayout();
            Lookup lookup = MethodHandles.publicLookup();
            boolean hasProto = false;
            for (Field field : holder.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()))
                    continue;
                Value value = field.getAnnotation(Value.class);
                if (value != null) {
                    if (layout.values == null) {
                        layout.values = new LinkedHashMap<>();
                    }
                    layout.values.put(value, getRawValue(field));
                } else {
                    Prototype prototype = field.getAnnotation(Prototype.class);
                    if (prototype != null) {
                        assert !hasProto;
                        hasProto = true;
                        layout.proto = prototype;
                        layout.protoValue = getRawValue(field);
                    }
                }
            }
            for (Method method : holder.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()))
                    continue;
                Function function = method.getAnnotation(Function.class);
                if (function != null) {
                    if (layout.functions == null) {
                        layout.functions = new LinkedHashMap<>();
                    }
                    layout.functions.put(function, getStaticMethodHandle(lookup, method));
                } else {
                    Accessor accessor = method.getAnnotation(Accessor.class);
                    if (accessor != null) {
                        if (layout.accessors == null) {
                            layout.accessors = new LinkedHashMap<>();
                        }
                        layout.accessors.put(accessor, getStaticMethodHandle(lookup, method));
                    }
                }
            }
            return layout;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Object getRawValue(Field field) throws IllegalAccessException {
        Object rawValue = field.get(null);
        return rawValue;
    }

    private static <T> MethodHandle getInstanceMethodHandle(Converter converter,
            MethodHandle unreflect, T owner) {
        MethodHandle handle = unreflect;
        handle = handle.bindTo(owner);

        MethodType type = handle.type();
        int pcount = type.parameterCount();
        Class<?>[] params = type.parameterArray();
        MethodHandle[] filters = null;
        for (int p = 0; p < pcount; ++p) {
            Class<?> c = params[p];
            if (c == Object.class) {
                continue;
            }
            if (c == Double.TYPE || c == Boolean.TYPE || c == String.class
                    || c == CharSequence.class || c == ScriptObject.class) {
                if (filters == null) {
                    filters = new MethodHandle[pcount];
                }
                if (c == Double.TYPE) {
                    filters[p] = converter.ToNumberMH;
                } else if (c == Boolean.TYPE) {
                    filters[p] = converter.ToBooleanMH;
                } else if (c == String.class) {
                    filters[p] = converter.ToFlatStringMH;
                } else if (c == CharSequence.class) {
                    filters[p] = converter.ToStringMH;
                } else {
                    filters[p] = converter.ToObjectMH;
                }
                continue;
            }
            throw new IllegalArgumentException();
        }
        if (filters != null) {
            handle = MethodHandles.filterArguments(handle, 0, filters);
        }

        Class<?> returnType = type.returnType();
        if (returnType == Double.TYPE || returnType == Boolean.TYPE || returnType == String.class
                || returnType == CharSequence.class || returnType == ScriptObject.class) {
            handle = MethodHandles.explicitCastArguments(handle,
                    handle.type().changeReturnType(Object.class));
        } else if (returnType == Void.TYPE) {
            handle = MethodHandles.filterReturnValue(handle,
                    MethodHandles.constant(Object.class, UNDEFINED));
        } else if (returnType != Object.class) {
            throw new IllegalArgumentException();
        }

        MethodHandle spreader = MethodHandles.spreadInvoker(handle.type(), 0);
        MethodHandle undefined = filter(pcount);
        handle = MethodHandles.insertArguments(spreader, 0, handle);
        handle = MethodHandles.filterArguments(handle, 0, undefined);
        handle = MethodHandles.dropArguments(handle, 0, Object.class);

        return handle;
    }

    private static MethodHandle getStaticMethodHandle(Lookup lookup, Method method)
            throws IllegalAccessException {
        // check: (Realm, Object, Object[]) -> Object
        MethodHandle handle = lookup.unreflect(method);
        MethodType type = handle.type();
        StaticMethodKind kind = staticMethodKind(type);
        if (kind == StaticMethodKind.Invalid) {
            throw new IllegalArgumentException(handle.toString());
        }
        if (kind == StaticMethodKind.Spreader) {
            int fixedArguments = 2;
            boolean varargs = handle.isVarargsCollector();
            int actual = type.parameterCount() - fixedArguments - (varargs ? 1 : 0);
            Object[] defaults = methodDefaults(method, fixedArguments, actual);
            MethodHandle filter;
            if (defaults != null && varargs) {
                filter = MethodHandles.insertArguments(ParameterFilter.filterVarArgsDefaults, 0,
                        actual, defaults);
            } else if (defaults != null) {
                filter = MethodHandles.insertArguments(ParameterFilter.filterDefaults, 0, actual,
                        defaults);
            } else if (varargs) {
                filter = MethodHandles.insertArguments(ParameterFilter.filterVarArgs, 0, actual);
            } else {
                filter = filter(actual);
            }
            MethodHandle spreader = MethodHandles.spreadInvoker(type, fixedArguments);
            spreader = MethodHandles.insertArguments(spreader, 0, handle);
            spreader = MethodHandles.filterArguments(spreader, fixedArguments, filter);
            handle = spreader;
        }
        return handle;
    }

    private static final MethodHandle filters[] = new MethodHandle[10];

    private static MethodHandle filter(int n) {
        assert n >= 0;
        if (n < filters.length) {
            MethodHandle filter = filters[n];
            if (filter == null) {
                filters[n] = filter = MethodHandles.insertArguments(ParameterFilter.filter, 0, n);
            }
            return filter;
        }
        return MethodHandles.insertArguments(ParameterFilter.filter, 0, n);
    }

    public static final class ParameterFilter {
        private ParameterFilter() {
        }

        private static final MethodHandle filterVarArgsDefaults;
        private static final MethodHandle filterDefaults;
        private static final MethodHandle filterVarArgs;
        private static final MethodHandle filter;
        static {
            Lookup lookup = MethodHandles.publicLookup();
            try {
                filterVarArgsDefaults = lookup.findStatic(ParameterFilter.class,
                        "filterVarArgsDefaults", MethodType.methodType(Object[].class,
                                Integer.TYPE, Object[].class, Object[].class));
                filterDefaults = lookup.findStatic(ParameterFilter.class, "filterDefaults",
                        MethodType.methodType(Object[].class, Integer.TYPE, Object[].class,
                                Object[].class));
                filterVarArgs = lookup.findStatic(ParameterFilter.class, "filterVarArgs",
                        MethodType.methodType(Object[].class, Integer.TYPE, Object[].class));
                filter = lookup.findStatic(ParameterFilter.class, "filter",
                        MethodType.methodType(Object[].class, Integer.TYPE, Object[].class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private static final Object[] EMPTY_ARRAY = new Object[] {};

        public static Object[] filterVarArgsDefaults(int n, Object[] defaultValues, Object[] args) {
            assert n == defaultValues.length;
            Object[] arguments = Arrays.copyOf(args, n + 1, Object[].class);
            if (args.length == n) {
                arguments[n] = EMPTY_ARRAY;
            } else if (args.length > n) {
                arguments[n] = Arrays.copyOfRange(args, n, args.length, Object[].class);
            } else {
                int argslen = args.length;
                System.arraycopy(defaultValues, argslen, arguments, argslen, (n - argslen));
                arguments[n] = EMPTY_ARRAY;
            }
            return arguments;
        }

        public static Object[] filterDefaults(int n, Object[] defaultValues, Object[] args) {
            assert n == defaultValues.length;
            if (args.length == n) {
                return args;
            } else if (args.length > n) {
                Object[] arguments = Arrays.copyOf(args, n, Object[].class);
                return arguments;
            } else {
                Object[] arguments = Arrays.copyOf(args, n, Object[].class);
                int argslen = args.length;
                System.arraycopy(defaultValues, argslen, arguments, argslen, (n - argslen));
                return arguments;
            }
        }

        public static Object[] filterVarArgs(int n, Object[] args) {
            Object[] arguments = Arrays.copyOf(args, n + 1, Object[].class);
            if (args.length == n) {
                arguments[n] = EMPTY_ARRAY;
            } else if (args.length > n) {
                arguments[n] = Arrays.copyOfRange(args, n, args.length, Object[].class);
            } else {
                Arrays.fill(arguments, args.length, n, UNDEFINED);
                arguments[n] = EMPTY_ARRAY;
            }
            return arguments;
        }

        public static Object[] filter(int n, Object[] args) {
            if (args.length == n) {
                return args;
            } else if (args.length > n) {
                Object[] arguments = Arrays.copyOf(args, n, Object[].class);
                return arguments;
            } else {
                Object[] arguments = Arrays.copyOf(args, n, Object[].class);
                Arrays.fill(arguments, args.length, n, UNDEFINED);
                return arguments;
            }
        }
    }

    private static void createPrototype(ScriptObject owner, Realm realm, Prototype proto,
            Object rawValue) {
        Object value = resolveValue(realm, rawValue);
        assert value == null || value instanceof ScriptObject;
        ScriptObject prototype = (ScriptObject) value;
        owner.setPrototype(realm.defaultContext(), prototype);
    }

    private static void createValue(ScriptObject owner, Realm realm, Value val, Object rawValue) {
        String name = val.name();
        BuiltinSymbol sym = val.symbol();
        Attributes attrs = val.attributes();
        Object value = resolveValue(realm, rawValue);
        if (sym == BuiltinSymbol.NONE) {
            owner.defineOwnProperty(realm.defaultContext(), name, propertyDescriptor(value, attrs));
        } else {
            owner.defineOwnProperty(realm.defaultContext(), sym.get(),
                    propertyDescriptor(value, attrs));
        }
    }

    private static void createFunction(ScriptObject owner, Realm realm, Function function,
            MethodHandle mh) {
        String name = function.name();
        BuiltinSymbol sym = function.symbol();
        int arity = function.arity();
        Attributes attrs = function.attributes();

        mh = MethodHandles.insertArguments(mh, 0, realm.defaultContext());

        NativeFunction fun = new NativeFunction(realm, name, arity, mh);
        if (sym == BuiltinSymbol.NONE) {
            owner.defineOwnProperty(realm.defaultContext(), name, propertyDescriptor(fun, attrs));
        } else {
            owner.defineOwnProperty(realm.defaultContext(), sym.get(),
                    propertyDescriptor(fun, attrs));
        }
    }

    private static void createAccessor(ScriptObject owner, Realm realm, Accessor accessor,
            MethodHandle mh, Map<String, PropertyDescriptor> accessors1,
            Map<BuiltinSymbol, PropertyDescriptor> accessors2) {
        String name = accessor.name();
        BuiltinSymbol sym = accessor.symbol();
        Accessor.Type type = accessor.type();
        int arity = (type == Accessor.Type.Getter ? 0 : 1);
        Attributes attrs = accessor.attributes();

        mh = MethodHandles.insertArguments(mh, 0, realm.defaultContext());

        NativeFunction fun = new NativeFunction(realm, name, arity, mh);
        PropertyDescriptor desc;
        if (sym == BuiltinSymbol.NONE) {
            if ((desc = accessors1.get(name)) == null) {
                accessors1.put(name, desc = propertyDescriptor(null, null, attrs));
            }
        } else {
            if ((desc = accessors2.get(sym)) == null) {
                accessors2.put(sym, desc = propertyDescriptor(null, null, attrs));
            }
        }
        assert !attrs.writable() && attrs.enumerable() == desc.isEnumerable()
                && attrs.configurable() == desc.isConfigurable();
        if (type == Accessor.Type.Getter) {
            assert desc.getGetter() == null;
            desc.setGetter(fun);
        } else {
            assert desc.getSetter() == null;
            desc.setSetter(fun);
        }
    }

    private static void completeAccessors(ScriptObject owner, Realm realm,
            Map<String, PropertyDescriptor> accessors1,
            Map<BuiltinSymbol, PropertyDescriptor> accessors2) {
        if (accessors1 != null) {
            for (Entry<String, PropertyDescriptor> entry : accessors1.entrySet()) {
                owner.defineOwnProperty(realm.defaultContext(), entry.getKey(), entry.getValue());
            }
        }
        if (accessors2 != null) {
            for (Entry<BuiltinSymbol, PropertyDescriptor> entry : accessors2.entrySet()) {
                owner.defineOwnProperty(realm.defaultContext(), entry.getKey().get(),
                        entry.getValue());
            }
        }
    }

    private static Object resolveValue(Realm realm, Object value) {
        if (value instanceof Intrinsics) {
            value = realm.getIntrinsic((Intrinsics) value);
        }
        return value;
    }

    private enum StaticMethodKind {
        Invalid, Spreader, ObjectArray
    }

    private static StaticMethodKind staticMethodKind(MethodType type) {
        int pcount = type.parameterCount();
        if (pcount < 2)
            return StaticMethodKind.Invalid;
        Class<?>[] params = type.parameterArray();
        int p = 0;
        // first two parameters are (ExecutionContext, Object=ThisValue)
        if (!(ExecutionContext.class.equals(params[p++]) && Object.class.equals(params[p++]))) {
            return StaticMethodKind.Invalid;
        }
        // always required to return Object (for now at least)
        if (!Object.class.equals(type.returnType())) {
            return StaticMethodKind.Invalid;
        }
        if (p + 1 == pcount && Object[].class.equals(params[p])) {
            // (Realm, Object, Object[]) case
            return StaticMethodKind.ObjectArray;
        }
        // otherwise all trailing arguments need to be of type Object
        for (; p < pcount; ++p) {
            if (Object.class.equals(params[p])) {
                continue;
            }
            if (p + 1 == pcount && Object[].class.equals(params[p])) {
                continue;
            }
            return StaticMethodKind.Invalid;
        }
        return StaticMethodKind.Spreader;
    }

    private static Object[] methodDefaults(Method method, int fixedArguments, int actual) {
        Object[] defaults = null;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int parameter = 0; parameter < actual; ++parameter) {
            Annotation[] annotations = parameterAnnotations[parameter + fixedArguments];
            for (Annotation annotation : annotations) {
                Class<? extends Annotation> type = annotation.annotationType();
                if (type == Optional.class) {
                    Optional optional = (Optional) annotation;
                    if (defaults == null) {
                        defaults = new Object[actual];
                        Arrays.fill(defaults, UNDEFINED);
                    }
                    defaults[parameter] = Optional.Default.defaultValue(optional);
                }
            }
        }
        return defaults;
    }

    private static PropertyDescriptor propertyDescriptor(Object value, Attributes attrs) {
        return new PropertyDescriptor(value, attrs.writable(), attrs.enumerable(),
                attrs.configurable());
    }

    private static PropertyDescriptor propertyDescriptor(Callable getter, Callable setter,
            Attributes attrs) {
        return new PropertyDescriptor(getter, setter, attrs.enumerable(), attrs.configurable());
    }
}
