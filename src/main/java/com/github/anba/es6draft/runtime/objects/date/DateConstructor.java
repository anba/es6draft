/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.date;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToPrimitive;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.date.DateAbstractOperations.*;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
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

    @Override
    public DateConstructor clone() {
        return new DateConstructor(getRealm());
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
        long now = System.currentTimeMillis();
        return DatePrototype.ToDateString(calleeContext.getRealm(), now, DateString.DateTime);
    }

    /**
     * 20.3.2.1 Date (year, month [, date [, hours [, minutes [, seconds [, ms ] ] ] ] ] )<br>
     * 20.3.2.2 Date (value)<br>
     * 20.3.2.3 Date ( )<br>
     */
    @Override
    public DateObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Realm realm = calleeContext.getRealm();
        /* steps 1-2 */
        int numberOfArgs = args.length;
        /* step 3 */
        final double dateValue;
        if (numberOfArgs >= 2) {
            // [20.3.2.1]
            double year = ToNumber(calleeContext, args[0]);
            double month = ToNumber(calleeContext, args[1]);
            double date = (args.length > 2 ? ToNumber(calleeContext, args[2]) : 1);
            double hour = (args.length > 3 ? ToNumber(calleeContext, args[3]) : 0);
            double min = (args.length > 4 ? ToNumber(calleeContext, args[4]) : 0);
            double sec = (args.length > 5 ? ToNumber(calleeContext, args[5]) : 0);
            double ms = (args.length > 6 ? ToNumber(calleeContext, args[6]) : 0);
            if (!Double.isNaN(year) && 0 <= ToInteger(year) && ToInteger(year) <= 99) {
                year = 1900 + ToInteger(year);
            }
            double finalDate = MakeDate(MakeDay(year, month, date), MakeTime(hour, min, sec, ms));
            dateValue = TimeClip(UTC(realm, finalDate));
        } else if (numberOfArgs == 1) {
            // [20.3.2.2]
            double tv;
            if (args[0] instanceof DateObject) {
                tv = thisTimeValue(calleeContext, args[0]);
            } else {
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
            dateValue = System.currentTimeMillis();
        }
        DateObject obj = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.DatePrototype, DateObjectAllocator.INSTANCE);
        obj.setDateValue(dateValue);
        return obj;
        /* step 4 (not applicable) */
    }

    private static final class DateObjectAllocator implements ObjectAllocator<DateObject> {
        static final ObjectAllocator<DateObject> INSTANCE = new DateObjectAllocator();

        @Override
        public DateObject newInstance(Realm realm) {
            return new DateObject(realm);
        }
    }

    /**
     * Abstract operation thisTimeValue(value)
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the date object
     * @return the date-time value
     */
    private static double thisTimeValue(ExecutionContext cx, Object object) {
        if (object instanceof DateObject) {
            return ((DateObject) object).getDateValue();
        }
        throw newTypeError(cx, Messages.Key.IncompatibleObject);
    }

    /**
     * 20.3.3 Properties of the Date Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 7;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "Date";

        /**
         * 20.3.3.3 Date.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
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
         * 20.3.3.4 Date.UTC (year, month [, date [, hours [, minutes [, seconds [, ms ] ] ] ] ] )
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
        public static Object UTC(ExecutionContext cx, Object thisValue, Object year, Object month,
                @Optional(value = Default.Number, numberValue = 1) Object date, @Optional(
                        value = Default.Number, numberValue = 0) Object hours, @Optional(
                        value = Default.Number, numberValue = 0) Object minutes, @Optional(
                        value = Default.Number, numberValue = 0) Object seconds, @Optional(
                        value = Default.Number, numberValue = 0) Object ms) {
            /* steps 1-2 */
            double y = ToNumber(cx, year);
            /* steps 3-4 */
            double m = ToNumber(cx, month);
            /* steps 5-6 */
            double dt = ToNumber(cx, date);
            /* steps 7-8 */
            double h = ToNumber(cx, hours);
            /* steps 9-10 */
            double min = ToNumber(cx, minutes);
            /* steps 11-12 */
            double sec = ToNumber(cx, seconds);
            /* steps 13-14 */
            double milli = ToNumber(cx, ms);
            /* step 15 */
            if (!Double.isNaN(y) && 0 <= ToInteger(y) && ToInteger(y) <= 99) {
                y = 1900 + ToInteger(y);
            }
            /* step 16 */
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
            return (double) System.currentTimeMillis();
        }
    }
}
