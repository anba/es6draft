/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.4 Symbol Objects</h2>
 * <ul>
 * <li>19.4.3 Properties of the Symbol Prototype Object
 * </ul>
 */
public final class SymbolPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Symbol prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public SymbolPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 19.4.3.2.1 Runtime Semantics: SymbolDescriptiveString ( sym )
     * 
     * @param sym
     *            the symbol value
     * @return the descriptive string
     */
    public static String SymbolDescriptiveString(Symbol sym) {
        /* step 1 (not applicable) */
        /* step 2 */
        String desc = sym.getDescription();
        /* step 3 */
        if (desc == null) {
            desc = "";
        }
        /* step 4 (not applicable) */
        /* steps 5-6 */
        return "Symbol(" + desc + ")";
    }

    /**
     * 19.4.3 Properties of the Symbol Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract operation thisSymbolValue(value)
         * 
         * @param cx
         *            the execution context
         * @param object
         *            the symbol object
         * @return the symbol value
         */
        private static Symbol thisSymbolValue(ExecutionContext cx, Object object) {
            if (Type.isSymbol(object)) {
                return (Symbol) object;
            }
            if (object instanceof SymbolObject) {
                return ((SymbolObject) object).getSymbolData();
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
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
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string representation
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            Symbol sym = thisSymbolValue(cx, thisValue);
            /* step 4 */
            return SymbolDescriptiveString(sym);
        }

        /**
         * 19.4.3.3 Symbol.prototype.valueOf ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the symbol value
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            /* steps 1-5 */
            return thisSymbolValue(cx, thisValue);
        }

        /**
         * 19.4.3.4 Symbol.prototype [ @@toPrimitive ] ( hint )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param hint
         *            the ToPrimitive hint string
         * @return always throws a TypeError
         */
        @Function(name = "[Symbol.toPrimitive]", symbol = BuiltinSymbol.toPrimitive, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object toPrimitive(ExecutionContext cx, Object thisValue, Object hint) {
            /* steps 1-5 */
            return thisSymbolValue(cx, thisValue);
        }

        /**
         * 19.4.3.5 Symbol.prototype [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Symbol";
    }
}
