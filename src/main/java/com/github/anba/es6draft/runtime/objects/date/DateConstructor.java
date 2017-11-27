/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.date;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToPrimitive;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.date.DateAbstractOperations.*;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Permission;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Optional.Default;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.date.DatePrototype.DateString;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>20 Numbers and Dates</h1><br>
 * <h2>20.3 Date Objects</h2>
 * <ul>
 * <li>20.3.2 The Date Constructor
 * <li>20.3.3 Properties of the Date Constructor
 * </ul>
 */
public final class DateConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Date constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public DateConstructor(Realm realm) {
        super(realm, "Date", 7);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 20.3.2.1 Date (year, month [, date [, hours [, minutes [, seconds [, ms ] ] ] ] ] )<br>
     * 20.3.2.2 Date (value)<br>
     * 20.3.2.3 Date ( )<br>
     */
    @Override
    public String call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* steps 1-3 (not applicable) */
        /* step 4 */
        if (!calleeContext.getRealm().isGranted(Permission.CurrentTime)) {
            throw newTypeError(calleeContext, Messages.Key.NoPermission, "Date");
        }
        long now = System.currentTimeMillis();
        return DatePrototype.ToDateString(calleeContext.getRealm(), now, DateString.DateTime);
    }

    /**
     * 20.3.2.1 Date (year, month [, date [, hours [, minutes [, seconds [, ms ] ] ] ] ] )<br>
     * 20.3.2.2 Date (value)<br>
     * 20.3.2.3 Date ( )<br>
     */
    @Override
    public DateObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Realm realm = calleeContext.getRealm();
        /* steps 1-2 */
        int numberOfArgs = args.length;
        /* step 3 */
        final double dateValue;
        if (numberOfArgs >= 2) {
            // [20.3.2.1]
            /* step 3.a */
            double year = ToNumber(calleeContext, args[0]);
            /* step 3.b */
            double month = ToNumber(calleeContext, args[1]);
            /* step 3.c */
            double date = (args.length > 2 ? ToNumber(calleeContext, args[2]) : 1);
            /* step 3.d */
            double hour = (args.length > 3 ? ToNumber(calleeContext, args[3]) : 0);
            /* step 3.e */
            double min = (args.length > 4 ? ToNumber(calleeContext, args[4]) : 0);
            /* step 3.f */
            double sec = (args.length > 5 ? ToNumber(calleeContext, args[5]) : 0);
            /* step 3.g */
            double ms = (args.length > 6 ? ToNumber(calleeContext, args[6]) : 0);
            /* step 3.h */
            int intYear = (int) year; // ToInteger
            if (!Double.isNaN(year) && 0 <= intYear && intYear <= 99) {
                year = 1900 + intYear;
            }
            /* step 3.i */
            double finalDate = MakeDate(MakeDay(year, month, date), MakeTime(hour, min, sec, ms));
            /* step 3.k */
            dateValue = TimeClip(UTC(realm, finalDate));
        } else if (numberOfArgs == 1) {
            // [20.3.2.2]
            double tv;
            if (args[0] instanceof DateObject) {
                /* step 3.a */
                // TODO: spec improvement - inline thisTimeValue() call.
                tv = ((DateObject) args[0]).getDateValue();
            } else {
                /* step 3.b */
                Object v = ToPrimitive(calleeContext, args[0]);
                if (Type.isString(v)) {
                    tv = (double) Properties.parse(calleeContext, null, v);
                } else {
                    tv = ToNumber(calleeContext, v);
                }
            }
            dateValue = TimeClip(tv);
        } else {
            // [20.3.2.3]
            if (!calleeContext.getRealm().isGranted(Permission.CurrentTime)) {
                throw newTypeError(calleeContext, Messages.Key.NoPermission, "Date");
            }
            dateValue = System.currentTimeMillis();
        }
        DateObject obj = OrdinaryCreateFromConstructor(calleeContext, newTarget, Intrinsics.DatePrototype,
                DateObject::new);
        obj.setDateValue(dateValue);
        return obj;
        /* step 4 (not applicable) */
    }

    /**
     * 20.3.3 Properties of the Date Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 7;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "Date";

        /**
         * 20.3.3.3 Date.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.DatePrototype;

        /**
         * 20.3.3.2 Date.parse (string)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the date string
         * @return the parsed date value
         */
        @Function(name = "parse", arity = 1)
        public static Object parse(ExecutionContext cx, Object thisValue, Object string) {
            String s = ToFlatString(cx, string);
            double d = parseISOString(cx.getRealm(), s, true);
            if (Double.isNaN(d)) {
                d = parseDateString(cx.getRealm(), s);
            }
            return d;
        }

        /**
         * 20.3.3.4 Date.UTC (year, [ month [, date [, hours [, minutes [, seconds [, ms ] ] ] ] ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param year
         *            the year value
         * @param month
         *            the month value
         * @param date
         *            the date value
         * @param hours
         *            the hours value
         * @param minutes
         *            the minutes value
         * @param seconds
         *            the seconds value
         * @param ms
         *            the milli-seconds value
         * @return the new date object
         */
        @Function(name = "UTC", arity = 7)
        public static Object UTC(ExecutionContext cx, Object thisValue, Object year,
                @Optional(value = Default.Number, numberValue = 0) Object month,
                @Optional(value = Default.Number, numberValue = 1) Object date,
                @Optional(value = Default.Number, numberValue = 0) Object hours,
                @Optional(value = Default.Number, numberValue = 0) Object minutes,
                @Optional(value = Default.Number, numberValue = 0) Object seconds,
                @Optional(value = Default.Number, numberValue = 0) Object ms) {
            /* step 1 */
            double y = ToNumber(cx, year);
            /* step 2 */
            double m = ToNumber(cx, month);
            /* step 3 */
            double dt = ToNumber(cx, date);
            /* step 4 */
            double h = ToNumber(cx, hours);
            /* step 5 */
            double min = ToNumber(cx, minutes);
            /* step 6 */
            double sec = ToNumber(cx, seconds);
            /* step 7 */
            double milli = ToNumber(cx, ms);
            /* step 8 */
            int intYear = (int) y; // ToInteger
            if (!Double.isNaN(y) && 0 <= intYear && intYear <= 99) {
                y = 1900 + intYear;
            }
            /* step 9 */
            return TimeClip(MakeDate(MakeDay(y, m, dt), MakeTime(h, min, sec, milli)));
        }

        /**
         * 20.3.3.1 Date.now ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the current date in milli-seconds since the epoch
         */
        @Function(name = "now", arity = 0)
        public static Object now(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            if (!cx.getRealm().isGranted(Permission.CurrentTime)) {
                throw newTypeError(cx, Messages.Key.NoPermission, "Date.now");
            }
            return (double) System.currentTimeMillis();
        }
    }
}
