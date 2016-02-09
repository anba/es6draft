/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public final class OrdinaryFunction extends FunctionObject {
    /**
     * Constructs a new Function object.
     * 
     * @param realm
     *            the realm object
     */
    protected OrdinaryFunction(Realm realm) {
        super(realm);
    }

    @Override
    protected OrdinaryFunction allocateNew() {
        return FunctionAllocate(getRealm().defaultContext(), getPrototype(), isStrict(), getFunctionKind());
    }

    /**
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... argumentsList) {
        try {
            return getCallMethod().invokeExact(this, callerContext, thisValue, argumentsList);
        } catch (Throwable e) {
            throw FunctionObject.<RuntimeException> rethrow(e);
        }
    }

    /**
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... argumentsList) throws Throwable {
        return getTailCallMethod().invokeExact(this, callerContext, thisValue, argumentsList);
    }

    /* ***************************************************************************************** */

    /**
     * 9.2.3 FunctionAllocate (functionPrototype, strict)
     * 
     * @param cx
     *            the execution context
     * @param functionPrototype
     *            the function prototype
     * @param strict
     *            the strict mode flag
     * @param functionKind
     *            the function kind
     * @return the new function object
     */
    public static OrdinaryFunction FunctionAllocate(ExecutionContext cx, ScriptObject functionPrototype, boolean strict,
            FunctionKind functionKind) {
        assert !(functionKind == FunctionKind.Normal || functionKind == FunctionKind.ClassConstructor);
        Realm realm = cx.getRealm();
        /* steps 1-5 (implicit) */
        /* steps 6-9 */
        OrdinaryFunction f = new OrdinaryFunction(realm);
        /* steps 10-14 */
        f.allocate(realm, functionPrototype, strict, functionKind, ConstructorKind.Base);
        /* step 15 */
        return f;
    }

    /**
     * 9.2.4 FunctionInitialize (F, kind, ParameterList, Body, Scope)
     * 
     * @param f
     *            the function object
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @param executable
     *            the executable object
     */
    public static void FunctionInitialize(FunctionObject f, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment<?> scope, Executable executable) {
        /* step 1 */
        assert f.isExtensible() && !f.ordinaryHasOwnProperty("length");
        /* step 2 */
        int len = function.expectedArgumentCount();
        /* steps 3-4 */
        f.infallibleDefineOwnProperty("length", new Property(len, false, false, true));
        /* steps 5-11 */
        f.initialize(kind, function, scope, executable);
        /* step 12 (return) */
    }

    /**
     * 9.2.5 FunctionCreate (kind, ParameterList, Body, Scope, Strict, prototype)
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
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment<?> scope) {
        assert !function.isGenerator() && !function.isAsync();
        /* step 1 */
        ScriptObject functionPrototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        /* steps 2-3 (not applicable) */
        /* step 4 */
        OrdinaryFunction f = FunctionAllocate(cx, functionPrototype, function.isStrict(), kind);
        /* step 5 */
        FunctionInitialize(f, kind, function, scope, cx.getCurrentExecutable());
        return f;
    }

    /**
     * 9.2.7 AddRestrictedFunctionProperties ( F, realm )
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param realm
     *            the realm object
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void AddRestrictedFunctionProperties(FUNCTION f,
            Realm realm) {
        /* steps 1-2 */
        Callable thrower = realm.getThrowTypeError();
        /* steps 3-4 */
        f.infallibleDefineOwnProperty("caller", new Property(thrower, thrower, false, true));
        /* steps 5-6 */
        f.infallibleDefineOwnProperty("arguments", new Property(thrower, thrower, false, true));
    }

    /**
     * 9.2.8 MakeConstructor (F, writablePrototype, prototype)
     * 
     * @param <CONSTRUCTOR>
     *            the constructor function type
     * @param cx
     *            the execution context
     * @param f
     *            the function object
     */
    public static <CONSTRUCTOR extends FunctionObject & Constructor> void MakeConstructor(ExecutionContext cx,
            CONSTRUCTOR f) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert f.isExtensible() && !f.ordinaryHasOwnProperty("prototype");
        /* step 4 */
        boolean writablePrototype = true;
        /* step 5.a */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 5.b-c */
        prototype.infallibleDefineOwnProperty("constructor", new Property(f, writablePrototype, false, true));
        /* steps 6-7 */
        f.infallibleDefineOwnProperty("prototype", new Property(prototype, writablePrototype, false, false));
        /* step 8 (return) */
    }

    /**
     * 9.2.8 MakeConstructor (F, writablePrototype, prototype)
     * 
     * @param <CONSTRUCTOR>
     *            the constructor function type
     * @param f
     *            the function object
     * @param writablePrototype
     *            the writable flag for the .prototype property
     * @param prototype
     *            the prototype object
     */
    public static <CONSTRUCTOR extends FunctionObject & Constructor> void MakeConstructor(CONSTRUCTOR f,
            boolean writablePrototype, ScriptObject prototype) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert f.isExtensible() && !f.ordinaryHasOwnProperty("prototype");
        /* steps 4-5 (not applicable) */
        /* steps 6-7 */
        f.infallibleDefineOwnProperty("prototype", new Property(prototype, writablePrototype, false, false));
        /* step 8 (return) */
    }

    /**
     * 9.2.9 MakeClassConstructor (F)
     * 
     * @param f
     *            the function object
     */
    public static void MakeClassConstructor(OrdinaryConstructorFunction f) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        assert f.getFunctionKind() == FunctionKind.ClassConstructor;
        /* step 4 (return) */
    }

    /**
     * 9.2.10 MakeMethod (F, homeObject)
     * 
     * @param f
     *            the function object
     * @param homeObject
     *            the home object
     */
    public static void MakeMethod(FunctionObject f, ScriptObject homeObject) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        f.toMethod(homeObject);
        /* step 4 (return) */
    }

    /**
     * 9.2.10 MakeMethod (F, homeObject)
     * 
     * @param f
     *            the function object
     * @param homeObject
     *            the home object
     */
    public static void MakeMethod(FunctionObject f, OrdinaryObject homeObject) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        f.toMethod(homeObject);
        /* step 4 (return) */
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, String name) {
        SetFunctionName(f, name, null);
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     * @param prefix
     *            the function name prefix
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, String name,
            String prefix) {
        /* step 1 */
        assert f.isExtensible() && !f.ordinaryHasOwnProperty("name");
        /* steps 2-4 (not applicable) */
        /* step 5 */
        if (prefix != null) {
            name = prefix + " " + name;
        }
        /* steps 6-7 */
        f.infallibleDefineOwnProperty("name", new Property(name, false, false, true));
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, Symbol name) {
        SetFunctionName(f, name, null);
    }

    /**
     * 9.2.11 SetFunctionName (F, name, prefix)
     * 
     * @param <FUNCTION>
     *            the function type
     * @param f
     *            the function object
     * @param name
     *            the function name
     * @param prefix
     *            the function name prefix
     */
    public static <FUNCTION extends OrdinaryObject & Callable> void SetFunctionName(FUNCTION f, Symbol name,
            String prefix) {
        /* step 1 */
        assert f.isExtensible() && !f.ordinaryHasOwnProperty("name");
        /* steps 2-3 (not applicable) */
        /* step 4 */
        String description = name.getDescription();
        String sname = description == null ? "" : "[" + description + "]";
        /* step 5 */
        if (prefix != null) {
            sname = prefix + " " + sname;
        }
        /* steps 6-7 */
        f.infallibleDefineOwnProperty("name", new Property(sname, false, false, true));
    }
}
