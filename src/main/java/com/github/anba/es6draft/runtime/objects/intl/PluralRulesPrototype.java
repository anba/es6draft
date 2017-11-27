/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
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
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>PluralRules Objects</h1>
 * <ul>
 * <li>Properties of the Intl.PluralRules Prototype Object
 * </ul>
 */
public final class PluralRulesPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new PluralRules prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public PluralRulesPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * ResolvePlural (pluralRules, x)
     * 
     * @param pluralRules
     *            the pluralRules object
     * @param x
     *            the number value
     * @return the locale specific plural form
     */
    @SuppressWarnings("deprecation")
    public static String ResolvePlural(PluralRulesObject pluralRules, double x) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        if (!Double.isFinite(x)) {
            return "other";
        }
        /* steps 4-8 */
        return pluralRules.getPluralRules().select(pluralRules.toFixedDecimal(x));
    }

    /**
     * Properties of the Intl.PluralRules Prototype Object
     */
    public enum Properties {
        ;

        private static PluralRulesObject thisPluralRulesObject(ExecutionContext cx, Object value, String method) {
            if (value instanceof PluralRulesObject) {
                return (PluralRulesObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * Intl.PluralRules.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Intl_PluralRules;

        /**
         * Intl.PluralRules.prototype[@@toStringTag]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Object";

        /**
         * Intl.PluralRules.prototype.select( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the number value
         * @return the bound compare function
         */
        @Function(name = "select", arity = 1)
        public static Object select(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-2 */
            PluralRulesObject pluralRules = thisPluralRulesObject(cx, thisValue, "Intl.PluralRules.prototype.select");
            /* step 3 (not applicable) */
            /* step 4 */
            double n = ToNumber(cx, value);
            /* step 5 */
            return ResolvePlural(pluralRules, n);
        }

        /**
         * Intl.PluralRules.prototype.resolvedOptions ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the resolved options object
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(ExecutionContext cx, Object thisValue) {
            PluralRulesObject pluralRules = thisPluralRulesObject(cx, thisValue,
                    "Intl.PluralRules.prototype.resolvedOptions");
            OrdinaryObject object = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            CreateDataProperty(cx, object, "locale", pluralRules.getLocale());
            CreateDataProperty(cx, object, "type", pluralRules.getType());
            CreateDataProperty(cx, object, "minimumIntegerDigits", pluralRules.getMinimumIntegerDigits());
            CreateDataProperty(cx, object, "minimumFractionDigits", pluralRules.getMinimumFractionDigits());
            CreateDataProperty(cx, object, "maximumFractionDigits", pluralRules.getMaximumFractionDigits());
            if (pluralRules.getMinimumSignificantDigits() != 0) {
                CreateDataProperty(cx, object, "minimumSignificantDigits", pluralRules.getMinimumSignificantDigits());
                CreateDataProperty(cx, object, "maximumSignificantDigits", pluralRules.getMaximumSignificantDigits());
            }
            // FIXME: spec issue - the array isn't frozen
            // FIXME: spec issue - the array is from a different realm
            CreateDataProperty(cx, object, "pluralCategories", pluralRules.getPluralCategories());
            return object;
        }
    }
}
