/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.GetPrototypeFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.MakeConstructor;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.FunctionAllocate;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.FunctionKind;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.19 The "std:iteration" Module</h2><br>
 * <h3>15.19.3 GeneratorFunction Objects</h3>
 * <ul>
 * <li>15.19.3.1 The GeneratorFunction Constructor
 * <li>15.19.3.2 Properties of the GeneratorFunction Constructor
 * </ul>
 */
public class GeneratorFunctionConstructor extends BuiltinFunction implements Constructor,
        Initialisable {
    public GeneratorFunctionConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 15.19.3.1.1 GeneratorFunction (p1, p2, ... , pn, body)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = realm().defaultContext();

        /* steps 1-7 */
        int argCount = args.length;
        StringBuilder p = new StringBuilder();
        CharSequence bodyText;
        if (argCount == 0) {
            bodyText = "";
        } else if (argCount == 1) {
            bodyText = ToString(calleeContext, args[0]);
        } else {
            Object firstArg = args[0];
            p.append(ToString(calleeContext, firstArg));
            int k = 2;
            for (; k < argCount; ++k) {
                Object nextArg = args[k - 1];
                CharSequence nextArgString = ToString(calleeContext, nextArg);
                p.append(',').append(nextArgString);
            }
            bodyText = ToString(calleeContext, args[k - 1]);
        }

        /* steps 8-12 */
        Script script = script(calleeContext, p, bodyText);

        /* step 13 */
        LexicalEnvironment scope = calleeContext.getRealm().getGlobalEnv();
        /* step 14 */
        Object f = thisValue;
        /* step 15 */
        if (!Type.isObject(f) || !(f instanceof FunctionObject)
                || ((FunctionObject) f).getCode() != null) {
            ScriptObject proto = calleeContext.getIntrinsic(Intrinsics.Generator);
            f = FunctionAllocate(calleeContext, proto, FunctionKind.Normal);
        }
        /* step 16 */
        if (!(f instanceof OrdinaryGenerator)) {
            throw throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        OrdinaryGenerator fn = (OrdinaryGenerator) f;

        /* step 17 */
        ExecutionContext scriptCxt = ExecutionContext.newScriptExecutionContext(calleeContext
                .getRealm());
        Object result = script.evaluate(scriptCxt);
        assert result instanceof OrdinaryGenerator;
        RuntimeInfo.Function function = ((OrdinaryGenerator) result).getFunction();
        assert function != null : "uninitialised function object";

        /* step 18 */
        FunctionInitialize(calleeContext, fn, FunctionKind.Normal, function, scope);
        /* step 19 */
        ScriptObject prototype = ObjectCreate(calleeContext, Intrinsics.GeneratorPrototype);
        /* step 20 */
        MakeConstructor(calleeContext, fn, true, prototype);
        /* step 21 */
        return fn;
    }

    /**
     * 15.19.3.1.2 new GeneratorFunction (...argumentsList)
     */
    @Override
    public Object construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    private static Script script(ExecutionContext cx, CharSequence p, CharSequence bodyText) {
        try {
            Parser parser = new Parser("<GeneratorFunction>", 1);
            com.github.anba.es6draft.ast.Script parsedScript = parser.parseGenerator(p, bodyText);
            String className = cx.getRealm().nextFunctionName();
            return ScriptLoader.load(className, parsedScript);
        } catch (ParserException e) {
            throw e.toScriptException(cx);
        }
    }

    /**
     * 15.19.3.2 Properties of the GeneratorFunction Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Function;

        /**
         * 15.19.3.2.1 GeneratorFunction.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Generator;

        /**
         * 15.19.3.2.2 GeneratorFunction.length
         */
        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "GeneratorFunction";

        /**
         * 15.19.3.2.3 GeneratorFunction[ @@create ] ( )
         */
        @Function(name = "@@create", arity = 0, symbol = BuiltinSymbol.create,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            ScriptObject proto = GetPrototypeFromConstructor(cx, thisValue, Intrinsics.Generator);
            OrdinaryGenerator obj = FunctionAllocate(cx, proto, FunctionKind.Normal);
            return obj;
        }
    }
}
