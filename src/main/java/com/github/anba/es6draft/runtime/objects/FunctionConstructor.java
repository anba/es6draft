/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionAllocate;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionInitialize;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.SetFunctionName;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.CompiledFunction;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.internal.StrBuilder;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.LegacyConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

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

    /**
     * 19.2.1.1 Function (p1, p2, ... , pn, body)
     */
    @Override
    public FunctionObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* steps 1-3 */
        return CreateDynamicFunction(callerContext, calleeContext(), this, SourceKind.Function, args);
    }

    /**
     * 19.2.1.1 Function (p1, p2, ... , pn, body)
     */
    @Override
    public FunctionObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        /* steps 1-3 */
        return CreateDynamicFunction(callerContext, calleeContext(), newTarget, SourceKind.Function, args);
    }

    public enum SourceKind {
        Function, Generator, AsyncFunction, AsyncGenerator
    }

    @FunctionalInterface
    private interface FunctionCompiler {
        CompiledFunction compile(Source source, String parameters, String bodyText)
                throws ParserException, CompilationException;
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
     * @param kind
     *            the function kind
     * @param args
     *            the function arguments
     * @return the new function object
     */
    public static FunctionObject CreateDynamicFunction(ExecutionContext callerContext, ExecutionContext cx,
            Constructor newTarget, SourceKind kind, Object... args) {
        /* steps 1-6 (not applicable) */
        /* steps 7-9 */
        ScriptLoader scriptLoader = cx.getRealm().getScriptLoader();
        FunctionCompiler compiler;
        Intrinsics fallbackProto;
        switch (kind) {
        case AsyncFunction:
            compiler = scriptLoader::asyncFunction;
            fallbackProto = Intrinsics.AsyncFunctionPrototype;
            break;
        case AsyncGenerator:
            compiler = scriptLoader::asyncGenerator;
            fallbackProto = Intrinsics.AsyncGenerator;
            break;
        case Function:
            compiler = scriptLoader::function;
            fallbackProto = Intrinsics.FunctionPrototype;
            break;
        case Generator:
            compiler = scriptLoader::generator;
            fallbackProto = Intrinsics.Generator;
            break;
        default:
            throw new AssertionError();
        }
        /* steps 10-15 */
        int argCount = args.length;
        String parameters, bodyText;
        if (argCount == 0) {
            parameters = "";
            bodyText = "";
        } else if (argCount == 1) {
            parameters = "";
            bodyText = ToFlatString(cx, args[0]);
        } else {
            StrBuilder sb = new StrBuilder(cx);
            Object firstArg = args[0];
            sb.append(ToFlatString(cx, firstArg));
            int k = 2;
            for (; k < argCount; ++k) {
                Object nextArg = args[k - 1];
                String nextArgString = ToFlatString(cx, nextArg);
                sb.append(',').append(nextArgString);
            }
            parameters = sb.toString();
            bodyText = ToFlatString(cx, args[k - 1]);
        }
        /* steps 16-17, 19-28 */
        Source source = functionSource(kind, callerContext);
        CompiledFunction compiledFunction;
        try {
            compiledFunction = compiler.compile(source, parameters, bodyText);
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        }
        /* step 29 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget, fallbackProto);
        /* steps 18, 30-38 */
        return CreateDynamicFunction(cx, kind, compiledFunction, proto);
    }

    /**
     * 19.2.1.1.1 RuntimeSemantics: CreateDynamicFunction(constructor, newTarget, kind, args)
     * 
     * @param cx
     *            the execution context
     * @param kind
     *            the function kind
     * @param compiledFunction
     *            the compiled function
     * @param proto
     *            the function prototype
     * @return the new function object
     */
    public static FunctionObject CreateDynamicFunction(ExecutionContext cx, SourceKind kind,
            CompiledFunction compiledFunction, ScriptObject proto) {
        RuntimeInfo.Function function = compiledFunction.getFunction();
        /* step 18 */
        boolean strict = function.isStrict();
        /* step 30 */
        ObjectAllocator<? extends FunctionObject> allocator;
        switch (kind) {
        case AsyncFunction:
            allocator = OrdinaryAsyncFunction::new;
            break;
        case AsyncGenerator:
            allocator = OrdinaryAsyncGenerator::new;
            break;
        case Function:
            if (function.is(RuntimeInfo.FunctionFlags.Legacy)) {
                assert !strict;
                allocator = LegacyConstructorFunction::new;
            } else {
                allocator = OrdinaryConstructorFunction::new;
            }
            break;
        case Generator:
            allocator = OrdinaryGenerator::new;
            break;
        default:
            throw new AssertionError();
        }
        FunctionObject f = FunctionAllocate(cx, allocator, proto, strict, FunctionKind.Normal);
        /* steps 31-32 */
        LexicalEnvironment<GlobalEnvironmentRecord> scope = f.getRealm().getGlobalEnv();
        /* step 33 */
        FunctionInitialize(f, FunctionKind.Normal, function, scope, compiledFunction);
        /* steps 34-36 */
        switch (kind) {
        case AsyncFunction:
            /* step 36 */
            break;
        case AsyncGenerator: {
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.AsyncGeneratorPrototype);
            f.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
            break;
        }
        case Function:
            /* step 35 */
            if (f instanceof LegacyConstructorFunction) {
                MakeConstructor(cx, (LegacyConstructorFunction) f);
            } else {
                MakeConstructor(cx, (OrdinaryConstructorFunction) f);
            }
            break;
        case Generator: {
            /* step 34 */
            OrdinaryObject prototype = ObjectCreate(cx, Intrinsics.GeneratorPrototype);
            f.infallibleDefineOwnProperty("prototype", new Property(prototype, true, false, false));
            break;
        }
        default:
            throw new AssertionError();
        }
        /* step 37 */
        SetFunctionName(f, "anonymous");
        /* step 38 */
        return f;
    }

    private static Source functionSource(SourceKind kind, ExecutionContext caller) {
        Source baseSource = caller.sourceInfo();
        String sourceName;
        if (baseSource != null) {
            sourceName = String.format("<%s> (%s)", kind.name(), baseSource.getName());
        } else {
            sourceName = String.format("<%s>", kind.name());
        }
        return new Source(baseSource, sourceName, 1);
    }

    /**
     * 19.2.2 Properties of the Function Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        /**
         * 19.2.2.1 Function.length
         */
        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "Function";

        /**
         * 19.2.2.2 Function.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.FunctionPrototype;
    }
}
