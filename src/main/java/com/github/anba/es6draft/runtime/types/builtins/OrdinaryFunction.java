/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.AbstractOperations.ConstructTailCall;
import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public class OrdinaryFunction extends FunctionObject {
    protected OrdinaryFunction(Realm realm) {
        super(realm);
    }

    private static final class OrdinaryConstructorFunction extends OrdinaryFunction implements
            Constructor {
        public OrdinaryConstructorFunction(Realm realm) {
            super(realm);
        }

        @Override
        public final boolean isConstructor() {
            return super.isConstructor();
        }

        /**
         * 9.2.1 [[Construct]] (argumentsList)
         */
        @Override
        public ScriptObject construct(ExecutionContext callerContext, Object... args) {
            return Construct(callerContext, this, args);
        }

        /**
         * 9.2.1 [[Construct]] (argumentsList)
         */
        @Override
        public Object tailConstruct(ExecutionContext callerContext, Object... args)
                throws Throwable {
            return ConstructTailCall(callerContext, this, args);
        }
    }

    @Override
    protected OrdinaryFunction allocateNew() {
        return FunctionAllocate(getRealm().defaultContext(), getPrototype(), isStrict(),
                getFunctionKind());
    }

    /**
     * 9.2.4 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        try {
            return getCallMethod().invokeExact(this, callerContext, thisValue, args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 9.2.4 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args)
            throws Throwable {
        return getTailCallMethod().invokeExact(this, callerContext, thisValue, args);
    }

    /* ***************************************************************************************** */

    /**
     * 9.2.3 FunctionAllocate Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param functionPrototype
     *            the function prototype
     * @param strict
     *            the strict mode flag
     * @param kind
     *            the function kind
     * @return the new function object
     */
    public static OrdinaryFunction FunctionAllocate(ExecutionContext cx,
            ScriptObject functionPrototype, boolean strict, FunctionKind kind) {
        Realm realm = cx.getRealm();
        /* steps 1-3 (implicit) */
        /* steps 4-6 */
        OrdinaryFunction f;
        if (kind == FunctionKind.Normal || kind == FunctionKind.ConstructorMethod) {
            f = new OrdinaryConstructorFunction(realm);
        } else {
            f = new OrdinaryFunction(realm);
        }
        /* steps 9-13 */
        f.allocate(realm, functionPrototype, strict, kind, uninitializedFunctionMH);
        /* step 14 */
        return f;
    }

    /**
     * 9.2.5 FunctionInitialize Abstract Operation
     * 
     * @param <FUNCTION>
     *            the function type
     * @param cx
     *            the execution context
     * @param f
     *            the function object
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @return the function object
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialize(
            ExecutionContext cx, FUNCTION f, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment<?> scope) {
        /* step 1 */
        int len = function.expectedArgumentCount();
        /* step 2 */
        boolean strict = f.isStrict();
        /* steps 3-4 */
        DefinePropertyOrThrow(cx, f, "length", new PropertyDescriptor(len, false, false, true));
        /* step 5 */
        if (strict) {
            f.addRestrictedFunctionProperties(cx);
        }
        /* steps 6-11 */
        f.initialize(kind, function, scope);
        /* step 12 */
        return f;
    }

    /**
     * 9.2.6 FunctionCreate Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @return the new function object
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment<?> scope) {
        return FunctionCreate(cx, kind, function, scope, null);
    }

    /**
     * 9.2.6 FunctionCreate Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @param functionPrototype
     *            the function prototype
     * @return the new function object
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment<?> scope,
            ScriptObject functionPrototype) {
        assert !function.isGenerator();
        /* step 1 */
        if (functionPrototype == null) {
            functionPrototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        }
        /* step 2 */
        OrdinaryFunction f = FunctionAllocate(cx, functionPrototype, function.isStrict(), kind);
        /* step 3 */
        return FunctionInitialize(cx, f, kind, function, scope);
    }

    /**
     * 9.2.8 AddRestrictedFunctionProperties Abstract Operation
     * 
     * @param <FUNCTION>
     *            the function type
     * @param cx
     *            the execution context
     * @param obj
     *            the script object
     */
    public static <FUNCTION extends ScriptObject & Callable> void AddRestrictedFunctionProperties(
            ExecutionContext cx, FUNCTION obj) {
        // TODO: %ThrowTypeError% from current realm or function's realm? (current realm per spec)
        /* step 1 */
        Callable thrower = cx.getRealm().getThrowTypeError();
        /* steps 2-3 */
        AddRestrictedFunctionProperties(cx, obj, thrower);
    }

    /**
     * 9.2.8 AddRestrictedFunctionProperties Abstract Operation
     * 
     * @param <FUNCTION>
     *            the function type
     * @param cx
     *            the execution context
     * @param obj
     *            the script object
     * @param thrower
     *            the thrower function object
     */
    public static <FUNCTION extends ScriptObject & Callable> void AddRestrictedFunctionProperties(
            ExecutionContext cx, FUNCTION obj, Callable thrower) {
        /* step 1 (not applicable) */
        /* step 2 */
        DefinePropertyOrThrow(cx, obj, "caller", new PropertyDescriptor(thrower, thrower, false,
                false));
        /* step 3 */
        DefinePropertyOrThrow(cx, obj, "arguments", new PropertyDescriptor(thrower, thrower, false,
                false));
    }

    /**
     * 9.2.8.1 %ThrowTypeError% ( )
     */
    private static final class TypeErrorThrower extends BuiltinFunction {
        TypeErrorThrower(Realm realm) {
            super(realm, ANONYMOUS);
            createDefaultFunctionProperties(ANONYMOUS, 0, this);
            // [[Extensible]] slot is false
            setExtensible(false);
        }

        private TypeErrorThrower(Realm realm, Void ignore) {
            super(realm, ANONYMOUS);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            throw newTypeError(calleeContext(), Messages.Key.StrictModePoisonPill);
        }

        @Override
        public TypeErrorThrower clone() {
            return new TypeErrorThrower(getRealm(), null);
        }
    }

    /**
     * 9.2.8.1 %ThrowTypeError% ( )
     * 
     * @param cx
     *            the execution context
     * @return the %ThrowTypeError% function object
     */
    public static Callable createThrowTypeError(ExecutionContext cx) {
        assert cx.getIntrinsic(Intrinsics.FunctionPrototype) != null : "%FunctionPrototype% not initialized";
        return new TypeErrorThrower(cx.getRealm());
    }

    /**
     * 9.2.9 MakeConstructor Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param f
     *            the function object
     */
    public static void MakeConstructor(ExecutionContext cx, FunctionObject f) {
        /* step 3 */
        boolean installNeeded = true;
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 4 */
        boolean writablePrototype = true;
        MakeConstructor(cx, f, writablePrototype, prototype, installNeeded);
    }

    /**
     * 9.2.9 MakeConstructor Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param f
     *            the function object
     * @param writablePrototype
     *            the writable flag for the .prototype property
     * @param prototype
     *            the prototype object
     */
    public static void MakeConstructor(ExecutionContext cx, FunctionObject f,
            boolean writablePrototype, ScriptObject prototype) {
        /* step 2 */
        boolean installNeeded = false;
        MakeConstructor(cx, f, writablePrototype, prototype, installNeeded);
    }

    /**
     * 9.2.9 MakeConstructor Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param f
     *            the function object
     * @param writablePrototype
     *            the writable flag for the .prototype property
     * @param prototype
     *            the prototype object
     * @param installNeeded
     *            the install flag
     */
    private static void MakeConstructor(ExecutionContext cx, FunctionObject f,
            boolean writablePrototype, ScriptObject prototype, boolean installNeeded) {
        assert f instanceof Constructor : "MakeConstructor applied on non-Constructor";
        /* step 5 */
        f.setConstructor(true);
        /* step 6 */
        if (installNeeded) {
            DefinePropertyOrThrow(cx, prototype, "constructor", new PropertyDescriptor(f,
                    writablePrototype, false, writablePrototype));
        }
        /* steps 7-8 */
        DefinePropertyOrThrow(cx, f, "prototype", new PropertyDescriptor(prototype,
                writablePrototype, false, false));
        /* step 9 (return) */
    }

    /**
     * 9.2.10 MakeMethod ( F, methodName, homeObject ) Abstract Operation
     * 
     * @param f
     *            the function object
     * @param methodName
     *            the method name
     * @param homeObject
     *            the home object
     */
    public static void MakeMethod(FunctionObject f, String methodName, ScriptObject homeObject) {
        /* steps 1-3 (not applicable) */
        /* steps 4-6 */
        f.toMethod(methodName, homeObject);
        /* step 7 (return) */
    }

    /**
     * 9.2.10 MakeMethod ( F, methodName, homeObject ) Abstract Operation
     * 
     * @param f
     *            the function object
     * @param methodName
     *            the method name
     * @param homeObject
     *            the home object
     */
    public static void MakeMethod(FunctionObject f, Symbol methodName, ScriptObject homeObject) {
        /* steps 1-3 (not applicable) */
        /* steps 3-6 */
        f.toMethod(methodName, homeObject);
        /* step 7 (return) */
    }

    /**
     * 9.2.11 SetFunctionName Abstract Operation
     * 
     * @param f
     *            the function object
     * @param name
     *            the function name
     */
    public static void SetFunctionName(FunctionObject f, String name) {
        SetFunctionName(f, name, null);
    }

    /**
     * 9.2.11 SetFunctionName Abstract Operation
     * 
     * @param f
     *            the function object
     * @param name
     *            the function name
     * @param prefix
     *            the function name prefix
     */
    public static void SetFunctionName(FunctionObject f, String name, String prefix) {
        /* step 1 */
        assert f.isExtensible() : "function is not extensible";
        assert !f.hasOwnProperty(f.getRealm().defaultContext(), "name") : "function has 'name' property";
        /* step 2 (implicit) */
        /* step 3 (not applicable) */
        /* step 4 */
        if (!name.isEmpty() && prefix != null) {
            name = prefix + " " + name;
        }
        /* step 5 */
        boolean success = f.ordinaryDefineOwnProperty(f.getRealm().defaultContext(), "name",
                new PropertyDescriptor(name, false, false, true));
        /* step 6 */
        assert success;
        /* step 7 (return) */
    }

    /**
     * 9.2.11 SetFunctionName Abstract Operation
     * 
     * @param f
     *            the function object
     * @param name
     *            the function name
     */
    public static void SetFunctionName(FunctionObject f, Symbol name) {
        SetFunctionName(f, name, null);
    }

    /**
     * 9.2.11 SetFunctionName Abstract Operation
     * 
     * @param f
     *            the function object
     * @param name
     *            the function name
     * @param prefix
     *            the function name prefix
     */
    public static void SetFunctionName(FunctionObject f, Symbol name, String prefix) {
        /* step 3 */
        String description = name.getDescription();
        String sname = description == null ? "" : "[" + description + "]";
        /* steps 1-2, 4-7 */
        SetFunctionName(f, sname, prefix);
    }

    /**
     * 9.2.12 GetSuperBinding(obj) Abstract Operation
     * 
     * @param obj
     *            the function object
     * @return the super binding
     */
    public static ScriptObject GetSuperBinding(Object obj) {
        /* steps 1-2 */
        if (!(obj instanceof FunctionObject)) {
            return null;
        }
        FunctionObject function = (FunctionObject) obj;
        if (!function.isNeedsSuper()) {
            return null;
        }
        /* step 3 */
        return function.getHomeObject();
    }

    /**
     * 9.2.13 CloneMethod(function, newHome, newName) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param function
     *            the function object
     * @param newHome
     *            the new home object
     * @param newName
     *            the new method name
     * @return the cloned function object
     */
    public static Callable CloneMethod(ExecutionContext cx, Callable function,
            ScriptObject newHome, Object newName) {
        /* step 1 */
        assert function instanceof FunctionObject || function instanceof BuiltinFunction;
        /* step 2 (not applicable) */
        /* step 3 */
        assert newName == null || newName instanceof String || newName instanceof Symbol;
        /* steps 4-10 */
        if (function instanceof FunctionObject) {
            FunctionObject fn = (FunctionObject) function;
            /* steps 4-7, 9 */
            FunctionObject clone = fn.clone(cx);
            /* step 8 */
            if (fn.isNeedsSuper()) {
                if (newName != null) {
                    clone.toMethod(newName, newHome);
                } else {
                    clone.toMethod(fn.getMethodName(), newHome);
                }
            }
            /* step 10 */
            return clone;
        } else {
            BuiltinFunction fn = (BuiltinFunction) function;
            /* steps 4-7, 9 */
            BuiltinFunction clone = fn.clone(cx);
            /* step 8 (not applicable) */
            /* step 10 */
            return clone;
        }
    }
}
