/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.DateConstructor;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>12 DateTimeFormat Objects</h1>
 * <ul>
 * <li>12.3 Properties of the Intl.DateTimeFormat Prototype Object
 * </ul>
 */
public class DateTimeFormatPrototype extends DateTimeFormatObject implements Initialisable,
        ScriptObject {
    public DateTimeFormatPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    /**
     * 12.3 Properties of the Intl.DateTimeFormat Prototype Object
     */
    public enum Properties {
        ;

        private static DateTimeFormatObject dateTimeFormat(Realm realm, Object object) {
            if (object instanceof DateTimeFormatObject) {
                // TODO: test for initialised state
                return (DateTimeFormatObject) object;
            }
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 12.3.1 Intl.DateTimeFormat.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Intl_DateTimeFormat;

        /**
         * 12.3.2 Intl.DateTimeFormat.prototype.format
         */
        @Accessor(name = "format", type = Accessor.Type.Getter)
        public static Object format(Realm realm, Object thisValue) {
            DateTimeFormatObject dateTimeFormat = dateTimeFormat(realm, thisValue);
            if (!dateTimeFormat.hasBoundFormat()) {
                FormatFunction f = new FormatFunction(realm);
                Callable bf = (Callable) FunctionPrototype.Properties.bind(realm, f, thisValue);
                dateTimeFormat.setBoundFormat(bf);
            }
            return dateTimeFormat.getBoundFormat();
        }

        /**
         * 12.3.3 Intl.DateTimeFormat.prototype.resolvedOptions ()
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(Realm realm, Object thisValue) {
            dateTimeFormat(realm, thisValue);
            return UNDEFINED;
        }
    }

    /**
     * Abstract Operation: FormatDateTime
     */
    public static String FormatDateTime(Realm realm, DateTimeFormatObject dateTimeFormat, double x) {
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            throwRangeError(realm, Messages.Key.InvalidPrecision);
        }
        return "";
    }

    private static class FormatFunction extends OrdinaryObject implements BuiltinFunction {
        private static final String NAME = "format";
        private static final int ARITY = 0;

        public FormatFunction(Realm realm) {
            super(realm);
            setPrototype(realm.getIntrinsic(Intrinsics.FunctionPrototype));
            defineOwnProperty("name", new PropertyDescriptor(NAME, false, false, false));
            defineOwnProperty("length", new PropertyDescriptor(ARITY, false, false, false));
            AddRestrictedFunctionProperties(realm, this);
        }

        /**
         * [[BuiltinBrand]]
         */
        @Override
        public BuiltinBrand getBuiltinBrand() {
            return BuiltinBrand.BuiltinFunction;
        }

        @Override
        public String toSource() {
            return String.format("function %s() { /* native code */ }", NAME);
        }

        /**
         * [[Call]]
         */
        @Override
        public Object call(Object thisValue, Object... args) {
            assert thisValue instanceof DateTimeFormatObject;
            Realm realm = realm();
            Object date = args.length > 0 ? args[0] : UNDEFINED;
            if (Type.isUndefined(date)) {
                date = DateConstructor.Properties.now(realm, realm.getIntrinsic(Intrinsics.Date));
            }
            double x = ToNumber(realm, date);
            return FormatDateTime(realm, (DateTimeFormatObject) thisValue, x);
        }
    }
}
