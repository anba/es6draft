/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeMethod;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.SetFunctionName;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.FunctionAllocate;

import java.util.Objects;

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
public final class GeneratorFunctionConstructor extends BuiltinConstructor implements
        Initializable, Creatable<OrdinaryGenerator> {
    /**
     * Constructs a new Generator Function constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public GeneratorFunctionConstructor(Realm realm) {
        super(realm, "GeneratorFunction");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
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
                CharSequence nextArgString = ToFlatString(calleeContext, nextArg);
                p.append(',').append(nextArgString);
            }
            bodyText = ToFlatString(calleeContext, args[k - 1]);
        }

        /* steps 8-10 */
        Source source = generatorSource(callerContext);
        CompiledGenerator exec = new CompiledGenerator(source);
        RuntimeInfo.Function function;
        try {
            ScriptLoader scriptLoader = calleeContext.getRealm().getScriptLoader();
            function = scriptLoader.generator(source, p.toString(), bodyText).getFunction();
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
                    Intrinsics.Generator);
            f = FunctionAllocate(calleeContext, proto, strict, FunctionKind.Normal);
        }
        /* step 15 */
        if (!(f instanceof OrdinaryGenerator)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        OrdinaryGenerator fn = (OrdinaryGenerator) f;
        /* steps 16-18 */
        if (!IsExtensible(calleeContext, fn)) {
            throw newTypeError(calleeContext, Messages.Key.NotExtensible);
        }
        /* steps 19-20 */
        FunctionInitialize(calleeContext, fn, FunctionKind.Normal, strict, function, scope, exec);
        /* step 21 */
        OrdinaryObject prototype = ObjectCreate(calleeContext, Intrinsics.GeneratorPrototype);
        /* step 22 */
        if (function.hasSuperReference()) {
            MakeMethod(fn, null);
        }
        /* steps 23-24 */
        MakeConstructor(calleeContext, fn, true, prototype);
        /* steps 25-27 */
        if (!HasOwnProperty(calleeContext, fn, "name")) {
            SetFunctionName(fn, "anonymous");
        }
        /* step 28 */
        return fn;
    }

    /**
     * 25.2.1.2 new GeneratorFunction (...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    private static final class GeneratorCreate implements CreateAction<OrdinaryGenerator> {
        static final CreateAction<OrdinaryGenerator> INSTANCE = new GeneratorCreate();

        @Override
        public OrdinaryGenerator create(ExecutionContext cx, Constructor constructor,
                Object... args) {
            /* steps 1-3 */
            ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, Intrinsics.Generator);
            /* step 4 */
            return FunctionAllocate(cx, proto, false, FunctionKind.Normal);
        }
    }

    @Override
    public CreateAction<OrdinaryGenerator> createAction() {
        return GeneratorCreate.INSTANCE;
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
        public String sourceName() {
            return source.getName();
        }

        @Override
        public String sourceFile() {
            return Objects.toString(source.getFile(), null);
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
