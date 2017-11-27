/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.github.anba.es6draft.compiler.assembler.Handle;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * Support class for native function calls.
 */
public final class NativeCalls {
    private NativeCalls() {
    }

    /**
     * Returns the native call name.
     * 
     * @param name
     *            the native call function name
     * @return the native call name
     */
    public static String getNativeCallName(String name) {
        return "native:" + name;
    }

    private static final Handle BOOTSTRAP;

    static {
        MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class,
                MethodType.class);
        BOOTSTRAP = MethodName.findStatic(NativeCalls.class, "bootstrapDynamic", mt).toHandle();
    }

    /**
     * Returns the native call bootstrap handle.
     * 
     * @return the bootstrap handle
     */
    public static Handle getNativeCallBootstrap() {
        return BOOTSTRAP;
    }

    private static final List<Class<?>> lookupClasses = Collections.singletonList(RuntimeFunctions.class);
    private static final ConcurrentHashMap<String, MethodHandle> nativeMethods = new ConcurrentHashMap<>();

    /**
     * Returns the native call {@code CallSite} object.
     * 
     * @param caller
     *            the caller lookup object
     * @param name
     *            the native call name
     * @param type
     *            the native call type
     * @return the native call {@code CallSite} object
     */
    public static CallSite bootstrapDynamic(MethodHandles.Lookup caller, String name, MethodType type) {
        MethodHandle target;
        try {
            if (!name.startsWith("native:")) {
                throw new IllegalArgumentException();
            }
            String methodName = name.substring("native:".length());
            MethodHandle mh = nativeMethods.computeIfAbsent(methodName, NativeCalls::getNativeMethodHandle);
            if (mh == null) {
                return createRuntimeCallSite(methodName, type);
            }
            target = adaptMethodHandle(methodName, type, mh);
        } catch (IllegalArgumentException e) {
            target = invalidCallHandle(name, type);
        }
        return new ConstantCallSite(target);
    }

    private static MutableCallSite createRuntimeCallSite(String name, MethodType type) {
        MutableCallSite callsite = new MutableCallSite(type);
        MethodHandle target = MethodHandles.insertArguments(nativeCallSetupMH, 0, callsite, name);
        callsite.setTarget(MethodHandles.foldArguments(MethodHandles.exactInvoker(type), target));
        return callsite;
    }

    private static final MethodHandle nativeCallSetupMH = MethodLookup.findStatic(MethodHandles.lookup(),
            "nativeCallSetup",
            MethodType.methodType(MethodHandle.class, MutableCallSite.class, String.class, ExecutionContext.class));

    @SuppressWarnings("unused")
    private static MethodHandle nativeCallSetup(MutableCallSite callsite, String name, ExecutionContext cx) {
        RuntimeContext context = cx.getRuntimeContext();
        MethodHandle target;
        try {
            MethodHandle mh = context.getNativeCallResolver().apply(name, callsite.type());
            if (mh == null) {
                throw new IllegalArgumentException();
            }
            target = adaptNativeMethodHandle(mh);
            target = adaptMethodHandle(name, callsite.type(), target);
        } catch (IllegalArgumentException e) {
            target = invalidCallHandle(name, callsite.type());
        }
        callsite.setTarget(target);
        return target;
    }

    private static MethodHandle adaptMethodHandle(String name, MethodType callsiteType, MethodHandle mh) {
        try {
            MethodHandle target;
            if (isSpreadCall(mh, callsiteType)) {
                target = forSpreadCall(mh, callsiteType);
            } else {
                target = mh.asType(callsiteType);
            }
            if (target != mh) {
                MethodHandle invalidArgumentsHandle = invalidCallArgumentsExceptionHandle(name, callsiteType);
                target = MethodHandles.catchException(target, ClassCastException.class, invalidArgumentsHandle);
            }
            return target;
        } catch (IllegalArgumentException e) {
            return invalidCallHandle(name, callsiteType);
        } catch (WrongMethodTypeException e) {
            return invalidCallArgumentsHandle(name, callsiteType, e);
        }
    }

    private static boolean isSpreadCall(MethodHandle mh, MethodType type) {
        int pcount = type.parameterCount();
        return pcount > 0 && type.parameterType(pcount - 1).equals(Object[].class);
    }

    private static MethodHandle forSpreadCall(MethodHandle mh, MethodType type) {
        int expectedParameters = mh.type().parameterCount();
        int actualParameters = type.parameterCount();
        if (!mh.isVarargsCollector() || !mh.type().parameterType(expectedParameters - 1).equals(Object[].class)) {
            throw new WrongMethodTypeException("Not Object[] var-args collector");
        }
        if (expectedParameters > actualParameters) {
            throw new WrongMethodTypeException("Too few arguments");
        }
        if (expectedParameters < actualParameters) {
            int fixedCount = actualParameters - expectedParameters;
            int firstFixed = expectedParameters - 1;
            List<Class<?>> fixed = type.parameterList().subList(firstFixed, firstFixed + fixedCount);
            mh = MethodHandles.collectArguments(mh, firstFixed, combineArraysMH);
            mh = MethodHandles.collectArguments(mh, firstFixed, toObjectArray(fixed));
        }
        return mh.asType(type);
    }

    private static MethodHandle toObjectArray(List<Class<?>> types) {
        MethodHandle mh = MethodHandles.identity(Object[].class);
        mh = mh.asCollector(Object[].class, types.size());
        return mh.asType(MethodType.methodType(Object[].class, types));
    }

    private static MethodHandle throwableGetMessageMH, combineArraysMH, invalidNativeCallMH,
            invalidNativeCallArgumentsMH;

    static {
        MethodLookup lookup = new MethodLookup(MethodHandles.lookup());
        throwableGetMessageMH = lookup.findVirtual(Throwable.class, "getMessage", MethodType.methodType(String.class));
        combineArraysMH = lookup.findStatic("combineArrays",
                MethodType.methodType(Object[].class, Object[].class, Object[].class));
        invalidNativeCallMH = lookup.findStatic("invalidNativeCall",
                MethodType.methodType(Object.class, String.class, ExecutionContext.class));
        invalidNativeCallArgumentsMH = lookup.findStatic("invalidNativeCallArguments",
                MethodType.methodType(Object.class, String.class, ExecutionContext.class));
    }

    @SuppressWarnings("unused")
    private static Object[] combineArrays(Object[] a, Object[] b) {
        Object[] result = new Object[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    @SuppressWarnings("unused")
    private static Object invalidNativeCall(String name, ExecutionContext cx) {
        throw newInternalError(cx, Messages.Key.InternalError, "Invalid native call: " + name);
    }

    @SuppressWarnings("unused")
    private static Object invalidNativeCallArguments(String reason, ExecutionContext cx) {
        throw newInternalError(cx, Messages.Key.InternalError, "Invalid native call arguments: " + reason);
    }

    private static MethodHandle invalidCallHandle(String name, MethodType type) {
        MethodHandle mh = MethodHandles.insertArguments(invalidNativeCallMH, 0, name);
        return MethodHandles.dropArguments(mh, 1, type.dropParameterTypes(0, 1).parameterArray());
    }

    private static MethodHandle invalidCallArgumentsHandle(String name, MethodType type, WrongMethodTypeException e) {
        // Add native call name?
        MethodHandle mh = MethodHandles.insertArguments(invalidNativeCallArgumentsMH, 0, e.getMessage());
        return MethodHandles.dropArguments(mh, 1, type.dropParameterTypes(0, 1).parameterArray());
    }

    private static MethodHandle invalidCallArgumentsExceptionHandle(String name, MethodType type) {
        // Add native call name?
        return MethodHandles.filterArguments(invalidNativeCallArgumentsMH, 0, throwableGetMessageMH);
    }

    private static MethodHandle getNativeMethodHandle(String methodName) {
        for (Class<?> lookupClass : lookupClasses) {
            MethodHandle mh = getNativeMethodHandle(lookupClass, methodName);
            if (mh != null) {
                return mh;
            }
        }
        return null;
    }

    private static MethodHandle getNativeMethodHandle(Class<?> lookupClass, String methodName) {
        MethodLookup lookup = new MethodLookup(MethodHandles.publicLookup().in(lookupClass));
        Method m = findMethod(lookup, methodName);
        if (m == null) {
            return null;
        }
        MethodHandle mh;
        try {
            mh = lookup.getLookup().unreflect(m);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException();
        }
        return adaptNativeMethodHandle(mh);
    }

    private static Method findMethod(MethodLookup lookup, String name) {
        for (Method m : lookup.getLookup().lookupClass().getDeclaredMethods()) {
            int modifiers = m.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    private static MethodHandle adaptNativeMethodHandle(MethodHandle mh) {
        MethodType type = mh.type();
        boolean varargs = mh.isVarargsCollector();
        // Allow to omit execution context argument.
        if (type.parameterCount() == 0 || !type.parameterType(0).equals(ExecutionContext.class)) {
            mh = MethodHandles.dropArguments(mh, 0, ExecutionContext.class);
        }
        // Allow void return type.
        if (type.returnType() == void.class) {
            mh = MethodHandles.filterReturnValue(mh, MethodHandles.constant(Object.class, UNDEFINED));
        }
        // Restore var-args flag.
        if (varargs && !mh.isVarargsCollector()) {
            mh = mh.asVarargsCollector(mh.type().parameterType(mh.type().parameterCount() - 1));
        }
        return mh;
    }
}
