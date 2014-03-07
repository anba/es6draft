/**
 * Copyright (c) 2012-2014 André Bargull
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
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Date;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
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
public final class DateTimeFormatPrototype extends DateTimeFormatObject implements Initialisable {
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

        private static DateTimeFormatObject thisDateTimeFormatValue(ExecutionContext cx,
                Object object) {
            if (object instanceof DateTimeFormatObject) {
                DateTimeFormatObject dateTimeFormat = (DateTimeFormatObject) object;
                if (dateTimeFormat.isInitializedDateTimeFormat()) {
                    return dateTimeFormat;
                }
                throw newTypeError(cx, Messages.Key.UninitialisedObject);
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
         */
        @Accessor(name = "format", type = Accessor.Type.Getter)
        public static Object format(ExecutionContext cx, Object thisValue) {
            DateTimeFormatObject dateTimeFormat = thisDateTimeFormatValue(cx, thisValue);
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
            DateTimeFormatObject dateTimeFormat = thisDateTimeFormatValue(cx, thisValue);
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
     */
    public static String FormatDateTime(ExecutionContext cx, DateTimeFormatObject dateTimeFormat,
            double x) {
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            throw newRangeError(cx, Messages.Key.InvalidDateValue);
        }
        return dateTimeFormat.getDateFormat().format(new Date((long) x));
    }

    private static final class FormatFunction extends BuiltinFunction {
        public FormatFunction(Realm realm) {
            super(realm, "format", 0);
        }

        /**
         * [[Call]]
         */
        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            assert thisValue instanceof DateTimeFormatObject;
            ExecutionContext calleeContext = calleeContext();
            Object date = args.length > 0 ? args[0] : UNDEFINED;
            if (Type.isUndefined(date)) {
                date = DateConstructor.Properties.now(calleeContext, null);
            }
            double x = ToNumber(calleeContext, date);
            return FormatDateTime(calleeContext, (DateTimeFormatObject) thisValue, x);
        }
    }
}
