/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.GeneratorStart;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialise;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * <h1>9 ECMAScript Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.1 Ordinary Object Internal Methods and Internal Data Properties</h2>
 * <ul>
 * <li>9.1.15 Ordinary Function Objects
 * </ul>
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

        @Override
        public final boolean isConstructor() {
            return isConstructor;
        }

        /**
         * 9.1.15.2 [[Construct]] (argumentsList)
         */
        @Override
        public ScriptObject construct(ExecutionContext callerContext, Object... args) {
            return OrdinaryConstruct(callerContext, this, args);
        }
    }

    @Override
    public OrdinaryGenerator rebind(ExecutionContext cx, ScriptObject newHomeObject) {
        assert isInitialised() : "uninitialised function object";
        Object methodName = getMethodName();
        OrdinaryGenerator f;
        if (methodName instanceof String) {
            f = GeneratorFunctionCreate(cx, getFunctionKind(), getFunction(), getScope(),
                    getPrototypeOf(cx), newHomeObject, (String) methodName);
        } else {
            assert methodName instanceof Symbol;
            f = GeneratorFunctionCreate(cx, getFunctionKind(), getFunction(), getScope(),
                    getPrototypeOf(cx), newHomeObject, (Symbol) methodName);
        }
        f.isConstructor = this.isConstructor;
        return f;
    }

    /**
     * 9.1.15.1 [[Call]] Internal Method
     */
    @Override
    public GeneratorObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        if (!isInitialised()) {
            throw throwTypeError(callerContext, Messages.Key.UninitialisedObject);
        }
        /* steps 1-12 */
        ExecutionContext calleeContext = ExecutionContext.newFunctionExecutionContext(this,
                thisValue);
        /* steps 13-14 */
        getFunction().functionDeclarationInstantiation(calleeContext, this, args);
        /* steps 15-16 */
        GeneratorObject result;
        if (getFunctionKind() != FunctionKind.Arrow) {
            result = EvaluateBody(calleeContext, this);
        } else {
            result = EvaluateBodyComprehension(calleeContext, this);
        }
        /* step 17 */
        return result;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * Runtime Semantics EvaluateBody
     * 
     * <pre>
     * GeneratorBody : FunctionBody
     * </pre>
     */
    public static GeneratorObject EvaluateBody(ExecutionContext cx, OrdinaryGenerator functionObject) {
        /* step 1 */
        assert cx.getLexicalEnvironment().getEnvRec() instanceof FunctionEnvironmentRecord;
        /* step 2 */
        EnvironmentRecord env = cx.getThisEnvironment();
        /* step 3 */
        Object g = env.getThisBinding();
        /* step 4 */
        GeneratorObject gen;
        if (!(g instanceof GeneratorObject) || ((GeneratorObject) g).getState() != null) {
            GeneratorObject newG = OrdinaryCreateFromConstructor(cx, functionObject,
                    Intrinsics.GeneratorPrototype, GeneratorObjectAllocator.INSTANCE);
            gen = newG;
        } else {
            gen = (GeneratorObject) g;
        }
        /* step 5 */
        return GeneratorStart(cx, gen, functionObject.getCode());
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * Runtime Semantics EvaluateBody
     * 
     * <pre>
     * GeneratorBody : Comprehension
     * </pre>
     */
    public static GeneratorObject EvaluateBodyComprehension(ExecutionContext cx,
            OrdinaryGenerator functionObject) {
        /* steps 1-2 */
        GeneratorObject g = OrdinaryCreateFromConstructor(cx, functionObject,
                Intrinsics.GeneratorPrototype, GeneratorObjectAllocator.INSTANCE);
        /* step 3 */
        assert g.getState() == null;
        /* steps 4-5 */
        GeneratorStart(cx, g, functionObject.getCode());
        /* step 6 */
        return g;
    }

    private static class GeneratorObjectAllocator implements ObjectAllocator<GeneratorObject> {
        static final ObjectAllocator<GeneratorObject> INSTANCE = new GeneratorObjectAllocator();

        @Override
        public GeneratorObject newInstance(Realm realm) {
            return new GeneratorObject(realm);
        }
    }

    /* ***************************************************************************************** */

    /**
     * 9.1.15.5 FunctionAllocate Abstract Operation
     */
    public static OrdinaryGenerator FunctionAllocate(ExecutionContext cx,
            ScriptObject functionPrototype, FunctionKind kind) {
        assert kind != FunctionKind.ConstructorMethod;
        Realm realm = cx.getRealm();
        /* steps 1-3 (implicit) */
        /* steps 4-6 */
        OrdinaryGenerator f = new OrdinaryConstructorGenerator(realm);
        /* step 7 */
        f.functionKind = kind;
        /* step 8 */
        f.setPrototype(functionPrototype);
        /* step 9 */
        // f.[[Extensible]] = true (implicit)
        /* step 10 */
        f.realm = realm;
        // support for legacy 'caller' and 'arguments' properties
        // TODO: 'caller' and 'arguments' properties are never updated for generator functions
        f.legacy = realm.isEnabled(CompatibilityOption.FunctionPrototype);
        /* step 11 */
        return f;
    }

    /**
     * 9.1.15.8 GeneratorFunctionCreate Abstract Operation
     */
    public static OrdinaryGenerator GeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope) {
        return GeneratorFunctionCreate(cx, kind, function, scope, null, null, (String) null);
    }

    /**
     * 9.1.15.8 GeneratorFunctionCreate Abstract Operation
     */
    public static OrdinaryGenerator GeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, ScriptObject functionPrototype) {
        return GeneratorFunctionCreate(cx, kind, function, scope, functionPrototype, null,
                (String) null);
    }

    /**
     * 9.1.15.8 GeneratorFunctionCreate Abstract Operation
     */
    public static OrdinaryGenerator GeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope,
            ScriptObject functionPrototype, ScriptObject homeObject, String methodName) {
        assert function.isGenerator() && kind != FunctionKind.ConstructorMethod;
        /* step 1 */
        if (functionPrototype == null) {
            functionPrototype = cx.getIntrinsic(Intrinsics.Generator);
        }
        /* step 2 */
        OrdinaryGenerator f = FunctionAllocate(cx, functionPrototype, kind);
        /* step 3 */
        return FunctionInitialise(cx, f, kind, function, scope, homeObject, methodName);
    }

    /**
     * 9.1.15.8 GeneratorFunctionCreate Abstract Operation
     */
    public static OrdinaryGenerator GeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope,
            ScriptObject functionPrototype, ScriptObject homeObject, Symbol methodName) {
        assert function.isGenerator() && kind != FunctionKind.ConstructorMethod;
        /* step 1 */
        if (functionPrototype == null) {
            functionPrototype = cx.getIntrinsic(Intrinsics.Generator);
        }
        /* step 2 */
        OrdinaryGenerator f = FunctionAllocate(cx, functionPrototype, kind);
        /* step 3 */
        return FunctionInitialise(cx, f, kind, function, scope, homeObject, methodName);
    }
}
