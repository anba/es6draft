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

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Optional.Default;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.9 Date Objects</h2>
 * <ul>
 * <li>15.9.2 The Date Constructor Called as a Function
 * <li>15.9.3 The Date Constructor
 * <li>15.9.4 Properties of the Date Constructor
 * </ul>
 */
public class DateConstructor extends OrdinaryObject implements Scriptable, Callable, Constructor,
        Initialisable {
    public DateConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
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
        return "function Date() { /* native code */ }";
    }

    /**
     * 15.9.2.1 Date ( [ year [, month [, date [, hours [, minutes [, seconds [, ms ] ] ] ] ] ] ] )
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        long now = System.currentTimeMillis();
        DateObject obj = new DateObject(realm(), now);
        obj.setPrototype(realm().getIntrinsic(Intrinsics.DatePrototype));
        return DatePrototype.Properties.toString(realm(), obj);
    }

    /**
     * 15.9.3.1 new Date (year, month [, date [, hours [, minutes [, seconds [, ms ] ] ] ] ] )<br>
     * 15.9.3.2 new Date (value)<br>
     * 15.9.3.3 new Date ( )<br>
     */
    @Override
    public Object construct(Object... args) {
        Realm realm = realm();
        double dateValue = 0;
        if (args.length >= 2) {
            // 15.9.3.1
            double year = ToNumber(realm, args[0]);
            double month = ToNumber(realm, args[1]);
            double date = (args.length > 2 ? ToNumber(realm, args[2]) : 1);
            double hour = (args.length > 3 ? ToNumber(realm, args[3]) : 0);
            double min = (args.length > 4 ? ToNumber(realm, args[4]) : 0);
            double sec = (args.length > 5 ? ToNumber(realm, args[5]) : 0);
            double ms = (args.length > 6 ? ToNumber(realm, args[6]) : 0);
            if (!Double.isNaN(year) && 0 <= ToInteger(realm, year) && ToInteger(realm, year) <= 99) {
                year = 1900 + ToInteger(realm, year);
            }
            double finalDate = MakeDate(MakeDay(year, month, date), MakeTime(hour, min, sec, ms));
            dateValue = TimeClip(UTC(realm, finalDate));
        } else if (args.length == 1) {
            // 15.9.3.2
            Object v = ToPrimitive(realm, args[0]);
            double d;
            if (Type.isString(v)) {
                d = (double) Properties.parse(realm, null, v);
            } else {
                d = ToNumber(realm, v);
            }
            dateValue = TimeClip(d);
        } else {
            // 15.9.3.3
            dateValue = System.currentTimeMillis();
        }
        DateObject obj = new DateObject(realm, dateValue);
        obj.setPrototype(realm.getIntrinsic(Intrinsics.DatePrototype));
        return obj;
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
        public static Object parse(Realm realm, Object thisValue, Object string) {
            CharSequence s = ToString(realm, string);
            double d = parseISOString(realm, s, true);
            return d;
        }

        /**
         * 15.9.4.3 Date.UTC (year, month [, date [, hours [, minutes [, seconds [, ms ] ] ] ] ] )
         */
        @Function(name = "UTC", arity = 7)
        public static Object UTC(Realm realm, Object thisValue, Object year, Object month,
                @Optional(value = Default.Number, numberValue = 1) Object date, @Optional(
                        value = Default.Number, numberValue = 0) Object hours, @Optional(
                        value = Default.Number, numberValue = 0) Object minutes, @Optional(
                        value = Default.Number, numberValue = 0) Object seconds, @Optional(
                        value = Default.Number, numberValue = 0) Object ms) {
            double y = ToNumber(realm, year);
            double m = ToNumber(realm, month);
            double dt = ToNumber(realm, date);
            double h = ToNumber(realm, hours);
            double min = ToNumber(realm, minutes);
            double sec = ToNumber(realm, seconds);
            double milli = ToNumber(realm, ms);
            if (!Double.isNaN(y) && 0 <= ToInteger(realm, y) && ToInteger(realm, y) <= 99) {
                y = 1900 + ToInteger(realm, y);
            }
            double finalDate = MakeDate(MakeDay(y, m, dt), MakeTime(h, min, sec, milli));
            return TimeClip(finalDate);
        }

        /**
         * 15.9.4.4 Date.now ( )
         */
        @Function(name = "now", arity = 0)
        public static Object now(Realm realm, Object thisValue) {
            return (double) System.currentTimeMillis();
        }

        // FIXME: spec bug (15.9.4.5 not present)

        /**
         * 15.9.4.6 @@create ( )
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0)
        public static Object create(Realm realm, Object thisValue) {
            Scriptable obj = OrdinaryCreateFromConstructor(realm, thisValue,
                    Intrinsics.DatePrototype);
            // FIXME: spec bug (NativeBrand instead of BuiltinBrand)
            // obj.[[DateValue]] = ?; (implicit)
            return obj;
        }
    }
}
