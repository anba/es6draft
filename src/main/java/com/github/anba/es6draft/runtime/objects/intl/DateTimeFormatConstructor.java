/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.*;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.ObjectConstructor;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.ExtensionKey;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.LocaleData;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.LocaleDataInfo;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.OptionsRecord;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.ResolvedLocale;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;

/**
 * <h1>12 DateTimeFormat Objects</h1>
 * <ul>
 * <li>12.1 The Intl.DateTimeFormat Constructor
 * <li>12.2 Properties of the Intl.DateTimeFormat Constructor
 * </ul>
 */
public class DateTimeFormatConstructor extends BuiltinFunction implements Constructor,
        Initialisable {
    /** [[availableLocales]] */
    private Set<String> availableLocales;

    public static Set<String> getAvailableLocales(ExecutionContext cx) {
        DateTimeFormatConstructor dateTimeFormat = (DateTimeFormatConstructor) cx
                .getIntrinsic(Intrinsics.Intl_DateTimeFormat);
        if (dateTimeFormat.availableLocales == null) {
            dateTimeFormat.availableLocales = GetAvailableLocales(DateFormat.getAvailableULocales());
        }
        return dateTimeFormat.availableLocales;
    }

    /** [[relevantExtensionKeys]] */
    private static List<ExtensionKey> relevantExtensionKeys = asList(ExtensionKey.ca,
            ExtensionKey.nu);

    /** [[localeData]] */
    private static final class DateTimeFormatLocaleData implements LocaleData {
        @Override
        public LocaleDataInfo info(ULocale locale) {
            return new DateTimeFormatLocaleDataInfo(locale);
        }
    }

    /** [[localeData]] */
    private static final class DateTimeFormatLocaleDataInfo implements LocaleDataInfo {
        private final ULocale locale;

        private DateTimeFormatLocaleDataInfo(ULocale locale) {
            this.locale = locale;
        }

        @Override
        public List<String> entries(ExtensionKey extensionKey) {
            switch (extensionKey) {
            case ca:
                return getCalendarInfo();
            case nu:
                return getNumberInfo();
            default:
                throw new IllegalArgumentException(extensionKey.name());
            }
        }

        private List<String> getCalendarInfo() {
            String[] values = Calendar.getKeywordValuesForLocale("calendar", locale, true);
            List<String> result = new ArrayList<>(values.length);
            for (int i = 0, len = values.length; i < len; ++i) {
                String value = values[i];
                if ("gregorian".equals(value)) {
                    value = "gregory";
                }
                result.add(value);
            }
            return result;
        }

        private List<String> getNumberInfo() {
            // ICU4J does not provide an API to retrieve the numbering systems per locale, go with
            // Spidermonkey instead and return default numbering system of locale + Table 2 entries
            String localeNumberingSystem = NumberingSystem.getInstance(locale).getName();
            return asList(localeNumberingSystem, "arab", "arabtext", "bali", "beng", "deva",
                    "fullwide", "gujr", "guru", "hanidec", "khmr", "knda", "laoo", "latn", "limb",
                    "mlym", "mong", "mymr", "orya", "tamldec", "telu", "thai", "tibt");
        }
    }

    public DateTimeFormatConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    /**
     * 12.1.1.1 InitializeDateTimeFormat (dateTimeFormat, locales, options)
     * 
     * @param cx
     */
    public static void InitializeDateTimeFormat(ExecutionContext cx, ScriptObject obj,
            Object locales, Object opts) {
        // spec allows any object to become a DateTimeFormat object, we don't allow this
        if (!(obj instanceof DateTimeFormatObject)) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 1-2 */
        DateTimeFormatObject dateTimeFormat = (DateTimeFormatObject) obj;
        if (dateTimeFormat.isInitializedIntlObject()) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        dateTimeFormat.setInitializedIntlObject(true);
        /* step 3 */
        Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
        /* step 4 */
        ScriptObject options = ToDateTimeOptions(cx, opts, "any", "date");
        /* step 5 */
        OptionsRecord opt = new OptionsRecord();
        /* step 6 */
        String matcher = GetStringOption(cx, options, "localeMatcher", set("lookup", "best fit"),
                "best fit");
        /* step 7 */
        opt.localeMatcher = OptionsRecord.MatcherType.forName(matcher);
        /* step 8-9 */
        DateTimeFormatLocaleData localeData = new DateTimeFormatLocaleData();
        /* step 10 */
        ResolvedLocale r = ResolveLocale(cx, getAvailableLocales(cx), requestedLocales, opt,
                relevantExtensionKeys, localeData);
        /* step 11 */
        dateTimeFormat.setLocale(r.locale);
        /* step 12 */
        dateTimeFormat.setCalendar(r.values.get(ExtensionKey.ca));
        /* step 13 */
        dateTimeFormat.setNumberingSystem(r.values.get(ExtensionKey.nu));
        /* step 14 */
        String dataLocale = r.dataLocale;
        /* step 15-17 */
        String timeZone = null;
        Object tz = Get(cx, options, "timeZone");
        if (!Type.isUndefined(tz)) {
            timeZone = ToUpperCase(ToFlatString(cx, tz));
            if (!"UTC".equals(timeZone)) {
                throw throwRangeError(cx, Messages.Key.IntlInvalidOption, timeZone);
            }
        }
        dateTimeFormat.setTimeZone(timeZone);
        /* step 18 */
        FormatMatcherRecord opt2 = new FormatMatcherRecord();
        /* step 19 */
        opt2.weekday = GetStringOption(cx, options, "weekday", set("narrow", "short", "long"), null);
        opt2.era = GetStringOption(cx, options, "era", set("narrow", "short", "long"), null);
        opt2.year = GetStringOption(cx, options, "year", set("2-digit", "numeric"), null);
        opt2.month = GetStringOption(cx, options, "month",
                set("2-digit", "numeric", "narrow", "short", "long"), null);
        opt2.day = GetStringOption(cx, options, "day", set("2-digit", "numeric"), null);
        opt2.hour = GetStringOption(cx, options, "hour", set("2-digit", "numeric"), null);
        opt2.minute = GetStringOption(cx, options, "minute", set("2-digit", "numeric"), null);
        opt2.second = GetStringOption(cx, options, "second", set("2-digit", "numeric"), null);
        opt2.timeZoneName = GetStringOption(cx, options, "timeZoneName", set("short", "long"), null);
        /* steps 20-21 */
        // not applicable
        /* step 22 */
        GetStringOption(cx, options, "formatMatcher", set("basic", "best fit"), "best fit");
        /* steps 23-25 */
        // not applicable
        /* step 26 */
        Boolean hr12 = GetBooleanOption(cx, options, "hour12", null);
        opt2.hour12 = hr12;
        /* steps 27-28 */
        String pattern = createPattern(opt2, dataLocale);
        /* step 29 */
        dateTimeFormat.setPattern(pattern);
        /* step 30 */
        dateTimeFormat.setBoundFormat(null);
        /* step 31 */
        dateTimeFormat.setInitializedDateTimeFormat(true);
    }

    /**
     * Abstract Operation: ToDateTimeOptions
     */
    public static ScriptObject ToDateTimeOptions(ExecutionContext cx, Object opts, String required,
            String defaults) {
        /* steps 1-3 */
        Object o = Type.isUndefined(opts) ? NULL : ToObject(cx, opts);
        ScriptObject options = (ScriptObject) ObjectConstructor.Properties.create(cx, UNDEFINED, o,
                UNDEFINED);
        /* step 4 */
        boolean needDefaults = true;
        /* step 5 */
        if ("date".equals(required) || "any".equals(required)) {
            for (String pk : asList("weekday", "year", "month", "day")) {
                Object kvalue = Get(cx, options, pk);
                if (!Type.isUndefined(kvalue)) {
                    needDefaults = false;
                }
            }
        }
        /* step 6 */
        if ("time".equals(required) || "any".equals(required)) {
            for (String pk : asList("hour", "minute", "second")) {
                Object kvalue = Get(cx, options, pk);
                if (!Type.isUndefined(kvalue)) {
                    needDefaults = false;
                }
            }
        }
        /* step 7 */
        if (needDefaults && ("date".equals(defaults) || "all".equals(defaults))) {
            for (String pk : asList("year", "month", "day")) {
                CreateOwnDataProperty(cx, options, pk, "numeric");
            }
        }
        /* step 8 */
        if (needDefaults && ("time".equals(defaults) || "all".equals(defaults))) {
            for (String pk : asList("hour", "minute", "second")) {
                CreateOwnDataProperty(cx, options, pk, "numeric");
            }
        }
        /* step 9 */
        return options;
    }

    private static final class FormatMatcherRecord {
        String weekday;
        String era;
        String year;
        String month;
        String day;
        String hour;
        String minute;
        String second;
        String timeZoneName;
        Boolean hour12;
    }

    private static String createPattern(FormatMatcherRecord opt, String dataLocale) {
        // Let ICU4J compute the best applicable pattern for the requested input values
        StringBuilder sb = new StringBuilder();
        PatternField.Weekday.append(sb, opt.weekday);
        PatternField.Era.append(sb, opt.era);
        PatternField.Year.append(sb, opt.year);
        PatternField.Month.append(sb, opt.month);
        PatternField.Day.append(sb, opt.day);
        PatternField.Hour.append(sb, opt.hour, opt.hour12);
        PatternField.Minute.append(sb, opt.minute);
        PatternField.Second.append(sb, opt.second);
        PatternField.TimeZone.append(sb, opt.timeZoneName);
        ULocale locale = ULocale.forLanguageTag(dataLocale);
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);
        String skeleton = sb.toString();
        return generator.getBestPattern(skeleton);
    }

    private enum PatternField {
        Era('G'), Year('y'), Month('M'), Day('d'), Weekday('E'), Hour('j') {
            @Override
            public void append(StringBuilder sb, String format, Boolean hour12) {
                char c = (hour12 != null ? hour12 ? 'h' : 'H' : character);
                for (int i = length(format); i != 0; --i) {
                    sb.append(c);
                }
            }
        },
        Minute('m'), Second('s'), TimeZone('z');

        protected final char character;

        private PatternField(char c) {
            this.character = c;
        }

        public void append(StringBuilder sb, String format, Boolean option) {
            throw new UnsupportedOperationException();
        }

        public void append(StringBuilder sb, String format) {
            for (int i = length(format); i != 0; --i) {
                sb.append(character);
            }
        }

        private static final int NARROW = 5, LONG = 4, SHORT = 3, TWO_DIGIT = 2, NUMERIC = 1;

        private static int length(String name) {
            if (name == null) {
                return 0;
            }
            switch (name) {
            case "narrow":
                return NARROW;
            case "long":
                return LONG;
            case "short":
                return SHORT;
            case "2-digit":
                return TWO_DIGIT;
            case "numeric":
                return NUMERIC;
            default:
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * 12.1.2.1 Intl.DateTimeFormat.call (this [, locales [, options]])
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        if (Type.isUndefined(thisValue) || thisValue == callerContext.getIntrinsic(Intrinsics.Intl)) {
            return construct(callerContext, args);
        }
        ScriptObject obj = ToObject(callerContext, thisValue);
        if (!IsExtensible(callerContext, obj)) {
            throwTypeError(callerContext, Messages.Key.NotExtensible);
        }
        InitializeDateTimeFormat(callerContext, obj, locales, options);
        return obj;
    }

    /**
     * 12.1.3.1 new Intl.DateTimeFormat ([locales [, options]])
     */
    @Override
    public Object construct(ExecutionContext callerContext, Object... args) {
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        DateTimeFormatObject obj = new DateTimeFormatObject(callerContext.getRealm());
        obj.setPrototype(callerContext,
                callerContext.getIntrinsic(Intrinsics.Intl_DateTimeFormatPrototype));
        InitializeDateTimeFormat(callerContext, obj, locales, options);
        return obj;
    }

    /**
     * 12.2 Properties of the Intl.DateTimeFormat Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "DateTimeFormat";

        /**
         * 12.2.1 Intl.DateTimeFormat.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_DateTimeFormatPrototype;

        /**
         * 12.2.2 Intl.DateTimeFormat.supportedLocalesOf (locales [, options])
         */
        @Function(name = "supportedLocalesOf", arity = 1)
        public static Object supportedLocalesOf(ExecutionContext cx, Object thisValue,
                Object locales, Object options) {
            Set<String> availableLocales = getAvailableLocales(cx);
            Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
            return SupportedLocales(cx, availableLocales, requestedLocales, options);
        }

        /**
         * Extension: Make subclassable for ES6 classes
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue,
                    Intrinsics.Intl_DateTimeFormatPrototype, DateTimeFormatObjectAllocator.INSTANCE);
        }
    }

    private static class DateTimeFormatObjectAllocator implements
            ObjectAllocator<DateTimeFormatObject> {
        static final ObjectAllocator<DateTimeFormatObject> INSTANCE = new DateTimeFormatObjectAllocator();

        @Override
        public DateTimeFormatObject newInstance(Realm realm) {
            return new DateTimeFormatObject(realm);
        }
    }
}
