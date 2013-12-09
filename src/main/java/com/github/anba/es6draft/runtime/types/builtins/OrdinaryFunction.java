/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.TailCallInvocation;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;

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

    private static class OrdinaryConstructorFunction extends OrdinaryFunction implements
            Constructor {
        public OrdinaryConstructorFunction(Realm realm) {
            super(realm);
        }

        @Override
        public final boolean isConstructor() {
            return isConstructor;
        }

        /**
         * 9.2.2 [[Construct]] (argumentsList)
         */
        @Override
        public ScriptObject construct(ExecutionContext callerContext, Object... args) {
            return OrdinaryConstruct(callerContext, this, args);
        }

        /**
         * 9.2.2 [[Construct]] (argumentsList)
         */
        @Override
        public Object tailConstruct(ExecutionContext callerContext, Object... args)
                throws Throwable {
            return OrdinaryConstructTailCall(callerContext, this, args);
        }
    }

    @Override
    protected OrdinaryFunction allocateCopy() {
        return FunctionAllocate(getRealm().defaultContext(), getPrototype(), isStrict(),
                getFunctionKind());
    }

    /**
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
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
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args)
            throws Throwable {
        return getTailCallMethod().invokeExact(this, callerContext, thisValue, args);
    }

    /* ***************************************************************************************** */

    /**
     * 9.2.2.1 OrdinaryConstruct (F, argumentsList)
     */
    public static <FUNCTION extends ScriptObject & Callable & Constructor> ScriptObject OrdinaryConstruct(
            ExecutionContext cx, FUNCTION f, Object[] args) {
        /* steps 1-2 */
        Object creator = Get(cx, f, BuiltinSymbol.create.get());
        /* steps 3-5 */
        Object obj;
        if (!Type.isUndefined(creator)) {
            if (!IsCallable(creator)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            obj = ((Callable) creator).call(cx, f);
        } else {
            obj = OrdinaryCreateFromConstructor(cx, f, Intrinsics.ObjectPrototype);
        }
        /* step 6 */
        if (!Type.isObject(obj)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* steps 7-8 */
        Object result = f.call(cx, obj, args);
        /* step 9 */
        if (Type.isObject(result)) {
            return Type.objectValue(result);
        }
        /* step 10 */
        return Type.objectValue(obj);
    }

    /**
     * 9.2.2.1 OrdinaryConstruct (F, argumentsList)
     */
    public static <FUNCTION extends ScriptObject & Callable & Constructor> Object OrdinaryConstructTailCall(
            ExecutionContext cx, FUNCTION f, Object[] args) throws Throwable {
        /* steps 1-2 */
        Object creator = Get(cx, f, BuiltinSymbol.create.get());
        /* steps 3-5 */
        Object obj;
        if (!Type.isUndefined(creator)) {
            if (!IsCallable(creator)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            obj = ((Callable) creator).call(cx, f);
        } else {
            obj = OrdinaryCreateFromConstructor(cx, f, Intrinsics.ObjectPrototype);
        }
        /* step 6 */
        if (!Type.isObject(obj)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* steps 7-8 */
        // Invoke 'tailCall()' instead of 'call()' to get TailCallInvocation objects
        Object result = f.tailCall(cx, obj, args);
        /* steps 9-10 (tail-call) */
        if (result instanceof TailCallInvocation) {
            // Don't unwind tail-call yet, instead store reference to 'obj'
            return ((TailCallInvocation) result).toConstructTailCall(Type.objectValue(obj));
        }
        /* step 9 */
        if (Type.isObject(result)) {
            return Type.objectValue(result);
        }
        /* step 10 */
        return Type.objectValue(obj);
    }

    /**
     * 9.2.5 FunctionAllocate Abstract Operation
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
        f.allocate(realm, functionPrototype, strict, kind, uninitialisedFunctionMH);
        /* step 14 */
        return f;
    }

    /**
     * 9.2.6 FunctionInitialise Abstract Operation
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialise(
            ExecutionContext cx, FUNCTION f, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment scope) {
        return FunctionInitialise(cx, f, kind, function, scope, null, (String) null);
    }

    /**
     * 9.2.6 FunctionInitialise Abstract Operation
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialise(
            ExecutionContext cx, FUNCTION f, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment scope, ScriptObject homeObject, String methodName) {
        /* step 1 */
        int len = function.expectedArgumentCount();
        /* step 2 */
        boolean strict = f.isStrict();
        /* steps 3-4 */
        DefinePropertyOrThrow(cx, f, "length", new PropertyDescriptor(len, false, false, true));
        /* step 5 */
        if (strict) {
            AddRestrictedFunctionProperties(cx, f);
        }
        /* steps 6-12 */
        f.initialise(kind, function, scope, homeObject, methodName);
        /* step 13 */
        return f;
    }

    /**
     * 9.2.6 FunctionInitialise Abstract Operation
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialise(
            ExecutionContext cx, FUNCTION f, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment scope, ScriptObject homeObject, Symbol methodName) {
        /* step 1 */
        int len = function.expectedArgumentCount();
        /* step 2 */
        boolean strict = f.isStrict();
        /* steps 3-4 */
        DefinePropertyOrThrow(cx, f, "length", new PropertyDescriptor(len, false, false, true));
        /* step 5 */
        if (strict) {
            AddRestrictedFunctionProperties(cx, f);
        }
        /* steps 6-12 */
        f.initialise(kind, function, scope, homeObject, methodName);
        /* step 13 */
        return f;
    }

    /**
     * 9.2.7 FunctionCreate Abstract Operation
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope) {
        return FunctionCreate(cx, kind, function, scope, null, null, (String) null);
    }

    /**
     * 9.2.7 FunctionCreate Abstract Operation
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, ScriptObject functionPrototype) {
        return FunctionCreate(cx, kind, function, scope, functionPrototype, null, (String) null);
    }

    /**
     * 9.2.7 FunctionCreate Abstract Operation
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope,
            ScriptObject functionPrototype, ScriptObject homeObject, String methodName) {
        assert !function.isGenerator();
        /* step 1 */
        if (functionPrototype == null) {
            functionPrototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        }
        /* step 2 */
        OrdinaryFunction f = FunctionAllocate(cx, functionPrototype, function.isStrict(), kind);
        /* step 3 */
        return FunctionInitialise(cx, f, kind, function, scope, homeObject, methodName);
    }

    /**
     * 9.2.7 FunctionCreate Abstract Operation
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope,
            ScriptObject functionPrototype, ScriptObject homeObject, Symbol methodName) {
        assert !function.isGenerator();
        /* step 1 */
        if (functionPrototype == null) {
            functionPrototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        }
        /* step 2 */
        OrdinaryFunction f = FunctionAllocate(cx, functionPrototype, function.isStrict(), kind);
        /* step 3 */
        return FunctionInitialise(cx, f, kind, function, scope, homeObject, methodName);
    }

    /**
     * 9.2.9 AddRestrictedFunctionProperties Abstract Operation
     */
    public static void AddRestrictedFunctionProperties(ExecutionContext cx, ScriptObject obj) {
        // TODO: %ThrowTypeError% from current realm or function's realm? (current realm per spec)
        /* step 1 */
        Callable thrower = cx.getRealm().getThrowTypeError();
        /* step 2 */
        DefinePropertyOrThrow(cx, obj, "caller", new PropertyDescriptor(thrower, thrower, false,
                false));
        /* step 3 */
        DefinePropertyOrThrow(cx, obj, "arguments", new PropertyDescriptor(thrower, thrower, false,
                false));
    }

    /**
     * 9.2.9 The %ThrowTypeError% Function Object
     */
    private static class TypeErrorThrower extends BuiltinFunction {
        TypeErrorThrower(Realm realm) {
            super(realm, "ThrowTypeError");
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            throw throwTypeError(calleeContext(), Messages.Key.StrictModePoisonPill);
        }
    }

    /**
     * 9.2.9 The %ThrowTypeError% Function Object
     */
    public static Callable createThrowTypeError(ExecutionContext cx) {
        /* step 1 */
        assert cx.getIntrinsic(Intrinsics.FunctionPrototype) != null;
        /* step 2 */
        ScriptObject functionPrototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        /* steps 3-8 (implicit) */
        // inlined FunctionAllocate()
        TypeErrorThrower f = new TypeErrorThrower(cx.getRealm());
        f.setPrototype(functionPrototype);
        // inlined FunctionInitialise()
        DefinePropertyOrThrow(cx, f, "length", new PropertyDescriptor(0, false, false, true));
        DefinePropertyOrThrow(cx, f, "name", new PropertyDescriptor("ThrowTypeError", false, false,
                false));
        // inlined AddRestrictedFunctionProperties()
        DefinePropertyOrThrow(cx, f, "caller", new PropertyDescriptor(f, f, false, false));
        DefinePropertyOrThrow(cx, f, "arguments", new PropertyDescriptor(f, f, false, false));
        /* step 9 */
        f.preventExtensions(cx);
        /* step 10 */
        return f;
    }

    /**
     * 9.2.10 MakeConstructor Abstract Operation
     */
    public static void MakeConstructor(ExecutionContext cx, FunctionObject f) {
        /* step 3 */
        boolean installNeeded = true;
        ScriptObject prototype = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 4 */
        boolean writablePrototype = true;
        MakeConstructor(cx, f, writablePrototype, prototype, installNeeded);
    }

    /**
     * 9.2.10 MakeConstructor Abstract Operation
     */
    public static void MakeConstructor(ExecutionContext cx, FunctionObject f,
            boolean writablePrototype, ScriptObject prototype) {
        /* step 2 */
        boolean installNeeded = false;
        MakeConstructor(cx, f, writablePrototype, prototype, installNeeded);
    }

    /**
     * 9.2.10 MakeConstructor Abstract Operation
     */
    private static void MakeConstructor(ExecutionContext cx, FunctionObject f,
            boolean writablePrototype, ScriptObject prototype, boolean installNeeded) {
        assert f instanceof Constructor : "MakeConstructor applied on non-Constructor";
        /* step 5 */
        f.isConstructor = true;
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
     * 9.2.11 SetFunctionName Abstract Operation
     */
    public static void SetFunctionName(ExecutionContext cx, FunctionObject f, String name) {
        SetFunctionName(cx, f, name, null);
    }

    /**
     * 9.2.11 SetFunctionName Abstract Operation
     */
    public static void SetFunctionName(ExecutionContext cx, FunctionObject f, String name,
            String prefix) {
        /* step 1 */
        assert f.isExtensible(cx) && !f.hasOwnProperty(cx, "name");
        /* step 2 (implicit) */
        /* step 3 (not applicable) */
        /* step 4 */
        if (!name.isEmpty() && prefix != null) {
            name = prefix + " " + name;
        }
        /* step 5 */
        boolean success = f.defineOwnProperty(cx, "name", new PropertyDescriptor(name, false,
                false, true));
        /* step 6 */
        assert success;
        /* step 7 (return) */
    }

    /**
     * 9.2.11 SetFunctionName Abstract Operation
     */
    public static void SetFunctionName(ExecutionContext cx, FunctionObject f, Symbol name) {
        SetFunctionName(cx, f, name, null);
    }

    /**
     * 9.2.11 SetFunctionName Abstract Operation
     */
    public static void SetFunctionName(ExecutionContext cx, FunctionObject f, Symbol name,
            String prefix) {
        /* step 3 */
        String description = name.getDescription();
        String sname = description == null ? "" : "[" + description + "]";
        /* steps 1-2, 4-7 */
        SetFunctionName(cx, f, sname, prefix);
    }

    /**
     * 9.2.12 GetSuperBinding(obj) Abstract Operation
     */
    public static ScriptObject GetSuperBinding(Object obj) {
        /* steps 1-2 */
        if (!(obj instanceof FunctionObject)) {
            return null;
        }
        /* step 3 */
        return ((FunctionObject) obj).getHomeObject();
    }

    /**
     * 9.2.13 RebindSuper(function, newHome) Abstract Operation
     */
    public static FunctionObject RebindSuper(ExecutionContext cx, FunctionObject function,
            ScriptObject newHome) {
        /* step 1 */
        assert function.getHomeObject() != null;
        /* step 2 */
        assert newHome != null;
        /* steps 3-6 */
        return function.rebind(newHome);
    }
}
