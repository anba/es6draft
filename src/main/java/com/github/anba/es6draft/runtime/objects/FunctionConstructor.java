/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.HasOwnProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction.FunctionAllocate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeMethod;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.SetFunctionName;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.CompiledObject;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.DebugInfo;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.ConstructorKind;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.2 Function Objects</h2>
 * <ul>
 * <li>19.2.1 The Function Constructor
 * <li>19.2.2 Properties of the Function Constructor
 * </ul>
 */
public final class FunctionConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Function constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public FunctionConstructor(Realm realm) {
        super(realm, "Function", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public FunctionConstructor clone() {
        return new FunctionConstructor(getRealm());
    }

    /**
     * 19.2.1.1 Function (p1, p2, ... , pn, body)
     */
    @Override
    public OrdinaryFunction call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* steps 1-3 */
        return CreateDynamicFunction(calleeContext, this, args);
    }

    /**
     * 19.2.1.1 Function (p1, p2, ... , pn, body)
     */
    @Override
    public OrdinaryFunction construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* steps 1-3 */
        return CreateDynamicFunction(calleeContext, newTarget, args);
    }

    private OrdinaryConstructorFunction CreateDynamicFunction(ExecutionContext cx,
            Constructor newTarget, Object[] args) {
        /* step 1 (not applicable) */
        /* step 2 */
        Intrinsics fallbackProto = Intrinsics.FunctionPrototype;
        /* step 3 (not applicable) */

        /* steps 4-10 */
        int argCount = args.length;
        String p, bodyText;
        if (argCount == 0) {
            p = "";
            bodyText = "";
        } else if (argCount == 1) {
            p = "";
            bodyText = ToFlatString(cx, args[0]);
        } else {
            StringBuilder sb = new StringBuilder();
            Object firstArg = args[0];
            sb.append(ToFlatString(cx, firstArg));
            int k = 2;
            for (; k < argCount; ++k) {
                Object nextArg = args[k - 1];
                String nextArgString = ToFlatString(cx, nextArg);
                sb.append(',').append(nextArgString);
            }
            p = sb.toString();
            bodyText = ToFlatString(cx, args[k - 1]);
        }

        /* steps 11-13 */
        Source source = functionSource(cx);
        CompiledFunction exec = new CompiledFunction(source);
        RuntimeInfo.Function function;
        try {
            ScriptLoader scriptLoader = cx.getRealm().getScriptLoader();
            function = scriptLoader.function(source, p, bodyText).getFunction();
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        }

        /* step 14 */
        boolean strict = function.isStrict();
        /* steps 15-16 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget, fallbackProto);
        /* steps 17-18 */
        OrdinaryConstructorFunction f = FunctionAllocate(cx, proto, strict, FunctionKind.Normal,
                ConstructorKind.Base);
        /* steps 19-20 */
        LexicalEnvironment<GlobalEnvironmentRecord> scope = f.getRealm().getGlobalEnv();
        /* steps 21-22 */
        FunctionInitialize(f, FunctionKind.Normal, strict, function, scope, exec);
        /* step 23 */
        if (function.hasSuperReference()) {
            MakeMethod(f, null);
        }
        /* step 24 (not applicable) */
        /* steps 25-26 */
        MakeConstructor(cx, f);
        /* steps 27-29 */
        if (!HasOwnProperty(cx, f, "name")) {
            SetFunctionName(f, "anonymous");
        }
        /* step 30 */
        return f;
    }

    /**
     * 19.2.2 Properties of the Function Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        /**
         * 19.2.2.2 Function.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.FunctionPrototype;

        /**
         * 19.2.2.1 Function.length
         */
        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "Function";
    }

    private Source functionSource(ExecutionContext caller) {
        Source baseSource = getRealm().sourceInfo(caller);
        String sourceName;
        if (baseSource != null) {
            sourceName = String.format("<Function> (%s)", baseSource.getName());
        } else {
            sourceName = "<Function>";
        }
        return new Source(baseSource, sourceName, 1);
    }

    private static final class CompiledFunction extends CompiledObject {
        CompiledFunction(Source source) {
            super(new FunctionSourceObject(source));
        }
    }

    private static final class FunctionSourceObject implements RuntimeInfo.SourceObject {
        private final Source source;

        FunctionSourceObject(Source source) {
            this.source = source;
        }

        @Override
        public Source toSource() {
            return source;
        }

        @Override
        public DebugInfo debugInfo() {
            return null;
        }
    }
}
