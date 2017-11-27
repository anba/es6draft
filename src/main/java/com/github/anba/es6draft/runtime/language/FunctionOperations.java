/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.LexicalEnvironment.newDeclarativeEnvironment;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.MakeMethod;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.SetFunctionName;
import static com.github.anba.es6draft.runtime.types.builtins.LegacyConstructorFunction.LegacyFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction.AsyncFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncGenerator.AsyncGeneratorFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction.ConstructorFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.GeneratorFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.LegacyConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 
 */
public final class FunctionOperations {
    private FunctionOperations() {
    }

    /**
     * 9.2.2.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument )
     * 
     * @param f
     *            the function object
     * @param thisArgument
     *            the thisArgument
     * @return the thisValue
     */
    public static ScriptObject functionThisValue(FunctionObject f, Object thisArgument) {
        Realm calleeRealm = f.getRealm();
        if (Type.isUndefinedOrNull(thisArgument)) {
            return calleeRealm.getGlobalThis();
        }
        return ToObject(calleeRealm.defaultContext(), thisArgument);
    }

    /**
     * 12.2.? Generator Comprehensions
     * <p>
     * 12.2.?.2 Runtime Semantics: Evaluation
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the generator object
     */
    public static GeneratorObject EvaluateGeneratorComprehension(RuntimeInfo.Function fd, ExecutionContext cx) {
        /* step 1 (omitted) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 3-4 (not applicable) */
        /* step 5 */
        OrdinaryGenerator closure = GeneratorFunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step 6 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 7 */
        closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 8 */
        GeneratorObject iterator = (GeneratorObject) closure.call(cx, UNDEFINED);
        /* step 9 */
        return iterator;
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * 14.1.20 Runtime Semantics: InstantiateFunctionObject
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new function instance
     */
    public static OrdinaryConstructorFunction InstantiateFunctionObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        OrdinaryConstructorFunction f = ConstructorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 4 */
        MakeConstructor(cx, f);
        /* step 4 */
        SetFunctionName(f, name);
        /* step 6 */
        return f;
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * 14.1.20 Runtime Semantics: InstantiateFunctionObject
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new function instance
     */
    public static LegacyConstructorFunction InstantiateLegacyFunctionObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        LegacyConstructorFunction f = LegacyFunctionCreate(cx, fd, scope);
        /* step 4 */
        MakeConstructor(cx, f);
        /* step 4 */
        SetFunctionName(f, name);
        /* step 6 */
        return f;
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * 14.1.21 Runtime Semantics: Evaluation
     * <ul>
     * <li>FunctionExpression : function ( FormalParameters ) { FunctionBody }
     * <li>FunctionExpression : function BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new function instance
     */
    public static OrdinaryConstructorFunction EvaluateFunctionExpression(RuntimeInfo.Function fd, ExecutionContext cx) {
        OrdinaryConstructorFunction closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = ConstructorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 4 */
            MakeConstructor(cx, closure);
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = ConstructorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            MakeConstructor(cx, closure);
            /* step 9 */
            SetFunctionName(closure, name);
            /* step 10 */
            envRec.initializeBinding(name, closure);
        }
        /* step 5/11 */
        return closure;
    }

    /**
     * 14.1 Function Definitions
     * <p>
     * 14.1.21 Runtime Semantics: Evaluation
     * <ul>
     * <li>FunctionExpression : function ( FormalParameters ) { FunctionBody }
     * <li>FunctionExpression : function BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new function instance
     */
    public static LegacyConstructorFunction EvaluateLegacyFunctionExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        LegacyConstructorFunction closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = LegacyFunctionCreate(cx, fd, scope);
            /* step 4 */
            MakeConstructor(cx, closure);
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = LegacyFunctionCreate(cx, fd, funcEnv);
            /* step 8 */
            MakeConstructor(cx, closure);
            /* step 9 */
            SetFunctionName(closure, name);
            /* step 10 */
            envRec.initializeBinding(name, closure);
        }
        /* step 5/11 */
        return closure;
    }

    /**
     * 14.2 Arrow Function Definitions
     * <p>
     * 14.2.17 Runtime Semantics: Evaluation
     * <ul>
     * <li>ArrowFunction : ArrowParameters {@literal =>} ConciseBody
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new function instance
     */
    public static OrdinaryFunction EvaluateArrowFunction(RuntimeInfo.Function fd, ExecutionContext cx) {
        /* step 1 (not applicable) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 4 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step 5 */
        return closure;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.12 Runtime Semantics: InstantiateFunctionObject
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new generator function instance
     */
    public static OrdinaryGenerator InstantiateGeneratorObject(LexicalEnvironment<?> scope, ExecutionContext cx,
            RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        OrdinaryGenerator f = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 4 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 5 */
        f.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 6 */
        SetFunctionName(f, name);
        /* step 7 */
        return f;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: Evaluation
     * <ul>
     * <li>GeneratorExpression: function* ( FormalParameters ) { FunctionBody }
     * <li>GeneratorExpression: function* BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new generator function instance
     */
    public static OrdinaryGenerator EvaluateGeneratorExpression(RuntimeInfo.Function fd, ExecutionContext cx) {
        OrdinaryGenerator closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 4 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
            /* step 5 */
            closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = GeneratorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
            /* step 9 */
            closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
            /* step 10 */
            SetFunctionName(closure, name);
            /* step 11 */
            envRec.initializeBinding(name, closure);
        }
        /* step 6/12 */
        return closure;
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new async function instance
     */
    public static OrdinaryAsyncFunction InstantiateAsyncFunctionObject(LexicalEnvironment<?> scope, ExecutionContext cx,
            RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        OrdinaryAsyncFunction f = AsyncFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 4 */
        SetFunctionName(f, name);
        /* step 5 */
        return f;
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new async function instance
     */
    public static OrdinaryAsyncFunction EvaluateAsyncFunctionExpression(RuntimeInfo.Function fd, ExecutionContext cx) {
        OrdinaryAsyncFunction closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = AsyncFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = AsyncFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            SetFunctionName(closure, name);
            /* step 9 */
            envRec.initializeBinding(name, closure);
        }
        /* step 4/10 */
        return closure;
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new async function instance
     */
    public static OrdinaryAsyncFunction EvaluateAsyncArrowFunction(RuntimeInfo.Function fd, ExecutionContext cx) {
        /* step 1 (not applicable) */
        /* step 2 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 3-4/3-5 */
        OrdinaryAsyncFunction closure = AsyncFunctionCreate(cx, FunctionKind.Arrow, fd, scope);
        /* step 5/6 */
        return closure;
    }

    /**
     * Extension: Async Generator Function Definitions
     * 
     * @param scope
     *            the current lexical scope
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the new async generator instance
     */
    public static OrdinaryAsyncGenerator InstantiateAsyncGeneratorObject(LexicalEnvironment<?> scope,
            ExecutionContext cx, RuntimeInfo.Function fd) {
        /* step 1 (not applicable) */
        /* step 2 */
        String name = fd.functionName();
        /* step 3 */
        OrdinaryAsyncGenerator f = AsyncGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
        /* step 4 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.AsyncGeneratorPrototype);
        /* step 5 */
        f.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 6 */
        SetFunctionName(f, name);
        /* step 7 */
        return f;
    }

    /**
     * Extension: Async Generator Function Definitions
     * 
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the new async generator instance
     */
    public static OrdinaryAsyncGenerator EvaluateAsyncGeneratorExpression(RuntimeInfo.Function fd,
            ExecutionContext cx) {
        OrdinaryAsyncGenerator closure;
        if (!fd.is(RuntimeInfo.FunctionFlags.ScopedName)) {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            closure = AsyncGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, scope);
            /* step 4 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.AsyncGeneratorPrototype);
            /* step 5 */
            closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        } else {
            /* step 1 (not applicable) */
            /* step 2 */
            LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
            /* step 3 */
            LexicalEnvironment<DeclarativeEnvironmentRecord> funcEnv = newDeclarativeEnvironment(scope);
            /* step 4 */
            DeclarativeEnvironmentRecord envRec = funcEnv.getEnvRec();
            /* step 5 */
            String name = fd.functionName();
            /* step 6 */
            envRec.createImmutableBinding(name, false);
            /* step 7 */
            closure = AsyncGeneratorFunctionCreate(cx, FunctionKind.Normal, fd, funcEnv);
            /* step 8 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.AsyncGeneratorPrototype);
            /* step 9 */
            closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
            /* step 10 */
            SetFunctionName(closure, name);
            /* step 11 */
            envRec.initializeBinding(name, closure);
        }
        /* step 6/12 */
        return closure;
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the method object
     */
    public static OrdinaryFunction EvaluatePropertyDefinition(OrdinaryObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (Call DefineMethod) */
        /* DefineMethod: steps 1-3 (generated code) */
        /* DefineMethod: step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* DefineMethod: steps 5-6 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* DefineMethod: step 7 */
        MakeMethod(closure, object);
        /* step 3 */
        SetFunctionName(closure, propKey);
        /* steps 4-5 (generated code) */
        return closure;
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>get PropertyName ( ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the getter method
     */
    public static OrdinaryFunction EvaluatePropertyDefinitionGetter(OrdinaryObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* steps 5-6 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 7 */
        MakeMethod(closure, object);
        /* step 8  */
        SetFunctionName(closure, propKey, "get");
        /* steps 9-10 (generated code) */
        return closure;
    }

    /**
     * 14.3 Method Definitions
     * <p>
     * 14.3.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the setter method
     */
    public static OrdinaryFunction EvaluatePropertyDefinitionSetter(OrdinaryObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-3 (generated code) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryFunction closure = FunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        SetFunctionName(closure, propKey, "set");
        /* steps 8-9 (generated code) */
        return closure;
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.12 Runtime Semantics: PropertyDefinitionEvaluation
     * <ul>
     * <li>GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </ul>
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param fd
     *            the function runtime info object
     * @param cx
     *            the execution context
     * @return the generator method
     */
    public static OrdinaryGenerator EvaluatePropertyDefinitionGenerator(OrdinaryObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (generated code) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryGenerator closure = GeneratorFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        /* step 8 */
        closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 9 */
        SetFunctionName(closure, propKey);
        /* steps 10-11 (generated code) */
        return closure;
    }

    /**
     * Extension: Async Function Definitions
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the async method
     */
    public static OrdinaryAsyncFunction EvaluatePropertyDefinitionAsync(OrdinaryObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (generated code) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryAsyncFunction closure = AsyncFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        SetFunctionName(closure, propKey);
        /* steps 8-9 (generated code) */
        return closure;
    }

    /**
     * Extension: Async Generator Function Definitions
     * 
     * @param object
     *            the script object
     * @param propKey
     *            the property key
     * @param cx
     *            the execution context
     * @param fd
     *            the function runtime info object
     * @return the async generator method
     */
    public static OrdinaryAsyncGenerator EvaluatePropertyDefinitionAsyncGenerator(OrdinaryObject object, Object propKey,
            RuntimeInfo.Function fd, ExecutionContext cx) {
        /* steps 1-2 (generated code) */
        /* step 3 (not applicable) */
        /* step 4 */
        LexicalEnvironment<?> scope = cx.getLexicalEnvironment();
        /* step 5 */
        OrdinaryAsyncGenerator closure = AsyncGeneratorFunctionCreate(cx, FunctionKind.Method, fd, scope);
        /* step 6 */
        MakeMethod(closure, object);
        /* step 7 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.AsyncGeneratorPrototype);
        /* step 8 */
        closure.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
        /* step 9 */
        SetFunctionName(closure, propKey);
        /* steps 10-11 (generated code) */
        return closure;
    }
}
