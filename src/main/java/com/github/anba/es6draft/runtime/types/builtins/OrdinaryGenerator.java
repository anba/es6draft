/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorStart;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialise;

import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public final class OrdinaryGenerator extends FunctionObject implements Constructor {
    public OrdinaryGenerator(Realm realm) {
        super(realm);
    }

    @Override
    public final boolean isConstructor() {
        return super.isConstructor();
    }

    @Override
    protected OrdinaryGenerator allocateNew() {
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

    /**
     * 9.2.2 [[Construct]] (argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    /**
     * 9.2.2 [[Construct]] (argumentsList)
     */
    @Override
    public ScriptObject tailConstruct(ExecutionContext callerContext, Object... args) {
        return construct(callerContext, args);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.11 Runtime Semantics: EvaluateBody
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
            gen = OrdinaryCreateFromConstructor(cx, functionObject, Intrinsics.GeneratorPrototype,
                    GeneratorObjectAllocator.INSTANCE);
        } else {
            gen = (GeneratorObject) g;
        }
        /* step 5 */
        return GeneratorStart(cx, gen, functionObject.getCode());
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.11 Runtime Semantics: EvaluateBody
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

    private static final class GeneratorObjectAllocator implements ObjectAllocator<GeneratorObject> {
        static final ObjectAllocator<GeneratorObject> INSTANCE = new GeneratorObjectAllocator();

        @Override
        public GeneratorObject newInstance(Realm realm) {
            return new GeneratorObject(realm);
        }
    }

    /* ***************************************************************************************** */

    /**
     * 9.2.4 FunctionAllocate Abstract Operation
     */
    public static OrdinaryGenerator FunctionAllocate(ExecutionContext cx,
            ScriptObject functionPrototype, boolean strict, FunctionKind kind) {
        assert kind != FunctionKind.ConstructorMethod;
        Realm realm = cx.getRealm();
        /* steps 1-3 (implicit) */
        /* steps 4-8 */
        OrdinaryGenerator f = new OrdinaryGenerator(realm);
        /* steps 9-13 */
        f.allocate(realm, functionPrototype, strict, kind, uninitialisedGeneratorMH);
        /* step 14 */
        return f;
    }

    /**
     * 9.2.7 GeneratorFunctionCreate Abstract Operation
     */
    public static OrdinaryGenerator GeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope) {
        return GeneratorFunctionCreate(cx, kind, function, scope, null);
    }

    /**
     * 9.2.7 GeneratorFunctionCreate Abstract Operation
     */
    public static OrdinaryGenerator GeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, ScriptObject functionPrototype) {
        assert function.isGenerator() && kind != FunctionKind.ConstructorMethod;
        /* step 1 */
        if (functionPrototype == null) {
            functionPrototype = cx.getIntrinsic(Intrinsics.Generator);
        }
        /* step 2 */
        OrdinaryGenerator f = FunctionAllocate(cx, functionPrototype, function.isStrict(), kind);
        /* step 3 */
        return FunctionInitialise(cx, f, kind, function, scope);
    }
}
