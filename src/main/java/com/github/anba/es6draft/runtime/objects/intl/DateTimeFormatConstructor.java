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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.DateField;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.FieldWeight;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.Skeleton;
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
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.text.SimpleDateFormat;
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

    /**
     * Calendar algorithm keys (BCP 47; CLDR, version 23)
     */
    private enum CalendarAlgorithm {/* @formatter:off */
        buddhist("buddhist"),
        chinese("chinese"),
        coptic("coptic"),
        dangi("dangi"),
        ethioaa("ethioaa", "ethiopic-amete-alem"),
        ethiopic("ethiopic"),
        gregory("gregory", "gregorian"),
        hebrew("hebrew"),
        indian("indian"),
        islamic("islamic"),
        islamicc("islamicc", "islamic-civil"),
        iso8601("iso8601"),
        japanese("japanese"),
        persian("persian"),
        roc("roc");
        /* @formatter:on */

        private final String name;
        private final String alias;

        private CalendarAlgorithm(String name) {
            this.name = name;
            this.alias = null;
        }

        private CalendarAlgorithm(String name, String alias) {
            this.name = name;
            this.alias = alias;
        }

        public String getName() {
            return name;
        }

        public static CalendarAlgorithm forName(String name) {
            for (CalendarAlgorithm co : values()) {
                if (name.equals(co.name) || name.equals(co.alias)) {
                    return co;
                }
            }
            throw new IllegalArgumentException(name);
        }
    }

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

        public DateTimeFormatLocaleDataInfo(ULocale locale) {
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
            String[] values = Calendar.getKeywordValuesForLocale("calendar", locale, false);
            List<String> result = new ArrayList<>(values.length);
            for (int i = 0, len = values.length; i < len; ++i) {
                CalendarAlgorithm algorithm = CalendarAlgorithm.forName(values[i]);
                result.add(algorithm.getName());
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
        return new HashSet<>(asList(elements));
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
        String timeZone;
        Object tz = Get(cx, options, "timeZone");
        if (!Type.isUndefined(tz)) {
            timeZone = ToFlatString(cx, tz);
            if (!IsValidTimeZoneName(timeZone)) {
                throw throwRangeError(cx, Messages.Key.IntlInvalidOption, timeZone);
            }
            timeZone = CanonicalizeTimeZoneName(timeZone);
        } else {
            timeZone = DefaultTimeZone(cx.getRealm());
        }
        dateTimeFormat.setTimeZone(timeZone);
        /* step 18 */
        FormatMatcherRecord opt2 = new FormatMatcherRecord();
        /* step 19 */
        // FIXME: spec should propably define exact iteration order here
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
        matcher = GetStringOption(cx, options, "formatMatcher", set("basic", "best fit"),
                "best fit");
        /* steps 25 */
        // not applicable
        /* step 26 */
        Boolean hr12 = GetBooleanOption(cx, options, "hour12", null);
        opt2.hour12 = hr12;
        /* steps 23-24, 27-28 */
        String pattern;
        if ("basic".equals(matcher)) {
            pattern = BasicFormatMatcher(opt2, dataLocale);
        } else {
            pattern = BestFitFormatMatcher(opt2, dataLocale);
        }
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
            // FIXME: spec vs. impl (short circuit after first undefined value?)
            for (String pk : asList("weekday", "year", "month", "day")) {
                Object kvalue = Get(cx, options, pk);
                if (!Type.isUndefined(kvalue)) {
                    needDefaults = false;
                }
            }
        }
        /* step 6 */
        if ("time".equals(required) || "any".equals(required)) {
            // FIXME: spec vs. impl (short circuit after first undefined value?)
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

        boolean isDate() {
            return (year != null || month != null || day != null);
        }

        boolean isTime() {
            return (hour != null || minute != null || second != null);
        }
    }

    /**
     * Abstract Operation: BasicFormatMatcher
     */
    public static String BasicFormatMatcher(FormatMatcherRecord opt, String dataLocale) {
        // ICU4J only provides access to date or time-only skeletons, with the exception of the
        // weekday property, which may also appear in time-only skeletons or as a single skeleton
        // property. That means we want to handle four different cases:
        // 1) opt contains only date properties
        // 2) opt contains only time properties
        // 3) opt contains date and time properties
        // 4) opt contains only the weekday property
        boolean optDate = opt.isDate(), optTime = opt.isTime(), optDateTime = optDate && optTime;

        // handle date and time patterns separately
        int bestDateScore = Integer.MIN_VALUE, bestTimeScore = Integer.MIN_VALUE;
        String bestDateFormat = null, bestTimeFormat = null;

        ULocale locale = ULocale.forLanguageTag(dataLocale);
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);

        // get the preferred hour representation (12-hour-cycle or 24-hour-cycle)
        char hourFormat = defaultHourFormat(locale);
        boolean hour12 = (hourFormat == 'h' || hourFormat == 'K');
        boolean optHour12 = (opt.hour12 != null ? opt.hour12 : hour12);

        Map<String, String> skeletons = addCanonicalSkeletons(generator.getSkeletons(null));
        for (Map.Entry<String, String> entry : skeletons.entrySet()) {
            Skeleton skeleton = new Skeleton(entry.getKey());
            // getSkeletons() does not return any date+time skeletons
            assert !(skeleton.isDate() && skeleton.isTime());
            // skip skeleton if it contains unsupported fields
            if (skeleton.has(DateField.Quarter) || skeleton.has(DateField.Week)) {
                continue;
            }
            if (skeleton.has(DateField.Year) && skeleton.getSymbol(DateField.Year) != 'y') {
                continue;
            }
            if (skeleton.has(DateField.Day) && skeleton.getSymbol(DateField.Day) != 'd') {
                continue;
            }
            if (skeleton.has(DateField.Second) && skeleton.getSymbol(DateField.Second) != 's') {
                continue;
            }
            if (optDateTime) {
                // skip time-skeletons with weekdays if date+time was requested, weekday gets into
                // the date-skeleton part
                if (skeleton.isTime() && skeleton.has(DateField.Weekday)) {
                    continue;
                }
                // skip time-skeleton if hour representation does not match requested value
                if (skeleton.isTime() && skeleton.isHour12() != optHour12) {
                    continue;
                }
                if (skeleton.isDate()) {
                    int score = computeScore(opt, skeleton);
                    if (score > bestDateScore) {
                        bestDateScore = score;
                        bestDateFormat = entry.getValue();
                    }
                } else {
                    int score = computeScore(opt, skeleton);
                    if (score > bestTimeScore) {
                        bestTimeScore = score;
                        bestTimeFormat = entry.getValue();
                    }
                }
            } else if (optDate) {
                // skip time-skeletons if only date fields were requested
                if (skeleton.isTime()) {
                    continue;
                }
                int score = computeScore(opt, skeleton);
                if (score > bestDateScore) {
                    bestDateScore = score;
                    bestDateFormat = entry.getValue();
                }
            } else if (optTime) {
                // skip date-skeletons if only time fields were requested
                if (skeleton.isDate()) {
                    continue;
                }
                // skip time-skeleton if hour representation does not match requested value
                if (skeleton.isHour12() != optHour12) {
                    continue;
                }
                int score = computeScore(opt, skeleton);
                if (score > bestTimeScore) {
                    bestTimeScore = score;
                    bestTimeFormat = entry.getValue();
                }
            } else {
                // weekday-only case
                int score = computeScore(opt, skeleton);
                if (score > bestDateScore) {
                    bestDateScore = score;
                    bestDateFormat = entry.getValue();
                }
            }
        }
        assert !optDate || bestDateFormat != null;
        assert !optTime || bestTimeFormat != null;
        assert !(!optDate && !optTime) || bestDateFormat != null;
        if (optDateTime) {
            String dateTimeFormat = generator.getDateTimeFormat();
            return MessageFormat.format(dateTimeFormat, bestTimeFormat, bestDateFormat);
        }
        if (optTime) {
            return bestTimeFormat;
        }
        return bestDateFormat;
    }

    /**
     * Retrieve the default hour format character for the supplied locale.
     * 
     * @see <a href="http://bugs.icu-project.org/trac/ticket/9997">ICU bug 9997</a>
     */
    private static char defaultHourFormat(ULocale locale) {
        // use short time format, just as ICU4J does internally
        int style = DateFormat.SHORT;
        SimpleDateFormat df = (SimpleDateFormat) DateFormat.getTimeInstance(style, locale);
        String pattern = df.toPattern();
        boolean quote = false;
        for (int i = 0, len = pattern.length(); i < len; ++i) {
            char c = pattern.charAt(i);
            if (!quote && (c == 'h' || c == 'H' || c == 'k' || c == 'K')) {
                return c;
            } else if (c == '\'') {
                quote = !quote;
            }
        }
        return 'H';
    }

    /**
     * Add canonical skeleton/pattern pairs which might have been omitted in
     * {@link DateTimePatternGenerator#getSkeletons(Map)}
     */
    private static Map<String, String> addCanonicalSkeletons(Map<String, String> skeletons) {
        String source = "GyQMwWEdDFHmsSv";
        for (int i = 0, len = source.length(); i < len; ++i) {
            String k = source.substring(i, i + 1);
            if (!skeletons.containsKey(k)) {
                skeletons.put(k, k);
            }
        }
        return skeletons;
    }

    /**
     * Abstract Operation: BasicFormatMatcher (score computation)
     */
    private static int computeScore(FormatMatcherRecord opt, Skeleton skeleton) {
        /* step 11.b */
        int score = 0;
        /* steps 11.c.i - 11.c.iv */
        score -= getPenalty(DateField.Weekday, opt.weekday, skeleton);
        score -= getPenalty(DateField.Era, opt.era, skeleton);
        score -= getPenalty(DateField.Year, opt.year, skeleton);
        score -= getPenalty(DateField.Month, opt.month, skeleton);
        score -= getPenalty(DateField.Day, opt.day, skeleton);
        score -= getPenalty(DateField.Hour, opt.hour, skeleton);
        score -= getPenalty(DateField.Minute, opt.minute, skeleton);
        score -= getPenalty(DateField.Second, opt.second, skeleton);
        score -= getPenalty(DateField.Timezone, opt.timeZoneName, skeleton);
        return score;
    }

    private static final int removalPenalty = 120, additionPenalty = 20, longLessPenalty = 8,
            longMorePenalty = 6, shortLessPenalty = 6, shortMorePenalty = 3;

    /**
     * Abstract Operation: BasicFormatMatcher (penalty computation)
     */
    private static int getPenalty(DateField field, String weight, Skeleton skeleton) {
        FieldWeight optionsProp = FieldWeight.forName(weight);
        FieldWeight formatProp = skeleton.getWeight(field);
        /* step 11.c.v */
        if (optionsProp == null && formatProp != null) {
            return additionPenalty;
        }
        /* step 11.c.vi */
        if (optionsProp != null && formatProp == null) {
            return removalPenalty;
        }
        /* step 11.c.vii */
        if (optionsProp != formatProp) {
            int optionsPropIndex = optionsProp.weight();
            int formatPropIndex = formatProp.weight();
            int delta = Math.max(Math.min(formatPropIndex - optionsPropIndex, 2), -2);
            if (delta == 2) {
                return longMorePenalty;
            } else if (delta == 1) {
                return shortMorePenalty;
            } else if (delta == -1) {
                return shortLessPenalty;
            } else if (delta == -2) {
                return longLessPenalty;
            }
        }
        return 0;
    }

    /**
     * Abstract Operation: BestFitFormatMatcher
     */
    public static String BestFitFormatMatcher(FormatMatcherRecord opt, String dataLocale) {
        // Let ICU4J compute the best applicable pattern for the requested input values
        StringBuilder sb = new StringBuilder();
        DateField.Weekday.append(sb, opt.weekday);
        DateField.Era.append(sb, opt.era);
        DateField.Year.append(sb, opt.year);
        DateField.Month.append(sb, opt.month);
        DateField.Day.append(sb, opt.day);
        DateField.Hour.append(sb, opt.hour, opt.hour12);
        DateField.Minute.append(sb, opt.minute);
        DateField.Second.append(sb, opt.second);
        DateField.Timezone.append(sb, opt.timeZoneName);
        ULocale locale = ULocale.forLanguageTag(dataLocale);
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);
        String skeleton = sb.toString();
        return generator.getBestPattern(skeleton);
    }

    /**
     * 12.1.2.1 Intl.DateTimeFormat.call (this [, locales [, options]])
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = realm().defaultContext();
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        if (Type.isUndefined(thisValue) || thisValue == calleeContext.getIntrinsic(Intrinsics.Intl)) {
            return construct(calleeContext, args);
        }
        ScriptObject obj = ToObject(calleeContext, thisValue);
        if (!IsExtensible(calleeContext, obj)) {
            throwTypeError(calleeContext, Messages.Key.NotExtensible);
        }
        InitializeDateTimeFormat(calleeContext, obj, locales, options);
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
