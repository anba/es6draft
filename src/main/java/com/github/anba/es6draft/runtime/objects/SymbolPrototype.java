/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.runtime.types.builtins.StringObject;

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
        createProperties(realm, this, SymbolDescriptionAccessor.class);
    }

    /**
     * 19.4.3.2.1 Runtime Semantics: SymbolDescriptiveString ( sym )
     * 
     * @param cx
     *            the execution context
     * @param sym
     *            the symbol value
     * @return the descriptive string
     */
    public static String SymbolDescriptiveString(ExecutionContext cx, Symbol sym) {
        /* step 1 (not applicable) */
        /* step 2 */
        String desc = sym.getDescription();
        /* step 3 */
        if (desc == null) {
            desc = "";
        }
        /* step 4 (not applicable) */
        /* step 5 */
        return StringObject.validateLength(cx, "Symbol(" + desc + ")");
    }

    /**
     * Abstract operation thisSymbolValue(value)
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the value
     * @param method
     *            the method
     * @return the symbol value
     */
    private static Symbol thisSymbolValue(ExecutionContext cx, Object value, String method) {
        if (Type.isSymbol(value)) {
            return (Symbol) value;
        }
        if (value instanceof SymbolObject) {
            return ((SymbolObject) value).getSymbolData();
        }
        throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
    }

    /**
     * 19.4.3 Properties of the Symbol Prototype Object
     */
    public enum Properties {
        ;

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
            Symbol sym = thisSymbolValue(cx, thisValue, "Symbol.prototype.toString");
            /* step 4 */
            return SymbolDescriptiveString(cx, sym);
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
            return thisSymbolValue(cx, thisValue, "Symbol.prototype.valueOf");
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
        @Function(name = "[Symbol.toPrimitive]", symbol = BuiltinSymbol.toPrimitive, arity = 1,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object toPrimitive(ExecutionContext cx, Object thisValue, Object hint) {
            /* steps 1-5 */
            return thisSymbolValue(cx, thisValue, "Symbol.prototype[@@toPrimitive]");
        }

        /**
         * 19.4.3.5 Symbol.prototype [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Symbol";
    }

    /**
     * Extension: Symbol.prototype.description
     */
    @CompatibilityExtension(CompatibilityOption.SymbolDescription)
    public enum SymbolDescriptionAccessor {
        ;

        /**
         * get Symbol.prototype.description
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the stack string
         */
        @Accessor(name = "description", type = Accessor.Type.Getter)
        public static Object get_description(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            Symbol sym = thisSymbolValue(cx, thisValue, "get Symbol.prototype.description");
            /* step 3 */
            String desc = sym.getDescription();
            return desc != null ? desc : UNDEFINED;
        }
    }
}
