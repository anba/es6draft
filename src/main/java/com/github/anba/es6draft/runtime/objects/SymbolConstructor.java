/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.4 Symbol Objects</h2>
 * <ul>
 * <li>19.4.1 The Symbol Constructor
 * <li>19.4.2 Properties of the Symbol Constructor
 * </ul>
 */
public class SymbolConstructor extends BuiltinConstructor implements Initialisable {
    public SymbolConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 19.4.1.1 Symbol ( description=undefined )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object description = args.length > 0 ? args[0] : UNDEFINED;
        /* steps 1-3 */
        String descString = Type.isUndefined(description) ? null : ToFlatString(calleeContext,
                description);
        /* step 4 */
        return new Symbol(descString);
    }

    /**
     * 19.4.1.2 new Symbol (... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 19.4.2 Properties of the Symbol Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "Symbol";

        /**
         * 19.4.2.6 Symbol.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.SymbolPrototype;

        /**
         * 19.4.2.1 Symbol.create
         */
        @Value(name = "create", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Symbol create = BuiltinSymbol.create.get();

        /**
         * 19.4.2.2 Symbol.hasInstance
         */
        @Value(name = "hasInstance", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Symbol hasInstance = BuiltinSymbol.hasInstance.get();

        /**
         * 19.4.2.3 Symbol.isConcatSpreadable
         */
        @Value(name = "isConcatSpreadable", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final Symbol isConcatSpreadable = BuiltinSymbol.isConcatSpreadable.get();

        /**
         * 19.4.2.4 Symbol.isRegExp
         */
        @Value(name = "isRegExp", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Symbol isRegExp = BuiltinSymbol.isRegExp.get();

        /**
         * 19.4.2.5 Symbol.iterator
         */
        @Value(name = "iterator", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Symbol iterator = BuiltinSymbol.iterator.get();

        /**
         * 19.4.2.7 Symbol.toPrimitive
         */
        @Value(name = "toPrimitive", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Symbol toPrimitive = BuiltinSymbol.toPrimitive.get();

        /**
         * 19.4.2.8 Symbol.toStringTag
         */
        @Value(name = "toStringTag", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Symbol toStringTag = BuiltinSymbol.toStringTag.get();

        /**
         * 19.4.2.9 Symbol.unscopables
         */
        @Value(name = "unscopables", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Symbol unscopables = BuiltinSymbol.unscopables.get();

        /**
         * 19.4.2.10 Symbol[ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            throw Errors.throwTypeError(cx, Messages.Key.SymbolCreate);
        }
    }
}
