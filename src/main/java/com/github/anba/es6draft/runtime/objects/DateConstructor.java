/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.DateAbstractOperations.*;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Optional.Default;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.9 Date Objects</h2>
 * <ul>
 * <li>15.9.2 The Date Constructor Called as a Function
 * <li>15.9.3 The Date Constructor
 * <li>15.9.4 Properties of the Date Constructor
 * </ul>
 */
public class DateConstructor extends BuiltinFunction implements Constructor, Initialisable {
    public DateConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 15.9.2.1 Date (year, month [, date [, hours [, minutes [, seconds [, ms ] ] ] ] ] )<br>
     * 15.9.2.2 Date (value)<br>
     * 15.9.2.3 Date ( )<br>
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        Realm realm = callerContext.getRealm();
        int numberOfArgs = args.length;
        if (numberOfArgs >= 2) {
            // [15.9.2.1]
            if (isUninitialisedDateObject(thisValue)) {
                DateObject obj = (DateObject) thisValue;
                double year = ToNumber(callerContext, args[0]);
                double month = ToNumber(callerContext, args[1]);
                double date = (args.length > 2 ? ToNumber(callerContext, args[2]) : 1);
                double hour = (args.length > 3 ? ToNumber(callerContext, args[3]) : 0);
                double min = (args.length > 4 ? ToNumber(callerContext, args[4]) : 0);
                double sec = (args.length > 5 ? ToNumber(callerContext, args[5]) : 0);
                double ms = (args.length > 6 ? ToNumber(callerContext, args[6]) : 0);
                if (!Double.isNaN(year) && 0 <= ToInteger(callerContext, year)
                        && ToInteger(callerContext, year) <= 99) {
                    year = 1900 + ToInteger(callerContext, year);
                }
                double finalDate = MakeDate(MakeDay(year, month, date),
                        MakeTime(hour, min, sec, ms));
                obj.setDateValue(TimeClip(UTC(realm, finalDate)));
                return obj;
            }
        } else if (numberOfArgs == 1) {
            // [15.9.2.2]
            if (isUninitialisedDateObject(thisValue)) {
                DateObject obj = (DateObject) thisValue;
                Object v = ToPrimitive(callerContext, args[0]);
                double d;
                if (Type.isString(v)) {
                    d = (double) Properties.parse(callerContext, null, v);
                } else {
                    d = ToNumber(callerContext, v);
                }
                obj.setDateValue(TimeClip(d));
                return obj;
            }
        } else {
            // [15.9.2.3]
            if (isUninitialisedDateObject(thisValue)) {
                DateObject obj = (DateObject) thisValue;
                obj.setDateValue(System.currentTimeMillis());
                return obj;
            }
        }
        long now = System.currentTimeMillis();
        DateObject obj = new DateObject(realm);
        obj.setPrototype(callerContext, realm.getIntrinsic(Intrinsics.DatePrototype));
        obj.setDateValue(now);
        return DatePrototype.Properties.toString(callerContext, obj);
    }

    private static boolean isUninitialisedDateObject(Object thisValue) {
        if (thisValue instanceof DateObject) {
            return !((DateObject) thisValue).isInitialised();
        }
        return false;
    }

    /**
     * 15.9.3.1 new Date ( ...args )
     */
    @Override
    public Object construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 15.9.4 Properties of the Date Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 7;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "Date";

        /**
         * 15.9.4.1 Date.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.DatePrototype;

        /**
         * 15.9.4.2 Date.parse (string)
         */
        @Function(name = "parse", arity = 1)
        public static Object parse(ExecutionContext cx, Object thisValue, Object string) {
            CharSequence s = ToString(cx, string);
            double d = parseISOString(cx.getRealm(), s, true);
            if (Double.isNaN(d)) {
                d = parseDateString(cx.getRealm(), s);
            }
            return d;
        }

        /**
         * 15.9.4.3 Date.UTC (year, month [, date [, hours [, minutes [, seconds [, ms ] ] ] ] ] )
         */
        @Function(name = "UTC", arity = 7)
        public static Object UTC(ExecutionContext cx, Object thisValue, Object year, Object month,
                @Optional(value = Default.Number, numberValue = 1) Object date, @Optional(
                        value = Default.Number, numberValue = 0) Object hours, @Optional(
                        value = Default.Number, numberValue = 0) Object minutes, @Optional(
                        value = Default.Number, numberValue = 0) Object seconds, @Optional(
                        value = Default.Number, numberValue = 0) Object ms) {
            double y = ToNumber(cx, year);
            double m = ToNumber(cx, month);
            double dt = ToNumber(cx, date);
            double h = ToNumber(cx, hours);
            double min = ToNumber(cx, minutes);
            double sec = ToNumber(cx, seconds);
            double milli = ToNumber(cx, ms);
            if (!Double.isNaN(y) && 0 <= ToInteger(cx, y) && ToInteger(cx, y) <= 99) {
                y = 1900 + ToInteger(cx, y);
            }
            double finalDate = MakeDate(MakeDay(y, m, dt), MakeTime(h, min, sec, milli));
            return TimeClip(finalDate);
        }

        /**
         * 15.9.4.4 Date.now ( )
         */
        @Function(name = "now", arity = 0)
        public static Object now(ExecutionContext cx, Object thisValue) {
            return (double) System.currentTimeMillis();
        }

        /**
         * 15.9.4.5 Date[ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.DatePrototype,
                    DateObjectAllocator.INSTANCE);
        }
    }

    private static class DateObjectAllocator implements ObjectAllocator<DateObject> {
        static final ObjectAllocator<DateObject> INSTANCE = new DateObjectAllocator();

        @Override
        public DateObject newInstance(Realm realm) {
            return new DateObject(realm);
        }
    }
}
