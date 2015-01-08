/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.*;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.CompiledObject;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.DebugInfo;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
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
public final class FunctionConstructor extends BuiltinConstructor implements Initializable,
        Creatable<OrdinaryFunction> {
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
        CompiledFunction exec = new CompiledFunction(source);
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
        Object f = thisValue;
        /* step 13 */
        if (!(f instanceof FunctionObject) || ((FunctionObject) f).getCode() != null) {
            ScriptObject proto = GetPrototypeFromConstructor(calleeContext, this,
                    Intrinsics.FunctionPrototype);
            f = FunctionAllocate(calleeContext, proto, strict, FunctionKind.Normal);
        }
        /* step 14 */
        if (!(f instanceof OrdinaryFunction)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        OrdinaryFunction fn = (OrdinaryFunction) f;
        /* steps 15-16 */
        LexicalEnvironment<GlobalEnvironmentRecord> scope = fn.getRealm().getGlobalEnv();
        /* steps 17-19 */
        if (!IsExtensible(calleeContext, fn)) {
            throw newTypeError(calleeContext, Messages.Key.NotExtensible);
        }
        /* steps 20-21 */
        FunctionInitialize(calleeContext, fn, FunctionKind.Normal, strict, function, scope, exec);
        /* step 22 */
        if (function.hasSuperReference()) {
            MakeMethod(fn, null);
        }
        /* steps 23-24 */
        MakeConstructor(calleeContext, fn);
        /* steps 25-27 */
        if (!HasOwnProperty(calleeContext, fn, "name")) {
            SetFunctionName(fn, "anonymous");
        }
        /* step 28 */
        return fn;
    }

    /**
     * 19.2.1.2 new Function ( ... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    private static final class FunctionCreate implements CreateAction<OrdinaryFunction> {
        static final CreateAction<OrdinaryFunction> INSTANCE = new FunctionCreate();

        @Override
        public OrdinaryFunction create(ExecutionContext cx, Constructor constructor, Object... args) {
            /* steps 1-2 */
            ScriptObject proto = GetPrototypeFromConstructor(cx, constructor,
                    Intrinsics.FunctionPrototype);
            /* step 3 */
            return FunctionAllocate(cx, proto, false, FunctionKind.Normal);
        }
    }

    @Override
    public CreateAction<OrdinaryFunction> createAction() {
        return FunctionCreate.INSTANCE;
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
