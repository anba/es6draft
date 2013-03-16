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

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.SourceCompressor;
import com.github.anba.es6draft.runtime.types.*;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.3 Ordinary Object Internal Methods and Internal Data Properties</h2>
 * <ul>
 * <li>8.3.19 Ordinary Function Objects
 * </ul>
 */
public class OrdinaryFunction extends OrdinaryObject implements Function {
    private FunctionKind kind;
    private RuntimeInfo.Function function;
    private LexicalEnvironment scope;
    private boolean strict;
    private Scriptable home;
    private String methodName;
    private Realm realm;
    private ThisMode thisMode;

    private String source = null;

    private OrdinaryFunction(Realm realm) {
        super(realm);
    }

    /**
     * [13.6 Creating Function Objects and Constructors] FunctionCreate
     */
    public static Function FunctionCreate(Realm realm, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope) {
        return FunctionCreate(realm, kind, function, scope, null, null, null);
    }

    /**
     * [13.6 Creating Function Objects and Constructors] FunctionCreate
     */
    public static Function FunctionCreate(Realm realm, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, Scriptable prototype) {
        return FunctionCreate(realm, kind, function, scope, prototype, null, null);
    }

    /**
     * [13.6 Creating Function Objects and Constructors] FunctionCreate
     */
    public static Function FunctionCreate(Realm realm, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, Scriptable prototype,
            Scriptable homeObject, String methodName) {
        assert !function.isGenerator();

        boolean strict = (kind != FunctionKind.Arrow ? function.isStrict() : true);
        /* step 1 */
        OrdinaryFunction f = new OrdinaryFunction(realm);
        /* step 2-4 (implicit) */
        /* step 5 */
        if (prototype == null) {
            prototype = realm.getIntrinsic(Intrinsics.FunctionPrototype);
        }
        /* step 6 */
        f.setPrototype(prototype);
        /* step 7 */
        f.scope = scope;
        /* step 8-9 */
        f.function = function;
        /* step 10 */
        // f.[[Extensible]] = true (implicit)
        /* step 11 */
        f.realm = realm;
        /* step 12 */
        f.home = homeObject;
        /* step 13 */
        f.methodName = methodName;
        /* step 14 */
        f.strict = strict;
        /* step 15-17 */
        f.kind = kind;
        if (kind == FunctionKind.Arrow) {
            f.thisMode = ThisMode.Lexical;
        } else if (strict) {
            f.thisMode = ThisMode.Strict;
        } else {
            f.thisMode = ThisMode.Global;
        }
        /*  step 18 */
        int len = function.expectedArgumentCount();
        /* step 19 */
        f.defineOwnProperty("length", new PropertyDescriptor(len, false, false, false));
        String name = function.functionName() != null ? function.functionName() : "";
        f.defineOwnProperty("name", new PropertyDescriptor(name, false, false, false));
        /* step 20 */
        if (strict) {
            AddRestrictedFunctionProperties(realm, f);
        }
        /* step 21 */
        return f;
    }

    /**
     * [13.6 Creating Function Objects and Constructors] MakeConstructor
     */
    public static void MakeConstructor(Realm realm, Function f) {
        /*  step 2 */
        boolean installNeeded = true;
        Scriptable prototype = ObjectCreate(realm, Intrinsics.ObjectPrototype);
        /*  step 3 */
        boolean writablePrototype = true;
        MakeConstructor(realm, f, writablePrototype, prototype, installNeeded);
    }

    /**
     * [13.6 Creating Function Objects and Constructors] MakeConstructor
     */
    public static void MakeConstructor(Realm realm, Function f, boolean writablePrototype,
            Scriptable prototype) {
        /* step 1 */
        boolean installNeeded = false;
        MakeConstructor(realm, f, writablePrototype, prototype, installNeeded);
    }

    /**
     * [13.6 Creating Function Objects and Constructors] MakeConstructor
     */
    private static void MakeConstructor(Realm realm, Function f, boolean writablePrototype,
            Scriptable prototype, boolean installNeeded) {
        /* step 4 (implicit) */
        /* step 5 */
        if (installNeeded) {
            prototype.defineOwnProperty("constructor", new PropertyDescriptor(f, writablePrototype,
                    false, writablePrototype));
        }
        /* step 7 */
        f.defineOwnProperty("prototype", new PropertyDescriptor(prototype, writablePrototype,
                false, false));
    }

    /**
     * 13.6.3 The [[ThrowTypeError]] Function Object
     */
    private static class TypeErrorThrower extends OrdinaryObject implements Callable {
        TypeErrorThrower(Realm realm) {
            super(realm);
        }

        @Override
        public BuiltinBrand getBuiltinBrand() {
            /* step 3 */
            return BuiltinBrand.BuiltinFunction;
        }

        @Override
        public String toSource() {
            return "function TypeErrorThrower() { /* native code */ }";
        }

        @Override
        public Object call(Object thisValue, Object... args) {
            /* step 8 */
            throw throwTypeError(realm(), Messages.Key.StrictModePoisonPill);
        }
    }

    /**
     * [13.6.3] The [[ThrowTypeError]] Function Object
     */
    public static Callable createThrowTypeError(Realm realm) {
        // FIXME: spec bug (section 8.12 does not exist) (bug 1057)
        /* step 1-3 (implicit) */
        TypeErrorThrower f = new TypeErrorThrower(realm);
        /* step 4 */
        f.setPrototype(realm.getIntrinsic(Intrinsics.FunctionPrototype));
        /* step 5-8 (implicit) */
        /* step 9 */
        f.defineOwnProperty("length", new PropertyDescriptor(0, false, false, false));
        f.defineOwnProperty("name", new PropertyDescriptor("ThrowTypeError", false, false, false));
        /* step 10 */
        f.setIntegrity(IntegrityLevel.NonExtensible);

        return f;
    }

    /**
     * [13.6 Creating Function Objects and Constructors] AddRestrictedFunctionProperties
     */
    public static void AddRestrictedFunctionProperties(Realm realm, Scriptable obj) {
        /*  step 1  */
        Callable thrower = realm.getThrowTypeError();
        /*  step 2  */
        obj.defineOwnProperty("caller", new PropertyDescriptor(thrower, thrower, false, false));
        /*  step 3  */
        obj.defineOwnProperty("arguments", new PropertyDescriptor(thrower, thrower, false, false));
    }

    /**
     * [Runtime Semantics: InstantiateFunctionObject]
     */
    public static Function InstantiateFunctionObject(Realm realm, LexicalEnvironment scope,
            RuntimeInfo.Function fd) {
        /* step 1-2 */
        Function f = FunctionCreate(realm, FunctionKind.Normal, fd, scope);
        /* step 3 */
        MakeConstructor(realm, f);
        /* step 4 */
        return f;
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        /* [13.6 FunctionCreate] step 3 */
        return BuiltinBrand.BuiltinFunction;
    }

    @Override
    public String toSource() {
        String source = this.source;
        if (source == null) {
            String src = function.source();
            if (src != null) {
                try {
                    source = SourceCompressor.decompress(src).call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                source = "function F() { /* source not available */ }";
            }
            this.source = source;
        }
        return source;
    }

    /**
     * 8.3.15.1 [[Call]] Internal Method
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        /* step 1-11 */
        ExecutionContext calleeContext = ExecutionContext.newFunctionExecutionContext(this,
                thisValue);
        /* step 12-13 */
        getFunction().functionDeclarationInstantiation(calleeContext, this, args);
        /* step 14-15 */
        Object result = getCode().evaluate(calleeContext);
        /* step 16 */
        return result;
    }

    /**
     * 8.3.15.2 [[Construct]] Internal Method
     */
    @Override
    public Object construct(Object... args) {
        return OrdinaryConstruct(realm, this, args);
    }

    /**
     * 8.3.15.2.1 OrdinaryConstruct (F, argumentsList)
     */
    public static <FUNCTION extends Scriptable & Callable & Constructor> Object OrdinaryConstruct(
            Realm realm, FUNCTION f, Object[] args) {
        Object creator = Get(f, BuiltinSymbol.create.get());
        Object obj;
        if (!Type.isUndefined(creator)) {
            if (!IsCallable(creator)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            obj = ((Callable) creator).call(f);
        } else {
            obj = OrdinaryCreateFromConstructor(realm, f, Intrinsics.ObjectPrototype);
        }
        Object result = f.call(obj, args);
        if (Type.isObject(result)) {
            return result;
        }
        return obj;
    }

    /**
     * 8.3.15.3 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(String propertyKey, Object receiver) {
        /* step 1-2 */
        Object v = super.get(propertyKey, receiver);
        /* step 3 */
        if ("caller".equals(propertyKey) && isStrictFunction(v)) {
            return NULL;
        }
        /* step 4 */
        return v;
    }

    // FIXME: spec bug (caption not updated from 8.3.19.4 to 8.3.15.4)
    /**
     * 8.3.15.4 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(String propertyKey) {
        /* step 1-2 */
        Property v = super.getOwnProperty(propertyKey);
        if (v != null && v.isDataDescriptor()) {
            if ("caller".equals(propertyKey) && isStrictFunction(v)) {
                PropertyDescriptor desc = v.toPropertyDescriptor();
                desc.setValue(NULL);
                v = desc.toProperty();
            }
        }
        return v;
    }

    private static boolean isStrictFunction(Object v) {
        return v instanceof Function && ((Function) v).isStrict();
    }

    @Override
    public FunctionKind getFunctionKind() {
        return kind;
    }

    @Override
    public RuntimeInfo.Function getFunction() {
        return function;
    }

    /**
     * [[Scope]]
     */
    @Override
    public LexicalEnvironment getScope() {
        return scope;
    }

    /**
     * [[Code]]
     */
    @Override
    public RuntimeInfo.Code getCode() {
        return function;
    }

    /**
     * [[Realm]]
     */
    @Override
    public Realm getRealm() {
        return realm;
    }

    /**
     * [[ThisMode]]
     */
    @Override
    public ThisMode getThisMode() {
        return thisMode;
    }

    /**
     * [[Strict]]
     */
    @Override
    public boolean isStrict() {
        return strict;
    }

    /**
     * [[Home]]
     */
    @Override
    public Scriptable getHome() {
        return home;
    }

    /**
     * [[MethodName]]
     */
    @Override
    public String getMethodName() {
        return methodName;
    }
}
