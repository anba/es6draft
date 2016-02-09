/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject.BoundFunctionCreate;

import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.CharacterIterator;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.date.DateConstructor;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.DateField;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.FieldWeight;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.Skeleton;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;

/**
 * <h1>12 DateTimeFormat Objects</h1>
 * <ul>
 * <li>12.3 Properties of the Intl.DateTimeFormat Prototype Object
 * </ul>
 */
public final class DateTimeFormatPrototype extends DateTimeFormatObject implements Initializable {
    /**
     * Constructs a new DateTimeFormat prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public DateTimeFormatPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, FormatToPartsProperty.class);

        // Initialize Intl.DateTimeFormat.prototype's internal state.
        DateTimeFormatConstructor.InitializeDefaultDateTimeFormat(realm, this);
    }

    /**
     * 12.3 Properties of the Intl.DateTimeFormat Prototype Object
     */
    public enum Properties {
        ;

        private static DateTimeFormatObject thisDateTimeFormatObject(ExecutionContext cx, Object object) {
            if (object instanceof DateTimeFormatObject) {
                return (DateTimeFormatObject) object;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 12.3.1 Intl.DateTimeFormat.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Intl_DateTimeFormat;

        /**
         * 12.3.2 Intl.DateTimeFormat.prototype[@@toStringTag]]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Object";

        /**
         * 12.3.3 Intl.DateTimeFormat.prototype.format
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the bound format function
         */
        @Accessor(name = "format", type = Accessor.Type.Getter)
        public static Object format(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            DateTimeFormatObject dateTimeFormat = thisDateTimeFormatObject(cx, thisValue);
            /* step 2 */
            if (dateTimeFormat.getBoundFormat() == null) {
                /* step 2.a */
                FormatFunction f = new FormatFunction(cx.getRealm());
                /* step 2.b (not applicable) */
                /* step 2.c */
                BoundFunctionObject bf = BoundFunctionCreate(cx, f, thisValue);
                // FIXME: spec bug - missing define for .length
                bf.infallibleDefineOwnProperty("length", new Property(0, false, false, true));
                // FIXME: spec issue - set .name property?
                /* step 2.d */
                dateTimeFormat.setBoundFormat(bf);
            }
            /* step 3 */
            return dateTimeFormat.getBoundFormat();
        }

        /**
         * 12.3.5 Intl.DateTimeFormat.prototype.resolvedOptions ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the resolved options object
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(ExecutionContext cx, Object thisValue) {
            DateTimeFormatObject dateTimeFormat = thisDateTimeFormatObject(cx, thisValue);
            OrdinaryObject object = OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype);
            CreateDataProperty(cx, object, "locale", dateTimeFormat.getLocale());
            CreateDataProperty(cx, object, "calendar", dateTimeFormat.getCalendar());
            CreateDataProperty(cx, object, "numberingSystem", dateTimeFormat.getNumberingSystem());
            assert dateTimeFormat.getTimeZone() != null;
            CreateDataProperty(cx, object, "timeZone", dateTimeFormat.getTimeZone());
            // hour12, weekday, era, year, month, day, hour, minute, second, and timeZoneName
            // properties are restored from pattern field or rather its corresponding skeleton.
            DateTimePatternGenerator generator = DateTimePatternGenerator.getEmptyInstance();
            Skeleton skeleton = new Skeleton(generator.getSkeleton(dateTimeFormat.getPattern()));
            for (DateField field : DateField.values()) {
                if (field == DateField.Quarter || field == DateField.Week || field == DateField.Period) {
                    continue;
                }
                FieldWeight weight = skeleton.getWeight(field);
                if (weight != null) {
                    CreateDataProperty(cx, object, field.toString(), weight.toString());
                    if (field == DateField.Hour) {
                        CreateDataProperty(cx, object, "hour12", skeleton.isHour12());
                    }
                }
            }
            return object;
        }
    }

    /**
     * 12.3 Properties of the Intl.DateTimeFormat Prototype Object
     */
    @CompatibilityExtension(CompatibilityOption.FormatToParts)
    public enum FormatToPartsProperty {
        ;

        private static DateTimeFormatObject thisDateTimeFormatObject(ExecutionContext cx, Object object) {
            if (object instanceof DateTimeFormatObject) {
                return (DateTimeFormatObject) object;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        /**
         * get Intl.DateTimeFormat.prototype.formatToParts
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the bound format function
         */
        @Accessor(name = "formatToParts", type = Accessor.Type.Getter)
        public static Object formatToParts(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            DateTimeFormatObject dateTimeFormat = thisDateTimeFormatObject(cx, thisValue);
            /* step 4 */
            if (dateTimeFormat.getBoundFormatToParts() == null) {
                /* step 4.a */
                FormatToPartsFunction f = new FormatToPartsFunction(cx.getRealm());
                /* step 4.b (not applicable) */
                /* step 4.c */
                BoundFunctionObject bf = BoundFunctionCreate(cx, f, thisValue);
                // FIXME: spec bug - missing define for .length
                bf.infallibleDefineOwnProperty("length", new Property(0, false, false, true));
                // FIXME: spec issue - set .name property?
                /* step 4.d */
                dateTimeFormat.setBoundFormatToParts(bf);
            }
            /* step 5 */
            return dateTimeFormat.getBoundFormatToParts();
        }
    }

    /**
     * Abstract Operation: FormatDateTime
     * 
     * @param cx
     *            the execution context
     * @param dateTimeFormat
     *            the date format object
     * @param x
     *            the number value
     * @return the formatted date-time string
     */
    public static String FormatDateTime(ExecutionContext cx, DateTimeFormatObject dateTimeFormat, double x) {
        /* step 1 */
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            throw newRangeError(cx, Messages.Key.InvalidDateValue);
        }
        /* steps 2-11 */
        return dateTimeFormat.getDateFormat().format(new Date((long) x));
    }

    /**
     * CreateDateTimeParts(dateTimeFormat, x)
     * 
     * @param dateTimeFormat
     *            the date format object
     * @param date
     *            the date object
     * @return the formatted date-time object
     */
    private static List<Map.Entry<String, String>> CreateDateTimeParts(DateTimeFormatObject dateTimeFormat, Date date) {
        ArrayList<Map.Entry<String, String>> parts = new ArrayList<>();
        DateFormat dateFormat = dateTimeFormat.getDateFormat();
        AttributedCharacterIterator iterator = dateFormat.formatToCharacterIterator(date);
        StringBuilder sb = new StringBuilder();
        for (char ch = iterator.first(); ch != CharacterIterator.DONE; ch = iterator.next()) {
            sb.append(ch);
            if (iterator.getIndex() + 1 == iterator.getRunLimit()) {
                Iterator<Attribute> keyIterator = iterator.getAttributes().keySet().iterator();
                String key;
                if (keyIterator.hasNext()) {
                    key = toString((DateFormat.Field) keyIterator.next());
                } else {
                    key = "separator";
                }
                String value = sb.toString();
                sb.setLength(0);
                parts.add(new AbstractMap.SimpleImmutableEntry<>(key, value));
            }
        }
        return parts;
    }

    private static String toString(DateFormat.Field field) {
        if (field == DateFormat.Field.DAY_OF_WEEK) {
            return "weekday";
        }
        if (field == DateFormat.Field.ERA) {
            return "era";
        }
        if (field == DateFormat.Field.YEAR) {
            return "year";
        }
        if (field == DateFormat.Field.MONTH) {
            return "month";
        }
        if (field == DateFormat.Field.DAY_OF_MONTH) {
            return "day";
        }
        if (field == DateFormat.Field.HOUR0) {
            return "hour";
        }
        if (field == DateFormat.Field.HOUR1) {
            return "hour";
        }
        if (field == DateFormat.Field.HOUR_OF_DAY0) {
            return "hour";
        }
        if (field == DateFormat.Field.HOUR_OF_DAY1) {
            return "hour";
        }
        if (field == DateFormat.Field.MINUTE) {
            return "minute";
        }
        if (field == DateFormat.Field.SECOND) {
            return "second";
        }
        if (field == DateFormat.Field.TIME_ZONE) {
            return "timeZoneName";
        }
        if (field == DateFormat.Field.AM_PM) {
            // FIXME: spec issue - rename to "dayPeriod" for consistency with "timeZoneName"?
            return "dayperiod";
        }
        // Report unsupported/unexpected date fields as separators.
        return "separator";
    }

    /**
     * FormatToPartDateTime(dateTimeFormat, x)
     * 
     * @param cx
     *            the execution context
     * @param dateTimeFormat
     *            the date format object
     * @param x
     *            the number value
     * @return the formatted date-time object
     */
    public static ArrayObject FormatToPartDateTime(ExecutionContext cx, DateTimeFormatObject dateTimeFormat, double x) {
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            throw newRangeError(cx, Messages.Key.InvalidDateValue);
        }
        /* step 1 */
        List<Map.Entry<String, String>> parts = CreateDateTimeParts(dateTimeFormat, new Date((long) x));
        /* step 2 */
        ArrayObject result = ArrayCreate(cx, 0);
        /* step 3 */
        int n = 0;
        /* step 4 */
        for (Map.Entry<String, String> part : parts) {
            /* step 4.a */
            OrdinaryObject o = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* steps 4.b-c */
            CreateDataProperty(cx, o, "type", part.getKey());
            /* steps 4.d-e */
            CreateDataProperty(cx, o, "value", part.getValue());
            /* steps 4.f-g */
            CreateDataProperty(cx, result, n++, o);
        }
        /* step 5 */
        return result;
    }

    /**
     * 12.3.4 DateTime Format Functions
     */
    public static final class FormatFunction extends BuiltinFunction {
        public FormatFunction(Realm realm) {
            super(realm, "format", 1);
            createDefaultFunctionProperties();
        }

        private FormatFunction(Realm realm, Void ignore) {
            super(realm, "format", 1);
        }

        @Override
        public FormatFunction clone() {
            return new FormatFunction(getRealm(), null);
        }

        @Override
        public String call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            /* steps 1-2 */
            assert thisValue instanceof DateTimeFormatObject;
            DateTimeFormatObject dtf = (DateTimeFormatObject) thisValue;
            /* step 3 */
            Object date = argument(args, 0);
            if (Type.isUndefined(date)) {
                date = DateConstructor.Properties.now(calleeContext, null);
            }
            /* step 4 */
            double x = ToNumber(calleeContext, date);
            /* step 5 */
            return FormatDateTime(calleeContext, dtf, x);
        }
    }

    /**
     * 12.3.4 DateTime Format Functions
     */
    public static final class FormatToPartsFunction extends BuiltinFunction {
        public FormatToPartsFunction(Realm realm) {
            super(realm, "formatToParts", 1);
            createDefaultFunctionProperties();
        }

        private FormatToPartsFunction(Realm realm, Void ignore) {
            super(realm, "formatToParts", 1);
        }

        @Override
        public FormatToPartsFunction clone() {
            return new FormatToPartsFunction(getRealm(), null);
        }

        @Override
        public ArrayObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            /* steps 1-2 */
            assert thisValue instanceof DateTimeFormatObject;
            DateTimeFormatObject dtf = (DateTimeFormatObject) thisValue;
            /* step 3 */
            Object date = argument(args, 0);
            if (Type.isUndefined(date)) {
                date = DateConstructor.Properties.now(calleeContext, null);
            }
            /* step 4 */
            double x = ToNumber(calleeContext, date);
            /* step 5 */
            return FormatToPartDateTime(calleeContext, dtf, x);
        }
    }
}
