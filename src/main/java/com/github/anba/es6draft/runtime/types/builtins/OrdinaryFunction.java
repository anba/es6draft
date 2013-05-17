/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArguments.CreateLegacyArguments;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo.Code;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.3 Ordinary Object Internal Methods and Internal Data Properties</h2>
 * <ul>
 * <li>8.3.19 Ordinary Function Objects
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

        /**
         * 8.3.15.2 [[Construct]] (argumentsList)
         */
        @Override
        public Object construct(ExecutionContext callerContext, Object... args) {
            return OrdinaryConstruct(callerContext, this, args);
        }
    }

    /**
     * 8.3.15.1 [[Call]] Internal Method
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        if (!isInitialised()) {
            throw throwTypeError(callerContext, Messages.Key.IncompatibleObject);
        }
        Object oldCaller = caller.getValue();
        Object oldArguments = arguments.getValue();
        try {
            FunctionObject caller = callerContext.getCurrentFunction();
            /* step 1-11 */
            ExecutionContext calleeContext = ExecutionContext.newFunctionExecutionContext(this,
                    thisValue);
            /* step 12-13 */
            ExoticArguments arguments = getFunction().functionDeclarationInstantiation(
                    calleeContext, this, args);
            if (!isStrict()) {
                updateLegacyProperties(calleeContext, caller, arguments);
            }
            /* step 14-15 */
            Object result = EvaluateBody(calleeContext, getCode());
            /* step 16 */
            return result;
        } finally {
            if (!isStrict()) {
                caller.apply(new PropertyDescriptor(oldCaller));
                arguments.apply(new PropertyDescriptor(oldArguments));
            }
        }
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

                if (!f.isInitialised()) {
                    throw throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
                }

                Object oldCaller = f.caller.getValue();
                Object oldArguments = f.arguments.getValue();
                try {
                    FunctionObject caller = calleeContext.getCurrentFunction();
                    /* step 1-11 */
                    calleeContext = ExecutionContext.newFunctionExecutionContext(f, thisValue);
                    /* step 12-13 */
                    ExoticArguments arguments = f.getFunction().functionDeclarationInstantiation(
                            calleeContext, f, args);
                    if (!f.isStrict()) {
                        f.updateLegacyProperties(calleeContext, caller, arguments);
                    }
                    /* step 14-15 */
                    result = f.getCode().handle().invokeExact(calleeContext);
                } finally {
                    if (!f.isStrict()) {
                        f.caller.apply(new PropertyDescriptor(oldCaller));
                        f.arguments.apply(new PropertyDescriptor(oldArguments));
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
        if (!(caller == null || caller.isStrict())) {
            this.caller.apply(new PropertyDescriptor(caller));
        } else {
            this.caller.apply(new PropertyDescriptor(NULL));
        }
        ExoticArguments args = CreateLegacyArguments(cx, arguments, this);
        this.arguments.apply(new PropertyDescriptor(args));
    }

    /* ***************************************************************************************** */

    /**
     * 8.3.15.2.1 OrdinaryConstruct (F, argumentsList)
     */
    public static <FUNCTION extends ScriptObject & Callable & Constructor> Object OrdinaryConstruct(
            ExecutionContext cx, FUNCTION f, Object[] args) {
        Object creator = Get(cx, f, BuiltinSymbol.create.get());
        Object obj;
        if (!Type.isUndefined(creator)) {
            if (!IsCallable(creator)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            obj = ((Callable) creator).call(cx, f);
        } else {
            obj = OrdinaryCreateFromConstructor(cx, f, Intrinsics.ObjectPrototype);
        }
        if (!Type.isObject(obj)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        Object result = f.call(cx, obj, args);
        if (Type.isObject(result)) {
            return result;
        }
        return obj;
    }

    /**
     * 8.3.15.5 FunctionAllocate Abstract Operation
     */
    public static OrdinaryFunction FunctionAllocate(ExecutionContext cx, ScriptObject prototype,
            FunctionKind kind) {
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
        f.setPrototype(cx, prototype);
        /* step 10 */
        // f.[[Extensible]] = true (implicit)
        /* step 10 */
        f.realm = realm;
        /* step 11 */
        return f;
    }

    /**
     * 8.3.15.6 FunctionInitialize Abstract Operation
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialize(
            ExecutionContext cx, FUNCTION f, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment scope) {
        return FunctionInitialize(cx, f, kind, function, scope, null, null);
    }

    /**
     * 8.3.15.6 FunctionInitialize Abstract Operation
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialize(
            ExecutionContext cx, FUNCTION f, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment scope, ScriptObject homeObject, String methodName) {
        boolean strict = (kind != FunctionKind.Arrow ? function.isStrict() : true);

        /* step 2 */
        f.scope = scope;
        /* steps 3-4 */
        f.function = function;
        /* step 4 */
        f.homeObject = homeObject;
        /* step 5 */
        f.methodName = methodName;
        /* step 7 */
        f.strict = strict;
        /* step 8 */
        if (kind == FunctionKind.Arrow) {
            f.thisMode = ThisMode.Lexical;
        } else if (strict) {
            f.thisMode = ThisMode.Strict;
        } else {
            f.thisMode = ThisMode.Global;
        }
        /*  step 11 */
        int len = function.expectedArgumentCount();
        /* step 12 */
        f.defineOwnProperty(cx, "length", new PropertyDescriptor(len, false, false, false));
        String name = function.functionName() != null ? function.functionName() : "";
        f.defineOwnProperty(cx, "name", new PropertyDescriptor(name, false, false, false));
        /* step 13 */
        if (strict) {
            AddRestrictedFunctionProperties(cx, f);
        }
        /* step 14 */
        return f;
    }

    /**
     * 8.3.15.7 FunctionCreate Abstract Operation
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope) {
        return FunctionCreate(cx, kind, function, scope, null, null, null);
    }

    /**
     * 8.3.15.7 FunctionCreate Abstract Operation
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, ScriptObject prototype) {
        return FunctionCreate(cx, kind, function, scope, prototype, null, null);
    }

    /**
     * 8.3.15.7 FunctionCreate Abstract Operation
     */
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, ScriptObject prototype,
            ScriptObject homeObject, String methodName) {
        assert !function.isGenerator();
        /* step 1 */
        if (prototype == null) {
            prototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        }
        /* step 2 */
        OrdinaryFunction f = FunctionAllocate(cx, prototype, kind);
        /* step 3 */
        return FunctionInitialize(cx, f, kind, function, scope, homeObject, methodName);
    }

    /**
     * 8.3.15.8 AddRestrictedFunctionProperties Abstract Operation
     */
    public static void AddRestrictedFunctionProperties(ExecutionContext cx, ScriptObject obj) {
        /*  step 1  */
        Callable thrower = cx.getRealm().getThrowTypeError();
        /*  step 2  */
        obj.defineOwnProperty(cx, "caller", new PropertyDescriptor(thrower, thrower, false, false));
        /*  step 3  */
        obj.defineOwnProperty(cx, "arguments", new PropertyDescriptor(thrower, thrower, false,
                false));
    }

    /**
     * 8.3.15.8 The %ThrowTypeError% Function Object
     */
    private static class TypeErrorThrower extends BuiltinFunction {
        TypeErrorThrower(Realm realm) {
            super(realm);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = realm().defaultContext();
            /* step 8 */
            throw throwTypeError(calleeContext, Messages.Key.StrictModePoisonPill);
        }
    }

    /**
     * 8.3.15.8 The %ThrowTypeError% Function Object
     */
    public static Callable createThrowTypeError(ExecutionContext cx) {
        /* step 1-4 (implicit) */
        TypeErrorThrower f = new TypeErrorThrower(cx.getRealm());
        f.setPrototype(cx, cx.getIntrinsic(Intrinsics.FunctionPrototype));
        f.defineOwnProperty(cx, "length", new PropertyDescriptor(0, false, false, false));
        f.defineOwnProperty(cx, "name", new PropertyDescriptor("ThrowTypeError", false, false,
                false));
        f.defineOwnProperty(cx, "caller", new PropertyDescriptor(f, f, false, false));
        f.defineOwnProperty(cx, "arguments", new PropertyDescriptor(f, f, false, false));
        /* step 5 */
        f.setIntegrity(cx, IntegrityLevel.NonExtensible);
        /* step 6 */
        return f;
    }

    /**
     * 8.3.15.9 MakeConstructor Abstract Operation
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
     * 8.3.15.9 MakeConstructor Abstract Operation
     */
    public static void MakeConstructor(ExecutionContext cx, FunctionObject f,
            boolean writablePrototype, ScriptObject prototype) {
        /* step 1 */
        boolean installNeeded = false;
        MakeConstructor(cx, f, writablePrototype, prototype, installNeeded);
    }

    /**
     * 8.3.15.9 MakeConstructor Abstract Operation
     */
    private static void MakeConstructor(ExecutionContext cx, FunctionObject f,
            boolean writablePrototype, ScriptObject prototype, boolean installNeeded) {
        assert f instanceof Constructor : "MakeConstructor applied on non-Constructor";
        /* step 4 (implicit) */
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
