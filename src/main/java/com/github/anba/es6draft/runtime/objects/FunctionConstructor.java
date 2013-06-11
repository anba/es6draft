/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.GetPrototypeFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.*;

import java.util.EnumSet;

import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.ast.FunctionDefinition;
import com.github.anba.es6draft.compiler.CompilationException;
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
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.3 Function Objects</h2>
 * <ul>
 * <li>15.3.1 The Function Constructor
 * <li>15.3.2 Properties of the Function Constructor
 * </ul>
 */
public class FunctionConstructor extends BuiltinFunction implements Constructor, Initialisable {
    public FunctionConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 15.3.1.1 Function (p1, p2, ... , pn, body)
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

        /* steps 8-13 */
        RuntimeInfo.Function function;
        try {
            EnumSet<Parser.Option> options = Parser.Option.from(calleeContext.getRealm()
                    .getOptions());
            Parser parser = new Parser("<Function>", 1, options);
            FunctionDefinition functionDef = parser.parseFunction(p, bodyText);
            String className = calleeContext.getRealm().nextFunctionName();
            function = ScriptLoader.compile(className, functionDef);
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(calleeContext);
        }

        /* step 14 */
        LexicalEnvironment scope = calleeContext.getRealm().getGlobalEnv();
        /* step 15 */
        Object f = thisValue;
        /* step 16 */
        if (!Type.isObject(f) || !(f instanceof FunctionObject)
                || ((FunctionObject) f).getCode() != null) {
            ScriptObject proto = calleeContext.getIntrinsic(Intrinsics.FunctionPrototype);
            f = FunctionAllocate(calleeContext, proto, FunctionKind.Normal);
        }
        /* step 17 */
        if (!(f instanceof OrdinaryFunction)) {
            throw throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        OrdinaryFunction fn = (OrdinaryFunction) f;

        /* step 18 */
        FunctionInitialize(calleeContext, fn, FunctionKind.Normal, function, scope);
        /* step 19 */
        MakeConstructor(calleeContext, fn);
        /* step 20 */
        return fn;
    }

    /**
     * 15.3.1.2 new Function ( ... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 15.3.2 Properties of the Function Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        /**
         * 15.3.2.1 Function.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.FunctionPrototype;

        /**
         * 15.3.2.2 Function.length
         */
        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "Function";

        /**
         * 15.3.2.3 Function[ @@create ] ( )
         */
        @Function(name = "@@create", arity = 0, symbol = BuiltinSymbol.create,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            ScriptObject proto = GetPrototypeFromConstructor(cx, thisValue,
                    Intrinsics.FunctionPrototype);
            OrdinaryFunction obj = FunctionAllocate(cx, proto, FunctionKind.Normal);
            return obj;
        }
    }
}
