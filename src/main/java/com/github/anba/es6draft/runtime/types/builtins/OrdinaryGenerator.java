/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorStart;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;

import com.github.anba.es6draft.runtime.ExecutionContext;
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
    /**
     * Constructs a new Generator Function object.
     * 
     * @param realm
     *            the realm object
     */
    protected OrdinaryGenerator(Realm realm) {
        super(realm);
    }

    @Override
    protected OrdinaryGenerator allocateNew() {
        return FunctionAllocate(getRealm().defaultContext(), getPrototype(), isStrict(),
                getFunctionKind());
    }

    /**
     * 9.2.2 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public GeneratorObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        try {
            return (GeneratorObject) getCallMethod().invokeExact(this, callerContext, thisValue,
                    args);
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
    public GeneratorObject tailCall(ExecutionContext callerContext, Object thisValue,
            Object... args) throws Throwable {
        return (GeneratorObject) getTailCallMethod().invokeExact(this, callerContext, thisValue,
                args);
    }

    /**
     * 9.2.3 [[Construct]] ( argumentsList, newTarget)
     */
    @Override
    public GeneratorObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... argumentsList) {
        try {
            return (GeneratorObject) getConstructMethod().invokeExact(this, callerContext,
                    newTarget, argumentsList);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 9.2.3 [[Construct]] ( argumentsList, newTarget)
     */
    @Override
    public GeneratorObject tailConstruct(ExecutionContext callerContext, Constructor newTarget,
            Object... argumentsList) throws Throwable {
        return (GeneratorObject) getTailConstructMethod().invokeExact(this, callerContext,
                newTarget, argumentsList);
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: EvaluateBody
     * 
     * <pre>
     * GeneratorBody : FunctionBody
     * </pre>
     * 
     * @param cx
     *            the execution context
     * @param functionObject
     *            the generator function object
     * @return the generator result value
     */
    public static GeneratorObject EvaluateBody(ExecutionContext cx, OrdinaryGenerator functionObject) {
        /* steps 1-2 */
        GeneratorObject gen = OrdinaryCreateFromConstructor(cx, functionObject,
                Intrinsics.GeneratorPrototype, GeneratorObjectAllocator.INSTANCE);
        /* steps 3-5 */
        return GeneratorStart(cx, gen, functionObject.getCode());
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
     * @return the new generator function object
     */
    public static OrdinaryGenerator FunctionAllocate(ExecutionContext cx,
            ScriptObject functionPrototype, boolean strict, FunctionKind kind) {
        assert kind != FunctionKind.ClassConstructor;
        Realm realm = cx.getRealm();
        /* steps 1-3 (implicit) */
        /* steps 4-8 */
        OrdinaryGenerator f = new OrdinaryGenerator(realm);
        /* steps 9-13 */
        f.allocate(realm, functionPrototype, strict, kind, ConstructorKind.Derived);
        /* step 14 */
        return f;
    }

    /**
     * 9.2.7 GeneratorFunctionCreate Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @return the new generator function object
     */
    public static OrdinaryGenerator GeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment<?> scope) {
        assert function.isGenerator() && kind != FunctionKind.ClassConstructor;
        /* step 1 */
        ScriptObject functionPrototype = cx.getIntrinsic(Intrinsics.Generator);
        /* step 2 */
        OrdinaryGenerator f = FunctionAllocate(cx, functionPrototype, function.isStrict(), kind);
        /* step 3 */
        FunctionInitialize(f, kind, function.isStrict(), function, scope, cx.getCurrentExecutable());
        return f;
    }
}
