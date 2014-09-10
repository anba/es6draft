/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.*;

import java.util.Objects;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.CompiledScript;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.*;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
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
        super(realm, "Function");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
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

        /* steps 1-7 */
        int argCount = args.length;
        StringBuilder p = new StringBuilder();
        String bodyText;
        if (argCount == 0) {
            bodyText = "";
        } else if (argCount == 1) {
            bodyText = ToFlatString(calleeContext, args[0]);
        } else {
            Object firstArg = args[0];
            p.append(ToFlatString(calleeContext, firstArg));
            int k = 2;
            for (; k < argCount; ++k) {
                Object nextArg = args[k - 1];
                String nextArgString = ToFlatString(calleeContext, nextArg);
                p.append(',').append(nextArgString);
            }
            bodyText = ToFlatString(calleeContext, args[k - 1]);
        }

        /* steps 8-10 */
        Source source = functionSource(callerContext);
        Script script = new FunctionScript(source);
        RuntimeInfo.Function function;
        try {
            ScriptLoader scriptLoader = calleeContext.getRealm().getScriptLoader();
            function = scriptLoader.function(source, p.toString(), bodyText).getFunction();
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(calleeContext);
        }

        /* step 11 */
        boolean strict = function.isStrict();
        /* step 12 */
        LexicalEnvironment<GlobalEnvironmentRecord> scope = calleeContext.getRealm().getGlobalEnv();
        /* step 13 */
        Object f = thisValue;
        /* step 14 */
        if (!(f instanceof FunctionObject) || ((FunctionObject) f).getCode() != null) {
            ScriptObject proto = GetPrototypeFromConstructor(calleeContext, this,
                    Intrinsics.FunctionPrototype);
            f = FunctionAllocate(calleeContext, proto, strict, FunctionKind.Normal);
        }
        /* step 15 */
        if (!(f instanceof OrdinaryFunction)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        OrdinaryFunction fn = (OrdinaryFunction) f;
        /* steps 16-18 */
        if (!IsExtensible(calleeContext, fn)) {
            throw newTypeError(calleeContext, Messages.Key.NotExtensible);
        }
        /* steps 19-20 */
        FunctionInitialize(calleeContext, fn, FunctionKind.Normal, strict, function, scope, script);
        /* step 21 */
        if (function.hasSuperReference()) {
            MakeMethod(fn, (String) null, null);
        }
        /* steps 22-23 */
        MakeConstructor(calleeContext, fn);
        /* steps 24-26 */
        if (!HasOwnProperty(calleeContext, fn, "name")) {
            SetFunctionName(fn, "anonymous");
        }
        /* step 27 */
        return fn;
    }

    /**
     * 19.2.1.2 new Function ( ... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
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

        /**
         * 19.2.2.3 Function[ @@create ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the new uninitialized function object
         */
        @Function(name = "[Symbol.create]", arity = 0, symbol = BuiltinSymbol.create,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            ScriptObject proto = GetPrototypeFromConstructor(cx, thisValue,
                    Intrinsics.FunctionPrototype);
            /* step 4 */
            return FunctionAllocate(cx, proto, false, FunctionKind.Normal);
        }
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

    private static final class FunctionScript extends CompiledScript {
        protected FunctionScript(Source source) {
            super(new FunctionScriptBody(source));
        }
    }

    private static final class FunctionScriptBody implements RuntimeInfo.ScriptBody {
        private final Source source;

        FunctionScriptBody(Source source) {
            this.source = source;
        }

        @Override
        public String sourceName() {
            return source.getName();
        }

        @Override
        public String sourceFile() {
            return Objects.toString(source.getFile(), null);
        }

        @Override
        public boolean isStrict() {
            throw new IllegalStateException();
        }

        @Override
        public void globalDeclarationInstantiation(ExecutionContext cx,
                LexicalEnvironment<GlobalEnvironmentRecord> globalEnv,
                LexicalEnvironment<?> lexicalEnv, boolean deletableBindings) {
            throw new IllegalStateException();
        }

        @Override
        public void evalDeclarationInstantiation(ExecutionContext cx,
                LexicalEnvironment<?> variableEnv, LexicalEnvironment<?> lexicalEnv,
                boolean deletableBindings) {
            throw new IllegalStateException();
        }

        @Override
        public Object evaluate(ExecutionContext cx) {
            throw new IllegalStateException();
        }

        @Override
        public DebugInfo debugInfo() {
            return null;
        }
    }
}
