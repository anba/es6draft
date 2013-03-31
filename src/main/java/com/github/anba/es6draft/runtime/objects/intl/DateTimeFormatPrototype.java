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

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.DateConstructor;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>12 DateTimeFormat Objects</h1>
 * <ul>
 * <li>12.3 Properties of the Intl.DateTimeFormat Prototype Object
 * </ul>
 */
public class DateTimeFormatPrototype extends DateTimeFormatObject implements Initialisable {
    public DateTimeFormatPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 12.3 Properties of the Intl.DateTimeFormat Prototype Object
     */
    public enum Properties {
        ;

        private static DateTimeFormatObject dateTimeFormat(ExecutionContext cx, Object object) {
            if (object instanceof DateTimeFormatObject) {
                // TODO: test for initialised state
                return (DateTimeFormatObject) object;
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
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
        public static Object format(ExecutionContext cx, Object thisValue) {
            DateTimeFormatObject dateTimeFormat = dateTimeFormat(cx, thisValue);
            if (!dateTimeFormat.hasBoundFormat()) {
                FormatFunction f = new FormatFunction(cx.getRealm());
                Callable bf = (Callable) FunctionPrototype.Properties.bind(cx, f, thisValue);
                dateTimeFormat.setBoundFormat(bf);
            }
            return dateTimeFormat.getBoundFormat();
        }

        /**
         * 12.3.3 Intl.DateTimeFormat.prototype.resolvedOptions ()
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(ExecutionContext cx, Object thisValue) {
            dateTimeFormat(cx, thisValue);
            return UNDEFINED;
        }
    }

    /**
     * Abstract Operation: FormatDateTime
     */
    public static String FormatDateTime(ExecutionContext cx, DateTimeFormatObject dateTimeFormat,
            double x) {
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            throwRangeError(cx, Messages.Key.InvalidPrecision);
        }
        return "";
    }

    private static class FormatFunction extends BuiltinFunction {
        public FormatFunction(Realm realm) {
            super(realm);
            ExecutionContext cx = realm.defaultContext();
            setPrototype(cx, realm.getIntrinsic(Intrinsics.FunctionPrototype));
            defineOwnProperty(cx, "name", new PropertyDescriptor("format", false, false, false));
            defineOwnProperty(cx, "length", new PropertyDescriptor(0, false, false, false));
            AddRestrictedFunctionProperties(cx, this);
        }

        /**
         * [[Call]]
         */
        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            assert thisValue instanceof DateTimeFormatObject;
            Realm realm = callerContext.getRealm();
            Object date = args.length > 0 ? args[0] : UNDEFINED;
            if (Type.isUndefined(date)) {
                date = DateConstructor.Properties.now(callerContext,
                        realm.getIntrinsic(Intrinsics.Date));
            }
            double x = ToNumber(callerContext, date);
            return FormatDateTime(callerContext, (DateTimeFormatObject) thisValue, x);
        }
    }
}
