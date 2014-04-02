/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetPrototypeFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.HasOwnProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.*;

import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.ast.FunctionDefinition;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
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
public final class FunctionConstructor extends BuiltinConstructor implements Initialisable {
    public FunctionConstructor(Realm realm) {
        super(realm, "Function");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
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

        /* steps 8-11 */
        RuntimeInfo.Function function;
        try {
            Realm realm = calleeContext.getRealm();
            Parser parser = new Parser("<Function>", 1, realm.getOptions());
            FunctionDefinition functionDef = parser.parseFunction(p, bodyText);
            function = ScriptLoader.compile(realm, functionDef);
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(calleeContext);
        }

        /* step 12 */
        boolean strict = function.isStrict();
        /* step 13 */
        LexicalEnvironment<GlobalEnvironmentRecord> scope = calleeContext.getRealm().getGlobalEnv();
        /* step 14 */
        Object f = thisValue;
        /* steps 15-16 */
        if (!(f instanceof FunctionObject) || ((FunctionObject) f).getCode() != null) {
            ScriptObject proto = GetPrototypeFromConstructor(calleeContext, this,
                    Intrinsics.FunctionPrototype);
            f = FunctionAllocate(calleeContext, proto, strict, FunctionKind.Normal);
        } else {
            // FIXME: this also updates uninitialised generator (not function!)
            ((FunctionObject) f).setStrict(strict);
        }
        /* step 17 */
        if (!(f instanceof OrdinaryFunction)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        OrdinaryFunction fn = (OrdinaryFunction) f;
        /* step 18 */
        FunctionInitialise(calleeContext, fn, FunctionKind.Normal, function, scope);
        /* step 19 */
        if (function.hasSuperReference()) {
            MakeMethod(fn, (String) null, null);
        }
        /* steps 20-21 */
        MakeConstructor(calleeContext, fn);
        /* step 22 */
        // FIXME: if-test missing in spec https://bugs.ecmascript.org/show_bug.cgi?id=2335
        if (!HasOwnProperty(calleeContext, fn, "name")) {
            SetFunctionName(fn, "anonymous");
        }
        /* step 23 */
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
         */
        @Function(name = "[Symbol.create]", arity = 0, symbol = BuiltinSymbol.create,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            ScriptObject proto = GetPrototypeFromConstructor(cx, thisValue,
                    Intrinsics.FunctionPrototype);
            OrdinaryFunction obj = FunctionAllocate(cx, proto, false, FunctionKind.Normal);
            return obj;
        }
    }
}
