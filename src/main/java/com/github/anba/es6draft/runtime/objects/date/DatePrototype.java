/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.date;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.date.DateAbstractOperations.*;
import static com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatConstructor.ToDateTimeOptions;
import static com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatPrototype.FormatDateTime;
import static com.github.anba.es6draft.runtime.types.Null.NULL;

import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.TimeZone;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.AbstractOperations.ToPrimitiveHint;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatConstructor;
import com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>20 Numbers and Dates</h1><br>
 * <h2>20.3 Date Objects</h2>
 * <ul>
 * <li>20.3.4 Properties of the Date Prototype Object
 * </ul>
 */
public final class DatePrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Date prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public DatePrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        createProperties(cx, this, AdditionalProperties.class);
    }

    private static final String ISO_FORMAT = "%04d-%02d-%02dT%02d:%02d:%02d.%03dZ";
    private static final String ISO_EXTENDED_FORMAT = "%+07d-%02d-%02dT%02d:%02d:%02d.%03dZ";

    private static final String UTC_FORMAT = "%04d-%02d-%02d %02d:%02d:%02d.%03dZ";
    private static final String UTC_EXTENDED_FORMAT = "%+07d-%02d-%02d %02d:%02d:%02d.%03dZ";

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

    public enum DateString {
        Date, Time, DateTime
    }

    /**
     * 20.3.4.41.1 Runtime Semantics: ToDateString(tv) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param tv
     *            the date-time value
     * @param dateString
     *            the date string modifier
     * @return the date-time string representation
     */
    public static String ToDateString(ExecutionContext cx, double tv, DateString dateString) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (Double.isNaN(tv)) {
            return "Invalid Date";
        }
        Realm realm = cx.getRealm();
        StringBuilder result = new StringBuilder();
        if (dateString == DateString.DateTime || dateString == DateString.Date) {
            DateFormatSymbols symbols = DateFormatSymbols.getInstance(Locale.US);
            double t = LocalTime(realm, tv);

            result.append(String.format("%s %s %02d %04d",
                    symbols.getShortWeekdays()[1 + (int) WeekDay(t)],
                    symbols.getShortMonths()[(int) MonthFromTime(t)], (int) DateFromTime(t),
                    (int) YearFromTime(t)));
        }
        if (dateString == DateString.DateTime) {
            result.append(' ');
        }
        if (dateString == DateString.DateTime || dateString == DateString.Time) {
            TimeZone tz = realm.getTimezone();
            int dstOffset = (int) DaylightSavingTA(realm, tv);
            int tzOffset = (((int) LocalTZA(realm) + dstOffset) / 60000);
            tzOffset = (tzOffset / 60) * 100 + tzOffset % 60;
            double t = LocalTime(realm, tv);

            result.append(String.format("%02d:%02d:%02d GMT%+05d (%s)", (int) HourFromTime(t),
                    (int) MinFromTime(t), (int) SecFromTime(t), tzOffset,
                    tz.getDisplayName(dstOffset != 0, TimeZone.SHORT, Locale.US)));
        }
        return result.toString();
    }

    /**
     * 20.3.4 Properties of the Date Prototype Object
     */
    public enum Properties {
        ;

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
                DateObject obj = (DateObject) object;
                if (obj.isInitialized()) {
                    return obj.getDateValue();
                }
                throw newTypeError(cx, Messages.Key.UninitializedObject);
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 20.3.4.1 Date.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Date;

        /**
         * 20.3.4.41 Date.prototype.toString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string representation
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            double tv;
            if (Type.isObject(thisValue) && !(thisValue instanceof DateObject)) {
                tv = Double.NaN;
            } else {
                tv = thisTimeValue(cx, thisValue);
            }
            /* step 4 */
            return ToDateString(cx, tv, DateString.DateTime);
        }

        /**
         * 20.3.4.35 Date.prototype.toDateString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the date string representation
         */
        @Function(name = "toDateString", arity = 0)
        public static Object toDateString(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            double tv = thisTimeValue(cx, thisValue);
            /* step 2 */
            return ToDateString(cx, tv, DateString.Date);
        }

        /**
         * 20.3.4.42 Date.prototype.toTimeString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the time string representation
         */
        @Function(name = "toTimeString", arity = 0)
        public static Object toTimeString(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            double tv = thisTimeValue(cx, thisValue);
            /* step 2 */
            return ToDateString(cx, tv, DateString.Time);
        }

        /**
         * 20.3.4.39 Date.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] )<br>
         * 13.3.1 Date.prototype.toLocaleString ([locales [, options]])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the locales array
         * @param options
         *            the options object
         * @return the locale string representation
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue, Object locales,
                Object options) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return "Invalid Date";
            }

            // ECMA-402
            options = ToDateTimeOptions(cx, options, "any", "all");
            DateTimeFormatConstructor constructor = (DateTimeFormatConstructor) cx
                    .getIntrinsic(Intrinsics.Intl_DateTimeFormat);
            DateTimeFormatObject dateTimeFormat = constructor.construct(cx, locales, options);
            return FormatDateTime(cx, dateTimeFormat, t);
        }

        /**
         * 20.3.4.38 Date.prototype.toLocaleDateString ( [ reserved1 [ , reserved2 ] ] )<br>
         * 13.3.2 Date.prototype.toLocaleDateString ([locales [, options]])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the locales array
         * @param options
         *            the options object
         * @return the locale date string representation
         */
        @Function(name = "toLocaleDateString", arity = 0)
        public static Object toLocaleDateString(ExecutionContext cx, Object thisValue,
                Object locales, Object options) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return "Invalid Date";
            }

            // ECMA-402
            options = ToDateTimeOptions(cx, options, "date", "date");
            DateTimeFormatConstructor constructor = (DateTimeFormatConstructor) cx
                    .getIntrinsic(Intrinsics.Intl_DateTimeFormat);
            DateTimeFormatObject dateTimeFormat = constructor.construct(cx, locales, options);
            return FormatDateTime(cx, dateTimeFormat, t);
        }

        /**
         * 20.3.4.40 Date.prototype.toLocaleTimeString ( [ reserved1 [ , reserved2 ] ] )<br>
         * 13.3.3 Date.prototype.toLocaleTimeString ([locales [, options]])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the locales array
         * @param options
         *            the options object
         * @return the locale time string representation
         */
        @Function(name = "toLocaleTimeString", arity = 0)
        public static Object toLocaleTimeString(ExecutionContext cx, Object thisValue,
                Object locales, Object options) {
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                return "Invalid Date";
            }

            // ECMA-402
            options = ToDateTimeOptions(cx, options, "time", "time");
            DateTimeFormatConstructor constructor = (DateTimeFormatConstructor) cx
                    .getIntrinsic(Intrinsics.Intl_DateTimeFormat);
            DateTimeFormatObject dateTimeFormat = constructor.construct(cx, locales, options);
            return FormatDateTime(cx, dateTimeFormat, t);
        }

        /**
         * 20.3.4.44 Date.prototype.valueOf ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the date-time value of this date object
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            return thisTimeValue(cx, thisValue);
        }

        /**
         * 20.3.4.10 Date.prototype.getTime ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the date-time value of this date object
         */
        @Function(name = "getTime", arity = 0)
        public static Object getTime(ExecutionContext cx, Object thisValue) {
            return thisTimeValue(cx, thisValue);
        }

        /**
         * 20.3.4.4 Date.prototype.getFullYear ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the full year of this date object
         */
        @Function(name = "getFullYear", arity = 0)
        public static Object getFullYear(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return YearFromTime(LocalTime(realm, t));
        }

        /**
         * 20.3.4.14 Date.prototype.getUTCFullYear ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the full year in the UTC time zone
         */
        @Function(name = "getUTCFullYear", arity = 0)
        public static Object getUTCFullYear(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return YearFromTime(t);
        }

        /**
         * 20.3.4.8 Date.prototype.getMonth ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the month of this date object
         */
        @Function(name = "getMonth", arity = 0)
        public static Object getMonth(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return MonthFromTime(LocalTime(realm, t));
        }

        /**
         * 20.3.4.18 Date.prototype.getUTCMonth ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the month in the UTC time zone
         */
        @Function(name = "getUTCMonth", arity = 0)
        public static Object getUTCMonth(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return MonthFromTime(t);
        }

        /**
         * 20.3.4.2 Date.prototype.getDate ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the date of this date object
         */
        @Function(name = "getDate", arity = 0)
        public static Object getDate(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return DateFromTime(LocalTime(realm, t));
        }

        /**
         * 20.3.4.12 Date.prototype.getUTCDate ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the date in the UTC time zone
         */
        @Function(name = "getUTCDate", arity = 0)
        public static Object getUTCDate(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return DateFromTime(t);
        }

        /**
         * 20.3.4.3 Date.prototype.getDay ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the week day of this date object
         */
        @Function(name = "getDay", arity = 0)
        public static Object getDay(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return WeekDay(LocalTime(realm, t));
        }

        /**
         * 20.3.4.13 Date.prototype.getUTCDay ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the week day in the UTC time zone
         */
        @Function(name = "getUTCDay", arity = 0)
        public static Object getUTCDay(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return WeekDay(t);
        }

        /**
         * 20.3.4.5 Date.prototype.getHours ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the hours of this date object
         */
        @Function(name = "getHours", arity = 0)
        public static Object getHours(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return HourFromTime(LocalTime(realm, t));
        }

        /**
         * 20.3.4.15 Date.prototype.getUTCHours ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the hours in the UTC time zone
         */
        @Function(name = "getUTCHours", arity = 0)
        public static Object getUTCHours(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return HourFromTime(t);
        }

        /**
         * 20.3.4.7 Date.prototype.getMinutes ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the minutes of this date object
         */
        @Function(name = "getMinutes", arity = 0)
        public static Object getMinutes(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return MinFromTime(LocalTime(realm, t));
        }

        /**
         * 20.3.4.17 Date.prototype.getUTCMinutes ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the minutes in the UTC time zone
         */
        @Function(name = "getUTCMinutes", arity = 0)
        public static Object getUTCMinutes(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return MinFromTime(t);
        }

        /**
         * 20.3.4.9 Date.prototype.getSeconds ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the seconds of this date object
         */
        @Function(name = "getSeconds", arity = 0)
        public static Object getSeconds(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return SecFromTime(LocalTime(realm, t));
        }

        /**
         * 20.3.4.19 Date.prototype.getUTCSeconds ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the seconds in the UTC time zone
         */
        @Function(name = "getUTCSeconds", arity = 0)
        public static Object getUTCSeconds(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return SecFromTime(t);
        }

        /**
         * 20.3.4.6 Date.prototype.getMilliseconds ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the milli-seconds of this date object
         */
        @Function(name = "getMilliseconds", arity = 0)
        public static Object getMilliseconds(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return msFromTime(LocalTime(realm, t));
        }

        /**
         * 20.3.4.16 Date.prototype.getUTCMilliseconds ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the milli-seconds in the UTC time zone
         */
        @Function(name = "getUTCMilliseconds", arity = 0)
        public static Object getUTCMilliseconds(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return msFromTime(t);
        }

        /**
         * 20.3.4.11 Date.prototype.getTimezoneOffset ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the time zone offset of this date object
         */
        @Function(name = "getTimezoneOffset", arity = 0)
        public static Object getTimezoneOffset(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return (t - LocalTime(realm, t)) / msPerMinute;
        }

        /**
         * 20.3.4.27 Date.prototype.setTime (time)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param time
         *            the new time value
         * @return the new date-time value
         */
        @Function(name = "setTime", arity = 1)
        public static Object setTime(ExecutionContext cx, Object thisValue, Object time) {
            // just to trigger type and initialization test
            thisTimeValue(cx, thisValue);
            /* steps 1-3 */
            double v = TimeClip(ToNumber(cx, time));
            /* step 4 */
            ((DateObject) thisValue).setDateValue(v);
            /* step 5 */
            return v;
        }

        /**
         * 20.3.4.23 Date.prototype.setMilliseconds (ms)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param ms
         *            the new milli-seconds value
         * @return the new date-time value
         */
        @Function(name = "setMilliseconds", arity = 1)
        public static Object setMilliseconds(ExecutionContext cx, Object thisValue, Object ms) {
            Realm realm = cx.getRealm();
            /* step 1 */
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            /* steps 2-3 */
            double milli = ToNumber(cx, ms);
            /* step 4 */
            double time = MakeTime(HourFromTime(t), MinFromTime(t), SecFromTime(t), milli);
            /* step 5 */
            double u = TimeClip(UTC(realm, MakeDate(Day(t), time)));
            /* step 6 */
            ((DateObject) thisValue).setDateValue(u);
            /* step 7 */
            return u;
        }

        /**
         * 20.3.4.31 Date.prototype.setUTCMilliseconds (ms)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param ms
         *            the new milli-seconds value
         * @return the new date-time value
         */
        @Function(name = "setUTCMilliseconds", arity = 1)
        public static Object setUTCMilliseconds(ExecutionContext cx, Object thisValue, Object ms) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* steps 3-4 */
            double milli = ToNumber(cx, ms);
            /* step 5 */
            double time = MakeTime(HourFromTime(t), MinFromTime(t), SecFromTime(t), milli);
            /* step 6 */
            double v = TimeClip(MakeDate(Day(t), time));
            /* step 7 */
            ((DateObject) thisValue).setDateValue(v);
            /* step 8 */
            return v;
        }

        /**
         * 20.3.4.26 Date.prototype.setSeconds (sec [, ms ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param sec
         *            the new seconds value
         * @param ms
         *            the new milli-seconds value
         * @return the new date-time value
         */
        @Function(name = "setSeconds", arity = 2)
        public static Object setSeconds(ExecutionContext cx, Object thisValue, Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            Realm realm = cx.getRealm();
            /* step 1 */
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            /* steps 2-3 */
            double s = ToNumber(cx, sec);
            /* steps 4-5 */
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            /* step 6 */
            double date = MakeDate(Day(t), MakeTime(HourFromTime(t), MinFromTime(t), s, milli));
            /* step 7 */
            double u = TimeClip(UTC(realm, date));
            /* step 8 */
            ((DateObject) thisValue).setDateValue(u);
            /* step 9 */
            return u;
        }

        /**
         * 20.3.4.34 Date.prototype.setUTCSeconds (sec [, ms ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param sec
         *            the new seconds value
         * @param ms
         *            the new milli-seconds value
         * @return the new date-time value
         */
        @Function(name = "setUTCSeconds", arity = 2)
        public static Object setUTCSeconds(ExecutionContext cx, Object thisValue, Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            double s = ToNumber(cx, sec);
            /* step 4 */
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            /* step 5 */
            double date = MakeDate(Day(t), MakeTime(HourFromTime(t), MinFromTime(t), s, milli));
            /* step 6 */
            double v = TimeClip(date);
            /* step 7 */
            ((DateObject) thisValue).setDateValue(v);
            /* step 8 */
            return v;
        }

        /**
         * 20.3.4.24 Date.prototype.setMinutes (min [, sec [, ms ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param min
         *            the new minutes value
         * @param sec
         *            the new seconds value
         * @param ms
         *            the new milli-seconds value
         * @return the new date-time value
         */
        @Function(name = "setMinutes", arity = 3)
        public static Object setMinutes(ExecutionContext cx, Object thisValue, Object min,
                @Optional(Optional.Default.NONE) Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            Realm realm = cx.getRealm();
            /* step 1 */
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            /* steps 2-3 */
            double m = ToNumber(cx, min);
            /* steps 4-5 */
            double s = (sec == null ? SecFromTime(t) : ToNumber(cx, sec));
            /* steps 6-7 */
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            /* step 8 */
            double date = MakeDate(Day(t), MakeTime(HourFromTime(t), m, s, milli));
            /* step 9 */
            double u = TimeClip(UTC(realm, date));
            /* step 10 */
            ((DateObject) thisValue).setDateValue(u);
            /* step 11 */
            return u;
        }

        /**
         * 20.3.4.32 Date.prototype.setUTCMinutes (min [, sec [, ms ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param min
         *            the new minutes value
         * @param sec
         *            the new seconds value
         * @param ms
         *            the new milli-seconds value
         * @return the new date-time value
         */
        @Function(name = "setUTCMinutes", arity = 3)
        public static Object setUTCMinutes(ExecutionContext cx, Object thisValue, Object min,
                @Optional(Optional.Default.NONE) Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            double m = ToNumber(cx, min);
            /* step 4 */
            double s = (sec == null ? SecFromTime(t) : ToNumber(cx, sec));
            /* step 5 */
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            /* step 6 */
            double date = MakeDate(Day(t), MakeTime(HourFromTime(t), m, s, milli));
            /* step 7 */
            double v = TimeClip(date);
            /* step 8 */
            ((DateObject) thisValue).setDateValue(v);
            /* step 9 */
            return v;
        }

        /**
         * 20.3.4.22 Date.prototype.setHours (hour [, min [, sec [, ms ] ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param hour
         *            the new hours value
         * @param min
         *            the new minutes value
         * @param sec
         *            the new seconds value
         * @param ms
         *            the new milli-seconds value
         * @return the new date-time value
         */
        @Function(name = "setHours", arity = 4)
        public static Object setHours(ExecutionContext cx, Object thisValue, Object hour,
                @Optional(Optional.Default.NONE) Object min,
                @Optional(Optional.Default.NONE) Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            Realm realm = cx.getRealm();
            /* step 1 */
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            /* steps 2-3 */
            double h = ToNumber(cx, hour);
            /* steps 4-5 */
            double m = (min == null ? MinFromTime(t) : ToNumber(cx, min));
            /* steps 6-7 */
            double s = (sec == null ? SecFromTime(t) : ToNumber(cx, sec));
            /* steps 8-9 */
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            /* step 10 */
            double date = MakeDate(Day(t), MakeTime(h, m, s, milli));
            /* step 11 */
            double u = TimeClip(UTC(realm, date));
            /* step 12 */
            ((DateObject) thisValue).setDateValue(u);
            /* step 13 */
            return u;
        }

        /**
         * 20.3.4.30 Date.prototype.setUTCHours (hour [, min [, sec [, ms ] ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param hour
         *            the new hours value
         * @param min
         *            the new minutes value
         * @param sec
         *            the new seconds value
         * @param ms
         *            the new milli-seconds value
         * @return the new date-time value
         */
        @Function(name = "setUTCHours", arity = 4)
        public static Object setUTCHours(ExecutionContext cx, Object thisValue, Object hour,
                @Optional(Optional.Default.NONE) Object min,
                @Optional(Optional.Default.NONE) Object sec,
                @Optional(Optional.Default.NONE) Object ms) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* steps 3-4 */
            double h = ToNumber(cx, hour);
            /* steps 5-6 */
            double m = (min == null ? MinFromTime(t) : ToNumber(cx, min));
            /* steps 7-8 */
            double s = (sec == null ? SecFromTime(t) : ToNumber(cx, sec));
            /* steps 9-10 */
            double milli = (ms == null ? msFromTime(t) : ToNumber(cx, ms));
            /* step 11 */
            double newDate = MakeDate(Day(t), MakeTime(h, m, s, milli));
            /* step 12 */
            double v = TimeClip(newDate);
            /* step 13 */
            ((DateObject) thisValue).setDateValue(v);
            /* step 14 */
            return v;
        }

        /**
         * 20.3.4.20 Date.prototype.setDate (date)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param date
         *            the new date value
         * @return the new date-time value
         */
        @Function(name = "setDate", arity = 1)
        public static Object setDate(ExecutionContext cx, Object thisValue, Object date) {
            Realm realm = cx.getRealm();
            /* step 1 */
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            /* steps 2-3 */
            double dt = ToNumber(cx, date);
            /* step 4 */
            double newDate = MakeDate(MakeDay(YearFromTime(t), MonthFromTime(t), dt),
                    TimeWithinDay(t));
            /* step 5 */
            double u = TimeClip(UTC(realm, newDate));
            /* step 6 */
            ((DateObject) thisValue).setDateValue(u);
            /* step 7 */
            return u;
        }

        /**
         * 20.3.4.28 Date.prototype.setUTCDate (date)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param date
         *            the new date value
         * @return the new date-time value
         */
        @Function(name = "setUTCDate", arity = 1)
        public static Object setUTCDate(ExecutionContext cx, Object thisValue, Object date) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* steps 3-4 */
            double dt = ToNumber(cx, date);
            /* step 5 */
            double newDate = MakeDate(MakeDay(YearFromTime(t), MonthFromTime(t), dt),
                    TimeWithinDay(t));
            /* step 6 */
            double v = TimeClip(newDate);
            /* step 7 */
            ((DateObject) thisValue).setDateValue(v);
            /* step 8 */
            return v;
        }

        /**
         * 20.3.4.25 Date.prototype.setMonth (month [, date ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param month
         *            the new month value
         * @param date
         *            the new date value
         * @return the new date-time value
         */
        @Function(name = "setMonth", arity = 2)
        public static Object setMonth(ExecutionContext cx, Object thisValue, Object month,
                @Optional(Optional.Default.NONE) Object date) {
            Realm realm = cx.getRealm();
            /* step 1 */
            double t = LocalTime(realm, thisTimeValue(cx, thisValue));
            /* steps 2-3 */
            double m = ToNumber(cx, month);
            /* steps 4-5 */
            double dt = (date == null ? DateFromTime(t) : ToNumber(cx, date));
            /* step 6 */
            double newDate = MakeDate(MakeDay(YearFromTime(t), m, dt), TimeWithinDay(t));
            /* step 7 */
            double u = TimeClip(UTC(realm, newDate));
            /* step 8 */
            ((DateObject) thisValue).setDateValue(u);
            /* step 9 */
            return u;
        }

        /**
         * 20.3.4.33 Date.prototype.setUTCMonth (month [, date ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param month
         *            the new month value
         * @param date
         *            the new date value
         * @return the new date-time value
         */
        @Function(name = "setUTCMonth", arity = 2)
        public static Object setUTCMonth(ExecutionContext cx, Object thisValue, Object month,
                @Optional(Optional.Default.NONE) Object date) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            double m = ToNumber(cx, month);
            /* step 4 */
            double dt = (date == null ? DateFromTime(t) : ToNumber(cx, date));
            /* step 5 */
            double newDate = MakeDate(MakeDay(YearFromTime(t), m, dt), TimeWithinDay(t));
            /* step 6 */
            double v = TimeClip(newDate);
            /* step 7 */
            ((DateObject) thisValue).setDateValue(v);
            /* step 8 */
            return v;
        }

        /**
         * 20.3.4.21 Date.prototype.setFullYear (year [, month [, date ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param year
         *            the new year value
         * @param month
         *            the new month value
         * @param date
         *            the new date value
         * @return the new date-time value
         */
        @Function(name = "setFullYear", arity = 3)
        public static Object setFullYear(ExecutionContext cx, Object thisValue, Object year,
                @Optional(Optional.Default.NONE) Object month,
                @Optional(Optional.Default.NONE) Object date) {
            Realm realm = cx.getRealm();
            /* step 1 */
            double t = thisTimeValue(cx, thisValue);
            t = Double.isNaN(t) ? +0 : LocalTime(realm, t);
            /* steps 2-3 */
            double y = ToNumber(cx, year);
            /* steps 4-5 */
            double m = (month == null ? MonthFromTime(t) : ToNumber(cx, month));
            /* steps 6-7 */
            double dt = (date == null ? DateFromTime(t) : ToNumber(cx, date));
            /* step 8 */
            double newDate = MakeDate(MakeDay(y, m, dt), TimeWithinDay(t));
            /* step 9 */
            double u = TimeClip(UTC(realm, newDate));
            /* step 10 */
            ((DateObject) thisValue).setDateValue(u);
            /* step 11 */
            return u;
        }

        /**
         * 20.3.4.29 Date.prototype.setUTCFullYear (year [, month [, date ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param year
         *            the new year value
         * @param month
         *            the new month value
         * @param date
         *            the new date value
         * @return the new date-time value
         */
        @Function(name = "setUTCFullYear", arity = 3)
        public static Object setUTCFullYear(ExecutionContext cx, Object thisValue, Object year,
                @Optional(Optional.Default.NONE) Object month,
                @Optional(Optional.Default.NONE) Object date) {
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            if (Double.isNaN(t)) {
                t = +0;
            }
            /* steps 3-4 */
            double y = ToNumber(cx, year);
            /* steps 5-6 */
            double m = (month == null ? MonthFromTime(t) : ToNumber(cx, month));
            /* steps 7-8 */
            double dt = (date == null ? DateFromTime(t) : ToNumber(cx, date));
            /* step 9 */
            double newDate = MakeDate(MakeDay(y, m, dt), TimeWithinDay(t));
            /* step 10 */
            double v = TimeClip(newDate);
            /* step 11 */
            ((DateObject) thisValue).setDateValue(v);
            /* step 12 */
            return v;
        }

        /**
         * 20.3.4.43 Date.prototype.toUTCString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the date-time string in the UTC time zone
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
         * 20.3.4.36 Date.prototype.toISOString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the date-time string in the ISO date-time format
         */
        @Function(name = "toISOString", arity = 0)
        public static Object toISOString(ExecutionContext cx, Object thisValue) {
            double dateValue = thisTimeValue(cx, thisValue);
            if (!isFinite(dateValue)) {
                throw newRangeError(cx, Messages.Key.InvalidDateValue);
            }
            return DatePrototype.toISOString(dateValue);
        }

        /**
         * 20.3.4.37 Date.prototype.toJSON ( key )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param key
         *            the key argument (unused)
         * @return the JSON string for this date object
         */
        @Function(name = "toJSON", arity = 1)
        public static Object toJSON(ExecutionContext cx, Object thisValue, Object key) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* steps 2-3 */
            Object tv = AbstractOperations.ToPrimitive(cx, o, ToPrimitiveHint.Number);
            /* step 4 */
            if (Type.isNumber(tv) && !isFinite(Type.numberValue(tv))) {
                return NULL;
            }
            /* step 5 */
            return Invoke(cx, o, "toISOString");
        }

        /**
         * 20.3.4.45 Date.prototype[@@toPrimitive] ( hint )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param hint
         *            the ToPrimitive hint string
         * @return the primitive value for this date object
         */
        @Function(name = "[Symbol.toPrimitive]", arity = 1, symbol = BuiltinSymbol.toPrimitive,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object toPrimitive(ExecutionContext cx, Object thisValue, Object hint) {
            // TODO: is this function generic?
            /* steps 1-2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 5 */
            if (!Type.isString(hint)) {
                throw newTypeError(cx, Messages.Key.InvalidToPrimitiveHint, "?");
            }
            /* steps 3-5 */
            ToPrimitiveHint tryFirst;
            String _hint = Type.stringValue(hint).toString();
            if ("string".equals(_hint) || "default".equals(_hint)) {
                tryFirst = ToPrimitiveHint.String;
            } else if ("number".equals(_hint)) {
                tryFirst = ToPrimitiveHint.Number;
            } else {
                throw newTypeError(cx, Messages.Key.InvalidToPrimitiveHint, _hint);
            }
            /* step 6 */
            return OrdinaryToPrimitive(cx, Type.objectValue(thisValue), tryFirst);
        }

    }

    /**
     * B.2.4 Additional Properties of the Date.prototype Object
     */
    @CompatibilityExtension(CompatibilityOption.DatePrototype)
    public enum AdditionalProperties {
        ;

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
                DateObject obj = (DateObject) object;
                if (obj.isInitialized()) {
                    return obj.getDateValue();
                }
                throw newTypeError(cx, Messages.Key.UninitializedObject);
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        /**
         * B.2.4.1 Date.prototype.getYear ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the year of this date object
         */
        @Function(name = "getYear", arity = 0)
        public static Object getYear(ExecutionContext cx, Object thisValue) {
            Realm realm = cx.getRealm();
            /* steps 1-2 */
            double t = thisTimeValue(cx, thisValue);
            /* step 3 */
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            /* step 4 */
            return YearFromTime(LocalTime(realm, t)) - 1900;
        }

        /**
         * B.2.4.2 Date.prototype.setYear (year)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param year
         *            the new year value
         * @return the new date-time value
         */
        @Function(name = "setYear", arity = 1)
        public static Object setYear(ExecutionContext cx, Object thisValue, Object year) {
            Realm realm = cx.getRealm();
            /* step 1 */
            double t = thisTimeValue(cx, thisValue);
            t = Double.isNaN(t) ? +0 : LocalTime(realm, t);
            /* steps 2-3 */
            double y = ToNumber(cx, year);
            /* step 4 */
            if (Double.isNaN(y)) {
                ((DateObject) thisValue).setDateValue(Double.NaN);
                return Double.NaN;
            }
            /* step 5 */
            double intYear = ToInteger(y);
            double yyyy = (0 <= intYear && intYear <= 99 ? intYear + 1900 : y);
            /* step 6 */
            double d = MakeDay(yyyy, MonthFromTime(t), DateFromTime(t));
            /* step 7 */
            double date = UTC(realm, MakeDate(d, TimeWithinDay(t)));
            /* step 8 */
            ((DateObject) thisValue).setDateValue(TimeClip(date));
            /* step 9 */
            return ((DateObject) thisValue).getDateValue();
        }

        /**
         * B.2.4.3 Date.prototype.toGMTString ( )
         * 
         * @param cx
         *            the execution context
         * @return the date-time string in the UTC time zone
         */
        @Value(name = "toGMTString")
        public static Object toGMTString(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.DatePrototype), "toUTCString");
        }
    }
}
