/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.v8;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.internal.MethodLookup.findStatic;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.MethodLookup;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorObject;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBuffer;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.objects.collection.MapIteratorObject;
import com.github.anba.es6draft.runtime.objects.collection.MapObject;
import com.github.anba.es6draft.runtime.objects.collection.SetIteratorObject;
import com.github.anba.es6draft.runtime.objects.collection.SetObject;
import com.github.anba.es6draft.runtime.objects.collection.WeakMapObject;
import com.github.anba.es6draft.runtime.objects.collection.WeakSetObject;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.objects.text.RegExpObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Implements or adds stubs for the following native v8-functions:
 * 
 * <ul>
 * <li>%ToNumber
 * <li>%ToLength
 * <li>%ToString
 * <li>%ToPrimitive
 * <li>%ToPrimitive_Number
 * <li>%ToPrimitive_String
 * <li>%ToName
 * <li>%SameValueZero
 * <li>%Call
 * <li>%StringCharFromCode
 * <li>%SmiLexicographicCompare
 * <li>%FlattenString
 * <li>%ArrayBufferNeuter
 * <li>%math_sqrt
 * <li>%ValueOf
 * <li>%IsArray
 * <li>%IsJSReceiver
 * <li>%IsRegExp
 * <li>%AbortJS
 * <li>%DefineAccessorPropertyUnchecked
 * <li>%DeoptimizeNow
 * <li>%OptimizeOsr
 * <li>%DeoptimizeFunction
 * <li>%NeverOptimizeFunction
 * <li>%ClearFunctionTypeFeedback
 * <li>%OptimizeFunctionOnNextCall
 * <li>%SetForceInlineFlag
 * <li>%SetFlags
 * <li>%OptimizeObjectForAddingMultipleProperties
 * <li>%AtomicsFutexNumWaitersForTesting
 * <li>%NormalizeElements
 * <li>%ToFastProperties
 * <li>%HasSloppyArgumentsElements
 * <li>%GetOptimizationStatus
 * <li>%ClassOf
 * <li>%ConstructDouble
 * <li>%DoubleHi
 * <li>%DoubleLo
 * <li>%EnqueueMicrotask
 * <li>%IsSmi
 * <li>%RegExpConstructResult
 * <li>%RunMicrotasks
 * <li>%StringCharCodeAt
 * <li>%StringCompare
 * </ul>
 */
final class NativeFunctions {
    private static final HashMap<String, MethodHandle> methodHandles;
    static {
        HashMap<String, MethodHandle> methods = new HashMap<>();

        // MethodHandles to AbstractOperations methods.
        MethodLookup abstractOps = new MethodLookup(publicLookup().in(AbstractOperations.class));
        methods.put("ToNumber",
                abstractOps.findStatic("ToNumber", methodType(double.class, ExecutionContext.class, Object.class)));
        methods.put("ToLength",
                abstractOps.findStatic("ToLength", methodType(long.class, ExecutionContext.class, Object.class)));
        methods.put("ToString", abstractOps.findStatic("ToString",
                methodType(CharSequence.class, ExecutionContext.class, Object.class)));
        MethodHandle toPrimitive = abstractOps.findStatic("ToPrimitive", methodType(Object.class,
                ExecutionContext.class, Object.class, AbstractOperations.ToPrimitiveHint.class));
        methods.put("ToPrimitive", insertArguments(toPrimitive, 2, AbstractOperations.ToPrimitiveHint.Default));
        methods.put("ToPrimitive_Number", insertArguments(toPrimitive, 2, AbstractOperations.ToPrimitiveHint.Number));
        methods.put("ToPrimitive_String", insertArguments(toPrimitive, 2, AbstractOperations.ToPrimitiveHint.String));
        methods.put("ToName", abstractOps.findStatic("ToPropertyKey",
                methodType(Object.class, ExecutionContext.class, Object.class)));
        methods.put("SameValueZero",
                abstractOps.findStatic("SameValueZero", methodType(boolean.class, Object.class, Object.class)));
        methods.put("Call", abstractOps.findStatic("Call",
                methodType(Object.class, ExecutionContext.class, Object.class, Object.class, Object[].class)));

        // StringCharFromCode(x) -> String.valueOf(ToUint16(x))
        MethodHandle toUint16 = abstractOps.findStatic("ToUint16",
                methodType(char.class, ExecutionContext.class, Object.class));
        methods.put("StringCharFromCode", filterReturnValue(toUint16,
                publicStatic(String.class, "valueOf", methodType(String.class, char.class))));

        // SmiLexicographicCompare(x, y) -> StringCompare(ToString(x), ToString(y))
        MethodHandle doubleToCharSeq = abstractOps.findStatic("ToString", methodType(String.class, double.class))
                .asType(methodType(CharSequence.class, double.class));
        MethodHandle stringCompare = findStatic(lookup(), "StringCompare",
                methodType(int.class, CharSequence.class, CharSequence.class));
        methods.put("SmiLexicographicCompare", filterArguments(stringCompare, 0, doubleToCharSeq, doubleToCharSeq));

        // Additional simple implementations.
        methods.put("FlattenString", publicVirtual(CharSequence.class, "toString", methodType(String.class)));
        methods.put("ArrayBufferNeuter", publicStatic(ArrayBufferConstructor.class, "DetachArrayBuffer",
                methodType(void.class, ExecutionContext.class, ArrayBuffer.class)));
        methods.put("math_sqrt", publicStatic(Math.class, "sqrt", methodType(double.class, double.class)));
        methods.put("ValueOf", identity(Object.class));

        // Type check methods.
        MethodHandle isInstance = publicVirtual(Class.class, "isInstance", methodType(boolean.class, Object.class));
        methods.put("IsArray", isInstance.bindTo(ArrayObject.class));
        methods.put("IsJSReceiver", isInstance.bindTo(ScriptObject.class));
        methods.put("IsRegExp", isInstance.bindTo(RegExpObject.class));

        // AbortJS(message) -> throw new AssertionError(message)
        MethodHandle newAssertionError = publicConstructor(AssertionError.class, methodType(void.class, Object.class));
        methods.put("AbortJS", filterReturnValue(newAssertionError, throwException(void.class, AssertionError.class)));

        // Not implemented.
        // DefineAccessorPropertyUnchecked(p1,p2,p3,p4,p5) -> throw new ScriptException("illegal access")
        MethodHandle createException = publicStatic(ScriptException.class, "create",
                methodType(ScriptException.class, Object.class)).bindTo("illegal access");
        methods.put("DefineAccessorPropertyUnchecked",
                dropArguments(filterReturnValue(createException, throwException(void.class, ScriptException.class)), 0,
                        Collections.nCopies(5, Object.class)));

        // Not implemented.
        // GeneratorGetFunction(cx, generator) -> cx.getIntrinsic(%FunctionPrototype%)
        MethodHandle intrinsic = publicVirtual(ExecutionContext.class, "getIntrinsic",
                methodType(OrdinaryObject.class, Intrinsics.class));
        MethodHandle functionProto = insertArguments(intrinsic, 1, Intrinsics.FunctionPrototype)
                .asType(methodType(Callable.class, ExecutionContext.class));
        methods.put("GeneratorGetFunction", dropArguments(functionProto, 1, GeneratorObject.class));

        // Not implemented.
        MethodHandle notImplemented = findStatic(lookup(), "notImplemented", methodType(void.class));
        MethodHandle notImplementedCallable = dropArguments(notImplemented, 0, Object.class);
        methods.put("Apply", notImplemented);
        methods.put("DeoptimizeNow", notImplemented);
        methods.put("OptimizeOsr", notImplemented);
        methods.put("DeoptimizeFunction", notImplementedCallable);
        methods.put("NeverOptimizeFunction", notImplementedCallable);
        methods.put("ClearFunctionTypeFeedback", notImplementedCallable);
        methods.put("OptimizeFunctionOnNextCall", notImplementedCallable);
        methods.put("SetForceInlineFlag", notImplementedCallable);
        methods.put("SetFlags", dropArguments(notImplemented, 0, CharSequence.class));
        methods.put("OptimizeObjectForAddingMultipleProperties",
                dropArguments(notImplemented, 0, Object.class, double.class));
        methods.put("AtomicsFutexNumWaitersForTesting",
                dropArguments(notImplemented, 0, TypedArrayObject.class, double.class));
        methods.put("NormalizeElements", identity(Object.class));
        methods.put("ToFastProperties", identity(Object.class));
        methods.put("HasSloppyArgumentsElements", dropArguments(constant(boolean.class, true), 0, Object.class));
        methods.put("GetOptimizationStatus", dropArguments(constant(int.class, 0), 0, Callable.class));

        // Method based implementations for complex functions.
        MethodLookup lookup = new MethodLookup(lookup());
        for (Method m : NativeFunctions.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())) {
                MethodHandle mh;
                try {
                    mh = lookup.getLookup().unreflect(m);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }
                methods.put(m.getName(), mh);
            }
        }

        methodHandles = methods;
    }

    private static MethodHandle publicStatic(Class<?> lookupClass, String name, MethodType type) {
        return MethodLookup.findStatic(publicLookup(), lookupClass, name, type);
    }

    private static MethodHandle publicVirtual(Class<?> lookupClass, String name, MethodType type) {
        return MethodLookup.findVirtual(publicLookup(), lookupClass, name, type);
    }

    private static MethodHandle publicConstructor(Class<?> lookupClass, MethodType type) {
        return MethodLookup.findConstructor(publicLookup(), lookupClass, type);
    }

    /**
     * Checks if the native call is supported.
     * 
     * @param name
     *            the native call name
     * @return {@code true} if the native call name is supported
     */
    static boolean isSupported(String name) {
        if (name.startsWith("_")) {
            name = name.substring(1, name.length());
        }
        return methodHandles.containsKey(name);
    }

    /**
     * Resolves the native call.
     * 
     * @param name
     *            the native call name
     * @param type
     *            the callsite method type
     * @return the resolved native call or {@code null} if not supported
     */
    static MethodHandle resolve(String name, MethodType type) {
        if (name.startsWith("_")) {
            name = name.substring(1, name.length());
        }
        MethodHandle mh = methodHandles.get(name);
        if (mh != null) {
            // Ignore optional second parameter.
            switch (name) {
            case "OptimizeFunctionOnNextCall":
            case "GetOptimizationStatus":
                if (type.parameterCount() > 2) {
                    mh = dropArguments(mh, 1, type.parameterType(2));
                }
            }
        }
        return mh;
    }

    @SuppressWarnings("unused")
    private static void notImplemented() {
        // Not implemented.
    }

    public static boolean IsSmi(Object value) {
        if (value instanceof Number) {
            double v = ((Number) value).doubleValue();
            return Double.compare(v, (int) v) == 0;
        }
        return false;
    }

    public static double StringCharCodeAt(CharSequence s, double index) {
        int i = (int) index;
        if (i < 0 || i >= s.length() || index != i) {
            return Double.NaN;
        }
        return s.charAt(i);
    }

    public static int StringCompare(CharSequence x, CharSequence y) {
        int c = x.toString().compareTo(y.toString());
        return c < 0 ? -1 : c > 0 ? 1 : 0;
    }

    public static ArrayObject RegExpConstructResult(ExecutionContext cx, int size, Object index, Object input) {
        ArrayObject array = ArrayCreate(cx, size);
        CreateDataProperty(cx, array, "index", index);
        CreateDataProperty(cx, array, "input", input);
        return array;
    }

    public static void EnqueueMicrotask(ExecutionContext cx, Callable c) {
        cx.getRealm().enqueuePromiseJob(() -> c.call(cx, UNDEFINED));
    }

    public static void RunMicrotasks(ExecutionContext cx) {
        cx.getRealm().getWorld().runEventLoop();
    }

    public static double ConstructDouble(double hi, double lo) {
        long bits = ((long) hi & 0xffff_ffffL) << 32 | ((long) lo & 0xffff_ffffL);
        return Double.longBitsToDouble(bits);
    }

    public static int DoubleHi(double d) {
        return (int) ((Double.doubleToRawLongBits(d) >>> 32) & 0xffff_ffffL);
    }

    public static int DoubleLo(double d) {
        return (int) ((Double.doubleToRawLongBits(d) >>> 0) & 0xffff_ffffL);
    }

    public static String ClassOf(ScriptObject o) {
        if (o instanceof ArrayIteratorObject) {
            return "Array Iterator";
        }
        if (o instanceof MapIteratorObject) {
            return "Map Iterator";
        }
        if (o instanceof SetIteratorObject) {
            return "Set Iterator";
        }
        if (o instanceof MapObject) {
            return "Map";
        }
        if (o instanceof SetObject) {
            return "Set";
        }
        if (o instanceof WeakMapObject) {
            return "WeakMap";
        }
        if (o instanceof WeakSetObject) {
            return "WeakSet";
        }
        if (o instanceof GeneratorObject) {
            return "Generator";
        }
        return o.className();
    }
}
