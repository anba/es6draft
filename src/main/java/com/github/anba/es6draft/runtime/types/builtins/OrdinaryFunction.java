/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.AbstractOperations.ConstructTailCall;
import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import com.github.anba.es6draft.Executable;
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
    /**
     * Constructs a new Function object.
     * 
     * @param realm
     *            the realm object
     */
    protected OrdinaryFunction(Realm realm) {
        super(realm);
    }

    private static final class OrdinaryConstructorFunction extends OrdinaryFunction implements
            Constructor {
        public OrdinaryConstructorFunction(Realm realm) {
            super(realm);
        }

        /**
         * 9.2.3 [[Construct]] (argumentsList)
         */
        @Override
        public ScriptObject construct(ExecutionContext callerContext, Object... args) {
            if (getCode() == null) {
                throw newTypeError(callerContext, Messages.Key.UninitializedObject);
            }
            return Construct(callerContext, this, args);
        }

        /**
         * 9.2.3 [[Construct]] (argumentsList)
         */
        @Override
        public Object tailConstruct(ExecutionContext callerContext, Object... args)
                throws Throwable {
            if (getCode() == null) {
                throw newTypeError(callerContext, Messages.Key.UninitializedObject);
            }
            return ConstructTailCall(callerContext, this, args);
        }
    }

    @Override
    protected OrdinaryFunction allocateNew() {
        return FunctionAllocate(getRealm().defaultContext(), getPrototype(), isStrict(),
                getFunctionKind());
    }

    /**
     * 9.2.2 [[Call]] (thisArgument, argumentsList)
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
     * 9.2.2 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args)
            throws Throwable {
        return getTailCallMethod().invokeExact(this, callerContext, thisValue, args);
    }

    /* ***************************************************************************************** */

    /**
     * 9.2.4 FunctionAllocate (functionPrototype, strict) Abstract Operation
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
     * 9.2.5 FunctionInitialize (F, kind, Strict, ParameterList, Body, Scope) Abstract Operation
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param kind
     *            the function kind
     * @param strict
     *            the strict mode flag
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @param executable
     *            the executable object
     * @return the function object
     */
    /*package*/static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialize(FUNCTION f,
            FunctionKind kind, boolean strict, RuntimeInfo.Function function,
            LexicalEnvironment<?> scope, Executable executable) {
        /* step 1 */
        int len = function.expectedArgumentCount();
        /* step 2 (not applicable) */
        /* steps 3-4 */
        f.infallibleDefineOwnProperty("length", new PropertyDescriptor(len, false, false, true));
        /* steps 5-11 */
        f.initialize(kind, strict, function, scope, executable);
        /* step 12 */
        return f;
    }

    /**
     * 9.2.5 FunctionInitialize (F, kind, Strict, ParameterList, Body, Scope) Abstract Operation
     * 
     * @param <FUNCTION>
     *            the function type
     * @param cx
     *            the execution context
     * @param f
     *            the function object
     * @param kind
     *            the function kind
     * @param strict
     *            the strict mode flag
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @param executable
     *            the executable object
     * @return the function object
     */
    public static <FUNCTION extends FunctionObject> FUNCTION FunctionInitialize(
            ExecutionContext cx, FUNCTION f, FunctionKind kind, boolean strict,
            RuntimeInfo.Function function, LexicalEnvironment<?> scope, Executable executable) {
        /* step 1 */
        int len = function.expectedArgumentCount();
        /* step 2 (not applicable) */
        /* steps 3-4 */
        DefinePropertyOrThrow(cx, f, "length", new PropertyDescriptor(len, false, false, true));
        /* steps 5-11 */
        f.initialize(kind, strict, function, scope, executable);
        /* step 12 */
        return f;
    }

    /**
     * 9.2.6 FunctionCreate (kind, ParameterList, Body, Scope, Strict) Abstract Operation
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
     * 9.2.6 FunctionCreate (kind, ParameterList, Body, Scope, Strict) Abstract Operation
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
        return FunctionInitialize(f, kind, function.isStrict(), function, scope,
                cx.getCurrentExecutable());
    }

    /**
     * 9.2.8 AddRestrictedFunctionProperties ( F, realm ) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param f
     *            the function object
     * @param realm
     *            the realm object
     */
    public static void AddRestrictedFunctionProperties(ExecutionContext cx, Callable f, Realm realm) {
        /* steps 1-2 */
        Callable thrower = realm.getThrowTypeError();
        /* steps 3-4 */
        DefinePropertyOrThrow(cx, f, "caller",
                new PropertyDescriptor(thrower, thrower, false, true));
        /* steps 5-6 */
        DefinePropertyOrThrow(cx, f, "arguments", new PropertyDescriptor(thrower, thrower, false,
                true));
    }

    /**
     * 9.2.9 MakeConstructor (F, writablePrototype, prototype) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param f
     *            the function object
     */
    public static void MakeConstructor(ExecutionContext cx, FunctionObject f) {
        /* step 1 (not applicable) */
        /* step 2 */
        assert f instanceof Constructor : "MakeConstructor applied on non-Constructor";
        /* step 3 (not applicable) */
        /* step 4 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 5 */
        boolean writablePrototype = true;
        /* step 6 */
        ScriptObject superF = f.getPrototype();
        /* steps 7-8 */
        f.setCreateActionFrom(superF);
        /* step 9 */
        DefinePropertyOrThrow(cx, prototype, "constructor", new PropertyDescriptor(f,
                writablePrototype, false, writablePrototype));
        /* steps 10-11 */
        DefinePropertyOrThrow(cx, f, "prototype", new PropertyDescriptor(prototype,
                writablePrototype, false, false));
        /* step 12 (return) */
    }

    /**
     * 9.2.9 MakeConstructor (F, writablePrototype, prototype) Abstract Operation
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
        /* step 1 (not applicable) */
        /* step 2 */
        assert f instanceof Constructor : "MakeConstructor applied on non-Constructor";
        /* steps 3-5 (not applicable) */
        /* step 6 */
        ScriptObject superF = f.getPrototype();
        /* steps 7-8 */
        f.setCreateActionFrom(superF);
        /* step 9 (not applicable) */
        /* steps 10-11 */
        DefinePropertyOrThrow(cx, f, "prototype", new PropertyDescriptor(prototype,
                writablePrototype, false, false));
        /* step 12 (return) */
    }

    /**
     * 9.2.10 MakeMethod ( F, homeObject ) Abstract Operation
     * 
     * @param f
     *            the function object
     * @param homeObject
     *            the home object
     */
    public static void MakeMethod(FunctionObject f, ScriptObject homeObject) {
        /* steps 1-3 (not applicable) */
        /* steps 4-6 */
        f.toMethod(homeObject);
        /* step 7 (return) */
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix) Abstract Operation
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
     * 9.2.11 SetFunctionName (F, name, prefix) Abstract Operation
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
        assert !f.ordinaryHasOwnProperty("name") : "function has 'name' property";
        /* steps 2-3 (implicit) */
        /* step 4 (not applicable) */
        /* step 5 */
        if (prefix != null) {
            name = prefix + " " + name;
        }
        /* step 6 */
        f.infallibleDefineOwnProperty("name", new PropertyDescriptor(name, false, false, true));
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix) Abstract Operation
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
     * 9.2.11 SetFunctionName (F, name, prefix) Abstract Operation
     * 
     * @param f
     *            the function object
     * @param name
     *            the function name
     * @param prefix
     *            the function name prefix
     */
    public static void SetFunctionName(FunctionObject f, Symbol name, String prefix) {
        /* step 1 */
        assert f.isExtensible() : "function is not extensible";
        assert !f.ordinaryHasOwnProperty("name") : "function has 'name' property";
        /* steps 2-3 (implicit) */
        /* step 4 */
        String description = name.getDescription();
        String sname = description == null ? "" : "[" + description + "]";
        /* step 5 */
        if (prefix != null) {
            sname = prefix + " " + sname;
        }
        /* step 6 */
        f.infallibleDefineOwnProperty("name", new PropertyDescriptor(sname, false, false, true));
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param f
     *            the function object
     * @param name
     *            the function name
     * @param prefix
     *            the function name prefix
     */
    public static void SetFunctionName(ExecutionContext cx, BoundFunctionObject f, String name,
            String prefix) {
        /* step 1 */
        assert f.isExtensible() : "function is not extensible";
        assert !f.ordinaryHasOwnProperty("name") : "function has 'name' property";
        /* steps 2-3 (implicit) */
        /* step 4 (not applicable) */
        /* step 5 */
        if (prefix != null) {
            name = prefix + " " + name;
        }
        /* step 6 */
        DefinePropertyOrThrow(cx, f, "name", new PropertyDescriptor(name, false, false, true));
    }

    /**
     * 9.2.12 CloneMethod(function, newHome) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param function
     *            the function object
     * @param newHome
     *            the new home object
     * @return the cloned function object
     */
    public static FunctionObject CloneMethod(ExecutionContext cx, FunctionObject function,
            ScriptObject newHome) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        /* steps 4-7, 9 */
        FunctionObject clone = function.clone(cx);
        /* step 8 */
        if (function.isNeedsSuper()) {
            clone.toMethod(newHome);
        }
        /* step 9 */
        return clone;
    }

    /**
     * 9.2.12 CloneMethod(function, newHome, newName) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param function
     *            the function object
     * @return the cloned function object
     */
    public static BuiltinFunction CloneMethod(ExecutionContext cx, BuiltinFunction function) {
        /* steps 1-3 (not applicable) */
        /* steps 4-7, 9 */
        BuiltinFunction clone = function.clone(cx);
        /* step 8 (not applicable) */
        /* step 10 */
        return clone;
    }
}
