/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.AccessorPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Objects;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.repl.global.StopExecutionException;
import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.NativeConstructor;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.NativeTailCallFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
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
        Class<?> nativeId() default void.class;
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

        /**
         * Returns the optional native function identifier.
         * 
         * @return the native function identifier
         */
        Class<?> nativeId() default void.class;
    }

    /**
     * Built-in function property as an alias function
     */
    @Documented
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(AliasFunctions.class)
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
                    return null;
                default:
                    throw new AssertionError();
                }
            }
        }
    }

    private static final ClassValue<CompactLayout> internalLayouts = new ClassValue<CompactLayout>() {
        @Override
        protected CompactLayout computeValue(Class<?> type) {
            return createInternalObjectLayout(type);
        }
    };

    // TODO: Consider adding SoftReference to avoid mem-leaks
    private static final ClassValue<ObjectLayout> externalLayouts = new ClassValue<ObjectLayout>() {
        @Override
        protected ObjectLayout computeValue(Class<?> type) {
            return createExternalObjectLayout(type, false);
        }
    };

    private static final ClassValue<ObjectLayout> externalClassLayouts = new ClassValue<ObjectLayout>() {
        @Override
        protected ObjectLayout computeValue(Class<?> type) {
            return createExternalObjectLayout(type, true);
        }
    };

    private static final class ObjectLayout {
        LinkedHashMap<Value, Object> values;
        LinkedHashMap<Function, MethodHandle> functions;
        LinkedHashMap<Accessor, MethodHandle> accessors;
    }

    private enum Tag {
        Value, Function, Accessor, Alias
    }

    private static class PropertyLayout {
        static final int WRITABLE = 0x01;
        static final int ENUMERABLE = 0x02;
        static final int CONFIGURABLE = 0x04;
        static final int TAILCALL = 0x08;
        static final int ACCESSOR = 0x00;
        static final int FUNCTION = 0x10;
        static final int VALUE = 0x20;
        static final int ALIAS = 0x30;

        final int attributes;
        final String name;
        final Symbol symbol;

        PropertyLayout(int tag, Attributes attributes, String name, BuiltinSymbol builtin) {
            this.attributes = tag | toAttributes(attributes);
            this.name = name;
            this.symbol = builtin != BuiltinSymbol.NONE ? builtin.get() : null;
        }

        final boolean writable() {
            return (attributes & WRITABLE) != 0;
        }

        final boolean enumerable() {
            return (attributes & ENUMERABLE) != 0;
        }

        final boolean configurable() {
            return (attributes & CONFIGURABLE) != 0;
        }

        final Tag tag() {
            switch ((attributes >> 4) & 0b11) {
            case 0:
                return Tag.Accessor;
            case 1:
                return Tag.Function;
            case 2:
                return Tag.Value;
            case 3:
                return Tag.Alias;
            }
            throw new AssertionError();
        }

        private static int toAttributes(Attributes attributes) {
            int attrs = 0;
            attrs |= (attributes.writable() ? WRITABLE : 0);
            attrs |= (attributes.enumerable() ? ENUMERABLE : 0);
            attrs |= (attributes.configurable() ? CONFIGURABLE : 0);
            return attrs;
        }
    }

    private static final class AccessorLayout extends PropertyLayout {
        final Accessor.Type type;
        final Class<?> nativeId;
        final MethodHandle methodHandle;
        final String accessorName;

        AccessorLayout(Accessor accessor, MethodHandle methodHandle) {
            super(ACCESSOR, accessor.attributes(), accessor.name(), accessor.symbol());
            this.type = accessor.type();
            this.nativeId = accessor.nativeId();
            this.methodHandle = methodHandle;
            this.accessorName = accessorName(type, name, symbol);
        }
    }

    private static final class FunctionLayout extends PropertyLayout {
        final int arity;
        final Class<?> nativeId;
        final MethodHandle methodHandle;

        FunctionLayout(Function function, MethodHandle methodHandle) {
            super(FUNCTION, function.attributes(), function.name(), function.symbol());
            this.arity = function.arity();
            this.nativeId = function.nativeId();
            this.methodHandle = methodHandle;
        }

        FunctionLayout(Function function, TailCall tailCall, MethodHandle methodHandle) {
            super(FUNCTION | TAILCALL, function.attributes(), function.name(), function.symbol());
            this.arity = function.arity();
            this.nativeId = function.nativeId();
            this.methodHandle = methodHandle;
        }

        boolean isTailCall() {
            return (attributes & TAILCALL) != 0;
        }
    }

    private static final class AliasFunctionLayout extends PropertyLayout {
        final Object propertyKey; // String|BuiltinSymbol

        AliasFunctionLayout(AliasFunction alias, Function function) {
            super(ALIAS, alias.attributes(), alias.name(), alias.symbol());
            this.propertyKey = propertyKey(function);
        }

        private static Object propertyKey(Function function) {
            BuiltinSymbol sym = function.symbol();
            return sym != BuiltinSymbol.NONE ? sym : function.name();
        }
    }

    private static final class ValueLayout extends PropertyLayout {
        final Object rawValue; // Object|MethodHandle

        ValueLayout(Value value, Object rawValue) {
            super(VALUE, value.attributes(), value.name(), value.symbol());
            this.rawValue = rawValue;
        }
    }

    private static final class CompactLayout {
        static final Object EMPTY = new Object();

        final Object prototype;
        final ArrayList<PropertyLayout> properties;
        final CompatibilityOption option;

        CompactLayout(Object prototype, ArrayList<PropertyLayout> properties,
                CompatibilityOption option) {
            this.prototype = prototype;
            this.properties = properties;
            this.option = option;
        }
    }

    /**
     * Sets the {@link Prototype} and creates own properties for {@link Value}, {@link Function} and
     * {@link Accessor} fields.
     * 
     * @param realm
     *            the realm instance
     * @param target
     *            the object instance
     * @param holder
     *            the class which holds the properties
     */
    public static void createProperties(Realm realm, OrdinaryObject target, Class<?> holder) {
        assert holder.getName().startsWith(INTERNAL_PACKAGE);
        createInternalProperties(realm, target, holder);
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
     * Creates a new native function.
     * 
     * @param <OWNER>
     *            the owner class type
     * @param cx
     *            the execution context
     * @param owner
     *            the owner object instance
     * @param holder
     *            the class which holds the properties
     * @return the new function object
     */
    public static <OWNER> Callable createFunction(ExecutionContext cx, OWNER owner,
            Class<OWNER> holder) {
        assert !holder.getName().startsWith(INTERNAL_PACKAGE);
        return createExternalFunction(cx, owner, holder);
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
     * @return the new native script class
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
            } else if (c == Integer.TYPE) {
                return MethodHandles.insertArguments(ToInt32MH, 0, cx);
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
                MethodHandle fallback = MethodHandles.insertArguments(ThrowTypeErrorIncompatibleMH,
                        0, cx);
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

        MethodHandle returnHandle(MethodHandle handle, Class<?> returnType) {
            if (returnType == Double.TYPE || returnType == Integer.TYPE
                    || returnType == Boolean.TYPE) {
                return MethodHandles.explicitCastArguments(handle,
                        handle.type().changeReturnType(Object.class));
            } else if (returnType == String.class
                    || ScriptObject.class.isAssignableFrom(returnType)) {
                return MethodHandles.filterReturnValue(
                        MethodHandles.explicitCastArguments(handle,
                                handle.type().changeReturnType(Object.class)), nullFilter());
            } else if (returnType == Void.TYPE) {
                return MethodHandles.filterReturnValue(handle,
                        MethodHandles.constant(Object.class, UNDEFINED));
            } else if (returnType == Object.class) {
                return MethodHandles.filterReturnValue(handle, typeFilter());
            }
            throw new IllegalArgumentException(returnType.toString());
        }

        MethodHandle constructReturnHandle(MethodHandle handle, Class<?> returnType) {
            if (ScriptObject.class.isAssignableFrom(returnType)) {
                return MethodHandles.filterReturnValue(
                        handle.asType(handle.type().changeReturnType(ScriptObject.class)),
                        nullObjectFilter());
            } else if (returnType == Object.class) {
                return MethodHandles.filterReturnValue(handle, objectFilter());
            }
            throw new IllegalArgumentException(returnType.toString());
        }

        private MethodHandle nullFilter() {
            MethodHandle fallback = MethodHandles.dropArguments(
                    MethodHandles.constant(Object.class, NULL), 0, Object.class);
            return MethodHandles.guardWithTest(isNotNullMH, MethodHandles.identity(Object.class),
                    fallback);
        }

        private MethodHandle nullObjectFilter() {
            MethodHandle fallback = MethodHandles.insertArguments(ThrowTypeErrorNotObjectMH, 0, cx);
            fallback = fallback.asType(fallback.type().changeParameterType(0, ScriptObject.class));
            fallback = fallback.asType(fallback.type().changeReturnType(ScriptObject.class));
            return MethodHandles.guardWithTest(isNotNullMH.asType(isNotNullMH.type()
                    .changeParameterType(0, ScriptObject.class)), MethodHandles
                    .identity(ScriptObject.class), fallback);
        }

        private MethodHandle typeFilter() {
            MethodHandle fallback = MethodHandles.insertArguments(ThrowTypeErrorIncompatibleMH, 0,
                    cx);
            fallback = fallback.asType(fallback.type().changeReturnType(Object.class));
            return MethodHandles.guardWithTest(isTypeMH, MethodHandles.identity(Object.class),
                    fallback);
        }

        private MethodHandle objectFilter() {
            MethodHandle target = MethodHandles.identity(ScriptObject.class);
            target = target.asType(target.type().changeParameterType(0, Object.class));
            MethodHandle fallback = MethodHandles.insertArguments(ThrowTypeErrorNotObjectMH, 0, cx);
            fallback = fallback.asType(fallback.type().changeReturnType(ScriptObject.class));
            return MethodHandles.guardWithTest(isObjectMH, target, fallback);
        }

        private static final MethodHandle ToBooleanMH, ToNumberMH, ToInt32MH, ToStringMH,
                ToFlatStringMH, ToObjectMH, ToCallableMH, IsInstanceMH;
        static {
            MethodLookup lookup = new MethodLookup(MethodHandles.publicLookup());
            ToBooleanMH = lookup.findStatic(AbstractOperations.class, "ToBoolean",
                    MethodType.methodType(Boolean.TYPE, Object.class));
            ToNumberMH = lookup.findStatic(AbstractOperations.class, "ToNumber",
                    MethodType.methodType(Double.TYPE, ExecutionContext.class, Object.class));
            ToInt32MH = lookup.findStatic(AbstractOperations.class, "ToInt32",
                    MethodType.methodType(Integer.TYPE, ExecutionContext.class, Object.class));
            ToStringMH = lookup
                    .findStatic(AbstractOperations.class, "ToString", MethodType.methodType(
                            CharSequence.class, ExecutionContext.class, Object.class));
            ToFlatStringMH = lookup.findStatic(AbstractOperations.class, "ToFlatString",
                    MethodType.methodType(String.class, ExecutionContext.class, Object.class));
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
                ThrowTypeErrorIncompatibleMH, ThrowTypeErrorNotObjectMH, isNotNullMH, isTypeMH,
                isObjectMH;
        static {
            MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
            ToStringArrayMH = lookup.findStatic("ToString", MethodType.methodType(
                    CharSequence[].class, ExecutionContext.class, Object[].class));
            ToFlatStringArrayMH = lookup.findStatic("ToFlatString",
                    MethodType.methodType(String[].class, ExecutionContext.class, Object[].class));
            ToNumberArrayMH = lookup.findStatic("ToNumber",
                    MethodType.methodType(double[].class, ExecutionContext.class, Object[].class));
            ToBooleanArrayMH = lookup.findStatic("ToBoolean",
                    MethodType.methodType(boolean[].class, Object[].class));
            ToObjectArrayMH = lookup.findStatic("ToObject", MethodType.methodType(
                    ScriptObject[].class, ExecutionContext.class, Object[].class));
            ToCallableArrayMH = lookup
                    .findStatic("ToCallable", MethodType.methodType(Callable[].class,
                            ExecutionContext.class, Object[].class));
            ToScriptExceptionMH = lookup.findStatic("ToScriptException", MethodType.methodType(
                    ScriptException.class, ExecutionContext.class, Exception.class));
            ThrowTypeErrorIncompatibleMH = lookup.findStatic("throwTypeErrorIncompatible",
                    MethodType.methodType(void.class, ExecutionContext.class, Object.class));
            ThrowTypeErrorNotObjectMH = lookup.findStatic("throwTypeErrorNotObject",
                    MethodType.methodType(void.class, ExecutionContext.class, Object.class));
            isNotNullMH = lookup.findStatic("isNotNull",
                    MethodType.methodType(boolean.class, Object.class));
            isTypeMH = lookup.findStatic(Type.class, "isType",
                    MethodType.methodType(boolean.class, Object.class));
            isObjectMH = lookup.findStatic(Type.class, "isObject",
                    MethodType.methodType(boolean.class, Object.class));
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
            if (cause instanceof InternalThrowable)
                return ((InternalThrowable) cause).toScriptException(cx);
            String info = Objects.toString(cause.getMessage(), cause.getClass().getSimpleName());
            return Errors.newInternalError(cx, cause, Messages.Key.InternalError, info);
        }

        private static void throwTypeErrorIncompatible(ExecutionContext cx, Object ignore) {
            throw Errors.newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        private static void throwTypeErrorNotObject(ExecutionContext cx, Object ignore) {
            throw Errors.newTypeError(cx, Messages.Key.NotObjectType);
        }

        private static boolean isNotNull(Object o) {
            return o != null;
        }
    }

    private static <OWNER> void createExternalProperties(ExecutionContext cx, ScriptObject target,
            OWNER owner, Class<OWNER> holder) {
        ObjectLayout layout = externalLayouts.get(holder);
        Converter converter = new Converter(cx);
        if (layout.values != null) {
            createExternalValues(cx, target, owner, layout, converter);
        }
        if (layout.functions != null) {
            createExternalFunctions(cx, target, owner, layout, converter);
        }
        if (layout.accessors != null) {
            createExternalAccessors(cx, target, owner, layout, converter);
        }
    }

    private static <OWNER> NativeFunction createExternalFunction(ExecutionContext cx, OWNER owner,
            Class<OWNER> holder) {
        ObjectLayout layout = externalLayouts.get(holder);
        if (layout.functions == null || layout.functions.size() != 1) {
            throw new IllegalArgumentException();
        }
        Converter converter = new Converter(cx);
        Entry<Function, MethodHandle> entry = layout.functions.entrySet().iterator().next();
        Function function = entry.getKey();
        MethodHandle handle = getInstanceMethodHandle(cx, converter, entry.getValue(), owner);
        return new NativeFunction(cx.getRealm(), function.name(), function.arity(), handle);
    }

    private static <OWNER> void createExternalValues(ExecutionContext cx, ScriptObject target,
            OWNER owner, ObjectLayout layout, Converter converter) {
        for (Entry<Value, Object> entry : layout.values.entrySet()) {
            Value val = entry.getKey();
            assert entry.getValue() instanceof MethodHandle;
            Object value = resolveValue(cx, converter, (MethodHandle) entry.getValue(), owner);
            defineProperty(cx, target, val.name(), val.symbol(), val.attributes(), value);
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
        ScriptObject constructorParent = objects[0];
        OrdinaryObject proto = (OrdinaryObject) objects[1];
        assert constructorParent == cx.getIntrinsic(Intrinsics.FunctionPrototype);

        OrdinaryObject constructor = createConstructor(cx, className, proto, converter,
                protoLayout);
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
            OrdinaryObject proto, Converter converter, ObjectLayout layout) {
        Entry<Function, MethodHandle> constructorEntry = findConstructor(layout);
        if (constructorEntry != null) {
            // User supplied method, perform manual ClassDefinitionEvaluation for constructors
            Function function = constructorEntry.getKey();
            MethodHandle unreflect = constructorEntry.getValue();
            MethodHandle callMethod = getConstructorStaticMethodHandle(cx, converter, unreflect,
                    MethodKind.Call);
            MethodHandle constructMethod = getConstructorStaticMethodHandle(cx, converter,
                    unreflect, MethodKind.Construct);
            NativeConstructor constructor = new NativeConstructor(cx.getRealm(), className,
                    function.arity(), callMethod, constructMethod);
            constructor.defineOwnProperty(cx, "prototype", new PropertyDescriptor(proto, false,
                    false, false));
            proto.defineOwnProperty(cx, "constructor", new PropertyDescriptor(constructor, true,
                    false, true));
            return constructor;
        }
        // Create default constructor
        String sourceText = String.format("(class %s { })", sanitizeName(className));
        ScriptLoader scriptLoader = cx.getRealm().getScriptLoader();
        Script script = scriptLoader.script(new Source("<Constructor>", 1), sourceText);
        Object constructor = script.evaluate(cx);
        assert constructor instanceof OrdinaryConstructorFunction : constructor.getClass();
        return (OrdinaryConstructorFunction) constructor;
    }

    private static String sanitizeName(String className) {
        if (className.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(className.length());
        for (int i = 0, codePoint; i < className.length(); i += Character.charCount(codePoint)) {
            codePoint = className.codePointAt(i);
            sb.appendCodePoint((i == 0 ? Character.isJavaIdentifierStart(codePoint) : Character
                    .isJavaIdentifierPart(codePoint)) ? codePoint : '_');
        }
        return sb.toString();
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

    private static <OWNER> Object resolveValue(ExecutionContext cx, Converter converter,
            MethodHandle handle, OWNER owner) {
        if (handle.type().parameterCount() == 1) {
            try {
                return converter.returnHandle(handle, handle.type().returnType()).invoke(owner);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        if (handle.type().parameterCount() == 2
                && handle.type().parameterType(1) == ExecutionContext.class) {
            try {
                return converter.returnHandle(handle, handle.type().returnType()).invoke(owner, cx);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException(handle.type().toString());
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
        Accessor.Type type = accessor.type();
        String name = accessor.name();
        BuiltinSymbol symbol = accessor.symbol();
        Attributes attributes = accessor.attributes();
        String functionName = accessorName(type, name, symbol);
        int arity = accessorArity(type);
        NativeFunction fun = new NativeFunction(cx.getRealm(), functionName, arity, mh);

        PropertyDescriptor existing;
        if (symbol == BuiltinSymbol.NONE) {
            existing = stringProps.get(name);
        } else {
            existing = symbolProps.get(symbol);
        }
        PropertyDescriptor desc;
        if (existing != null) {
            if (attributes.enumerable() != existing.isEnumerable()
                    || attributes.configurable() != existing.isConfigurable()) {
                throw new IllegalArgumentException();
            }
            if (type == Accessor.Type.Getter ? existing.getGetter() != null
                    : existing.getSetter() != null) {
                throw new IllegalArgumentException();
            }
            desc = existing;
        } else {
            desc = propertyDescriptor(null, null, attributes);
        }
        if (type == Accessor.Type.Getter) {
            desc.setGetter(fun);
        } else {
            desc.setSetter(fun);
        }
        if (symbol == BuiltinSymbol.NONE) {
            stringProps.put(name, desc);
        } else {
            symbolProps.put(symbol, desc);
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
                Value value = method.getAnnotation(Value.class);
                if (function != null) {
                    if (accessor != null || value != null) {
                        throw new IllegalArgumentException();
                    }
                    if (layout.functions == null) {
                        layout.functions = new LinkedHashMap<>();
                    }
                    layout.functions.put(function, lookup.unreflect(method));
                }
                if (accessor != null) {
                    if (value != null) {
                        throw new IllegalArgumentException();
                    }
                    if (layout.accessors == null) {
                        layout.accessors = new LinkedHashMap<>();
                    }
                    layout.accessors.put(accessor, lookup.unreflect(method));
                }
                if (value != null) {
                    if (layout.values == null) {
                        layout.values = new LinkedHashMap<>();
                    }
                    layout.values.put(value, lookup.unreflect(method));
                }
            }
            return layout;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static CompactLayout createInternalObjectLayout(Class<?> holder) {
        try {
            Lookup lookup = MethodHandles.publicLookup();
            CompatibilityExtension extension = holder.getAnnotation(CompatibilityExtension.class);
            CompatibilityOption option = extension != null ? extension.value() : null;
            Object prototypeValue = CompactLayout.EMPTY;
            ArrayList<PropertyLayout> properties = new ArrayList<>();
            boolean hasProto = false;
            for (Field field : holder.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()))
                    continue;
                Value value = field.getAnnotation(Value.class);
                Prototype prototype = field.getAnnotation(Prototype.class);
                assert value == null || prototype == null;

                if (value != null) {
                    properties.add(new ValueLayout(value, getRawValue(field)));
                }
                if (prototype != null) {
                    assert !hasProto && (hasProto = true);
                    prototypeValue = getRawValue(field);
                }
            }
            for (Method method : holder.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()))
                    continue;
                Function function = method.getAnnotation(Function.class);
                Accessor accessor = method.getAnnotation(Accessor.class);
                AliasFunction[] aliases = method.getAnnotationsByType(AliasFunction.class);
                TailCall tailCall = method.getAnnotation(TailCall.class);
                Value value = method.getAnnotation(Value.class);
                assert function == null || (accessor == null && value == null);
                assert accessor == null || (function == null && value == null);
                assert value == null || (function == null && accessor == null);
                assert aliases.length == 0 || function != null;
                assert tailCall == null || function != null;

                if (value != null) {
                    MethodHandle mh = getComputedValueMethodHandle(lookup, method);
                    properties.add(new ValueLayout(value, mh));
                } else if (accessor != null) {
                    MethodHandle mh = getStaticMethodHandle(lookup, method);
                    properties.add(new AccessorLayout(accessor, mh));
                } else if (function != null) {
                    MethodHandle mh = getStaticMethodHandle(lookup, method);
                    if (tailCall == null) {
                        properties.add(new FunctionLayout(function, mh));
                    } else {
                        properties.add(new FunctionLayout(function, tailCall, mh));
                    }
                    for (AliasFunction a : aliases) {
                        properties.add(new AliasFunctionLayout(a, function));
                    }
                }
            }
            return new CompactLayout(prototypeValue, properties, option);
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
        // var-args collector flag is not preserved when applying method handle combinators
        boolean varargs = unreflect.isVarargsCollector();
        MethodHandle handle = unreflect.bindTo(owner);
        boolean callerContext = isCallerSensitive(handle);
        final int fixedArguments = callerContext ? 1 : 0;

        handle = bindContext(handle, cx);
        handle = convertArguments(handle, fixedArguments, varargs, converter);
        handle = convertReturn(handle, converter);
        handle = catchExceptions(handle, converter);
        handle = toCanonical(handle, fixedArguments, varargs, null);
        if (!callerContext) {
            handle = MethodHandles.dropArguments(handle, 0, ExecutionContext.class, Object.class);
        } else {
            handle = MethodHandles.dropArguments(handle, 1, Object.class);
        }

        assert handle.type().parameterCount() == 3;
        assert handle.type().parameterType(0) == ExecutionContext.class;
        assert handle.type().parameterType(1) == Object.class;
        assert handle.type().parameterType(2) == Object[].class;
        assert handle.type().returnType() == Object.class;

        return handle;
    }

    private enum MethodKind {
        Default, Call, Construct
    }

    private static MethodHandle getStaticMethodHandle(ExecutionContext cx, Converter converter,
            MethodHandle unreflect) {
        return getStaticMethodHandle(cx, converter, unreflect, MethodKind.Default);
    }

    private static MethodHandle getConstructorStaticMethodHandle(ExecutionContext cx,
            Converter converter, MethodHandle unreflect, MethodKind methodKind) {
        return getStaticMethodHandle(cx, converter, unreflect, methodKind);
    }

    private static MethodHandle getStaticMethodHandle(ExecutionContext cx, Converter converter,
            MethodHandle unreflect, MethodKind methodKind) {
        // var-args collector flag is not preserved when applying method handle combinators
        boolean varargs = unreflect.isVarargsCollector();
        MethodHandle handle = unreflect;
        boolean callerContext = isCallerSensitive(handle);
        final int fixedArguments = callerContext ? 2 : 1;
        final int constructorArgument = callerContext ? 1 : 0;

        handle = bindContext(handle, cx);
        if (methodKind == MethodKind.Default) {
            handle = convertThis(handle, callerContext, converter);
        } else if (methodKind == MethodKind.Call) {
            handle = MethodHandles.insertArguments(handle, constructorArgument, (Constructor) null);
            handle = convertThis(handle, callerContext, converter);
        } else {
            assert methodKind == MethodKind.Construct;
            handle = MethodHandles.insertArguments(handle, constructorArgument + 1, (Object) null);
        }
        handle = convertArguments(handle, fixedArguments, varargs, converter);
        if (methodKind != MethodKind.Construct) {
            handle = convertReturn(handle, converter);
        } else {
            handle = convertConstructReturn(handle, converter);
        }
        handle = catchExceptions(handle, converter);
        handle = toCanonical(handle, fixedArguments, varargs, null);
        if (!callerContext) {
            handle = MethodHandles.dropArguments(handle, 0, ExecutionContext.class);
        }

        if (methodKind != MethodKind.Construct) {
            assert handle.type().parameterCount() == 3;
            assert handle.type().parameterType(0) == ExecutionContext.class;
            assert handle.type().parameterType(1) == Object.class;
            assert handle.type().parameterType(2) == Object[].class;
            assert handle.type().returnType() == Object.class;
        } else {
            assert handle.type().parameterCount() == 3;
            assert handle.type().parameterType(0) == ExecutionContext.class;
            assert handle.type().parameterType(1) == Constructor.class;
            assert handle.type().parameterType(2) == Object[].class;
            assert handle.type().returnType() == ScriptObject.class;
        }

        return handle;
    }

    private static MethodHandle bindContext(MethodHandle handle, ExecutionContext cx) {
        MethodType type = handle.type();
        if (type.parameterCount() > 0 && ExecutionContext.class.equals(type.parameterType(0))) {
            handle = MethodHandles.insertArguments(handle, 0, cx);
        }
        return handle;
    }

    private static boolean isCallerSensitive(MethodHandle handle) {
        MethodType type = handle.type();
        return type.parameterCount() > 1 && ExecutionContext.class.equals(type.parameterType(1));
    }

    private static MethodHandle convertThis(MethodHandle handle, boolean callerContext,
            Converter converter) {
        int thisArgument = callerContext ? 1 : 0;
        if (handle.type().parameterCount() == thisArgument) {
            handle = MethodHandles.dropArguments(handle, thisArgument, Object.class);
        } else if (handle.type().parameterType(thisArgument) != Object.class) {
            handle = MethodHandles.filterArguments(handle, thisArgument,
                    converter.filterFor(handle.type().parameterType(thisArgument)));
        }
        return handle;
    }

    private static MethodHandle convertArguments(MethodHandle handle, int fixedArguments,
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
        return handle;
    }

    private static MethodHandle convertReturn(MethodHandle handle, Converter converter) {
        return converter.returnHandle(handle, handle.type().returnType());
    }

    private static MethodHandle convertConstructReturn(MethodHandle handle, Converter converter) {
        return converter.constructReturnHandle(handle, handle.type().returnType());
    }

    private static MethodHandle catchExceptions(MethodHandle handle, Converter converter) {
        MethodHandle thrower = MethodHandles.throwException(handle.type().returnType(),
                ScriptException.class);
        thrower = MethodHandles.filterArguments(thrower, 0, converter.toScriptException());
        return MethodHandles.catchException(handle, Exception.class, thrower);
    }

    private static MethodHandle toCanonical(MethodHandle handle, int fixedArguments,
            boolean varargs, Method method) {
        assert !handle.isVarargsCollector();
        MethodType type = handle.type();
        int actual = type.parameterCount() - fixedArguments - (varargs ? 1 : 0);
        Object[] defaults = method != null ? methodDefaults(method, fixedArguments, actual) : null;
        MethodHandle filter = Parameters.filter(actual, varargs, defaults);
        MethodHandle spreader = MethodHandles.spreadInvoker(type, fixedArguments);
        spreader = MethodHandles.insertArguments(spreader, 0, handle);
        spreader = MethodHandles.filterArguments(spreader, fixedArguments, filter);
        return spreader;
    }

    private static MethodHandle getStaticMethodHandle(Lookup lookup, Method method)
            throws IllegalAccessException {
        MethodHandle handle = lookup.unreflect(method);
        MethodType type = handle.type();
        Class<?>[] params = type.parameterArray();
        int p = 0, pcount = type.parameterCount();
        boolean callerContext = false;

        // First three parameters are (ExecutionContext, ExecutionContext?, Object=ThisValue)
        if (!(p < pcount && ExecutionContext.class.equals(params[p++]))) {
            throw new IllegalArgumentException(type.toString());
        }
        if (p < pcount && ExecutionContext.class.equals(params[p])) {
            callerContext = true;
            p++;
        }
        if (!(p < pcount && Object.class.equals(params[p++]))) {
            throw new IllegalArgumentException(type.toString());
        }
        // Always required to return Object (for now at least)
        if (!Object.class.equals(type.returnType())) {
            throw new IllegalArgumentException(type.toString());
        }
        // Collect remaining arguments into Object[]
        if (!(p + 1 == pcount && Object[].class.equals(params[p]))) {
            final int fixedArguments = callerContext ? 3 : 2;
            final boolean varargs = handle.isVarargsCollector();
            // Otherwise all trailing arguments need to be of type Object or Object[]
            for (; p < pcount; ++p) {
                if (Object.class.equals(params[p])) {
                    continue;
                }
                if (p + 1 == pcount && Object[].class.equals(params[p]) && varargs) {
                    continue;
                }
                throw new IllegalArgumentException(type.toString());
            }
            // Trailing Object[] arguments are no longer spread in var-args methods (jdk8u40, jdk9).
            if (varargs) {
                handle = handle.asFixedArity();
            }
            // Convert to (ExecutionContext, Object, ...) -> Object handle
            handle = toCanonical(handle, fixedArguments, varargs, method);
        }
        if (!callerContext) {
            handle = MethodHandles.dropArguments(handle, 1, ExecutionContext.class);
        }

        // assert handle.type().parameterCount() == 4;
        // assert handle.type().parameterType(0) == ExecutionContext.class;
        // assert handle.type().parameterType(1) == ExecutionContext.class;
        // assert handle.type().parameterType(2) == Object.class;
        // assert handle.type().parameterType(3) == Object[].class;
        // assert handle.type().returnType() == Object.class;

        return handle;
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
            }
            Object[] arguments = Arrays.copyOf(args, n, Object[].class);
            if (args.length < n) {
                int argslen = args.length;
                System.arraycopy(defaultValues, argslen, arguments, argslen, (n - argslen));
            }
            return arguments;
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
            }
            Object[] arguments = Arrays.copyOf(args, n, Object[].class);
            if (args.length < n) {
                Arrays.fill(arguments, args.length, n, UNDEFINED);
            }
            return arguments;
        }
    }

    private static void createInternalProperties(Realm realm, OrdinaryObject target, Class<?> holder) {
        CompactLayout layout = internalLayouts.get(holder);
        if (layout.option != null && !realm.isEnabled(layout.option)) {
            // return if extension is not enabled
            return;
        }
        if (layout.prototype != CompactLayout.EMPTY) {
            createPrototype(realm, target, layout.prototype);
        }
        for (PropertyLayout property : layout.properties) {
            switch (property.tag()) {
            case Value:
                createValue(realm, target, (ValueLayout) property);
                break;
            case Function:
                createFunction(realm, target, (FunctionLayout) property);
                break;
            case Accessor:
                createAccessor(realm, target, (AccessorLayout) property);
                break;
            case Alias:
                createAliasFunction(realm, target, (AliasFunctionLayout) property);
                break;
            default:
                throw new AssertionError();
            }
        }
    }

    private static void createPrototype(Realm realm, OrdinaryObject target, Object rawValue) {
        Object value = resolveValue(realm, rawValue);
        assert value == null || value instanceof ScriptObject;
        target.infallibleSetPrototype((ScriptObject) value);
    }

    private static void createValue(Realm realm, OrdinaryObject target, ValueLayout layout) {
        Object value = resolveValue(realm, layout.rawValue);
        defineProperty(target, layout, valueProperty(layout, value));
    }

    private static void createFunction(Realm realm, OrdinaryObject target, FunctionLayout layout) {
        BuiltinFunction fun;
        if (layout.isTailCall()) {
            fun = new NativeTailCallFunction(realm, layout.name, layout.arity, layout.methodHandle);
        } else {
            fun = new NativeFunction(realm, layout.name, layout.arity, layout.nativeId,
                    layout.methodHandle);
        }
        defineProperty(target, layout, valueProperty(layout, fun));
    }

    private static void createAccessor(Realm realm, OrdinaryObject target, AccessorLayout layout) {
        int arity = accessorArity(layout.type);
        NativeFunction fun = new NativeFunction(realm, layout.accessorName, arity, layout.nativeId,
                layout.methodHandle);
        Property accessorProperty = lookupOwnProperty(target, layout);
        if (accessorProperty == null) {
            defineProperty(target, layout, accessorProperty(layout, fun));
        } else {
            assert accessorProperty.isAccessorDescriptor();
            assert accessorProperty.isConfigurable() == layout.configurable();
            assert accessorProperty.isEnumerable() == layout.enumerable();
            assert (layout.type == Accessor.Type.Getter ? accessorProperty.getGetter()
                    : accessorProperty.getSetter()) == null;
            accessorProperty.apply(accessorPropertyDescriptor(layout, fun));
        }
    }

    private static void createAliasFunction(Realm realm, OrdinaryObject target,
            AliasFunctionLayout layout) {
        Object propertyKey = layout.propertyKey;
        Property fun;
        if (propertyKey instanceof String) {
            fun = target.lookupOwnProperty((String) propertyKey);
        } else {
            fun = target.lookupOwnProperty(((BuiltinSymbol) propertyKey).get());
        }
        assert fun != null : "property not found: " + propertyKey;
        defineProperty(target, layout, valueProperty(layout, fun.getValue()));
    }

    private static Object resolveValue(Realm realm, Object value) {
        Object resolvedValue;
        if (value instanceof Intrinsics) {
            resolvedValue = realm.getIntrinsic((Intrinsics) value);
            assert resolvedValue != null : "intrinsic not defined: " + value;
        } else if (value instanceof MethodHandle) {
            try {
                resolvedValue = (Object) ((MethodHandle) value).invokeExact(realm.defaultContext());
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

    private static void defineProperty(OrdinaryObject target, PropertyLayout layout,
            Property property) {
        if (layout.symbol == null) {
            target.infallibleDefineOwnProperty(layout.name, property);
        } else {
            target.infallibleDefineOwnProperty(layout.symbol, property);
        }
    }

    private static Property lookupOwnProperty(OrdinaryObject target, PropertyLayout layout) {
        if (layout.symbol == null) {
            return target.lookupOwnProperty(layout.name);
        } else {
            return target.lookupOwnProperty(layout.symbol);
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

    private static Property valueProperty(PropertyLayout layout, Object value) {
        return new Property(value, layout.writable(), layout.enumerable(), layout.configurable());
    }

    private static Property accessorProperty(AccessorLayout layout, NativeFunction accessor) {
        if (layout.type == Accessor.Type.Getter) {
            return new Property(accessor, null, layout.enumerable(), layout.configurable());
        } else {
            return new Property(null, accessor, layout.enumerable(), layout.configurable());
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

    private static PropertyDescriptor accessorPropertyDescriptor(AccessorLayout layout,
            NativeFunction accessor) {
        if (layout.type == Accessor.Type.Getter) {
            return AccessorPropertyDescriptor(accessor, null, layout.enumerable(),
                    layout.configurable());
        } else {
            return AccessorPropertyDescriptor(null, accessor, layout.enumerable(),
                    layout.configurable());
        }
    }

    private static String accessorName(Accessor.Type type, String name, BuiltinSymbol symbol) {
        return symbol == BuiltinSymbol.NONE ? (type == Accessor.Type.Getter ? "get " : "set ")
                + name : name;
    }

    private static String accessorName(Accessor.Type type, String name, Symbol symbol) {
        return symbol == null ? (type == Accessor.Type.Getter ? "get " : "set ") + name : name;
    }

    private static int accessorArity(Accessor.Type type) {
        return type == Accessor.Type.Getter ? 0 : 1;
    }
}
