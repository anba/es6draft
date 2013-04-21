/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.DateAbstractOperations.*;
import static com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatConstructor.ToDateTimeOptions;
import static com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatPrototype.FormatDateTime;
import static com.github.anba.es6draft.runtime.types.Null.NULL;

import java.util.Locale;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatConstructor;
import com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.9 Date Objects</h2>
 * <ul>
 * <li>15.9.5 Properties of the Date Prototype Object
 * </ul>
 */
public class DatePrototype extends OrdinaryObject implements Initialisable {
    public DatePrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);

        // B.2.3.3 Date.prototype.toGMTString ( )
        defineOwnProperty(cx, "toGMTString", new PropertyDescriptor(Get(cx, this, "toUTCString"),
                true, false, true));
    }

    private static final String ISO_FORMAT = "%04d-%02d-%02dT%02d:%02d:%02d.%03dZ";
    private static final String ISO_EXTENDED_FORMAT = "%06d-%02d-%02dT%02d:%02d:%02d.%03dZ";

    private static final String UTC_FORMAT = "%04d-%02d-%02d %02d:%02d:%02d.%03dZ";
    private static final String UTC_EXTENDED_FORMAT = "%06d-%02d-%02d %02d:%02d:%02d.%03dZ";

    private static String toISOString(double t) {
        assert !Double.isNaN(t);
        int year = (int) YearFromTime(t);
        int month = (int) MonthFromTime(t) + 1;
        int date = (int) DateFromTime(t);
        int hour = (int) HourFromTime(t);
        int min = (int) MinFromTime(t);
        int sec = (int) SecFromTime(t);
        int milli = (int) msFromTime(t);

        String format;
        if (year < 0 || year > 9999) {
            format = ISO_EXTENDED_FORMAT;
        } else {
            format = ISO_FORMAT;
        }
        return String.format(format, year, month, date, hour, min, sec, milli);
    }

    private static String toUTCString(double t) {
        assert !Double.isNaN(t);
        int year = (int) YearFromTime(t);
        int month = (int) MonthFromTime(t) + 1;
        int date = (int) DateFromTime(t);
        int hour = (int) HourFromTime(t);
        int min = (int) MinFromTime(t);
        int sec = (int) SecFromTime(t);
        int milli = (int) msFromTime(t);

        String format;
        if (year < 0 || year > 9999) {
            format = UTC_EXTENDED_FORMAT;
        } else {
            format = UTC_FORMAT;
        }
        return String.format(format, year, month, date, hour, min, sec, milli);
    }

    private static boolean isFinite(double d) {
        return !(Double.isNaN(d) || Double.isInfinite(d));
    }

    /**
     * 15.9.5 Properties of the Date Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract operation thisTimeValue(value)
         */
        private static double thisTimeValue(ExecutionContext cx, Object object) {
            if (object instanceof DateObject) {
                DateObject obj = (DateObject) object;
                if (obj.isInitialised()) {
                    return obj.getDateValue();
                }
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Function(name = "toSource", arity = 0)
        public static Object toSource(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            return new StringBuilder().append("(new Date(").append(ToString(t)).append("))")
                    .toString();
        }

        /**
         * 15.9.5.1 Date.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Date;

        /**
         * 15.9.5.2 Date.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return "Invalid Date";
            }
            return String.format(Locale.US, "%1$ta %1$tb %1$td %1$tY %1$tT GMT%1$tz (%1$tZ)",
                    (long) t);
        }

        /**
         * 15.9.5.3 Date.prototype.toDateString ( )
         */
        @Function(name = "toDateString", arity = 0)
        public static Object toDateString(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return "Invalid Date";
            }
            return String.format(Locale.US, "%1$ta %1$tb %1$td %1$tY", (long) t);
        }

        /**
         * 15.9.5.4 Date.prototype.toTimeString ( )
         */
        @Function(name = "toTimeString", arity = 0)
        public static Object toTimeString(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return "Invalid Date";
            }
            return String.format(Locale.US, "%1$tT GMT%1$tz (%1$tZ)", (long) t);
        }

        /**
         * 15.9.5.5 Date.prototype.toLocaleString ( )
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue, Object locales,
                Object options) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return "Invalid Date";
            }
            // ES5/6
            // return String.format(cx.getRealm().getLocale(), "%1$tc", (long) t);

            // ECMA-402
            options = ToDateTimeOptions(cx, options, "any", "all");
            DateTimeFormatConstructor constructor = (DateTimeFormatConstructor) cx
                    .getIntrinsic(Intrinsics.Intl_DateTimeFormat);
            DateTimeFormatObject dateTimeFormat = (DateTimeFormatObject) constructor.construct(cx,
                    locales, options);
            return FormatDateTime(cx, dateTimeFormat, t);
        }

        /**
         * 15.9.5.6 Date.prototype.toLocaleDateString ( )
         */
        @Function(name = "toLocaleDateString", arity = 0)
        public static Object toLocaleDateString(ExecutionContext cx, Object thisValue,
                Object locales, Object options) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return "Invalid Date";
            }
            // ES5/6
            // return String.format(cx.getRealm().getLocale(), "%1$ta %1$tb %1$td %1$tY", (long) t);

            // ECMA-402
            options = ToDateTimeOptions(cx, options, "date", "date");
            DateTimeFormatConstructor constructor = (DateTimeFormatConstructor) cx
                    .getIntrinsic(Intrinsics.Intl_DateTimeFormat);
            DateTimeFormatObject dateTimeFormat = (DateTimeFormatObject) constructor.construct(cx,
                    locales, options);
            return FormatDateTime(cx, dateTimeFormat, t);
        }

        /**
         * 15.9.5.7 Date.prototype.toLocaleTimeString ( )
         */
        @Function(name = "toLocaleTimeString", arity = 0)
        public static Object toLocaleTimeString(ExecutionContext cx, Object thisValue,
                Object locales, Object options) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return "Invalid Date";
            }
            // ES5/6
            // return String.format(cx.getRealm().getLocale(), "%1$tT GMT%1$tz (%1$tZ)", (long) t);

            // ECMA-402
            options = ToDateTimeOptions(cx, options, "time", "time");
            DateTimeFormatConstructor constructor = (DateTimeFormatConstructor) cx
                    .getIntrinsic(Intrinsics.Intl_DateTimeFormat);
            DateTimeFormatObject dateTimeFormat = (DateTimeFormatObject) constructor.construct(cx,
                    locales, options);
            return FormatDateTime(cx, dateTimeFormat, t);
        }

        /**
         * 15.9.5.8 Date.prototype.valueOf ( )
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            return thisTimeValue(cx, thisValue);
        }

        /**
         * 15.9.5.9 Date.prototype.getTime ( )
         */
        @Function(name = "getTime", arity = 0)
        public static Object getTime(ExecutionContext cx, Object thisValue) {
            return thisTimeValue(cx, thisValue);
        }

        /**
         * 15.9.5.10 Date.prototype.getFullYear ( )
         */
        @Function(name = "getFullYear", arity = 0)
        public static Object getFullYear(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return YearFromTime(LocalTime(realm, t));
        }

        /**
         * 15.9.5.11 Date.prototype.getUTCFullYear ( )
         */
        @Function(name = "getUTCFullYear", arity = 0)
        public static Object getUTCFullYear(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return YearFromTime(t);
        }

        /**
         * 15.9.5.12 Date.prototype.getMonth ( )
         */
        @Function(name = "getMonth", arity = 0)
        public static Object getMonth(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return MonthFromTime(LocalTime(realm, t));
        }

        /**
         * 15.9.5.13 Date.prototype.getUTCMonth ( )
         */
        @Function(name = "getUTCMonth", arity = 0)
        public static Object getUTCMonth(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return MonthFromTime(t);
        }

        /**
         * 15.9.5.14 Date.prototype.getDate ( )
         */
        @Function(name = "getDate", arity = 0)
        public static Object getDate(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return DateFromTime(LocalTime(realm, t));
        }

        /**
         * 15.9.5.15 Date.prototype.getUTCDate ( )
         */
        @Function(name = "getUTCDate", arity = 0)
        public static Object getUTCDate(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return DateFromTime(t);
        }

        /**
         * 15.9.5.16 Date.prototype.getDay ( )
         */
        @Function(name = "getDay", arity = 0)
        public static Object getDay(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return WeekDay(LocalTime(realm, t));
        }

        /**
         * 15.9.5.17 Date.prototype.getUTCDay ( )
         */
        @Function(name = "getUTCDay", arity = 0)
        public static Object getUTCDay(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return WeekDay(t);
        }

        /**
         * 15.9.5.18 Date.prototype.getHours ( )
         */
        @Function(name = "getHours", arity = 0)
        public static Object getHours(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return HourFromTime(LocalTime(realm, t));
        }

        /**
         * 15.9.5.19 Date.prototype.getUTCHours ( )
         */
        @Function(name = "getUTCHours", arity = 0)
        public static Object getUTCHours(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return HourFromTime(t);
        }

        /**
         * 15.9.5.20 Date.prototype.getMinutes ( )
         */
        @Function(name = "getMinutes", arity = 0)
        public static Object getMinutes(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return MinFromTime(LocalTime(realm, t));
        }

        /**
         * 15.9.5.21 Date.prototype.getUTCMinutes ( )
         */
        @Function(name = "getUTCMinutes", arity = 0)
        public static Object getUTCMinutes(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return MinFromTime(t);
        }

        /**
         * 15.9.5.22 Date.prototype.getSeconds ( )
         */
        @Function(name = "getSeconds", arity = 0)
        public static Object getSeconds(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return SecFromTime(LocalTime(realm, t));
        }

        /**
         * 15.9.5.23 Date.prototype.getUTCSeconds ( )
         */
        @Function(name = "getUTCSeconds", arity = 0)
        public static Object getUTCSeconds(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return SecFromTime(t);
        }

        /**
         * 15.9.5.24 Date.prototype.getMilliseconds ( )
         */
        @Function(name = "getMilliseconds", arity = 0)
        public static Object getMilliseconds(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return msFromTime(LocalTime(realm, t));
        }

        /**
         * 15.9.5.25 Date.prototype.getUTCMilliseconds ( )
         */
        @Function(name = "getUTCMilliseconds", arity = 0)
        public static Object getUTCMilliseconds(ExecutionContext cx, Object thisValue) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return msFromTime(t);
        }

        /**
         * 15.9.5.26 Date.prototype.getTimezoneOffset ( )
         */
        @Function(name = "getTimezoneOffset", arity = 0)
        public static Object getTimezoneOffset(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return (t - LocalTime(realm, t)) / msPerMinute;
        }

        /**
         * 15.9.5.27 Date.prototype.setTime (time)
         */
        @Function(name = "setTime", arity = 1)
        public static Object setTime(ExecutionContext cx, Object thisValue, Object time) {
            // just to trigger type and initialisation test
            thisTimeValue(cx, thisValue);
            double v = TimeClip(ToNumber(cx, time));
            ((DateObject) thisValue).setDateValue(v);
            return v;
        }

        /**
         * 15.9.5.28 Date.prototype.setMilliseconds (ms)
         */
        @Function(name = "setMilliseconds", arity = 1)
        public static Object setMilliseconds(ExecutionContext cx, Object thisValue, Object ms) {
            Realm realm = cx.getRealm();
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            double time = MakeTime(HourFromTime(t), MinFromTime(t), SecFromTime(t),
                    ToNumber(cx, ms));
            double u = TimeClip(UTC(realm, MakeDate(Day(t), time)));
            ((DateObject) thisValue).setDateValue(u);
            return u;
        }

        /**
         * 15.9.5.29 Date.prototype.setUTCMilliseconds (ms)
         */
        @Function(name = "setUTCMilliseconds", arity = 1)
        public static Object setUTCMilliseconds(ExecutionContext cx, Object thisValue, Object ms) {
            double t = thisTimeValue(cx, thisValue);
            double time = MakeTime(HourFromTime(t), MinFromTime(t), SecFromTime(t),
                    ToNumber(cx, ms));
            double v = TimeClip(MakeDate(Day(t), time));
            ((DateObject) thisValue).setDateValue(v);
            return v;
        }

        /**
         * 15.9.5.30 Date.prototype.setSeconds (sec [, ms ] )
         */
        @Function(name = "setSeconds", arity = 2)
        public static Object setSeconds(ExecutionContext cx, Object thisValue, Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            Realm realm = cx.getRealm();
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            double s = ToNumber(cx, sec);
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            double date = MakeDate(Day(t), MakeTime(HourFromTime(t), MinFromTime(t), s, milli));
            double u = TimeClip(UTC(realm, date));
            ((DateObject) thisValue).setDateValue(u);
            return u;
        }

        /**
         * 15.9.5.31 Date.prototype.setUTCSeconds (sec [, ms ] )
         */
        @Function(name = "setUTCSeconds", arity = 2)
        public static Object setUTCSeconds(ExecutionContext cx, Object thisValue, Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            double t = thisTimeValue(cx, thisValue);
            double s = ToNumber(cx, sec);
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            double date = MakeDate(Day(t), MakeTime(HourFromTime(t), MinFromTime(t), s, milli));
            double v = TimeClip(date);
            ((DateObject) thisValue).setDateValue(v);
            return v;
        }

        /**
         * 15.9.5.32 Date.prototype.setMinutes (min [, sec [, ms ] ] )
         */
        @Function(name = "setMinutes", arity = 3)
        public static Object setMinutes(ExecutionContext cx, Object thisValue, Object min,
                @Optional(Optional.Default.NONE) Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            Realm realm = cx.getRealm();
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            double m = ToNumber(cx, min);
            double s = (sec == null ? SecFromTime(t) : ToNumber(cx, sec));
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            double date = MakeDate(Day(t), MakeTime(HourFromTime(t), m, s, milli));
            double u = TimeClip(UTC(realm, date));
            ((DateObject) thisValue).setDateValue(u);
            return u;
        }

        /**
         * 15.9.5.33 Date.prototype.setUTCMinutes (min [, sec [, ms ] ] )
         */
        @Function(name = "setUTCMinutes", arity = 3)
        public static Object setUTCMinutes(ExecutionContext cx, Object thisValue, Object min,
                @Optional(Optional.Default.NONE) Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            double t = thisTimeValue(cx, thisValue);
            double m = ToNumber(cx, min);
            double s = (sec == null ? SecFromTime(t) : ToNumber(cx, sec));
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            double date = MakeDate(Day(t), MakeTime(HourFromTime(t), m, s, milli));
            double v = TimeClip(date);
            ((DateObject) thisValue).setDateValue(v);
            return v;
        }

        /**
         * 15.9.5.34 Date.prototype.setHours (hour [, min [, sec [, ms ] ] ] )
         */
        @Function(name = "setHours", arity = 4)
        public static Object setHours(ExecutionContext cx, Object thisValue, Object hour,
                @Optional(Optional.Default.NONE) Object min,
                @Optional(Optional.Default.NONE) Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            Realm realm = cx.getRealm();
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            double h = ToNumber(cx, hour);
            double m = (min == null ? MinFromTime(t) : ToNumber(cx, min));
            double s = (sec == null ? SecFromTime(t) : ToNumber(cx, sec));
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            double date = MakeDate(Day(t), MakeTime(h, m, s, milli));
            double u = TimeClip(UTC(realm, date));
            ((DateObject) thisValue).setDateValue(u);
            return u;
        }

        /**
         * 15.9.5.35 Date.prototype.setUTCHours (hour [, min [, sec [, ms ] ] ] )
         */
        @Function(name = "setUTCHours", arity = 4)
        public static Object setUTCHours(ExecutionContext cx, Object thisValue, Object hour,
                @Optional(Optional.Default.NONE) Object min,
                @Optional(Optional.Default.NONE) Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            double t = thisTimeValue(cx, thisValue);
            double h = ToNumber(cx, hour);
            double m = (min == null ? MinFromTime(t) : ToNumber(cx, min));
            double s = (sec == null ? SecFromTime(t) : ToNumber(cx, sec));
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            double newDate = MakeDate(Day(t), MakeTime(h, m, s, milli));
            double v = TimeClip(newDate);
            ((DateObject) thisValue).setDateValue(v);
            return v;
        }

        /**
         * 15.9.5.36 Date.prototype.setDate (date)
         */
        @Function(name = "setDate", arity = 1)
        public static Object setDate(ExecutionContext cx, Object thisValue, Object date) {
            Realm realm = cx.getRealm();
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            double dt = ToNumber(cx, date);
            double newDate = MakeDate(MakeDay(YearFromTime(t), MonthFromTime(t), dt),
                    TimeWithinDay(t));
            double u = TimeClip(UTC(realm, newDate));
            ((DateObject) thisValue).setDateValue(u);
            return u;
        }

        /**
         * 15.9.5.37 Date.prototype.setUTCDate (date)
         */
        @Function(name = "setUTCDate", arity = 1)
        public static Object setUTCDate(ExecutionContext cx, Object thisValue, Object date) {
            double t = thisTimeValue(cx, thisValue);
            double dt = ToNumber(cx, date);
            double newDate = MakeDate(MakeDay(YearFromTime(t), MonthFromTime(t), dt),
                    TimeWithinDay(t));
            double v = TimeClip(newDate);
            ((DateObject) thisValue).setDateValue(v);
            return v;
        }

        /**
         * 15.9.5.38 Date.prototype.setMonth (month [, date ] )
         */
        @Function(name = "setMonth", arity = 2)
        public static Object setMonth(ExecutionContext cx, Object thisValue, Object month,
                @Optional(Optional.Default.NONE) Object date) {
            Realm realm = cx.getRealm();
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            double m = ToNumber(cx, month);
            double dt = (date == null ? DateFromTime(t) : ToNumber(cx, date));
            double newDate = MakeDate(MakeDay(YearFromTime(t), m, dt), TimeWithinDay(t));
            double u = TimeClip(UTC(realm, newDate));
            ((DateObject) thisValue).setDateValue(u);
            return u;
        }

        /**
         * 15.9.5.39 Date.prototype.setUTCMonth (month [, date ] )
         */
        @Function(name = "setUTCMonth", arity = 2)
        public static Object setUTCMonth(ExecutionContext cx, Object thisValue, Object month,
                @Optional(Optional.Default.NONE) Object date) {
            double t = thisTimeValue(cx, thisValue);
            double m = ToNumber(cx, month);
            double dt = (date == null ? DateFromTime(t) : ToNumber(cx, date));
            double newDate = MakeDate(MakeDay(YearFromTime(t), m, dt), TimeWithinDay(t));
            double v = TimeClip(newDate);
            ((DateObject) thisValue).setDateValue(v);
            return v;
        }

        /**
         * 15.9.5.40 Date.prototype.setFullYear (year [, month [, date ] ] )
         */
        @Function(name = "setFullYear", arity = 3)
        public static Object setFullYear(ExecutionContext cx, Object thisValue, Object year,
                @Optional(Optional.Default.NONE) Object month,
                @Optional(Optional.Default.NONE) Object date) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            t = Double.isNaN(t) ? +0 : LocalTime(realm, t);
            double y = ToNumber(cx, year);
            double m = (month == null ? MonthFromTime(t) : ToNumber(cx, month));
            double dt = (date == null ? DateFromTime(t) : ToNumber(cx, date));
            double newDate = MakeDate(MakeDay(y, m, dt), TimeWithinDay(t));
            double u = TimeClip(UTC(realm, newDate));
            ((DateObject) thisValue).setDateValue(u);
            return u;
        }

        /**
         * 15.9.5.41 Date.prototype.setUTCFullYear (year [, month [, date ] ] )
         */
        @Function(name = "setUTCFullYear", arity = 3)
        public static Object setUTCFullYear(ExecutionContext cx, Object thisValue, Object year,
                @Optional(Optional.Default.NONE) Object month,
                @Optional(Optional.Default.NONE) Object date) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                t = +0;
            }
            double y = ToNumber(cx, year);
            double m = (month == null ? MonthFromTime(t) : ToNumber(cx, month));
            double dt = (date == null ? DateFromTime(t) : ToNumber(cx, date));
            double newDate = MakeDate(MakeDay(y, m, dt), TimeWithinDay(t));
            double v = TimeClip(newDate);
            ((DateObject) thisValue).setDateValue(v);
            return v;
        }

        /**
         * 15.9.5.42 Date.prototype.toUTCString ( )
         */
        @Function(name = "toUTCString", arity = 0)
        public static Object toUTCString(ExecutionContext cx, Object thisValue) {
            double dateValue = thisTimeValue(cx, thisValue);
            if (Double.isNaN(dateValue)) {
                return "Invalid Date";
            }
            return DatePrototype.toUTCString(dateValue);
        }

        /**
         * 15.9.5.43 Date.prototype.toISOString ( )
         */
        @Function(name = "toISOString", arity = 0)
        public static Object toISOString(ExecutionContext cx, Object thisValue) {
            double dateValue = thisTimeValue(cx, thisValue);
            if (!isFinite(dateValue)) {
                throw throwRangeError(cx, Messages.Key.InvalidDateValue);
            }
            return DatePrototype.toISOString(dateValue);
        }

        /**
         * 15.9.5.44 Date.prototype.toJSON ( key )
         */
        @Function(name = "toJSON", arity = 1)
        public static Object toJSON(ExecutionContext cx, Object thisValue, Object key) {
            ScriptObject o = ToObject(cx, thisValue);
            Object tv = AbstractOperations.ToPrimitive(cx, o, Type.Number);
            if (Type.isNumber(tv) && !isFinite(Type.numberValue(tv))) {
                return NULL;
            }
            Object toISO = Get(cx, o, "toISOString");
            if (!IsCallable(toISO)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            return ((Callable) toISO).call(cx, o);
        }

        /**
         * 15.9.5.45 Date.prototype.@@ToPrimitive ( hint )
         */
        @Function(name = "@@ToPrimitive", arity = 1, symbol = BuiltinSymbol.ToPrimitive)
        public static Object ToPrimitive(ExecutionContext cx, Object thisValue, Object hint) {
            // just to trigger type and initialisation test
            thisTimeValue(cx, thisValue);
            // FIXME: spec bug (missing argument type check) (bug 1403)
            Type tryFirst;
            if (!Type.isString(hint)) {
                throw throwTypeError(cx, Messages.Key.InvalidToPrimitiveHint, "?");
            }
            String _hint = Type.stringValue(hint).toString();
            if ("string".equals(_hint) || "default".equals(_hint)) {
                tryFirst = Type.String;
            } else if ("number".equals(_hint)) {
                tryFirst = Type.Number;
            } else {
                throw throwTypeError(cx, Messages.Key.InvalidToPrimitiveHint, _hint);
            }
            return OrdinaryToPrimitive(cx, Type.objectValue(thisValue), tryFirst);
        }

        /**
         * B.2.3.1 Date.prototype.getYear ( )
         */
        @Function(name = "getYear", arity = 0)
        public static Object getYear(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            return YearFromTime(LocalTime(realm, t)) - 1900;
        }

        /**
         * B.2.3.2 Date.prototype.setYear (year)
         */
        @Function(name = "setYear", arity = 1)
        public static Object setYear(ExecutionContext cx, Object thisValue, Object year) {
            Realm realm = cx.getRealm();
            double t = thisTimeValue(cx, thisValue);
            t = Double.isNaN(t) ? +0 : LocalTime(realm, t);
            double y = ToNumber(cx, year);
            if (Double.isNaN(y)) {
                ((DateObject) thisValue).setDateValue(Double.NaN);
                return Double.NaN;
            }
            double intYear = ToInteger(y);
            double yyyy = (0 <= intYear && intYear <= 99 ? intYear + 1900 : y);
            double d = MakeDay(yyyy, MonthFromTime(t), DateFromTime(t));
            double date = UTC(realm, MakeDate(d, TimeWithinDay(t)));
            ((DateObject) thisValue).setDateValue(TimeClip(date));
            return ((DateObject) thisValue).getDateValue();
        }
    }
}
