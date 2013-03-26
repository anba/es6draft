/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 *
 */
public class OrdinaryGenerator extends FunctionObject {
    public OrdinaryGenerator(Realm realm) {
        super(realm);
    }

    private static class OrdinaryConstructorGenerator extends OrdinaryGenerator implements
            Constructor {
        public OrdinaryConstructorGenerator(Realm realm) {
            super(realm);
        }

        /**
         * 8.3.15.2 [[Construct]] Internal Method
         */
        @Override
        public Object construct(Object... args) {
            return OrdinaryConstruct(realm, this, args);
        }
    }

    /**
     * 
     */
    public static OrdinaryGenerator GeneratorCreate(Realm realm, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope) {
        return GeneratorCreate(realm, kind, function, scope, null, null, null);
    }

    /**
     * 
     */
    public static OrdinaryGenerator GeneratorCreate(Realm realm, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, ScriptObject prototype,
            ScriptObject homeObject, String methodName) {
        assert function.isGenerator();
        assert kind != FunctionKind.Arrow && kind != FunctionKind.ConstructorMethod;

        boolean strict = function.isStrict();
        /* step 1 */
        OrdinaryGenerator f;
        if (kind == FunctionKind.Normal) {
            f = new OrdinaryConstructorGenerator(realm);
        } else {
            f = new OrdinaryGenerator(realm);
        }
        /* step 2-4 (implicit) */
        /* step 5 */
        if (prototype == null) {
            prototype = realm.getIntrinsic(Intrinsics.FunctionPrototype);
        }
        /* step 6 */
        f.setPrototype(realm, prototype);
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
        if (strict) {
            f.thisMode = ThisMode.Strict;
        } else {
            f.thisMode = ThisMode.Global;
        }
        /*  step 18 */
        int len = function.expectedArgumentCount();
        /* step 19 */
        f.defineOwnProperty(realm, "length", new PropertyDescriptor(len, false, false, false));
        String name = function.functionName() != null ? function.functionName() : "";
        f.defineOwnProperty(realm, "name", new PropertyDescriptor(name, false, false, false));
        /* step 20 */
        if (strict) {
            AddRestrictedFunctionProperties(realm, f);
        }
        /* step 21 */
        return f;
    }

    /**
     * 
     */
    public static OrdinaryGenerator InstantiateGeneratorObject(Realm realm,
            LexicalEnvironment scope, RuntimeInfo.Function fd) {
        /* step 1-2 */
        OrdinaryGenerator f = GeneratorCreate(realm, FunctionKind.Normal, fd, scope);
        /* step 3 */
        MakeConstructor(realm, f);
        /* step 4 */
        return f;
    }

    /**
     * 8.3.19.1 [[Call]] Internal Method
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        /* step 1-11 */
        ExecutionContext calleeContext = ExecutionContext.newFunctionExecutionContext(this,
                thisValue);
        /* step 12-13 */
        getFunction().functionDeclarationInstantiation(calleeContext, this, args);
        /* step 14-15 */
        GeneratorObject result = new GeneratorObject(getRealm(), this, calleeContext);
        result.initialise(getRealm());
        /* step 16 */
        return result;
    }
}
