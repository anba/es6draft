/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.NumberFormatConstructor.InitializeDefaultNumberFormat;
import static com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject.BoundFunctionCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.intl.NumberFormatConstructor.FormatFunction;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>11 NumberFormat Objects</h1>
 * <ul>
 * <li>11.4 Properties of the Intl.NumberFormat Prototype Object
 * </ul>
 */
public final class NumberFormatPrototype extends NumberFormatObject implements Initializable {
    /**
     * Constructs a new NumberFormat prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public NumberFormatPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);

        // Initialize Intl.NumberFormat.prototype's internal state.
        InitializeDefaultNumberFormat(realm, this);
    }

    /**
     * 11.4 Properties of the Intl.NumberFormat Prototype Object
     */
    public enum Properties {
        ;

        private static NumberFormatObject thisNumberFormatObject(ExecutionContext cx, Object object) {
            if (object instanceof NumberFormatObject) {
                return (NumberFormatObject) object;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 11.4.1 Intl.NumberFormat.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Intl_NumberFormat;

        /**
         * 11.4.2 Intl.NumberFormat.prototype[@@toStringTag]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Object";

        /**
         * 11.4.3 get Intl.NumberFormat.prototype.format
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the bound format function
         */
        @Accessor(name = "format", type = Accessor.Type.Getter)
        public static Object format(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            NumberFormatObject numberFormat = thisNumberFormatObject(cx, thisValue);
            /* step 4 */
            if (numberFormat.getBoundFormat() == null) {
                /* step 4.a */
                FormatFunction f = new FormatFunction(cx.getRealm());
                /* step 4.b */
                BoundFunctionObject bf = BoundFunctionCreate(cx, f, thisValue);
                /* step 4.c */
                bf.infallibleDefineOwnProperty("length", new Property(1, false, false, true));
                /* step 4.d */
                numberFormat.setBoundFormat(bf);
            }
            /* step 5 */
            return numberFormat.getBoundFormat();
        }

        /**
         * 11.4.4 Intl.NumberFormat.prototype.resolvedOptions ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the resolved options object
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(ExecutionContext cx, Object thisValue) {
            NumberFormatObject numberFormat = thisNumberFormatObject(cx, thisValue);
            OrdinaryObject object = OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype);
            CreateDataProperty(cx, object, "locale", numberFormat.getLocale());
            CreateDataProperty(cx, object, "numberingSystem", numberFormat.getNumberingSystem());
            CreateDataProperty(cx, object, "style", numberFormat.getStyle());
            if (numberFormat.getCurrency() != null) {
                CreateDataProperty(cx, object, "currency", numberFormat.getCurrency());
            }
            if (numberFormat.getCurrencyDisplay() != null) {
                CreateDataProperty(cx, object, "currencyDisplay", numberFormat.getCurrencyDisplay());
            }
            CreateDataProperty(cx, object, "minimumIntegerDigits", numberFormat.getMinimumIntegerDigits());
            CreateDataProperty(cx, object, "minimumFractionDigits", numberFormat.getMinimumFractionDigits());
            CreateDataProperty(cx, object, "maximumFractionDigits", numberFormat.getMaximumFractionDigits());
            if (numberFormat.getMinimumSignificantDigits() != 0) {
                CreateDataProperty(cx, object, "minimumSignificantDigits", numberFormat.getMinimumSignificantDigits());
            }
            if (numberFormat.getMaximumSignificantDigits() != 0) {
                CreateDataProperty(cx, object, "maximumSignificantDigits", numberFormat.getMaximumSignificantDigits());
            }
            CreateDataProperty(cx, object, "useGrouping", numberFormat.isUseGrouping());
            return object;
        }
    }
}
