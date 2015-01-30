/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.HasOwnProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeMethod;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.SetFunctionName;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.FunctionAllocate;

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
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.2 GeneratorFunction Objects</h2>
 * <ul>
 * <li>25.2.1 The GeneratorFunction Constructor
 * <li>25.2.2 Properties of the GeneratorFunction Constructor
 * </ul>
 */
public final class GeneratorFunctionConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Generator Function constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public GeneratorFunctionConstructor(Realm realm) {
        super(realm, "GeneratorFunction", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public GeneratorFunctionConstructor clone() {
        return new GeneratorFunctionConstructor(getRealm());
    }

    /**
     * 25.2.1.1 GeneratorFunction (p1, p2, ... , pn, body)
     */
    @Override
    public OrdinaryGenerator call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* steps 1-3 */
        return CreateDynamicFunction(calleeContext, this, args);
    }

    /**
     * 25.2.1.1 GeneratorFunction (p1, p2, ... , pn, body)
     */
    @Override
    public OrdinaryGenerator construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* steps 1-3 */
        return CreateDynamicFunction(calleeContext, newTarget, args);
    }

    private OrdinaryGenerator CreateDynamicFunction(ExecutionContext cx, Constructor newTarget,
            Object[] args) {
        /* step 1 (not applicable) */
        /* step 2 (not applicable) */
        /* step 3 */
        Intrinsics fallbackProto = Intrinsics.Generator;
        /* steps 4-10 */
        int argCount = args.length;
        StringBuilder p = new StringBuilder();
        String bodyText;
        if (argCount == 0) {
            bodyText = "";
        } else if (argCount == 1) {
            bodyText = ToFlatString(cx, args[0]);
        } else {
            Object firstArg = args[0];
            p.append(ToFlatString(cx, firstArg));
            int k = 2;
            for (; k < argCount; ++k) {
                Object nextArg = args[k - 1];
                String nextArgString = ToFlatString(cx, nextArg);
                p.append(',').append(nextArgString);
            }
            bodyText = ToFlatString(cx, args[k - 1]);
        }

        /* steps 11-13 */
        Source source = generatorSource(cx);
        CompiledGenerator exec = new CompiledGenerator(source);
        RuntimeInfo.Function function;
        try {
            ScriptLoader scriptLoader = cx.getRealm().getScriptLoader();
            function = scriptLoader.generator(source, p.toString(), bodyText).getFunction();
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        }

        /* step 14 */
        boolean strict = function.isStrict();
        /* steps 15-16 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget, fallbackProto);
        /* steps 17-18 */
        OrdinaryGenerator f = FunctionAllocate(cx, proto, strict, FunctionKind.Normal);
        /* steps 19-20 */
        LexicalEnvironment<GlobalEnvironmentRecord> scope = f.getRealm().getGlobalEnv();
        /* steps 21-22 */
        FunctionInitialize(cx, f, FunctionKind.Normal, strict, function, scope, exec);
        /* step 23 */
        if (function.hasSuperReference()) {
            MakeMethod(f, null);
        }
        /* step 25 (not applicable) */
        /* steps 24, 26 */
        OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
        MakeConstructor(cx, f, true, prototype);
        /* steps 27-29 */
        if (!HasOwnProperty(cx, f, "name")) {
            SetFunctionName(f, "anonymous");
        }
        /* step 30 */
        return f;
    }

    /**
     * 25.2.2 Properties of the GeneratorFunction Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Function;

        /**
         * 25.2.2.2 GeneratorFunction.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Generator;

        /**
         * 25.2.2.1 GeneratorFunction.length
         */
        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "GeneratorFunction";
    }

    private Source generatorSource(ExecutionContext caller) {
        Source baseSource = getRealm().sourceInfo(caller);
        String sourceName;
        if (baseSource != null) {
            sourceName = String.format("<GeneratorFunction> (%s)", baseSource.getName());
        } else {
            sourceName = "<GeneratorFunction>";
        }
        return new Source(baseSource, sourceName, 1);
    }

    private static final class CompiledGenerator extends CompiledObject {
        CompiledGenerator(Source source) {
            super(new GeneratorSourceObject(source));
        }
    }

    private static final class GeneratorSourceObject implements RuntimeInfo.SourceObject {
        private final Source source;

        GeneratorSourceObject(Source source) {
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
