/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
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
public final class SymbolConstructor extends BuiltinConstructor implements Initializable,
        Creatable<SymbolObject> {
    /**
     * Constructs a new Symbol constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public SymbolConstructor(Realm realm) {
        super(realm, "Symbol", 1);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    @Override
    public SymbolConstructor clone() {
        return new SymbolConstructor(getRealm());
    }

    /**
     * 19.4.1.1 Symbol ( [ description ] )
     */
    @Override
    public Symbol call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object description = argument(args, 0);
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
        return Construct(callerContext, this, args);
    }

    private static final class SymbolCreate implements CreateAction<SymbolObject> {
        static final CreateAction<SymbolObject> INSTANCE = new SymbolCreate();

        @Override
        public SymbolObject create(ExecutionContext cx, Constructor constructor, Object... args) {
            throw newTypeError(cx, Messages.Key.SymbolCreate);
        }
    }

    @Override
    public CreateAction<SymbolObject> createAction() {
        return SymbolCreate.INSTANCE;
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
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "Symbol";

        /**
         * 19.4.2.7 Symbol.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.SymbolPrototype;

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
         * 19.4.2.8 Symbol.toPrimitive
         */
        @Value(name = "toPrimitive", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Symbol toPrimitive = BuiltinSymbol.toPrimitive.get();

        /**
         * 19.4.2.9 Symbol.toStringTag
         */
        @Value(name = "toStringTag", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Symbol toStringTag = BuiltinSymbol.toStringTag.get();

        /**
         * 19.4.2.10 Symbol.unscopables
         */
        @Value(name = "unscopables", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Symbol unscopables = BuiltinSymbol.unscopables.get();

        /**
         * 19.4.2.1 Symbol.for (key)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param key
         *            the symbol string key
         * @return the mapped symbol
         */
        @Function(name = "for", arity = 1)
        public static Object _for(ExecutionContext cx, Object thisValue, Object key) {
            /* steps 1-2 */
            String stringKey = ToFlatString(cx, key);
            /* steps 3-7 */
            return cx.getRealm().getSymbolRegistry().getSymbol(stringKey);
        }

        /**
         * 19.4.2.6 Symbol.keyFor (sym)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param sym
         *            the global symbol
         * @return the symbol string key or undefined if the symbol is not in the registry
         */
        @Function(name = "keyFor", arity = 1)
        public static Object keyFor(ExecutionContext cx, Object thisValue, Object sym) {
            /* step 1 */
            if (!Type.isSymbol(sym)) {
                throw newTypeError(cx, Messages.Key.NotSymbol);
            }
            /* steps 2-3 */
            String key = cx.getRealm().getSymbolRegistry().getKey(Type.symbolValue(sym));
            /* step 4 */
            return key != null ? key : UNDEFINED;
        }
    }
}
