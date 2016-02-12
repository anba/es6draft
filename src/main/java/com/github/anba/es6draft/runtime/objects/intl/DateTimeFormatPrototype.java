/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatConstructor.InitializeDefaultDateTimeFormat;
import static com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject.BoundFunctionCreate;

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
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.DateField;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.FieldWeight;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.Skeleton;
import com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatConstructor.FormatFunction;
import com.github.anba.es6draft.runtime.objects.intl.DateTimeFormatConstructor.FormatToPartsFunction;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.DateTimePatternGenerator;

/**
 * <h1>12 DateTimeFormat Objects</h1>
 * <ul>
 * <li>12.4 Properties of the Intl.DateTimeFormat Prototype Object
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
        InitializeDefaultDateTimeFormat(realm, this);
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
                /* step 2.b */
                BoundFunctionObject bf = BoundFunctionCreate(cx, f, thisValue);
                /* step 2.c */
                bf.infallibleDefineOwnProperty("length", new Property(1, false, false, true));
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
                bf.infallibleDefineOwnProperty("length", new Property(1, false, false, true));
                /* step 4.d */
                dateTimeFormat.setBoundFormatToParts(bf);
            }
            /* step 5 */
            return dateTimeFormat.getBoundFormatToParts();
        }
    }
}
