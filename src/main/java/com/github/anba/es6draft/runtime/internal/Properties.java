/**
 * Copyright (c) 2012-2014 André Bargull
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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Objects;

import com.github.anba.es6draft.repl.global.StopExecutionException;
import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.NativeConstructor;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction.NativeFunctionId;
import com.github.anba.es6draft.runtime.types.builtins.NativeTailCallFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
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
         * Returns the function name.
         * 
         * @return the function name
         */
        String name();

        /**
         * Returns the function symbol.
         * 
         * @return the function symbol
         */
        BuiltinSymbol symbol() default BuiltinSymbol.NONE;

        /**
         * Returns the function arity.
         * 
         * @return the function arity
         */
        int arity();

        /**
         * Returns the function attributes, defaults to <code>{[[Writable]]: true, [[Enumerable]]:
         * false, [[Configurable]]: true}</code>.
         * 
         * @return the function attributes
         */
        Attributes attributes() default @Attributes(writable = true, enumerable = false,
                configurable = true);

        /**
         * Returns the optional native function identifier.
         * 
         * @return the native function identifier
         */
        NativeFunctionId nativeId() default NativeFunctionId.None;
    }

    /**
     * Built-in value property
     */
    @Documented
    @Target({ ElementType.FIELD, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Value {
        /**
         * Returns the value name.
         * 
         * @return the value name
         */
        String name();

        /**
         * Returns the value symbol.
         * 
         * @return the value symbol
         */
        BuiltinSymbol symbol() default BuiltinSymbol.NONE;

        /**
         * Returns the value attributes, defaults to <code>{[[Writable]]: true, [[Enumerable]]:
         * false, [[Configurable]]: true}</code>.
         * 
         * @return the value attributes
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
         * Returns the accessor name.
         * 
         * @return the accessor name
         */
        String name();

        /**
         * Returns the accessor symbol.
         * 
         * @return the accessor symbol
         */
        BuiltinSymbol symbol() default BuiltinSymbol.NONE;

        enum Type {
            Getter, Setter
        }

        /**
         * Returns the accessor type.
         * 
         * @return the accessor type
         */
        Type type();

        /**
         * Returns the accessor attributes, defaults to <code>{[[Enumerable]]:
         * false, [[Configurable]]: true}</code>.
         * 
         * @return the accessor attributes
         */
        Attributes attributes() default @Attributes(writable = false /*unused*/,
                enumerable = false, configurable = true);
    }

    /**
     * Built-in function property as an alias function
     */
    @Documented
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface AliasFunction {
        /**
         * Returns the function name.
         * 
         * @return the function name
         */
        String name();

        /**
         * Returns the function symbol.
         * 
         * @return the function symbol
         */
        BuiltinSymbol symbol() default BuiltinSymbol.NONE;

        /**
         * Returns the function attributes, defaults to <code>{[[Writable]]: true, [[Enumerable]]:
         * false, [[Configurable]]: true}</code>.
         * 
         * @return the function attributes
         */
        Attributes attributes() default @Attributes(writable = true, enumerable = false,
                configurable = true);
    }

    /**
     * Built-in function property as an alias function
     */
    @Documented
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface AliasFunctions {
        AliasFunction[] value();
    }

    /**
     * Built-in function property with tail-call
     */
    @Documented
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface TailCall {
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
        /**
         * Returns the default runtime type.
         * 
         * @return the default type
         */
        Default value() default Default.Undefined;

        /**
         * Returns the default boolean value, only applicable if {@code value()} is
         * {@link Default#Boolean}.
         * 
         * @return the default boolean value
         */
        boolean booleanValue() default false;

        /**
         * Returns the default string value, only applicable if {@code value()} is
         * {@link Default#String}.
         * 
         * @return the default string value
         */
        String stringValue() default "";

        /**
         * Returns the default number value, only applicable if {@code value()} is
         * {@link Default#Number}.
         * 
         * @return the default number value
         */
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
            return createExternalObjectLayout(type, false);
        }
    };

    private static ClassValue<ObjectLayout> externalClassLayouts = new ClassValue<ObjectLayout>() {
        @Override
        protected ObjectLayout computeValue(Class<?> type) {
            return createExternalObjectLayout(type, true);
        }
    };

    private static final class ObjectLayout {
        CompatibilityExtension extension;
        Prototype proto;
        Object protoValue;
        LinkedHashMap<Value, Object> values;
        LinkedHashMap<Function, MethodHandle> functions;
        LinkedHashMap<Function, MethodHandle> tcfunctions;
        LinkedHashMap<Accessor, MethodHandle> accessors;
        ArrayList<Entry<AliasFunction, Function>> aliases;
    }

    /**
     * Sets the {@link Prototype} and creates own properties for {@link Value}, {@link Function} and
     * {@link Accessor} fields.
     * 
     * @param cx
     *            the execution context
     * @param target
     *            the object instance
     * @param holder
     *            the class which holds the properties
     */
    public static void createProperties(ExecutionContext cx, OrdinaryObject target, Class<?> holder) {
        assert holder.getName().startsWith(INTERNAL_PACKAGE);
        createInternalProperties(cx, target, holder);
    }

    /**
     * Sets the {@link Prototype} and creates own properties for {@link Value}, {@link Function} and
     * {@link Accessor} fields.
     * 
     * @param <OWNER>
     *            the owner class type
     * @param cx
     *            the execution context
     * @param target
     *            the target object instance
     * @param owner
     *            the owner object instance
     * @param holder
     *            the class which holds the properties
     */
    public static <OWNER> void createProperties(ExecutionContext cx, ScriptObject target,
            OWNER owner, Class<OWNER> holder) {
        assert !holder.getName().startsWith(INTERNAL_PACKAGE);
        createExternalProperties(cx, target, owner, holder);
    }

    /**
     * Creates a new native script class.
     * 
     * @param cx
     *            the execution context
     * @param className
     *            the class-name
     * @param constructorProperties
     *            the class which holds the constructor properties
     * @param prototypeProperties
     *            the class which holds the prototype properties
     * @return
     */
    public static Constructor createClass(ExecutionContext cx, String className,
            Class<?> constructorProperties, Class<?> prototypeProperties) {
        assert !constructorProperties.getName().startsWith(INTERNAL_PACKAGE);
        assert !prototypeProperties.getName().startsWith(INTERNAL_PACKAGE);
        return createExternalClass(cx, className, constructorProperties, prototypeProperties);
    }

    private static final String INTERNAL_PACKAGE = "com.github.anba.es6draft.runtime.objects.";

    @SuppressWarnings("unused")
    private static final class Converter {
        private final ExecutionContext cx;
        private final MethodHandle toScriptException;

        Converter(ExecutionContext cx) {
            this.cx = cx;
            this.toScriptException = MethodHandles.insertArguments(ToScriptExceptionMH, 0, cx);
        }

        MethodHandle toScriptException() {
            return toScriptException;
        }

        MethodHandle filterFor(Class<?> c) {
            if (c == Object.class) {
                return null;
            } else if (c == Double.TYPE) {
                return MethodHandles.insertArguments(ToNumberMH, 0, cx);
            } else if (c == Boolean.TYPE) {
                return ToBooleanMH;
            } else if (c == String.class) {
                return MethodHandles.insertArguments(ToFlatStringMH, 0, cx);
            } else if (c == CharSequence.class) {
                return MethodHandles.insertArguments(ToStringMH, 0, cx);
            } else if (c == ScriptObject.class) {
                return MethodHandles.insertArguments(ToObjectMH, 0, cx);
            } else if (c == Callable.class) {
                return MethodHandles.insertArguments(ToCallableMH, 0, cx);
            } else if (ScriptObject.class.isAssignableFrom(c)) {
                MethodHandle test = IsInstanceMH.bindTo(c);
                MethodHandle target = MethodHandles.identity(c);
                target = target.asType(target.type().changeParameterType(0, Object.class));
                MethodHandle fallback = MethodHandles.insertArguments(ThrowTypeErrorMH, 0, cx);
                fallback = fallback.asType(fallback.type().changeReturnType(c));
                return MethodHandles.guardWithTest(test, target, fallback);
            }
            throw new IllegalArgumentException(c.toString());
        }

        MethodHandle arrayFilterFor(Class<?> c) {
            assert c.isArray();
            c = c.getComponentType();
            if (c == Object.class) {
                return null;
            } else if (c == Double.TYPE) {
                return MethodHandles.insertArguments(ToNumberArrayMH, 0, cx);
            } else if (c == Boolean.TYPE) {
                return ToBooleanArrayMH;
            } else if (c == String.class) {
                return MethodHandles.insertArguments(ToFlatStringArrayMH, 0, cx);
            } else if (c == CharSequence.class) {
                return MethodHandles.insertArguments(ToStringArrayMH, 0, cx);
            } else if (c == ScriptObject.class) {
                return MethodHandles.insertArguments(ToObjectArrayMH, 0, cx);
            } else if (c == Callable.class) {
                return MethodHandles.insertArguments(ToCallableArrayMH, 0, cx);
            }
            throw new IllegalArgumentException(c.toString());
        }

        private static final MethodHandle ToBooleanMH, ToStringMH, ToFlatStringMH, ToNumberMH,
                ToObjectMH, ToCallableMH, IsInstanceMH;
        static {
            MethodLookup lookup = new MethodLookup(MethodHandles.publicLookup());
            ToStringMH = lookup
                    .findStatic(AbstractOperations.class, "ToString", MethodType.methodType(
                            CharSequence.class, ExecutionContext.class, Object.class));
            ToFlatStringMH = lookup.findStatic(AbstractOperations.class, "ToFlatString",
                    MethodType.methodType(String.class, ExecutionContext.class, Object.class));
            ToNumberMH = lookup.findStatic(AbstractOperations.class, "ToNumber",
                    MethodType.methodType(Double.TYPE, ExecutionContext.class, Object.class));
            ToBooleanMH = lookup.findStatic(AbstractOperations.class, "ToBoolean",
                    MethodType.methodType(Boolean.TYPE, Object.class));
            ToObjectMH = lookup
                    .findStatic(AbstractOperations.class, "ToObject", MethodType.methodType(
                            ScriptObject.class, ExecutionContext.class, Object.class));

            ToCallableMH = MethodHandles.permuteArguments(lookup.findStatic(ScriptRuntime.class,
                    "CheckCallable",
                    MethodType.methodType(Callable.class, Object.class, ExecutionContext.class)),
                    MethodType.methodType(Callable.class, ExecutionContext.class, Object.class), 1,
                    0);

            IsInstanceMH = lookup.findVirtual(Class.class, "isInstance",
                    MethodType.methodType(Boolean.TYPE, Object.class));
        }

        private static final MethodHandle ToBooleanArrayMH, ToStringArrayMH, ToFlatStringArrayMH,
                ToNumberArrayMH, ToObjectArrayMH, ToCallableArrayMH, ToScriptExceptionMH,
                ThrowTypeErrorMH;
        static {
            MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
            ToStringArrayMH = lookup.findStatic(Converter.class, "ToString", MethodType.methodType(
                    CharSequence[].class, ExecutionContext.class, Object[].class));
            ToFlatStringArrayMH = lookup.findStatic(Converter.class, "ToFlatString",
                    MethodType.methodType(String[].class, ExecutionContext.class, Object[].class));
            ToNumberArrayMH = lookup.findStatic(Converter.class, "ToNumber",
                    MethodType.methodType(double[].class, ExecutionContext.class, Object[].class));
            ToBooleanArrayMH = lookup.findStatic(Converter.class, "ToBoolean",
                    MethodType.methodType(boolean[].class, Object[].class));
            ToObjectArrayMH = lookup.findStatic(Converter.class, "ToObject", MethodType.methodType(
                    ScriptObject[].class, ExecutionContext.class, Object[].class));
            ToCallableArrayMH = lookup
                    .findStatic(Converter.class, "ToCallable", MethodType.methodType(
                            Callable[].class, ExecutionContext.class, Object[].class));
            ToScriptExceptionMH = lookup.findStatic(Converter.class, "ToScriptException",
                    MethodType.methodType(ScriptException.class, ExecutionContext.class,
                            Exception.class));
            ThrowTypeErrorMH = lookup.findStatic(Converter.class, "throwTypeError",
                    MethodType.methodType(void.class, ExecutionContext.class, Object.class));
        }

        private static boolean[] ToBoolean(Object[] source) {
            boolean[] target = new boolean[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = AbstractOperations.ToBoolean(source[i]);
            }
            return target;
        }

        private static CharSequence[] ToString(ExecutionContext cx, Object[] source) {
            CharSequence[] target = new CharSequence[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = AbstractOperations.ToString(cx, source[i]);
            }
            return target;
        }

        private static String[] ToFlatString(ExecutionContext cx, Object[] source) {
            String[] target = new String[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = AbstractOperations.ToFlatString(cx, source[i]);
            }
            return target;
        }

        private static double[] ToNumber(ExecutionContext cx, Object[] source) {
            double[] target = new double[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = AbstractOperations.ToNumber(cx, source[i]);
            }
            return target;
        }

        private static ScriptObject[] ToObject(ExecutionContext cx, Object[] source) {
            ScriptObject[] target = new ScriptObject[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = AbstractOperations.ToObject(cx, source[i]);
            }
            return target;
        }

        private static Callable[] ToCallable(ExecutionContext cx, Object[] source) {
            Callable[] target = new Callable[source.length];
            for (int i = 0; i < target.length; i++) {
                target[i] = ScriptRuntime.CheckCallable(source[i], cx);
            }
            return target;
        }

        private static ScriptException ToScriptException(ExecutionContext cx, Exception cause) {
            if (cause instanceof StopExecutionException)
                throw (StopExecutionException) cause;
            if (cause instanceof ScriptException)
                return (ScriptException) cause;
            if (cause instanceof InternalException)
                return ((InternalException) cause).toScriptException(cx);
            String info = Objects.toString(cause.getMessage(), cause.getClass().getSimpleName());
            return Errors.newInternalError(cx, Messages.Key.InternalError, info);
        }

        private static void throwTypeError(ExecutionContext cx, Object ignore) {
            throw Errors.newTypeError(cx, Messages.Key.IncompatibleObject);
        }
    }

    private static <OWNER> void createExternalProperties(ExecutionContext cx, ScriptObject target,
            OWNER owner, Class<OWNER> holder) {
        ObjectLayout layout = externalLayouts.get(holder);
        Converter converter = new Converter(cx);
        if (layout.functions != null) {
            createExternalFunctions(cx, target, owner, layout, converter);
        }
        if (layout.accessors != null) {
            createExternalAccessors(cx, target, owner, layout, converter);
        }
    }

    private static <OWNER> void createExternalFunctions(ExecutionContext cx, ScriptObject target,
            OWNER owner, ObjectLayout layout, Converter converter) {
        for (Entry<Function, MethodHandle> entry : layout.functions.entrySet()) {
            MethodHandle handle = getInstanceMethodHandle(cx, converter, entry.getValue(), owner);
            createExternalFunction(cx, target, entry.getKey(), handle);
        }
    }

    private static <OWNER> void createExternalAccessors(ExecutionContext cx, ScriptObject target,
            OWNER owner, ObjectLayout layout, Converter converter) {
        LinkedHashMap<String, PropertyDescriptor> stringProps = new LinkedHashMap<>();
        EnumMap<BuiltinSymbol, PropertyDescriptor> symbolProps = new EnumMap<>(BuiltinSymbol.class);
        for (Entry<Accessor, MethodHandle> entry : layout.accessors.entrySet()) {
            MethodHandle handle = getInstanceMethodHandle(cx, converter, entry.getValue(), owner);
            createExternalAccessor(cx, entry.getKey(), handle, stringProps, symbolProps);
        }
        defineProperties(cx, target, stringProps, symbolProps);
    }

    private static Constructor createExternalClass(ExecutionContext cx, String className,
            Class<?> constructorProperties, Class<?> prototypeProperties) {
        ObjectLayout ctorLayout = externalClassLayouts.get(constructorProperties);
        ObjectLayout protoLayout = externalClassLayouts.get(prototypeProperties);
        Converter converter = new Converter(cx);

        ScriptObject[] objects = ScriptRuntime.getDefaultClassProto(cx);
        OrdinaryObject proto = (OrdinaryObject) objects[0];
        ScriptObject constructorParent = objects[1];
        assert constructorParent == cx.getIntrinsic(Intrinsics.FunctionPrototype);

        OrdinaryObject constructor = createConstructor(cx, className, proto, constructorParent,
                converter, protoLayout);
        assert constructor instanceof Constructor;
        if (ctorLayout.functions != null) {
            createExternalFunctions(cx, constructor, ctorLayout, converter);
        }
        if (ctorLayout.accessors != null) {
            createExternalAccessors(cx, constructor, ctorLayout, converter);
        }
        if (protoLayout.functions != null) {
            createExternalFunctions(cx, proto, protoLayout, converter);
        }
        if (protoLayout.accessors != null) {
            createExternalAccessors(cx, proto, protoLayout, converter);
        }
        return (Constructor) constructor;
    }

    private static OrdinaryObject createConstructor(ExecutionContext cx, String className,
            OrdinaryObject proto, ScriptObject constructorParent, Converter converter,
            ObjectLayout layout) {
        Entry<Function, MethodHandle> constructorEntry = findConstructor(layout);
        if (constructorEntry != null) {
            // User supplied method, perform manual ClassDefinitionEvaluation for constructors
            Function function = constructorEntry.getKey();
            MethodHandle unreflect = constructorEntry.getValue();
            MethodHandle mh = getStaticMethodHandle(cx, converter, unreflect);
            NativeConstructor constructor = new NativeConstructor(cx.getRealm(), className,
                    function.arity(), mh);
            constructor.defineOwnProperty(cx, "prototype", new PropertyDescriptor(proto, false,
                    false, false));
            proto.defineOwnProperty(cx, "constructor", new PropertyDescriptor(constructor, true,
                    false, true));
            return constructor;
        }
        // Create default constructor
        RuntimeInfo.Function fd = ScriptRuntime.CreateDefaultEmptyConstructor();
        OrdinaryFunction constructor = ScriptRuntime.EvaluateConstructorMethod(constructorParent,
                proto, fd, cx);
        OrdinaryFunction.SetFunctionName(constructor, className);
        return constructor;
    }

    private static Entry<Function, MethodHandle> findConstructor(ObjectLayout layout) {
        if (layout.functions != null) {
            for (Entry<Function, MethodHandle> entry : layout.functions.entrySet()) {
                if ("constructor".equals(entry.getKey().name())) {
                    return entry;
                }
            }
        }
        return null;
    }

    private static void createExternalFunctions(ExecutionContext cx, OrdinaryObject target,
            ObjectLayout layout, Converter converter) {
        for (Entry<Function, MethodHandle> entry : layout.functions.entrySet()) {
            MethodHandle handle = getStaticMethodHandle(cx, converter, entry.getValue());
            createExternalFunction(cx, target, entry.getKey(), handle);
        }
    }

    private static void createExternalAccessors(ExecutionContext cx, OrdinaryObject target,
            ObjectLayout layout, Converter converter) {
        LinkedHashMap<String, PropertyDescriptor> stringProps = new LinkedHashMap<>();
        EnumMap<BuiltinSymbol, PropertyDescriptor> symbolProps = new EnumMap<>(BuiltinSymbol.class);
        for (Entry<Accessor, MethodHandle> entry : layout.accessors.entrySet()) {
            MethodHandle handle = getStaticMethodHandle(cx, converter, entry.getValue());
            createExternalAccessor(cx, entry.getKey(), handle, stringProps, symbolProps);
        }
        defineProperties(cx, target, stringProps, symbolProps);
    }

    private static void createExternalFunction(ExecutionContext cx, ScriptObject target,
            Function function, MethodHandle handle) {
        String name = function.name();
        NativeFunction fun = new NativeFunction(cx.getRealm(), name, function.arity(), handle);
        defineProperty(cx, target, name, function.symbol(), function.attributes(), fun);
    }

    private static void createExternalAccessor(ExecutionContext cx, Accessor accessor,
            MethodHandle mh, LinkedHashMap<String, PropertyDescriptor> stringProps,
            EnumMap<BuiltinSymbol, PropertyDescriptor> symbolProps) {
        NativeFunction fun = createAccessor(cx, accessor, mh);
        PropertyDescriptor desc = createAccessorDescriptor(accessor, stringProps, symbolProps);
        if (!isCompatibleDescriptor(accessor, desc)) {
            throw new IllegalArgumentException();
        }
        if (accessor.type() == Accessor.Type.Getter) {
            desc.setGetter(fun);
        } else {
            desc.setSetter(fun);
        }
    }

    private static ObjectLayout createExternalObjectLayout(Class<?> holder, boolean staticMethods) {
        try {
            ObjectLayout layout = new ObjectLayout();
            Lookup lookup = MethodHandles.publicLookup();
            for (Method method : holder.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) != staticMethods)
                    continue;
                Function function = method.getAnnotation(Function.class);
                Accessor accessor = method.getAnnotation(Accessor.class);
                if (function != null && accessor != null) {
                    throw new IllegalArgumentException();
                }
                if (function != null) {
                    if (layout.functions == null) {
                        layout.functions = new LinkedHashMap<>();
                    }
                    layout.functions.put(function, lookup.unreflect(method));
                }
                if (accessor != null) {
                    if (layout.accessors == null) {
                        layout.accessors = new LinkedHashMap<>();
                    }
                    layout.accessors.put(accessor, lookup.unreflect(method));
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
                Prototype prototype = field.getAnnotation(Prototype.class);
                assert value == null || prototype == null;

                if (value != null) {
                    if (layout.values == null) {
                        layout.values = new LinkedHashMap<>();
                    }
                    layout.values.put(value, getRawValue(field));
                }
                if (prototype != null) {
                    assert !hasProto && (hasProto = true);
                    layout.proto = prototype;
                    layout.protoValue = getRawValue(field);
                }
            }
            for (Method method : holder.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()))
                    continue;
                Function function = method.getAnnotation(Function.class);
                Accessor accessor = method.getAnnotation(Accessor.class);
                AliasFunction alias = method.getAnnotation(AliasFunction.class);
                AliasFunctions aliases = method.getAnnotation(AliasFunctions.class);
                TailCall tailCall = method.getAnnotation(TailCall.class);
                Value value = method.getAnnotation(Value.class);
                assert function == null || (accessor == null && value == null);
                assert accessor == null || (function == null && value == null);
                assert value == null || (function == null && accessor == null);
                assert alias == null || function != null;
                assert aliases == null || function != null;
                assert tailCall == null || function != null;

                if (function != null && tailCall == null) {
                    if (layout.functions == null) {
                        layout.functions = new LinkedHashMap<>();
                    }
                    layout.functions.put(function, getStaticMethodHandle(lookup, method));
                }
                if (function != null && tailCall != null) {
                    if (layout.tcfunctions == null) {
                        layout.tcfunctions = new LinkedHashMap<>();
                    }
                    layout.tcfunctions.put(function, getStaticMethodHandle(lookup, method));
                }
                if (accessor != null) {
                    if (layout.accessors == null) {
                        layout.accessors = new LinkedHashMap<>();
                    }
                    layout.accessors.put(accessor, getStaticMethodHandle(lookup, method));
                }
                if (value != null) {
                    if (layout.values == null) {
                        layout.values = new LinkedHashMap<>();
                    }
                    layout.values.put(value, getComputedValueMethodHandle(lookup, method));
                }
                if (alias != null) {
                    if (layout.aliases == null) {
                        layout.aliases = new ArrayList<>();
                    }
                    layout.aliases.add(new SimpleImmutableEntry<>(alias, function));
                }
                if (aliases != null) {
                    if (layout.aliases == null) {
                        layout.aliases = new ArrayList<>();
                    }
                    for (AliasFunction a : aliases.value()) {
                        layout.aliases.add(new SimpleImmutableEntry<>(a, function));
                    }
                }
            }
            return layout;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Object getRawValue(Field field) throws IllegalAccessException {
        assert Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers());
        return field.get(null);
    }

    private static <OWNER> MethodHandle getInstanceMethodHandle(ExecutionContext cx,
            Converter converter, MethodHandle unreflect, OWNER owner) {
        final int fixedArguments = 0;
        // var-args collector flag is not preserved when applying method handle combinators
        boolean varargs = unreflect.isVarargsCollector();

        MethodHandle handle = unreflect.bindTo(owner);
        handle = bindContext(handle, cx);
        handle = convertTypes(handle, fixedArguments, varargs, converter);
        handle = catchExceptions(handle, converter);
        handle = toCanonical(handle, fixedArguments, varargs, null);
        handle = MethodHandles.dropArguments(handle, 0, Object.class);

        assert handle.type().parameterCount() == 2;
        assert handle.type().parameterType(0) == Object.class;
        assert handle.type().parameterType(1) == Object[].class;
        assert handle.type().returnType() == Object.class;

        return handle;
    }

    private static MethodHandle getStaticMethodHandle(ExecutionContext cx, Converter converter,
            MethodHandle unreflect) {
        final int fixedArguments = 1;
        // var-args collector flag is not preserved when applying method handle combinators
        boolean varargs = unreflect.isVarargsCollector();

        MethodHandle handle = unreflect;
        handle = bindContext(handle, cx);
        if (handle.type().parameterCount() == 0) {
            handle = MethodHandles.dropArguments(handle, 0, Object.class);
        } else if (handle.type().parameterType(0) != Object.class) {
            handle = MethodHandles.filterArguments(handle, 0,
                    converter.filterFor(handle.type().parameterType(0)));
        }
        handle = convertTypes(handle, fixedArguments, varargs, converter);
        handle = catchExceptions(handle, converter);
        handle = toCanonical(handle, fixedArguments, varargs, null);

        assert handle.type().parameterCount() == 2;
        assert handle.type().parameterType(0) == Object.class;
        assert handle.type().parameterType(1) == Object[].class;
        assert handle.type().returnType() == Object.class;

        return handle;
    }

    private static MethodHandle bindContext(MethodHandle handle, ExecutionContext cx) {
        MethodType type = handle.type();
        if (type.parameterCount() > 0 && ExecutionContext.class.equals(type.parameterType(0))) {
            handle = MethodHandles.insertArguments(handle, 0, cx);
        }
        return handle;
    }

    private static MethodHandle convertTypes(MethodHandle handle, int fixedArguments,
            boolean varargs, Converter converter) {
        MethodType type = handle.type();
        int pcount = type.parameterCount();
        int actual = pcount - fixedArguments - (varargs ? 1 : 0);
        Class<?>[] params = type.parameterArray();
        MethodHandle[] filters = new MethodHandle[pcount];
        for (int p = 0; p < actual; ++p) {
            filters[fixedArguments + p] = converter.filterFor(params[fixedArguments + p]);
        }
        if (varargs) {
            filters[pcount - 1] = converter.arrayFilterFor(params[pcount - 1]);
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
            throw new IllegalArgumentException(returnType.toString());
        }
        return handle;
    }

    private static MethodHandle catchExceptions(MethodHandle handle, Converter converter) {
        MethodHandle thrower = MethodHandles.throwException(handle.type().returnType(),
                ScriptException.class);
        thrower = MethodHandles.filterArguments(thrower, 0, converter.toScriptException());
        return MethodHandles.catchException(handle, Exception.class, thrower);
    }

    private static MethodHandle getStaticMethodHandle(Lookup lookup, Method method)
            throws IllegalAccessException {
        // check: (ExecutionContext, Object, Object[]) -> Object
        MethodHandle handle = lookup.unreflect(method);
        switch (staticMethodKind(handle.type())) {
        case ObjectArray:
            // Already in (ExecutionContext, Object, Object[]) -> Object form
            return handle;
        case Spreader:
            // Convert (ExecutionContext, Object, ...) -> Object handle
            final int fixedArguments = 2;
            boolean varargs = handle.isVarargsCollector();
            return toCanonical(handle, fixedArguments, varargs, method);
        case Invalid:
        default:
            throw new IllegalArgumentException(handle.toString());
        }
    }

    private static MethodHandle toCanonical(MethodHandle handle, int fixedArguments,
            boolean varargs, Method method) {
        MethodType type = handle.type();
        int actual = type.parameterCount() - fixedArguments - (varargs ? 1 : 0);
        Object[] defaults = method != null ? methodDefaults(method, fixedArguments, actual) : null;
        MethodHandle filter = Parameters.filter(actual, varargs, defaults);
        MethodHandle spreader = MethodHandles.spreadInvoker(type, fixedArguments);
        spreader = MethodHandles.insertArguments(spreader, 0, handle);
        spreader = MethodHandles.filterArguments(spreader, fixedArguments, filter);
        return spreader;
    }

    private static MethodHandle getComputedValueMethodHandle(Lookup lookup, Method method)
            throws IllegalAccessException {
        // check: (ExecutionContext) -> Object
        MethodHandle handle = lookup.unreflect(method);
        MethodType type = handle.type();
        if (type.parameterCount() != 1 || !ExecutionContext.class.equals(type.parameterType(0))) {
            throw new IllegalArgumentException(handle.toString());
        }
        if (!Object.class.equals(type.returnType())) {
            throw new IllegalArgumentException(handle.toString());
        }
        return handle;
    }

    @SuppressWarnings("unused")
    private static final class Parameters {
        private Parameters() {
        }

        static MethodHandle filter(int actual, boolean varargs, Object[] defaults) {
            assert actual >= 0;
            if (defaults != null && varargs) {
                return MethodHandles.insertArguments(filterVarArgsDefaults, 0, actual, defaults);
            }
            if (defaults != null) {
                return MethodHandles.insertArguments(filterDefaults, 0, actual, defaults);
            }
            if (varargs) {
                return MethodHandles.insertArguments(filterVarArgs, 0, actual);
            }
            if (actual < filters.length) {
                MethodHandle f = filters[actual];
                return f != null ? f : (filters[actual] = MethodHandles.insertArguments(filter, 0,
                        actual));
            }
            return MethodHandles.insertArguments(filter, 0, actual);
        }

        private static final MethodHandle filters[] = new MethodHandle[5];
        private static final MethodHandle filterVarArgsDefaults;
        private static final MethodHandle filterDefaults;
        private static final MethodHandle filterVarArgs;
        private static final MethodHandle filter;
        static {
            MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
            filterVarArgsDefaults = lookup.findStatic(Parameters.class, "filterVarArgsDefaults",
                    MethodType.methodType(Object[].class, Integer.TYPE, Object[].class,
                            Object[].class));
            filterDefaults = lookup.findStatic(Parameters.class, "filterDefaults", MethodType
                    .methodType(Object[].class, Integer.TYPE, Object[].class, Object[].class));
            filterVarArgs = lookup.findStatic(Parameters.class, "filterVarArgs",
                    MethodType.methodType(Object[].class, Integer.TYPE, Object[].class));
            filter = lookup.findStatic(Parameters.class, "filter",
                    MethodType.methodType(Object[].class, Integer.TYPE, Object[].class));
        }

        private static final Object[] EMPTY_ARRAY = new Object[] {};

        private static Object[] filterVarArgsDefaults(int n, Object[] defaultValues, Object[] args) {
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

        private static Object[] filterDefaults(int n, Object[] defaultValues, Object[] args) {
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

        private static Object[] filterVarArgs(int n, Object[] args) {
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

        private static Object[] filter(int n, Object[] args) {
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

    private static void createInternalProperties(ExecutionContext cx, OrdinaryObject target,
            Class<?> holder) {
        ObjectLayout layout = internalLayouts.get(holder);
        if (layout.extension != null && !cx.getRealm().isEnabled(layout.extension.value())) {
            // return if extension is not enabled
            return;
        }
        if (layout.proto != null) {
            createPrototype(cx, target, layout.proto, layout.protoValue);
        }
        if (layout.values != null) {
            createValues(cx, target, layout);
        }
        if (layout.functions != null) {
            createFunctions(cx, target, layout);
        }
        if (layout.tcfunctions != null) {
            createTailCallFunctions(cx, target, layout);
        }
        if (layout.accessors != null) {
            createInternalAccessors(cx, target, layout);
        }
        if (layout.aliases != null) {
            createAliasFunctions(cx, target, layout);
        }
    }

    private static void createPrototype(ExecutionContext cx, OrdinaryObject target,
            Prototype proto, Object rawValue) {
        Object value = resolveValue(cx, rawValue);
        assert value == null || value instanceof ScriptObject;
        target.setPrototype((ScriptObject) value);
    }

    private static void createValues(ExecutionContext cx, OrdinaryObject target, ObjectLayout layout) {
        for (Entry<Value, Object> entry : layout.values.entrySet()) {
            Value val = entry.getKey();
            Object value = resolveValue(cx, entry.getValue());
            defineProperty(cx, target, val.name(), val.symbol(), val.attributes(), value);
        }
    }

    private static void createFunctions(ExecutionContext cx, OrdinaryObject target,
            ObjectLayout layout) {
        for (Entry<Function, MethodHandle> entry : layout.functions.entrySet()) {
            Function function = entry.getKey();
            String name = function.name();
            NativeFunction fun = new NativeFunction(cx.getRealm(), name, function.arity(),
                    function.nativeId(), MethodHandles.insertArguments(entry.getValue(), 0, cx));
            defineProperty(cx, target, name, function.symbol(), function.attributes(), fun);
        }
    }

    private static void createTailCallFunctions(ExecutionContext cx, OrdinaryObject target,
            ObjectLayout layout) {
        for (Entry<Function, MethodHandle> entry : layout.tcfunctions.entrySet()) {
            Function function = entry.getKey();
            String name = function.name();
            NativeTailCallFunction fun = new NativeTailCallFunction(cx.getRealm(), name,
                    function.arity(), MethodHandles.insertArguments(entry.getValue(), 0, cx));
            defineProperty(cx, target, name, function.symbol(), function.attributes(), fun);
        }
    }

    private static void createInternalAccessors(ExecutionContext cx, OrdinaryObject target,
            ObjectLayout layout) {
        LinkedHashMap<String, PropertyDescriptor> stringProps = new LinkedHashMap<>();
        EnumMap<BuiltinSymbol, PropertyDescriptor> symbolProps = new EnumMap<>(BuiltinSymbol.class);
        for (Entry<Accessor, MethodHandle> entry : layout.accessors.entrySet()) {
            Accessor accessor = entry.getKey();
            NativeFunction fun = createAccessor(cx, accessor,
                    MethodHandles.insertArguments(entry.getValue(), 0, cx));
            PropertyDescriptor desc = createAccessorDescriptor(accessor, stringProps, symbolProps);
            assert isCompatibleDescriptor(accessor, desc);
            if (accessor.type() == Accessor.Type.Getter) {
                desc.setGetter(fun);
            } else {
                desc.setSetter(fun);
            }
        }
        defineProperties(cx, target, stringProps, symbolProps);
    }

    private static void createAliasFunctions(ExecutionContext cx, OrdinaryObject target,
            ObjectLayout layout) {
        for (Entry<AliasFunction, Function> entry : layout.aliases) {
            AliasFunction alias = entry.getKey();
            Function function = entry.getValue();
            Property fun;
            if (function.symbol() == BuiltinSymbol.NONE) {
                fun = target.getOwnProperty(cx, function.name());
            } else {
                fun = target.getOwnProperty(cx, function.symbol().get());
            }
            assert fun != null;
            defineProperty(cx, target, alias.name(), alias.symbol(), alias.attributes(),
                    fun.getValue());
        }
    }

    private static Object resolveValue(ExecutionContext cx, Object value) {
        Object resolvedValue;
        if (value instanceof Intrinsics) {
            resolvedValue = cx.getIntrinsic((Intrinsics) value);
            assert resolvedValue != null : "intrinsic not defined: " + value;
        } else if (value instanceof MethodHandle) {
            try {
                resolvedValue = (Object) ((MethodHandle) value).invokeExact(cx);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            resolvedValue = value;
        }
        return resolvedValue;
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
        // otherwise all trailing arguments need to be of type Object or Object[]
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
            for (Annotation annotation : parameterAnnotations[parameter + fixedArguments]) {
                if (annotation.annotationType() == Optional.class) {
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

    private static PropertyDescriptor createAccessorDescriptor(Accessor accessor,
            LinkedHashMap<String, PropertyDescriptor> strProps,
            EnumMap<BuiltinSymbol, PropertyDescriptor> symProps) {
        PropertyDescriptor desc;
        if (accessor.symbol() == BuiltinSymbol.NONE) {
            String name = accessor.name();
            if ((desc = strProps.get(name)) == null) {
                strProps.put(name, desc = propertyDescriptor(null, null, accessor.attributes()));
            }
        } else {
            BuiltinSymbol sym = accessor.symbol();
            if ((desc = symProps.get(sym)) == null) {
                symProps.put(sym, desc = propertyDescriptor(null, null, accessor.attributes()));
            }
        }
        return desc;
    }

    private static NativeFunction createAccessor(ExecutionContext cx, Accessor accessor,
            MethodHandle mh) {
        String name = accessor.name();
        BuiltinSymbol sym = accessor.symbol();
        Accessor.Type type = accessor.type();
        int arity = (type == Accessor.Type.Getter ? 0 : 1);
        String functionName = sym == BuiltinSymbol.NONE ? (type == Accessor.Type.Getter ? "get "
                : "set ") + name : name;
        return new NativeFunction(cx.getRealm(), functionName, arity, mh);
    }

    private static boolean isCompatibleDescriptor(Accessor accessor, PropertyDescriptor desc) {
        Attributes attrs = accessor.attributes();
        return (!attrs.writable() && attrs.enumerable() == desc.isEnumerable() && attrs
                .configurable() == desc.isConfigurable())
                && (accessor.type() == Accessor.Type.Getter ? desc.getGetter() == null : desc
                        .getSetter() == null);
    }

    private static void defineProperty(ExecutionContext cx, OrdinaryObject target, String name,
            BuiltinSymbol sym, Attributes attrs, Object value) {
        if (sym == BuiltinSymbol.NONE) {
            target.defineOwnProperty(cx, name, propertyDescriptor(value, attrs));
        } else {
            target.defineOwnProperty(cx, sym.get(), propertyDescriptor(value, attrs));
        }
    }

    private static void defineProperty(ExecutionContext cx, ScriptObject target, String name,
            BuiltinSymbol sym, Attributes attrs, Object value) {
        if (sym == BuiltinSymbol.NONE) {
            target.defineOwnProperty(cx, name, propertyDescriptor(value, attrs));
        } else {
            target.defineOwnProperty(cx, sym.get(), propertyDescriptor(value, attrs));
        }
    }

    private static void defineProperties(ExecutionContext cx, OrdinaryObject target,
            LinkedHashMap<String, PropertyDescriptor> stringProperties,
            EnumMap<BuiltinSymbol, PropertyDescriptor> symbolProperties) {
        for (Entry<String, PropertyDescriptor> entry : stringProperties.entrySet()) {
            target.defineOwnProperty(cx, entry.getKey(), entry.getValue());
        }
        for (Entry<BuiltinSymbol, PropertyDescriptor> entry : symbolProperties.entrySet()) {
            target.defineOwnProperty(cx, entry.getKey().get(), entry.getValue());
        }
    }

    private static void defineProperties(ExecutionContext cx, ScriptObject target,
            LinkedHashMap<String, PropertyDescriptor> stringProperties,
            EnumMap<BuiltinSymbol, PropertyDescriptor> symbolProperties) {
        for (Entry<String, PropertyDescriptor> entry : stringProperties.entrySet()) {
            target.defineOwnProperty(cx, entry.getKey(), entry.getValue());
        }
        for (Entry<BuiltinSymbol, PropertyDescriptor> entry : symbolProperties.entrySet()) {
            target.defineOwnProperty(cx, entry.getKey().get(), entry.getValue());
        }
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
