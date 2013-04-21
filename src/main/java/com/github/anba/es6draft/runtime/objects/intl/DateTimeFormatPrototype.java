/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateOwnDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import java.util.Date;

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
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.DateTimePatternGenerator;

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

        // initialise Intl.DateTimeFormat.prototype's internal state
        DateTimeFormatConstructor.InitializeDateTimeFormat(cx, this, UNDEFINED, UNDEFINED);
    }

    /**
     * 12.3 Properties of the Intl.DateTimeFormat Prototype Object
     */
    public enum Properties {
        ;

        private static DateTimeFormatObject dateTimeFormat(ExecutionContext cx, Object object) {
            if (object instanceof DateTimeFormatObject) {
                DateTimeFormatObject dateTimeFormat = (DateTimeFormatObject) object;
                if (dateTimeFormat.isInitializedDateTimeFormat()) {
                    return dateTimeFormat;
                }
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
            if (dateTimeFormat.getBoundFormat() == null) {
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
            DateTimeFormatObject dateTimeFormat = dateTimeFormat(cx, thisValue);
            OrdinaryObject object = OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype);
            CreateOwnDataProperty(cx, object, "locale", dateTimeFormat.getLocale());
            CreateOwnDataProperty(cx, object, "calendar", dateTimeFormat.getCalendar());
            CreateOwnDataProperty(cx, object, "numberingSystem",
                    dateTimeFormat.getNumberingSystem());
            if (dateTimeFormat.getTimeZone() != null) {
                CreateOwnDataProperty(cx, object, "timeZone", dateTimeFormat.getTimeZone());
            } else {
                CreateOwnDataProperty(cx, object, "timeZone", UNDEFINED);
            }
            // hour12, weekday, era, year, month, day, hour, minute, second, and timeZoneName
            // properties are restored from pattern field or rather its corresponding skeleton
            DateTimePatternGenerator generator = DateTimePatternGenerator.getEmptyInstance();
            String skeleton = generator.getSkeleton(dateTimeFormat.getPattern());
            for (int i = 0, len = skeleton.length(); i < len;) {
                char c = skeleton.charAt(i++);
                int count = 1;
                for (; i < len && skeleton.charAt(i) == c; ++i) {
                    count += 1;
                }
                PatternField field = PatternField.forSymbol(c);
                if (field == null) {
                    // unknown/unsupported pattern field
                    // System.err.println(c);
                    continue;
                }
                CreateOwnDataProperty(cx, object, field.getName(), field.getAbbrevation(count));
                if (field == PatternField.Hour) {
                    boolean hour12 = (c == 'h' || c == 'K');
                    CreateOwnDataProperty(cx, object, "hour12", hour12);
                }
            }
            return object;
        }
    }

    private static final String NARROW = "narrow", LONG = "long", SHORT = "short",
            TWO_DIGIT = "2-digit", NUMERIC = "numeric";

    private enum PatternField {
        Era("era", SHORT, SHORT, SHORT, LONG, NARROW), //
        Year("year", NUMERIC, TWO_DIGIT, NUMERIC, NUMERIC, NUMERIC), //
        Month("month", NUMERIC, TWO_DIGIT, SHORT, LONG, NARROW), //
        Day("day", NUMERIC, TWO_DIGIT, NUMERIC, NUMERIC, NUMERIC), //
        Weekday("weekday", SHORT, SHORT, SHORT, LONG, NARROW), //
        Hour("hour", NUMERIC, TWO_DIGIT, NUMERIC, NUMERIC, NUMERIC), //
        Minute("minute", NUMERIC, TWO_DIGIT, NUMERIC, NUMERIC, NUMERIC), //
        Second("second", NUMERIC, TWO_DIGIT, NUMERIC, NUMERIC, NUMERIC), //
        TimeZone("timeZoneName", SHORT, SHORT, SHORT, LONG, NARROW);

        private String name;
        private String[] abbrevations = new String[5];

        private PatternField(String name, String numeric, String twoDigit, String _short,
                String _long, String narrow) {
            this.name = name;
            this.abbrevations = new String[] { numeric, twoDigit, _short, _long, narrow };
        }

        public String getName() {
            return name;
        }

        public String getAbbrevation(int n) {
            assert n >= 1;
            // some symbols support more than six numbers, e.g. weekday (E), clamp to five
            if (n > 5) {
                n = 5;
            }
            return abbrevations[n - 1];
        }

        public static PatternField forSymbol(char sym) {
            switch (sym) {
            case 'G':
                return PatternField.Era;
            case 'y':
                return PatternField.Year;
            case 'M':
                return PatternField.Month;
            case 'd':
                return PatternField.Day;
            case 'E':
                return PatternField.Weekday;
            case 'h':
            case 'K':
            case 'H':
            case 'k':
                return PatternField.Hour;
            case 'm':
                return PatternField.Minute;
            case 's':
                return PatternField.Second;
            case 'z':
                return PatternField.TimeZone;
            default:
                return null;
            }
        }
    }

    /**
     * Abstract Operation: FormatDateTime
     */
    public static String FormatDateTime(ExecutionContext cx, DateTimeFormatObject dateTimeFormat,
            double x) {
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            throwRangeError(cx, Messages.Key.InvalidDateValue);
        }
        return dateTimeFormat.getDateFormat().format(new Date((long) x));
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
            Object date = args.length > 0 ? args[0] : UNDEFINED;
            if (Type.isUndefined(date)) {
                date = DateConstructor.Properties.now(callerContext, null);
            }
            double x = ToNumber(callerContext, date);
            return FormatDateTime(callerContext, (DateTimeFormatObject) thisValue, x);
        }
    }
}
