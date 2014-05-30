/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.objects.collection.MapObject;
import com.github.anba.es6draft.runtime.objects.collection.SetObject;
import com.github.anba.es6draft.runtime.objects.collection.WeakMapObject;
import com.github.anba.es6draft.runtime.objects.collection.WeakSetObject;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Support class for native function calls.
 */
public final class NativeCalls {
    private NativeCalls() {
    }

    private static final class Types {
        static final org.objectweb.asm.Type Object = org.objectweb.asm.Type.getType(Object.class);
        static final org.objectweb.asm.Type Object_ = org.objectweb.asm.Type
                .getType(Object[].class);
        static final org.objectweb.asm.Type ExecutionContext = org.objectweb.asm.Type
                .getType(ExecutionContext.class);
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
        MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class,
                String.class, MethodType.class);
        BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC,
                org.objectweb.asm.Type.getInternalName(NativeCalls.class), "bootstrapDynamic",
                mt.toMethodDescriptorString());
    }

    /**
     * Returns the native call bootstrap handle.
     * 
     * @return the bootstrap handle
     */
    public static Handle getNativeCallBootstrap() {
        return BOOTSTRAP;
    }

    private static final String OP_NATIVE_CALL = org.objectweb.asm.Type.getMethodDescriptor(
            Types.Object, Types.Object_, Types.ExecutionContext);

    /**
     * Returns the native call method descriptor.
     * 
     * @return the method descriptor
     */
    public static String getNativeCallMethodDescriptor() {
        return OP_NATIVE_CALL;
    }

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
    public static CallSite bootstrapDynamic(MethodHandles.Lookup caller, String name,
            MethodType type) {
        MethodHandle target;
        switch (name) {
        case "native:Intrinsic":
            target = callIntrinsicMH;
            break;
        case "native:SetIntrinsic":
            target = callSetIntrinsicMH;
            break;
        case "native:GlobalObject":
            target = callGlobalObjectMH;
            break;
        case "native:GlobalThis":
            target = callGlobalThisMH;
            break;
        case "native:CallFunction":
            target = callCallFunctionMH;
            break;
        case "native:IsGenerator":
            target = callIsGeneratorMH;
            break;
        case "native:IsUninitializedMap":
            target = callIsUninitializedMapMH;
            break;
        case "native:IsUninitializedSet":
            target = callIsUninitializedSetMH;
            break;
        case "native:IsUninitializedWeakMap":
            target = callIsUninitializedWeakMapMH;
            break;
        case "native:IsUninitializedWeakSet":
            target = callIsUninitializedWeakSetMH;
            break;
        default:
            target = MethodHandles.insertArguments(invalidNativeCallMH, 0, name);
            target = MethodHandles.dropArguments(target, 0, Object[].class);
            break;
        }
        return new ConstantCallSite(target);
    }

    private static final MethodHandle callIntrinsicMH, callSetIntrinsicMH, callGlobalObjectMH,
            callGlobalThisMH, callCallFunctionMH, callIsGeneratorMH, callIsUninitializedMapMH,
            callIsUninitializedSetMH, callIsUninitializedWeakMapMH, callIsUninitializedWeakSetMH,
            invalidNativeCallMH;
    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> thisClass = lookup.lookupClass();
        try {
            MethodType type = MethodType.methodType(Object.class, Object[].class,
                    ExecutionContext.class);
            callIntrinsicMH = lookup.findStatic(thisClass, "call_Intrinsic", type);
            callSetIntrinsicMH = lookup.findStatic(thisClass, "call_SetIntrinsic", type);
            callGlobalObjectMH = lookup.findStatic(thisClass, "call_GlobalObject", type);
            callGlobalThisMH = lookup.findStatic(thisClass, "call_GlobalThis", type);
            callCallFunctionMH = lookup.findStatic(thisClass, "call_CallFunction", type);
            callIsGeneratorMH = lookup.findStatic(thisClass, "call_IsGenerator", type);
            callIsUninitializedMapMH = lookup
                    .findStatic(thisClass, "call_IsUninitializedMap", type);
            callIsUninitializedSetMH = lookup
                    .findStatic(thisClass, "call_IsUninitializedSet", type);
            callIsUninitializedWeakMapMH = lookup.findStatic(thisClass,
                    "call_IsUninitializedWeakMap", type);
            callIsUninitializedWeakSetMH = lookup.findStatic(thisClass,
                    "call_IsUninitializedWeakSet", type);
            invalidNativeCallMH = lookup.findStatic(thisClass, "invalidNativeCall",
                    MethodType.methodType(Object.class, String.class, ExecutionContext.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    @SuppressWarnings("unused")
    private static Object call_Intrinsic(Object[] args, ExecutionContext cx) {
        if (args.length == 1 && Type.isString(args[0])) {
            String intrinsicName = Type.stringValue(args[0]).toString();
            return Intrinsic(cx, intrinsicName);
        }
        return invalidNativeCallArguments(cx);
    }

    @SuppressWarnings("unused")
    private static Object call_SetIntrinsic(Object[] args, ExecutionContext cx) {
        if (args.length == 2 && Type.isString(args[0]) && args[1] instanceof OrdinaryObject) {
            String intrinsicName = Type.stringValue(args[0]).toString();
            OrdinaryObject intrinsicValue = (OrdinaryObject) args[1];
            return SetIntrinsic(cx, intrinsicName, intrinsicValue);
        }
        return invalidNativeCallArguments(cx);
    }

    @SuppressWarnings("unused")
    private static Object call_GlobalObject(Object[] args, ExecutionContext cx) {
        if (args.length == 0) {
            return GlobalObject(cx);
        }
        return invalidNativeCallArguments(cx);
    }

    @SuppressWarnings("unused")
    private static Object call_GlobalThis(Object[] args, ExecutionContext cx) {
        if (args.length == 0) {
            return GlobalThis(cx);
        }
        return invalidNativeCallArguments(cx);
    }

    @SuppressWarnings("unused")
    private static Object call_CallFunction(Object[] args, ExecutionContext cx) {
        if (args.length >= 2 && args[0] instanceof Callable) {
            Object[] arguments;
            if (args.length > 2) {
                arguments = new Object[args.length - 2];
                System.arraycopy(args, 2, arguments, 0, arguments.length);
            } else {
                arguments = ScriptRuntime.EMPTY_ARRAY;
            }
            return CallFunction(cx, (Callable) args[0], args[1], arguments);
        }
        return invalidNativeCallArguments(cx);
    }

    @SuppressWarnings("unused")
    private static Object call_IsGenerator(Object[] args, ExecutionContext cx) {
        if (args.length == 1) {
            return IsGenerator(args[0]);
        }
        return invalidNativeCallArguments(cx);
    }

    @SuppressWarnings("unused")
    private static Object call_IsUninitializedMap(Object[] args, ExecutionContext cx) {
        if (args.length == 1) {
            return IsUninitializedMap(args[0]);
        }
        return invalidNativeCallArguments(cx);
    }

    @SuppressWarnings("unused")
    private static Object call_IsUninitializedSet(Object[] args, ExecutionContext cx) {
        if (args.length == 1) {
            return IsUninitializedSet(args[0]);
        }
        return invalidNativeCallArguments(cx);
    }

    @SuppressWarnings("unused")
    private static Object call_IsUninitializedWeakMap(Object[] args, ExecutionContext cx) {
        if (args.length == 1) {
            return IsUninitializedWeakMap(args[0]);
        }
        return invalidNativeCallArguments(cx);
    }

    @SuppressWarnings("unused")
    private static Object call_IsUninitializedWeakSet(Object[] args, ExecutionContext cx) {
        if (args.length == 1) {
            return IsUninitializedWeakSet(args[0]);
        }
        return invalidNativeCallArguments(cx);
    }

    @SuppressWarnings("unused")
    private static Object invalidNativeCall(String name, ExecutionContext cx) {
        throw newInternalError(cx, Messages.Key.InternalError, "Invalid native call " + name);
    }

    private static Object invalidNativeCallArguments(ExecutionContext cx) {
        throw newInternalError(cx, Messages.Key.InternalError, "Invalid native call arguments");
    }

    private static Intrinsics getIntrinsicByName(ExecutionContext cx, String name) {
        try {
            return Intrinsics.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw newInternalError(cx, Messages.Key.InternalError, "Invalid intrinsic: " + name);
        }
    }

    /**
     * Native function: {@code %Intrinsic(<name>)}.
     * <p>
     * Returns the intrinsic by name.
     * 
     * @param cx
     *            the execution class
     * @param name
     *            the intrinsic name
     * @return the intrinsic
     */
    public static OrdinaryObject Intrinsic(ExecutionContext cx, String name) {
        Intrinsics id = getIntrinsicByName(cx, name);
        return cx.getRealm().getIntrinsic(id);
    }

    /**
     * Native function: {@code %SetIntrinsic(<name>, <realm>)}.
     * <p>
     * Sets the intrinsic to a new value.
     * 
     * @param cx
     *            the execution class
     * @param name
     *            the intrinsic name
     * @param intrinsic
     *            the new intrinsic object
     * @return the intrinsic
     */
    public static OrdinaryObject SetIntrinsic(ExecutionContext cx, String name,
            OrdinaryObject intrinsic) {
        Intrinsics id = getIntrinsicByName(cx, name);
        cx.getRealm().setIntrinsic(id, intrinsic);
        return intrinsic;
    }

    /**
     * Native function: {@code %GlobalObject()}.
     * <p>
     * Returns the global object.
     * 
     * @param cx
     *            the execution class
     * @return the global object
     */
    public static GlobalObject GlobalObject(ExecutionContext cx) {
        return cx.getRealm().getGlobalObject();
    }

    /**
     * Native function: {@code %GlobalThis()}.
     * <p>
     * Returns the global this.
     * 
     * @param cx
     *            the execution class
     * @return the global this
     */
    public static ScriptObject GlobalThis(ExecutionContext cx) {
        return cx.getRealm().getGlobalThis();
    }

    /**
     * Native function: {@code %CallFunction(<function>, <thisValue>, ...<arguments>)}.
     * <p>
     * Calls the function object.
     * 
     * @param cx
     *            the execution class
     * @param fn
     *            the function
     * @param thisValue
     *            the this-value
     * @param args
     *            the function arguments
     * @return the function return value
     */
    public static Object CallFunction(ExecutionContext cx, Callable fn, Object thisValue,
            Object... args) {
        return fn.call(cx, thisValue, args);
    }

    /**
     * Native function: {@code %IsGenerator(<value>)}.
     * <p>
     * Tests whether the input argument is a generator object.
     * 
     * @param value
     *            the input argument
     * @return {@code true} if the object is a generator
     */
    public static boolean IsGenerator(Object value) {
        return value instanceof GeneratorObject;
    }

    /**
     * Native function: {@code %IsUninitializedMap(<value>)}.
     * <p>
     * Tests whether the input argument is an uninitialized map object.
     * 
     * @param value
     *            the input argument
     * @return {@code true} if the object is an uninitialized map object
     */
    public static boolean IsUninitializedMap(Object value) {
        return value instanceof MapObject && !((MapObject) value).isInitialized();
    }

    /**
     * Native function: {@code %IsUninitializedSet(<value>)}.
     * <p>
     * Tests whether the input argument is an uninitialized set object.
     * 
     * @param value
     *            the input argument
     * @return {@code true} if the object is an uninitialized set object
     */
    public static boolean IsUninitializedSet(Object value) {
        return value instanceof SetObject && !((SetObject) value).isInitialized();
    }

    /**
     * Native function: {@code %IsUninitializedWeakMap(<value>)}.
     * <p>
     * Tests whether the input argument is an uninitialized weak map object.
     * 
     * @param value
     *            the input argument
     * @return {@code true} if the object is an uninitialized weak map object
     */
    public static boolean IsUninitializedWeakMap(Object value) {
        return value instanceof WeakMapObject && !((WeakMapObject) value).isInitialized();
    }

    /**
     * Native function: {@code %IsUninitializedWeakSet(<value>)}.
     * <p>
     * Tests whether the input argument is an uninitialized weak set object.
     * 
     * @param value
     *            the input argument
     * @return {@code true} if the object is an uninitialized weak set object
     */
    public static boolean IsUninitializedWeakSet(Object value) {
        return value instanceof WeakSetObject && !((WeakSetObject) value).isInitialized();
    }
}
