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
import java.util.Objects;
import java.util.Map.Entry;

import com.github.anba.es6draft.repl.StopExecutionException;
import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Utility class to set-up initial properties for objects
 */
public final class Properties {
    private Properties() {
    }

    /**
     * Compatiblity extension marker
     */
    @Documented
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface CompatibilityExtension {
        CompatibilityOption value();
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
        CompatibilityExtension extension;
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
        if (holder.getName().startsWith(INTERNAL_PACKAGE)) {
            assert owner instanceof OrdinaryObject;
            createInternalProperties((OrdinaryObject) owner, cx.getRealm(), holder);
        } else {
            createExternalProperties(owner, cx.getRealm(), holder);
        }
    }

    private static final String INTERNAL_PACKAGE = "com.github.anba.es6draft.runtime.objects.";

    public static final class Converter {
        private final MethodHandle ToBooleanMH;
        private final MethodHandle ToStringMH;
        private final MethodHandle ToFlatStringMH;
        private final MethodHandle ToNumberMH;
        private final MethodHandle ToObjectMH;
        private final MethodHandle ToCallableMH;
        private final MethodHandle ToBooleanArrayMH;
        private final MethodHandle ToStringArrayMH;
        private final MethodHandle ToFlatStringArrayMH;
        private final MethodHandle ToNumberArrayMH;
        private final MethodHandle ToObjectArrayMH;
        private final MethodHandle ToCallableArrayMH;
        private final MethodHandle ToScriptExceptionMH;

        Converter(ExecutionContext cx) {
            ToBooleanMH = _ToBooleanMH;
            ToStringMH = MethodHandles.insertArguments(_ToStringMH, 0, cx);
            ToFlatStringMH = MethodHandles.insertArguments(_ToFlatStringMH, 0, cx);
            ToNumberMH = MethodHandles.insertArguments(_ToNumberMH, 0, cx);
            ToObjectMH = MethodHandles.insertArguments(_ToObjectMH, 0, cx);
            ToCallableMH = MethodHandles.insertArguments(_ToCallableMH, 0, cx);

            ToBooleanArrayMH = _ToBooleanArrayMH;
            ToStringArrayMH = MethodHandles.insertArguments(_ToStringArrayMH, 0, cx);
            ToFlatStringArrayMH = MethodHandles.insertArguments(_ToFlatStringArrayMH, 0, cx);
            ToNumberArrayMH = MethodHandles.insertArguments(_ToNumberArrayMH, 0, cx);
            ToObjectArrayMH = MethodHandles.insertArguments(_ToObjectArrayMH, 0, cx);
            ToCallableArrayMH = MethodHandles.insertArguments(_ToCallableArrayMH, 0, cx);

            ToScriptExceptionMH = MethodHandles.insertArguments(_ToScriptExceptionMH, 0, cx);
        }

        private MethodHandle filterFor(Class<?> c) {
            if (c == Object.class) {
                return null;
            } else if (c == Double.TYPE) {
                return ToNumberMH;
            } else if (c == Boolean.TYPE) {
                return ToBooleanMH;
            } else if (c == String.class) {
                return ToFlatStringMH;
            } else if (c == CharSequence.class) {
                return ToStringMH;
            } else if (c == ScriptObject.class) {
                return ToObjectMH;
            } else if (c == Callable.class) {
                return ToCallableMH;
            }
            throw new IllegalArgumentException();
        }

        private MethodHandle arrayfilterFor(Class<?> c) {
            assert c.isArray();
            c = c.getComponentType();
            if (c == Object.class) {
                return null;
            } else if (c == Double.TYPE) {
                return ToNumberArrayMH;
            } else if (c == Boolean.TYPE) {
                return ToBooleanArrayMH;
            } else if (c == String.class) {
                return ToFlatStringArrayMH;
            } else if (c == CharSequence.class) {
                return ToStringArrayMH;
            } else if (c == ScriptObject.class) {
                return ToObjectArrayMH;
            } else if (c == Callable.class) {
                return ToCallableArrayMH;
            }
            throw new IllegalArgumentException();
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

        private static final MethodHandle _ToCallableMH;
        static {
            Lookup lookup = MethodHandles.publicLookup();
            try {
                MethodHandle mh = lookup
                        .findStatic(ScriptRuntime.class, "CheckCallable", MethodType.methodType(
                                Callable.class, Object.class, ExecutionContext.class));
                _ToCallableMH = MethodHandles
                        .permuteArguments(mh, MethodType.methodType(Callable.class,
                                ExecutionContext.class, Object.class), 1, 0);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private static final MethodHandle _ToBooleanArrayMH;
        private static final MethodHandle _ToStringArrayMH;
        private static final MethodHandle _ToFlatStringArrayMH;
        private static final MethodHandle _ToNumberArrayMH;
        private static final MethodHandle _ToObjectArrayMH;
        private static final MethodHandle _ToCallableArrayMH;
        static {
            Lookup lookup = MethodHandles.publicLookup();
            try {
                _ToStringArrayMH = lookup.findStatic(Converter.class, "ToString", MethodType
                        .methodType(CharSequence[].class, ExecutionContext.class, Object[].class));
                _ToFlatStringArrayMH = lookup.findStatic(Converter.class, "ToFlatString",
                        MethodType.methodType(String[].class, ExecutionContext.class,
                                Object[].class));
                _ToNumberArrayMH = lookup.findStatic(Converter.class, "ToNumber", MethodType
                        .methodType(double[].class, ExecutionContext.class, Object[].class));
                _ToBooleanArrayMH = lookup.findStatic(Converter.class, "ToBoolean",
                        MethodType.methodType(boolean[].class, Object[].class));
                _ToObjectArrayMH = lookup.findStatic(Converter.class, "ToObject", MethodType
                        .methodType(ScriptObject[].class, ExecutionContext.class, Object[].class));
                _ToCallableArrayMH = lookup.findStatic(Converter.class, "ToCallable", MethodType
                        .methodType(Callable[].class, ExecutionContext.class, Object[].class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public static boolean[] ToBoolean(Object[] source) {
            boolean[] target = new boolean[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = AbstractOperations.ToBoolean(source[i]);
            }
            return target;
        }

        public static CharSequence[] ToString(ExecutionContext cx, Object[] source) {
            CharSequence[] target = new CharSequence[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = AbstractOperations.ToString(cx, source[i]);
            }
            return target;
        }

        public static String[] ToFlatString(ExecutionContext cx, Object[] source) {
            String[] target = new String[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = AbstractOperations.ToFlatString(cx, source[i]);
            }
            return target;
        }

        public static double[] ToNumber(ExecutionContext cx, Object[] source) {
            double[] target = new double[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = AbstractOperations.ToNumber(cx, source[i]);
            }
            return target;
        }

        public static ScriptObject[] ToObject(ExecutionContext cx, Object[] source) {
            ScriptObject[] target = new ScriptObject[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = AbstractOperations.ToObject(cx, source[i]);
            }
            return target;
        }

        public static Callable[] ToCallable(ExecutionContext cx, Object[] source) {
            Callable[] target = new Callable[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = ScriptRuntime.CheckCallable(source[i], cx);
            }
            return target;
        }

        private static final MethodHandle _ToScriptExceptionMH;
        static {
            Lookup lookup = MethodHandles.publicLookup();
            try {
                _ToScriptExceptionMH = lookup.findStatic(Converter.class, "ToScriptException",
                        MethodType.methodType(ScriptException.class, ExecutionContext.class,
                                Exception.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public static ScriptException ToScriptException(ExecutionContext cx, Exception cause) {
            if (cause instanceof StopExecutionException)
                throw (StopExecutionException) cause;
            if (cause instanceof ScriptException)
                return (ScriptException) cause;
            String info = Objects.toString(cause.getMessage(), cause.getClass().getSimpleName());
            return Errors.throwInternalError(cx, Messages.Key.InternalError, info);
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

    private static void createInternalProperties(OrdinaryObject owner, Realm realm, Class<?> holder) {
        ObjectLayout layout = internalLayouts.get(holder);
        if (layout.extension != null && !realm.isEnabled(layout.extension.value())) {
            // return if extension is not enabled
            return;
        }
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
            layout.extension = holder.getAnnotation(CompatibilityExtension.class);
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
        boolean varargs = unreflect.isVarargsCollector();

        if (varargs) {
            // bindTo() removes the var-args collector flag, restore it
            MethodType type = unreflect.type();
            Class<?> parameterType = type.parameterType(type.parameterCount() - 1);
            handle = handle.asVarargsCollector(parameterType);
        }

        MethodType type = handle.type();
        int pcount = type.parameterCount();
        int actual = type.parameterCount() - (varargs ? 1 : 0);
        Class<?>[] params = type.parameterArray();
        MethodHandle[] filters = new MethodHandle[pcount];
        for (int p = 0; p < actual; ++p) {
            filters[p] = converter.filterFor(params[p]);
        }
        if (varargs) {
            filters[pcount - 1] = converter.arrayfilterFor(params[pcount - 1]);
        }
        handle = MethodHandles.filterArguments(handle, 0, filters);

        Class<?> returnType = type.returnType();
        if (returnType == Double.TYPE || returnType == Boolean.TYPE || returnType == String.class
                || returnType == CharSequence.class
                || ScriptObject.class.isAssignableFrom(returnType)) {
            handle = MethodHandles.explicitCastArguments(handle,
                    handle.type().changeReturnType(Object.class));
        } else if (returnType == Void.TYPE) {
            handle = MethodHandles.filterReturnValue(handle,
                    MethodHandles.constant(Object.class, UNDEFINED));
        } else if (returnType != Object.class) {
            throw new IllegalArgumentException();
        }

        MethodHandle filter;
        if (varargs) {
            filter = MethodHandles.insertArguments(ParameterFilter.filterVarArgs, 0, actual);
        } else {
            filter = filter(actual);
        }

        MethodHandle spreader = MethodHandles.spreadInvoker(handle.type(), 0);
        handle = MethodHandles.insertArguments(spreader, 0, handle);
        handle = MethodHandles.filterArguments(handle, 0, filter);
        handle = MethodHandles.dropArguments(handle, 0, Object.class);

        MethodHandle thrower = MethodHandles.throwException(handle.type().returnType(),
                ScriptException.class);
        thrower = MethodHandles.filterArguments(thrower, 0, converter.ToScriptExceptionMH);
        handle = MethodHandles.catchException(handle, Exception.class, thrower);

        return handle;
    }

    private static MethodHandle getStaticMethodHandle(Lookup lookup, Method method)
            throws IllegalAccessException {
        // check: (ExecutionContext, Object, Object[]) -> Object
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

    private static final MethodHandle filters[] = new MethodHandle[5];

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

    private static void createPrototype(OrdinaryObject owner, Realm realm, Prototype proto,
            Object rawValue) {
        Object value = resolveValue(realm, rawValue);
        assert value == null || value instanceof ScriptObject;
        ScriptObject prototype = (ScriptObject) value;
        owner.setPrototype(prototype);
    }

    private static void createValue(OrdinaryObject owner, Realm realm, Value val, Object rawValue) {
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

    private static void createFunction(OrdinaryObject owner, Realm realm, Function function,
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

    private static void createAccessor(OrdinaryObject owner, Realm realm, Accessor accessor,
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

    private static void completeAccessors(OrdinaryObject owner, Realm realm,
            Map<String, PropertyDescriptor> accessors1,
            Map<BuiltinSymbol, PropertyDescriptor> accessors2) {
        if (accessors1 != null) {
            ExecutionContext cx = realm.defaultContext();
            for (Entry<String, PropertyDescriptor> entry : accessors1.entrySet()) {
                owner.defineOwnProperty(cx, entry.getKey(), entry.getValue());
            }
        }
        if (accessors2 != null) {
            ExecutionContext cx = realm.defaultContext();
            for (Entry<BuiltinSymbol, PropertyDescriptor> entry : accessors2.entrySet()) {
                owner.defineOwnProperty(cx, entry.getKey().get(), entry.getValue());
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
