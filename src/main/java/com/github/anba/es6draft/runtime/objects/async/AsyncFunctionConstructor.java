/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.FunctionConstructor.functionSource;
import static com.github.anba.es6draft.runtime.objects.FunctionConstructor.functionSourceText;
import static com.github.anba.es6draft.runtime.objects.FunctionConstructor.newFunctionExecutable;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction.FunctionAllocate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.SetFunctionName;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.objects.FunctionConstructor.SourceKind;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction;

/**
 * <h1>Extension: Async Function Definitions</h1><br>
 * <h2>Async Function Objects</h2>
 * <ul>
 * <li>The Async Function Constructor
 * <li>Properties of the AsyncFunction constructor
 * </ul>
 */
public final class AsyncFunctionConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new AsyncFunction constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public AsyncFunctionConstructor(Realm realm) {
        super(realm, "AsyncFunction", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public AsyncFunctionConstructor clone() {
        return new AsyncFunctionConstructor(getRealm());
    }

    /**
     * AsyncFunction(p1, p2, ..., pn, body)
     */
    @Override
    public OrdinaryAsyncFunction call(ExecutionContext callerContext, Object thisValue,
            Object... args) {
        /* steps 1-3 */
        return CreateDynamicFunction(callerContext, calleeContext(), this, args);
    }

    /**
     * AsyncFunction(p1, p2, ..., pn, body)
     */
    @Override
    public OrdinaryAsyncFunction construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        /* steps 1-3 */
        return CreateDynamicFunction(callerContext, calleeContext(), newTarget, args);
    }

    /**
     * 19.2.1.1.1 RuntimeSemantics: CreateDynamicFunction(constructor, newTarget, kind, args)
     * 
     * @param callerContext
     *            the caller execution context
     * @param cx
     *            the execution context
     * @param newTarget
     *            the newTarget constructor function
     * @param args
     *            the function arguments
     * @return the new generator function object
     */
    private static OrdinaryAsyncFunction CreateDynamicFunction(ExecutionContext callerContext,
            ExecutionContext cx, Constructor newTarget, Object... args) {
        /* step 1 (not applicable) */
        /* step 2 (not applicable) */
        /* step 3 */
        Intrinsics fallbackProto = Intrinsics.AsyncFunction;

        /* steps 4-10 */
        String[] sourceText = functionSourceText(cx, args);
        String parameters = sourceText[0], bodyText = sourceText[1];

        /* steps 11, 13-20 */
        Source source = functionSource(SourceKind.AsyncFunction, cx.getRealm(), callerContext);
        RuntimeInfo.Function function;
        try {
            ScriptLoader scriptLoader = cx.getRealm().getScriptLoader();
            function = scriptLoader.asyncFunction(source, parameters, bodyText).getFunction();
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        }

        /* step 12 */
        boolean strict = function.isStrict();
        /* steps 21-22 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget, fallbackProto);
        /* step 23 */
        OrdinaryAsyncFunction f = FunctionAllocate(cx, proto, strict, FunctionKind.Normal);
        /* steps 24-25 */
        LexicalEnvironment<GlobalEnvironmentRecord> scope = f.getRealm().getGlobalEnv();
        /* step 26 */
        FunctionInitialize(f, FunctionKind.Normal, function, scope, newFunctionExecutable(source));
        /* steps 27-28 (not applicable) */
        /* step 29 */
        SetFunctionName(f, "anonymous");
        /* step 30 */
        return f;
    }

    /**
     * Properties of the AsyncFunction constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Function;

        /**
         * AsyncFunction.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.AsyncFunctionPrototype;

        /**
         * AsyncFunction.length
         */
        @Value(name = "length",
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name",
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "AsyncFunction";
    }
}
