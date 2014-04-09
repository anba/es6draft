/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.NumberFormat;

/**
 * <h1>11 NumberFormat Objects</h1>
 * <ul>
 * <li>11.3 Properties of the Intl.NumberFormat Prototype Object
 * </ul>
 */
public final class NumberFormatPrototype extends NumberFormatObject implements Initialisable {
    public NumberFormatPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);

        // initialise Intl.NumberFormat.prototype's internal state
        NumberFormatConstructor.InitializeNumberFormat(cx, this, UNDEFINED, UNDEFINED);
    }

    /**
     * 11.3 Properties of the Intl.NumberFormat Prototype Object
     */
    public enum Properties {
        ;

        private static NumberFormatObject thisNumberFormatValue(ExecutionContext cx, Object object) {
            if (object instanceof NumberFormatObject) {
                NumberFormatObject numberFormat = (NumberFormatObject) object;
                if (numberFormat.isInitializedNumberFormat()) {
                    return numberFormat;
                }
                throw newTypeError(cx, Messages.Key.UninitialisedObject);
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 11.3.1 Intl.NumberFormat.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Intl_NumberFormat;

        /**
         * 11.3.2 Intl.NumberFormat.prototype.format
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the bound format function
         */
        @Accessor(name = "format", type = Accessor.Type.Getter)
        public static Object format(ExecutionContext cx, Object thisValue) {
            NumberFormatObject numberFormat = thisNumberFormatValue(cx, thisValue);
            if (numberFormat.getBoundFormat() == null) {
                FormatFunction f = new FormatFunction(cx.getRealm());
                Callable bf = (Callable) FunctionPrototype.Properties.bind(cx, f, thisValue);
                numberFormat.setBoundFormat(bf);
            }
            return numberFormat.getBoundFormat();
        }

        /**
         * 11.3.3 Intl.NumberFormat.prototype.resolvedOptions ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the resolved options object
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(ExecutionContext cx, Object thisValue) {
            NumberFormatObject numberFormat = thisNumberFormatValue(cx, thisValue);
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
            CreateDataProperty(cx, object, "minimumIntegerDigits",
                    numberFormat.getMinimumIntegerDigits());
            CreateDataProperty(cx, object, "minimumFractionDigits",
                    numberFormat.getMinimumFractionDigits());
            CreateDataProperty(cx, object, "maximumFractionDigits",
                    numberFormat.getMaximumFractionDigits());
            if (numberFormat.getMinimumSignificantDigits() != 0) {
                CreateDataProperty(cx, object, "minimumSignificantDigits",
                        numberFormat.getMinimumSignificantDigits());
            }
            if (numberFormat.getMaximumSignificantDigits() != 0) {
                CreateDataProperty(cx, object, "maximumSignificantDigits",
                        numberFormat.getMaximumSignificantDigits());
            }
            CreateDataProperty(cx, object, "useGrouping", numberFormat.isUseGrouping());
            return object;
        }
    }

    /**
     * Abstract Operation: FormatNumber
     * 
     * @param cx
     *            the execution context
     * @param numberFormat
     *            the number format object
     * @param x
     *            the number value
     * @return the formatted number string
     */
    public static String FormatNumber(ExecutionContext cx, NumberFormatObject numberFormat, double x) {
        if (x == -0.0) {
            // -0 is not considered to be negative, cf. step 3a
            x = +0.0;
        }
        NumberFormat format = numberFormat.getNumberFormat();
        return format.format(x);
    }

    private static final class FormatFunction extends BuiltinFunction {
        public FormatFunction(Realm realm) {
            super(realm, "format", 1);
        }

        /**
         * [[Call]]
         */
        @Override
        public String call(ExecutionContext callerContext, Object thisValue, Object... args) {
            assert thisValue instanceof NumberFormatObject;
            ExecutionContext calleeContext = calleeContext();
            Object value = args.length > 0 ? args[0] : UNDEFINED;
            double x = ToNumber(calleeContext, value);
            return FormatNumber(calleeContext, (NumberFormatObject) thisValue, x);
        }
    }
}
