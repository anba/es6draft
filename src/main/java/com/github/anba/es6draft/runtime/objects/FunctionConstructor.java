/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction.FunctionAllocate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.SetFunctionName;

import com.github.anba.es6draft.Executable;
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
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.ConstructorKind;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.LegacyConstructorFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryConstructorFunction;

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
    public FunctionObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* steps 1-3 */
        return CreateDynamicFunction(callerContext, calleeContext(), this, args);
    }

    /**
     * 19.2.1.1 Function (p1, p2, ... , pn, body)
     */
    @Override
    public FunctionObject construct(ExecutionContext callerContext, Constructor newTarget,
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
     * @return the new function object
     */
    private static FunctionObject CreateDynamicFunction(
            ExecutionContext callerContext, ExecutionContext cx, Constructor newTarget,
            Object... args) {
        /* step 1 (not applicable) */
        /* step 2 */
        Intrinsics fallbackProto = Intrinsics.FunctionPrototype;
        /* step 3 (not applicable) */

        /* steps 4-10 */
        String[] sourceText = functionSourceText(cx, args);
        String parameters = sourceText[0], bodyText = sourceText[1];

        /* steps 11, 13-20 */
        Source source = functionSource(SourceKind.Function, cx.getRealm(), callerContext);
        RuntimeInfo.Function function;
        try {
            ScriptLoader scriptLoader = cx.getRealm().getScriptLoader();
            function = scriptLoader.function(source, parameters, bodyText).getFunction();
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        }

        /* steps 12, 21-30 */
        return CreateDynamicFunction(cx, source, function, newTarget, fallbackProto);
    }

    /**
     * 19.2.1.1.1 RuntimeSemantics: CreateDynamicFunction(constructor, newTarget, kind, args)
     * 
     * @param cx
     *            the execution context
     * @param source
     *            the source object
     * @param function
     *            the compiled function
     * @return the new function object
     */
    public static FunctionObject CreateDynamicFunction(ExecutionContext cx,
            Source source, RuntimeInfo.Function function) {
        return CreateDynamicFunction(cx, source, function,
                (Constructor) cx.getIntrinsic(Intrinsics.Function), Intrinsics.FunctionPrototype);
    }

    /**
     * 19.2.1.1.1 RuntimeSemantics: CreateDynamicFunction(constructor, newTarget, kind, args)
     * 
     * @param cx
     *            the execution context
     * @param source
     *            the source object
     * @param function
     *            the compiled function
     * @param newTarget
     *            the newTarget constructor function
     * @param fallbackProto
     *            the fallback prototype
     * @return the new function object
     */
    private static FunctionObject CreateDynamicFunction(ExecutionContext cx,
            Source source, RuntimeInfo.Function function, Constructor newTarget,
            Intrinsics fallbackProto) {
        /* steps 1-11, 13-20 (not applicable) */
        /* step 12 */
        boolean strict = function.isStrict();
        /* steps 21-22 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget, fallbackProto);
        /* step 23 */
        FunctionObject f;
        if (function.is(RuntimeInfo.FunctionFlags.Legacy)) {
            assert !strict;
            f = LegacyConstructorFunction.FunctionAllocate(cx, proto);
        } else {
            f = FunctionAllocate(cx, proto, strict, FunctionKind.Normal, ConstructorKind.Base);
        }
        /* steps 24-25 */
        LexicalEnvironment<GlobalEnvironmentRecord> scope = f.getRealm().getGlobalEnv();
        /* step 26 */
        FunctionInitialize(f, FunctionKind.Normal, function, scope, newFunctionExecutable(source));
        /* step 27 (not applicable) */
        /* step 28 */
        // MakeConstructor(cx, uncheckedCast(f));
        // Work around for: https://bugs.eclipse.org/bugs/show_bug.cgi?id=479802
        if (f instanceof LegacyConstructorFunction) {
            MakeConstructor(cx, (LegacyConstructorFunction) f);
        } else {
            MakeConstructor(cx, (OrdinaryConstructorFunction) f);
        }
        /* step 29 */
        SetFunctionName(f, "anonymous");
        /* step 30 */
        return f;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private static <T extends FunctionObject & Constructor> T uncheckedCast(FunctionObject f) {
        return (T) f;
    }

    /**
     * 19.2.1.1.1 RuntimeSemantics: CreateDynamicFunction(constructor, newTarget, kind, args)
     * 
     * @param cx
     *            the execution context
     * @param args
     *            the function arguments
     * @return the function source text as a tuple {@code <parameters, body>}
     */
    public static String[] functionSourceText(ExecutionContext cx, Object... args) {
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
        return new String[] { p, bodyText };
    }

    public enum SourceKind {
        Function, Generator, AsyncFunction
    }

    /**
     * Creates a {@link Source} object for a dynamic function.
     * 
     * @param kind
     *            the function kind
     * @param realm
     *            the realm
     * @param caller
     *            the caller execution context
     * @return the function source object
     */
    public static Source functionSource(SourceKind kind, Realm realm, ExecutionContext caller) {
        Source baseSource = realm.sourceInfo(caller);
        String sourceName;
        if (baseSource != null) {
            sourceName = String.format("<%s> (%s)", kind.name(), baseSource.getName());
        } else {
            sourceName = String.format("<%s>", kind.name());
        }
        return new Source(baseSource, sourceName, 1);
    }

    /**
     * Creates a new executable object for a dynamic function.
     * 
     * @param source
     *            the function source object
     * @return a new executable object
     */
    public static Executable newFunctionExecutable(Source source) {
        return new CompiledFunction(source);
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
        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "Function";

        /**
         * 19.2.2.2 Function.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.FunctionPrototype;
    }
}
