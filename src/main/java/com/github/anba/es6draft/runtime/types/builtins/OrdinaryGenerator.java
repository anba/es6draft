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
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
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
         * 8.3.15.2 [[Construct]] (argumentsList)
         */
        @Override
        public ScriptObject construct(ExecutionContext callerContext, Object... args) {
            return OrdinaryConstruct(callerContext, this, args);
        }
    }

    /**
     * 8.3.19.1 [[Call]] Internal Method
     */
    @Override
    public GeneratorObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        if (!isInitialised()) {
            throw throwTypeError(callerContext, Messages.Key.IncompatibleObject);
        }

        /* step 1-11 */
        ExecutionContext calleeContext = ExecutionContext.newFunctionExecutionContext(this,
                thisValue);
        /* step 12-13 */
        getFunction().functionDeclarationInstantiation(calleeContext, this, args);
        /* step 14-15 */
        GeneratorObject result = EvaluateBody(calleeContext, this);
        /* step 16 */
        return result;
    }

    /**
     * 13.4 Generator Function Definitions
     * <p>
     * Runtime Semantics EvaluateBody
     * 
     * <pre>
     * GeneratorBody : FunctionBody
     * </pre>
     */
    public static GeneratorObject EvaluateBody(ExecutionContext cx, OrdinaryGenerator functionObject) {
        /* step 1 */
        Object thisValue = cx.thisResolution();
        /* step 2 */
        GeneratorObject g;
        if (!(thisValue instanceof GeneratorObject)) {
            g = OrdinaryCreateFromConstructor(cx, functionObject, Intrinsics.GeneratorPrototype,
                    GeneratorObjectAllocator.INSTANCE);
        } else {
            g = (GeneratorObject) thisValue;
        }
        /* step 3 */
        return GeneratorStart(cx, g, functionObject.getCode());
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
     * 8.3.15.5 FunctionAllocate Abstract Operation
     */
    public static OrdinaryGenerator FunctionAllocate(ExecutionContext cx, ScriptObject prototype,
            FunctionKind kind) {
        Realm realm = cx.getRealm();
        /* steps 1-3 (implicit) */
        /* steps 4-6 */
        OrdinaryGenerator f = new OrdinaryConstructorGenerator(realm);
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
     * 8.3.15.7 GeneratorFunctionCreate Abstract Operation
     */
    public static OrdinaryGenerator GeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope) {
        return GeneratorFunctionCreate(cx, kind, function, scope, null, null, null);
    }

    /**
     * 8.3.15.7 GeneratorFunctionCreate Abstract Operation
     */
    public static OrdinaryGenerator GeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, ScriptObject prototype) {
        return GeneratorFunctionCreate(cx, kind, function, scope, prototype, null, null);
    }

    /**
     * 8.3.15.7 GeneratorFunctionCreate Abstract Operation
     */
    public static OrdinaryGenerator GeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment scope, ScriptObject prototype,
            ScriptObject homeObject, String methodName) {
        assert function.isGenerator() && kind != FunctionKind.ConstructorMethod;
        /* step 1 */
        if (prototype == null) {
            prototype = cx.getIntrinsic(Intrinsics.Generator);
        }
        /* step 2 */
        OrdinaryGenerator f = FunctionAllocate(cx, prototype, kind);
        /* step 3 */
        return FunctionInitialize(cx, f, kind, function, scope, homeObject, methodName);
    }
}
