/**
 * Copyright (c) 2012-2015 André Bargull
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

import java.util.Date;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.objects.date.DateConstructor;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.DateField;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.FieldWeight;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.Skeleton;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
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

        // initialize Intl.DateTimeFormat.prototype's internal state
        DateTimeFormatConstructor.InitializeDefaultDateTimeFormat(realm, this);
    }

    /**
     * 12.3 Properties of the Intl.DateTimeFormat Prototype Object
     */
    public enum Properties {
        ;

        private static DateTimeFormatObject thisDateTimeFormatObject(ExecutionContext cx,
                Object object) {
            if (object instanceof DateTimeFormatObject) {
                DateTimeFormatObject dateTimeFormat = (DateTimeFormatObject) object;
                if (dateTimeFormat.isInitializedDateTimeFormat()) {
                    return dateTimeFormat;
                }
                throw newTypeError(cx, Messages.Key.UninitializedObject);
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
         * 12.3.2 Intl.DateTimeFormat.prototype.format
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the bound format function
         */
        @Accessor(name = "format", type = Accessor.Type.Getter)
        public static Object format(ExecutionContext cx, Object thisValue) {
            DateTimeFormatObject dateTimeFormat = thisDateTimeFormatObject(cx, thisValue);
            /* step 1 */
            if (dateTimeFormat.getBoundFormat() == null) {
                /* step 1.a */
                FormatFunction f = new FormatFunction(cx.getRealm());
                /* steps 1.b-c */
                Callable bf = (Callable) FunctionPrototype.Properties.bind(cx, f, thisValue);
                /* step 1.d */
                dateTimeFormat.setBoundFormat(bf);
            }
            /* step 2 */
            return dateTimeFormat.getBoundFormat();
        }

        /**
         * 12.3.3 Intl.DateTimeFormat.prototype.resolvedOptions ()
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
            // properties are restored from pattern field or rather its corresponding skeleton
            DateTimePatternGenerator generator = DateTimePatternGenerator.getEmptyInstance();
            Skeleton skeleton = new Skeleton(generator.getSkeleton(dateTimeFormat.getPattern()));
            for (DateField field : DateField.values()) {
                if (field == DateField.Quarter || field == DateField.Week
                        || field == DateField.Period) {
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
    public static String FormatDateTime(ExecutionContext cx, DateTimeFormatObject dateTimeFormat,
            double x) {
        /* step 1 */
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            throw newRangeError(cx, Messages.Key.InvalidDateValue);
        }
        /* steps 2-9 */
        return dateTimeFormat.getDateFormat().format(new Date((long) x));
    }

    private static final class FormatFunction extends BuiltinFunction {
        public FormatFunction(Realm realm) {
            super(realm, "format", 0);
            createDefaultFunctionProperties();
        }

        private FormatFunction(Realm realm, Void ignore) {
            super(realm, "format", 0);
        }

        @Override
        public FormatFunction clone() {
            return new FormatFunction(getRealm(), null);
        }

        @Override
        public String call(ExecutionContext callerContext, Object thisValue, Object... args) {
            assert thisValue instanceof DateTimeFormatObject;
            ExecutionContext calleeContext = calleeContext();
            Object date = argument(args, 0);
            /* step 1.a.i (12.3.2) */
            if (Type.isUndefined(date)) {
                date = DateConstructor.Properties.now(calleeContext, null);
            }
            /* step 1.a.ii (12.3.2) */
            double x = ToNumber(calleeContext, date);
            /* step 1.a.iii (12.3.2) */
            return FormatDateTime(calleeContext, (DateTimeFormatObject) thisValue, x);
        }
    }
}
