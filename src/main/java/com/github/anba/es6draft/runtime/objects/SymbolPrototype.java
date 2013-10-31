/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.util.Objects;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.4 Symbol Objects</h2>
 * <ul>
 * <li>19.4.3 Properties of the Symbol Prototype Object
 * </ul>
 */
public class SymbolPrototype extends OrdinaryObject implements Initialisable {
    public SymbolPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 19.4.3 Properties of the Symbol Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract operation thisSymbolValue(value)
         */
        private static Symbol thisSymbolValue(ExecutionContext cx, Object object) {
            if (object instanceof SymbolObject) {
                return ((SymbolObject) object).getSymbolData();
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 19.4.3.1 Symbol.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Symbol;

        /**
         * 19.4.3.2 Symbol.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            Symbol sym = thisSymbolValue(cx, thisValue);
            /* steps 4-6 */
            String desc = Objects.toString(sym.getDescription(), "");
            /* steps 7-8 */
            return "Symbol(" + desc + ")";
        }

        /**
         * 19.4.3.3 Symbol.prototype.valueOf ( )
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            return thisSymbolValue(cx, thisValue);
        }

        /**
         * 19.4.3.4 Symbol.prototype [ @@toPrimitive ] ( hint )
         */
        @Function(name = "[Symbol.toPrimitive]", symbol = BuiltinSymbol.toPrimitive, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object toPrimitive(ExecutionContext cx, Object thisValue, Object hint) {
            /* step 1 */
            throw throwTypeError(cx, Messages.Key.SymbolPrimitive);
        }

        /**
         * 19.4.3.5 Symbol.prototype [ @@toStringTag ]
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Symbol";
    }
}
