/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataPropertyOrThrow;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.*;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Lazy;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.DateField;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.FieldWeight;
import com.github.anba.es6draft.runtime.objects.intl.DateFieldSymbolTable.Skeleton;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.ExtensionKey;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.LocaleData;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.LocaleDataInfo;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.OptionsRecord;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.ResolvedLocale;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
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
public final class DateTimeFormatConstructor extends BuiltinConstructor implements Initializable {
    /** [[availableLocales]] */
    private final Lazy<Set<String>> availableLocales = Lazy
            .of(() -> GetAvailableLocales(LanguageData.getAvailableDateFormatLocales()));

    /**
     * [[availableLocales]]
     * 
     * @param cx
     *            the execution context
     * @return the set of available locales supported by {@code Intl.DateTimeFormat}
     */
    public static Set<String> getAvailableLocales(ExecutionContext cx) {
        return getAvailableLocalesLazy(cx).get();
    }

    private static Lazy<Set<String>> getAvailableLocalesLazy(ExecutionContext cx) {
        DateTimeFormatConstructor dateTimeFormat = (DateTimeFormatConstructor) cx
                .getIntrinsic(Intrinsics.Intl_DateTimeFormat);
        return dateTimeFormat.availableLocales;
    }

    /** [[relevantExtensionKeys]] */
    private static final List<ExtensionKey> relevantExtensionKeys = asList(ExtensionKey.ca, ExtensionKey.nu);

    /**
     * Calendar algorithm keys (BCP 47; CLDR, version 28)
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
        islamic_umalqura("islamic-umalqura"),
        islamic_tbla("islamic-tbla"),
        // commented out, instead handled by "islamicc"
        /* islamic_civil("islamic-civil"), */
        islamic_rgsa("islamic-rgsa"),
        iso8601("iso8601"),
        japanese("japanese"),
        persian("persian"),
        roc("roc"),
        islamicc("islamicc", "islamic-civil"), // deprecated
        ;
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

        public String getAlias() {
            return alias;
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
        public String defaultValue(ExtensionKey extensionKey) {
            switch (extensionKey) {
            case ca:
                String[] values = Calendar.getKeywordValuesForLocale("calendar", locale, false);
                return CalendarAlgorithm.forName(values[0]).getName();
            case nu:
                return NumberingSystem.getInstance(locale).getName();
            default:
                throw new IllegalArgumentException(extensionKey.name());
            }
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
            ArrayList<String> result = new ArrayList<>(values.length);
            for (int i = 0, len = values.length; i < len; ++i) {
                String calendarName = values[i];
                // Ignore "unknown" calendar entry in result set
                if ("unknown".equals(calendarName)) {
                    continue;
                }
                CalendarAlgorithm algorithm = CalendarAlgorithm.forName(calendarName);
                result.add(algorithm.getName());
                if (algorithm.getAlias() != null) {
                    result.add(algorithm.getAlias());
                }
            }
            return result;
        }

        private List<String> getNumberInfo() {
            // ICU4J does not provide an API to retrieve the numbering systems per locale, go with
            // Spidermonkey instead and return default numbering system of locale + Table 2 entries
            String localeNumberingSystem = NumberingSystem.getInstance(locale).getName();
            return asList(localeNumberingSystem, "arab", "arabtext", "bali", "beng", "deva", "fullwide", "gujr", "guru",
                    "hanidec", "khmr", "knda", "laoo", "latn", "limb", "mlym", "mong", "mymr", "orya", "tamldec",
                    "telu", "thai", "tibt");
        }
    }

    /**
     * Constructs a new DateTimeFormat constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public DateTimeFormatConstructor(Realm realm) {
        super(realm, "DateTimeFormat", 0);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public DateTimeFormatConstructor clone() {
        return new DateTimeFormatConstructor(getRealm());
    }

    @SafeVarargs
    private static <T> T[] array(T... elements) {
        return elements;
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(asList(elements));
    }

    /**
     * 12.1.1 InitializeDateTimeFormat (dateTimeFormat, locales, options)
     * 
     * @param cx
     *            the execution context
     * @param dateTimeFormat
     *            the date format object
     * @param locales
     *            the locales array
     * @param opts
     *            the options object
     */
    public static void InitializeDateTimeFormat(ExecutionContext cx, DateTimeFormatObject dateTimeFormat,
            Object locales, Object opts) {
        /* steps 1-2 (FIXME: spec bug - unnecessary internal slot) */
        /* steps 3-4 */
        Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
        /* steps 5-6 */
        ScriptObject options = ToDateTimeOptions(cx, opts, "any", "date");
        /* steps 8-9 */
        String matcher = GetStringOption(cx, options, "localeMatcher", set("lookup", "best fit"), "best fit");
        /* step 7, 10 */
        OptionsRecord opt = new OptionsRecord(OptionsRecord.MatcherType.forName(matcher));
        /* step 11 */
        DateTimeFormatLocaleData localeData = new DateTimeFormatLocaleData();
        /* step 12 */
        ResolvedLocale r = ResolveLocale(cx.getRealm(), getAvailableLocalesLazy(cx), requestedLocales, opt,
                relevantExtensionKeys, localeData);
        /* step 13 */
        dateTimeFormat.setLocale(r.getLocale());
        /* step 14 */
        dateTimeFormat.setCalendar(r.getValue(ExtensionKey.ca));
        /* step 15 */
        dateTimeFormat.setNumberingSystem(r.getValue(ExtensionKey.nu));
        /* step 16 */
        String dataLocale = r.getDataLocale();
        /* steps 17-18 */
        Object tz = Get(cx, options, "timeZone");
        /* steps 19-20 */
        String timeZone;
        if (!Type.isUndefined(tz)) {
            timeZone = ToFlatString(cx, tz);
            if (!IsValidTimeZoneName(timeZone)) {
                throw newRangeError(cx, Messages.Key.IntlInvalidOption, timeZone);
            }
            timeZone = CanonicalizeTimeZoneName(timeZone);
        } else {
            timeZone = DefaultTimeZone(cx.getRealm());
        }
        /* step 21 */
        dateTimeFormat.setTimeZone(timeZone);
        /* step 22 (moved) */
        /* step 23 */
        // FIXME: spec should propably define exact iteration order here
        String weekday = GetStringOption(cx, options, "weekday", set("narrow", "short", "long"), null);
        String era = GetStringOption(cx, options, "era", set("narrow", "short", "long"), null);
        String year = GetStringOption(cx, options, "year", set("2-digit", "numeric"), null);
        String month = GetStringOption(cx, options, "month", set("2-digit", "numeric", "narrow", "short", "long"),
                null);
        String day = GetStringOption(cx, options, "day", set("2-digit", "numeric"), null);
        String hour = GetStringOption(cx, options, "hour", set("2-digit", "numeric"), null);
        String minute = GetStringOption(cx, options, "minute", set("2-digit", "numeric"), null);
        String second = GetStringOption(cx, options, "second", set("2-digit", "numeric"), null);
        String timeZoneName = GetStringOption(cx, options, "timeZoneName", set("short", "long"), null);
        /* steps 26-27 */
        String formatMatcher = GetStringOption(cx, options, "formatMatcher", set("basic", "best fit"), "best fit");
        /* steps 32-33 */
        Boolean hour12 = GetBooleanOption(cx, options, "hour12", null);
        /* steps 22, 24-25, 28-31, 34-35 */
        FormatMatcherRecord formatRecord = new FormatMatcherRecord(weekday, era, year, month, day, hour, minute, second,
                timeZoneName, hour12);
        Lazy<String> pattern;
        if ("basic".equals(formatMatcher)) {
            pattern = new BasicFormatPattern(formatRecord, dataLocale);
        } else {
            pattern = new BestFitFormatPattern(formatRecord, dataLocale);
        }
        /* step 36 */
        dateTimeFormat.setPattern(pattern);
        /* step 37 */
        dateTimeFormat.setBoundFormat(null);
        /* step 38 (FIXME: spec bug - unnecessary internal slot) */
        /* step 39 (omitted) */
    }

    /**
     * 12.1.1 InitializeDateTimeFormat (dateTimeFormat, locales, options)
     * 
     * @param realm
     *            the realm instance
     * @param dateTimeFormat
     *            the date format object
     */
    public static void InitializeDefaultDateTimeFormat(Realm realm, DateTimeFormatObject dateTimeFormat) {
        /* steps 1-2 (FIXME: spec bug - unnecessary internal slot) */
        /* steps 3-10 (not applicable) */
        /* step 11 */
        DateTimeFormatLocaleData localeData = new DateTimeFormatLocaleData();
        /* step 12 */
        ResolvedLocale r = ResolveDefaultLocale(realm, relevantExtensionKeys, localeData);
        /* step 13 */
        dateTimeFormat.setLocale(r.getLocale());
        /* step 14 */
        dateTimeFormat.setCalendar(r.getValue(ExtensionKey.ca));
        /* step 15 */
        dateTimeFormat.setNumberingSystem(r.getValue(ExtensionKey.nu));
        /* step 16 */
        String dataLocale = r.getDataLocale();
        /* steps 17-21 */
        dateTimeFormat.setTimeZone(DefaultTimeZone(realm));
        /* steps 22-35 */
        FormatMatcherRecord formatRecord = new FormatMatcherRecord(null, null, "numeric", "numeric", "numeric", null,
                null, null, null, null);
        Lazy<String> pattern = new BestFitFormatPattern(formatRecord, dataLocale);
        /* step 36 */
        dateTimeFormat.setPattern(pattern);
        /* step 37 */
        dateTimeFormat.setBoundFormat(null);
        /* step 38 (FIXME: spec bug - unnecessary internal slot) */
        /* step 39 (omitted) */
    }

    /**
     * Abstract Operation: ToDateTimeOptions
     * 
     * @param cx
     *            the execution context
     * @param opts
     *            the options object
     * @param required
     *            the required date field
     * @param defaults
     *            the default date field
     * @return the date-time options script object
     */
    public static ScriptObject ToDateTimeOptions(ExecutionContext cx, Object opts, String required, String defaults) {
        /* steps 1-3 */
        OrdinaryObject options = ObjectCreate(cx, Type.isUndefined(opts) ? null : ToObject(cx, opts));
        /* step 4 */
        boolean needDefaults = true;
        /* step 5 */
        if ("date".equals(required) || "any".equals(required)) {
            // FIXME: spec vs. impl (short circuit after first undefined value?)
            for (String prop : array("weekday", "year", "month", "day")) {
                Object kvalue = Get(cx, options, prop);
                if (!Type.isUndefined(kvalue)) {
                    needDefaults = false;
                }
            }
        }
        /* step 6 */
        if ("time".equals(required) || "any".equals(required)) {
            // FIXME: spec vs. impl (short circuit after first undefined value?)
            for (String prop : array("hour", "minute", "second")) {
                Object kvalue = Get(cx, options, prop);
                if (!Type.isUndefined(kvalue)) {
                    needDefaults = false;
                }
            }
        }
        /* step 7 */
        if (needDefaults && ("date".equals(defaults) || "all".equals(defaults))) {
            for (String prop : array("year", "month", "day")) {
                CreateDataPropertyOrThrow(cx, options, prop, "numeric");
            }
        }
        /* step 8 */
        if (needDefaults && ("time".equals(defaults) || "all".equals(defaults))) {
            for (String prop : array("hour", "minute", "second")) {
                CreateDataPropertyOrThrow(cx, options, prop, "numeric");
            }
        }
        /* step 9 */
        return options;
    }

    private static final class BasicFormatPattern extends Lazy<String> {
        private final FormatMatcherRecord record;
        private final String dataLocale;

        BasicFormatPattern(FormatMatcherRecord record, String dataLocale) {
            this.record = record;
            this.dataLocale = dataLocale;
        }

        @Override
        protected String computeValue() {
            return BasicFormatMatcher(record, dataLocale);
        }
    }

    private static final class BestFitFormatPattern extends Lazy<String> {
        private final FormatMatcherRecord record;
        private final String dataLocale;

        BestFitFormatPattern(FormatMatcherRecord record, String dataLocale) {
            this.record = record;
            this.dataLocale = dataLocale;
        }

        @Override
        protected String computeValue() {
            return BestFitFormatMatcher(record, dataLocale);
        }
    }

    private static final class FormatMatcherRecord {
        private final FieldWeight weekday;
        private final FieldWeight era;
        private final FieldWeight year;
        private final FieldWeight month;
        private final FieldWeight day;
        private final FieldWeight hour;
        private final FieldWeight minute;
        private final FieldWeight second;
        private final FieldWeight timeZoneName;
        private final Boolean hour12;

        FormatMatcherRecord(String weekday, String era, String year, String month, String day, String hour,
                String minute, String second, String timeZoneName, Boolean hour12) {
            this.weekday = FieldWeight.forName(weekday);
            this.era = FieldWeight.forName(era);
            this.year = FieldWeight.forName(year);
            this.month = FieldWeight.forName(month);
            this.day = FieldWeight.forName(day);
            this.hour = FieldWeight.forName(hour);
            this.minute = FieldWeight.forName(minute);
            this.second = FieldWeight.forName(second);
            this.timeZoneName = FieldWeight.forName(timeZoneName);
            this.hour12 = hour12;
        }

        boolean isDate() {
            return (year != null || month != null || day != null);
        }

        boolean isTime() {
            return (hour != null || minute != null || second != null);
        }

        boolean isHour12(ULocale locale) {
            if (hour12 != null) {
                return hour12;
            }
            char hourFormat = defaultHourFormat(locale);
            return (hourFormat == 'h' || hourFormat == 'K');
        }

        FieldWeight getWeight(DateField field) {
            switch (field) {
            case Era:
                return era;
            case Year:
                return year;
            case Quarter:
                return null;
            case Month:
                return month;
            case Week:
                return null;
            case Day:
                return day;
            case Weekday:
                return weekday;
            case Period:
                return null;
            case Hour:
                return hour;
            case Minute:
                return minute;
            case Second:
                return second;
            case Timezone:
                return timeZoneName;
            default:
                throw new AssertionError();
            }
        }

        String toSkeleton() {
            StringBuilder sb = new StringBuilder();
            DateField.Weekday.append(sb, weekday);
            DateField.Era.append(sb, era);
            DateField.Year.append(sb, year);
            DateField.Month.append(sb, month);
            DateField.Day.append(sb, day);
            DateField.Hour.append(sb, hour, hour12);
            DateField.Minute.append(sb, minute);
            DateField.Second.append(sb, second);
            DateField.Timezone.append(sb, timeZoneName);
            return sb.toString();
        }
    }

    /**
     * Abstract Operation: BasicFormatMatcher
     * 
     * @param formatRecord
     *            the format matcher record
     * @param dataLocale
     *            the locale
     * @return the basic format matcher
     */
    public static String BasicFormatMatcher(FormatMatcherRecord formatRecord, String dataLocale) {
        ULocale locale = ULocale.forLanguageTag(dataLocale);
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);

        // ICU4J only provides access to date- or time-only skeletons, with the exception of the
        // weekday property, which may also appear in time-only skeletons or as a single skeleton
        // property. That means we need to handle four different cases:
        // 1) formatRecord contains only date properties
        // 2) formatRecord contains only time properties
        // 3) formatRecord contains date and time properties
        // 4) formatRecord contains only the weekday property
        boolean optDate = formatRecord.isDate();
        boolean optTime = formatRecord.isTime();
        boolean optDateTime = optDate && optTime;

        // get the preferred hour representation (12-hour-cycle or 24-hour-cycle)
        boolean optHour12 = formatRecord.isHour12(locale);

        // handle date and time patterns separately
        int bestDateScore = Integer.MIN_VALUE, bestTimeScore = Integer.MIN_VALUE;
        String bestDateFormat = null, bestTimeFormat = null;

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
                    int score = computeScore(formatRecord, skeleton);
                    if (score > bestDateScore) {
                        bestDateScore = score;
                        bestDateFormat = entry.getValue();
                    }
                } else {
                    int score = computeScore(formatRecord, skeleton);
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
                int score = computeScore(formatRecord, skeleton);
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
                int score = computeScore(formatRecord, skeleton);
                if (score > bestTimeScore) {
                    bestTimeScore = score;
                    bestTimeFormat = entry.getValue();
                }
            } else {
                // weekday-only case
                int score = computeScore(formatRecord, skeleton);
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
     * @param locale
     *            the locale
     * @return the hour format character
     * @see <a href="http://bugs.icu-project.org/trac/ticket/9997">ICU bug 9997</a>
     */
    private static char defaultHourFormat(ULocale locale) {
        // use short time format, just as ICU4J does internally
        final int style = DateFormat.SHORT;
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
     * Adds canonical skeleton/pattern pairs which might have been omitted in
     * {@link DateTimePatternGenerator#getSkeletons(Map)}.
     * 
     * @param skeletons
     *            the skeletons map
     * @return the updated skeletons map
     */
    private static Map<String, String> addCanonicalSkeletons(Map<String, String> skeletons) {
        final String source = "GyQMwWEdDFHmsSv"; // see DateTimePatternGenerator#CANONICAL_ITEMS
        for (int i = 0, len = source.length(); i < len; ++i) {
            String k = String.valueOf(source.charAt(i));
            if (!skeletons.containsKey(k)) {
                skeletons.put(k, k);
            }
        }
        return skeletons;
    }

    /**
     * Abstract Operation: BasicFormatMatcher (score computation)
     * 
     * @param formatRecord
     *            the format matcher record
     * @param skeleton
     *            the pattern skeleton
     * @return the computed score
     */
    private static int computeScore(FormatMatcherRecord formatRecord, Skeleton skeleton) {
        /* step 12.b */
        int score = 0;
        /* step 12.c */
        score -= computePenalty(formatRecord, skeleton, DateField.Weekday).value();
        score -= computePenalty(formatRecord, skeleton, DateField.Era).value();
        score -= computePenalty(formatRecord, skeleton, DateField.Year).value();
        score -= computePenalty(formatRecord, skeleton, DateField.Month).value();
        score -= computePenalty(formatRecord, skeleton, DateField.Day).value();
        score -= computePenalty(formatRecord, skeleton, DateField.Hour).value();
        score -= computePenalty(formatRecord, skeleton, DateField.Minute).value();
        score -= computePenalty(formatRecord, skeleton, DateField.Second).value();
        score -= computePenalty(formatRecord, skeleton, DateField.Timezone).value();
        return score;
    }

    private enum Penalty {
        Removal(120), Addition(20), LongLess(8), LongMore(6), ShortLess(6), ShortMore(3), None(0);

        private final int value;

        private Penalty(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * Abstract Operation: BasicFormatMatcher (penalty computation)
     * 
     * @param formatRecord
     *            the format matcher record
     * @param skeleton
     *            the pattern skeleton
     * @param field
     *            the date field
     * @return the computed penalty
     */
    private static Penalty computePenalty(FormatMatcherRecord formatRecord, Skeleton skeleton, DateField field) {
        FieldWeight optionsProp = formatRecord.getWeight(field);
        FieldWeight formatProp = skeleton.getWeight(field);
        /* step 12.c.iii */
        if (optionsProp == null && formatProp != null) {
            return Penalty.Addition;
        }
        /* step 12.c.iv */
        if (optionsProp != null && formatProp == null) {
            return Penalty.Removal;
        }
        /* step 12.c.v */
        if (optionsProp != formatProp) {
            int optionsPropIndex = optionsProp.index();
            int formatPropIndex = formatProp.index();
            int delta = Math.max(Math.min(formatPropIndex - optionsPropIndex, 2), -2);
            if (delta == 2) {
                return Penalty.LongMore;
            } else if (delta == 1) {
                return Penalty.ShortMore;
            } else if (delta == -1) {
                return Penalty.ShortLess;
            } else if (delta == -2) {
                return Penalty.LongLess;
            }
        }
        return Penalty.None;
    }

    /**
     * Abstract Operation: BestFitFormatMatcher
     * 
     * @param formatRecord
     *            the format matcher record
     * @param dataLocale
     *            the locale
     * @return the best applicable pattern
     */
    public static String BestFitFormatMatcher(FormatMatcherRecord formatRecord, String dataLocale) {
        // Let ICU4J compute the best applicable pattern for the requested input values
        ULocale locale = ULocale.forLanguageTag(dataLocale);
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);
        return generator.getBestPattern(formatRecord.toSkeleton());
    }

    /**
     * 12.1.2 Intl.DateTimeFormat([ locales [, options ]])
     */
    @Override
    public ScriptObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* steps 1-4 */
        return construct(callerContext, this, args);
    }

    /**
     * 12.1.2 Intl.DateTimeFormat([ locales [, options ]])
     */
    @Override
    public DateTimeFormatObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object locales = argument(args, 0);
        Object options = argument(args, 1);

        /* step 1 (not applicable) */
        /* steps 2-3 */
        DateTimeFormatObject obj = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.Intl_DateTimeFormatPrototype, DateTimeFormatObject::new);
        /* step 4 */
        InitializeDateTimeFormat(calleeContext, obj, locales, options);
        return obj;
    }

    /**
     * 12.2 Properties of the Intl.DateTimeFormat Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "DateTimeFormat";

        /**
         * 12.2.1 Intl.DateTimeFormat.prototype
         */
        @Value(name = "prototype",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_DateTimeFormatPrototype;

        /**
         * 12.2.2 Intl.DateTimeFormat.supportedLocalesOf (locales [, options])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the locales array
         * @param options
         *            the options object
         * @return the array of supported locales
         */
        @Function(name = "supportedLocalesOf", arity = 1)
        public static Object supportedLocalesOf(ExecutionContext cx, Object thisValue, Object locales, Object options) {
            /* step 1 */
            Set<String> availableLocales = getAvailableLocales(cx);
            /* steps 2-3 */
            Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
            /* step 4 */
            return SupportedLocales(cx, availableLocales, requestedLocales, options);
        }
    }
}
