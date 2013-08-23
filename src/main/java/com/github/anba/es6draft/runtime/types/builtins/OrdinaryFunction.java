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
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArguments.CreateLegacyArguments;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo.Code;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.3 Ordinary Object Internal Methods and Internal Data Properties</h2>
 * <ul>
 * <li>8.3.16 Ordinary Function Objects
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
         * 8.3.16.2 [[Construct]] (argumentsList)
         */
        @Override
        public ScriptObject construct(ExecutionContext callerContext, Object... args) {
            return OrdinaryConstruct(callerContext, this, args);
        }
    }

    @Override
    public OrdinaryFunction rebind(ExecutionContext cx, ScriptObject newHomeObject) {
        assert isInitialised() : "uninitialised function object";
        Object methodName = getMethodName();
        if (methodName instanceof String) {
            return FunctionCreate(cx, getFunctionKind(), getFunction(), getScope(),
                    getInheritance(cx), newHomeObject, (String) methodName);
        }
        assert methodName instanceof ExoticSymbol;
        return FunctionCreate(cx, getFunctionKind(), getFunction(), getScope(), getInheritance(cx),
                newHomeObject, (ExoticSymbol) methodName);
    }

    /**
     * 8.3.16.1 [[Call]] Internal Method
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        if (!isInitialised()) {
            throw throwTypeError(callerContext, Messages.Key.IncompatibleObject);
        }
        /* steps 2-3 (implicit) */
        Object oldCaller = caller.getValue();
        Object oldArguments = arguments.getValue();
        try {
            /* steps 4-14 */
            ExecutionContext calleeContext = EvaluateArguments(callerContext, this, thisValue, args);
            /* step 15-16 */
            Object result = EvaluateBody(calleeContext, getCode());
            /* step 17 */
            return result;
        } finally {
            if (isLegacy()) {
                restoreLegacyProperties(oldCaller, oldArguments);
            }
        }
    }

    private static ExecutionContext EvaluateArguments(ExecutionContext callerContext,
            OrdinaryFunction f, Object thisValue, Object[] args) {
        /* step 4-12 */
        ExecutionContext calleeContext = ExecutionContext.newFunctionExecutionContext(f, thisValue);
        /* step 13-14 */
        ExoticArguments arguments = f.getFunction().functionDeclarationInstantiation(calleeContext,
                f, args);
        if (f.isLegacy()) {
            f.updateLegacyProperties(calleeContext, callerContext.getCurrentFunction(), arguments);
        }
        return calleeContext;
    }

    public static Object EvaluateBody(ExecutionContext calleeContext, Code code) {
        try {
            Object result = code.handle().invokeExact(calleeContext);
            // tail-call with trampoline
            while (result instanceof Object[]) {
                // <func(Callable), thisValue, args>
                Object[] h = (Object[]) result;
                OrdinaryFunction f = (OrdinaryFunction) h[0];
                Object thisValue = h[1];
                Object[] args = (Object[]) h[2];

                /* step 1 */
                if (!f.isInitialised()) {
                    throw throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
                }
                /* steps 2-3 (implicit) */
                Object oldCaller = f.caller.getValue();
                Object oldArguments = f.arguments.getValue();
                try {
                    /* step 4-14 */
                    calleeContext = EvaluateArguments(calleeContext, f, thisValue, args);
                    /* step 15-17 */
                    result = f.getCode().handle().invokeExact(calleeContext);
                } finally {
                    if (f.isLegacy()) {
                        f.restoreLegacyProperties(oldCaller, oldArguments);
                    }
                }
            }
            return result;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void updateLegacyProperties(ExecutionContext cx, FunctionObject caller,
            ExoticArguments arguments) {
        if (caller == null || caller.isStrict()) {
            this.caller.apply(new PropertyDescriptor(NULL));
        } else {
            this.caller.apply(new PropertyDescriptor(caller));
        }
        ExoticArguments args = CreateLegacyArguments(cx, arguments, this);
        this.arguments.apply(new PropertyDescriptor(args));
    }

    private void restoreLegacyProperties(Object oldCaller, Object oldArguments) {
        this.caller.apply(new PropertyDescriptor(oldCaller));
        this.arguments.apply(new PropertyDescriptor(oldArguments));
    }

    /* ***************************************************************************************** */

    /**
     * 8.3.16.2.1 OrdinaryConstruct (F, argumentsList)
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
     * 8.3.16.5 FunctionAllocate Abstract Operation
     */
    public static OrdinaryFunction FunctionAllocate(ExecutionContext cx,
            ScriptObject functionPrototype, FunctionKind kind) {
        Realm realm = cx.getRealm();
        /* steps 1-3 (implicit) */
        /* steps 4-6 */
        OrdinaryFunction f;
        if (kind == FunctionKind.Normal || kind == FunctionKind.ConstructorMethod) {
            f = new OrdinaryConstructorFunction(realm);
        } else {
            f = new OrdinaryFunction(realm);
        }
        /* step 7 */
        f.functionKind = kind;
        /* step 8 */
        f.setPrototype(functionPrototype);
        /* step 9 */
        // f.[[Extensible]] = true (implicit)
        /* step 10 */
        f.realm = realm;
        // support for legacy 'caller' and 'arguments' properties
        f.legacy = realm.isEnabled(CompatibilityOption.FunctionPrototype);
        /* step 11 */
        return f;
    }

    /**
     * 8.3.16.6 FunctionInitialise Abstract Operation
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialise(
            ExecutionContext cx, FUNCTION f, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment scope) {
        return FunctionInitialise(cx, f, kind, function, scope, null, (String) null);
    }

    /**
     * 8.3.16.6 FunctionInitialise Abstract Operation
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialise(
            ExecutionContext cx, FUNCTION f, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment scope, ScriptObject homeObject, String methodName) {
        boolean strict = (kind != FunctionKind.Arrow ? function.isStrict() : true);
        // first update 'legacy' flag, otherwise AddRestrictedFunctionProperties() fails
        f.legacy = f.legacy && !strict;

        /* step 1 */
        int len = function.expectedArgumentCount();
        /* step 2 */
        DefinePropertyOrThrow(cx, f, "length", new PropertyDescriptor(len, false, false, true));
        String name = function.functionName() != null ? function.functionName() : "";
        DefinePropertyOrThrow(cx, f, "name", new PropertyDescriptor(name, false, false, false));
        /* step 3 */
        if (strict) {
            AddRestrictedFunctionProperties(cx, f);
        }
        /* step 4 */
        f.scope = scope;
        /* steps 5-6 */
        f.function = function;
        /* step 7 */
        f.homeObject = homeObject;
        /* step 8 */
        f.methodName = methodName;
        /* step 9 */
        f.strict = strict;
        /* steps 10-12 */
        if (kind == FunctionKind.Arrow) {
            f.thisMode = ThisMode.Lexical;
        } else if (strict) {
            f.thisMode = ThisMode.Strict;
        } else {
            f.thisMode = ThisMode.Global;
        }
        /* step 13 */
        return f;
    }

    /**
     * 8.3.16.6 FunctionInitialise Abstract Operation
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialise(
            ExecutionContext cx, FUNCTION f, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment scope, ScriptObject homeObject, ExoticSymbol methodName) {
        boolean strict = (kind != FunctionKind.Arrow ? function.isStrict() : true);
        // first update 'legacy' flag, otherwise AddRestrictedFunctionProperties() fails
        f.legacy = f.legacy && !strict;

        /* step 1 */
        int len = function.expectedArgumentCount();
        /* step 2 */
        DefinePropertyOrThrow(cx, f, "length", new PropertyDescriptor(len, false, false, true));
        String name = function.functionName() != null ? function.functionName() : "";
        DefinePropertyOrThrow(cx, f, "name", new PropertyDescriptor(name, false, false, false));
        /* step 3 */
        if (strict) {
            AddRestrictedFunctionProperties(cx, f);
        }
        /* step 4 */
        f.scope = scope;
        /* steps 5-6 */
        f.function = function;
        /* step 7 */
        f.homeObject = homeObject;
        /* step 8 */
        f.methodName = methodName;
        /* step 9 */
        f.strict = strict;
        /* steps 10-12 */
        if (kind == FunctionKind.Arrow) {
            f.thisMode = ThisMode.Lexical;
        } else if (strict) {
            f.thisMode = ThisMode.Strict;
        } else {
            f.thisMode = ThisMode.Global;
        }
        /* step 13 */
        return f;
    }

    /**
     * 8.3.16.7 FunctionCreate Abstract Operation
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope) {
        return FunctionCreate(cx, kind, function, scope, null, null, (String) null);
    }

    /**
     * 8.3.16.7 FunctionCreate Abstract Operation
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, ScriptObject functionPrototype) {
        return FunctionCreate(cx, kind, function, scope, functionPrototype, null, (String) null);
    }

    /**
     * 8.3.16.7 FunctionCreate Abstract Operation
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
        OrdinaryFunction f = FunctionAllocate(cx, functionPrototype, kind);
        /* step 3 */
        return FunctionInitialise(cx, f, kind, function, scope, homeObject, methodName);
    }

    /**
     * 8.3.16.7 FunctionCreate Abstract Operation
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope,
            ScriptObject functionPrototype, ScriptObject homeObject, ExoticSymbol methodName) {
        assert !function.isGenerator();
        /* step 1 */
        if (functionPrototype == null) {
            functionPrototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        }
        /* step 2 */
        OrdinaryFunction f = FunctionAllocate(cx, functionPrototype, kind);
        /* step 3 */
        return FunctionInitialise(cx, f, kind, function, scope, homeObject, methodName);
    }

    /**
     * 8.3.16.9 AddRestrictedFunctionProperties Abstract Operation
     */
    public static void AddRestrictedFunctionProperties(ExecutionContext cx, ScriptObject obj) {
        /*  step 1  */
        Callable thrower = cx.getRealm().getThrowTypeError();
        /*  step 2  */
        DefinePropertyOrThrow(cx, obj, "caller", new PropertyDescriptor(thrower, thrower, false,
                false));
        /*  step 3  */
        DefinePropertyOrThrow(cx, obj, "arguments", new PropertyDescriptor(thrower, thrower, false,
                false));
    }

    /**
     * 8.3.16.9 The %ThrowTypeError% Function Object
     */
    private static class TypeErrorThrower extends BuiltinFunction {
        TypeErrorThrower(Realm realm) {
            super(realm);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            /* step 8 */
            throw throwTypeError(calleeContext(), Messages.Key.StrictModePoisonPill);
        }
    }

    /**
     * 8.3.16.9 The %ThrowTypeError% Function Object
     */
    public static Callable createThrowTypeError(ExecutionContext cx) {
        /* step 1 */
        assert cx.getIntrinsic(Intrinsics.FunctionPrototype) != null;
        /* step 2 */
        ScriptObject functionPrototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        /* step 3-8 (implicit) */
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

        return f;
    }

    /**
     * 8.3.16.10 MakeConstructor Abstract Operation
     */
    public static void MakeConstructor(ExecutionContext cx, FunctionObject f) {
        /*  step 2 */
        boolean installNeeded = true;
        ScriptObject prototype = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /*  step 3 */
        boolean writablePrototype = true;
        MakeConstructor(cx, f, writablePrototype, prototype, installNeeded);
    }

    /**
     * 8.3.16.9 MakeConstructor Abstract Operation
     */
    public static void MakeConstructor(ExecutionContext cx, FunctionObject f,
            boolean writablePrototype, ScriptObject prototype) {
        /* step 1 */
        boolean installNeeded = false;
        MakeConstructor(cx, f, writablePrototype, prototype, installNeeded);
    }

    /**
     * 8.3.16.9 MakeConstructor Abstract Operation
     */
    private static void MakeConstructor(ExecutionContext cx, FunctionObject f,
            boolean writablePrototype, ScriptObject prototype, boolean installNeeded) {
        assert f instanceof Constructor : "MakeConstructor applied on non-Constructor";
        /* step 4 */
        f.isConstructor = true;
        /* step 5 */
        if (installNeeded) {
            prototype.defineOwnProperty(cx, "constructor", new PropertyDescriptor(f,
                    writablePrototype, false, writablePrototype));
        }
        /* step 7 */
        f.defineOwnProperty(cx, "prototype", new PropertyDescriptor(prototype, writablePrototype,
                false, false));
    }
}
