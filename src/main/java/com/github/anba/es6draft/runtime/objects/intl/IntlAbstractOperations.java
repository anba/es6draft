/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Lazy;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.OptionsRecord.MatcherType;
import com.github.anba.es6draft.runtime.objects.intl.LanguageTagParser.LanguageTag;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZone.SystemTimeZoneType;
import com.ibm.icu.util.ULocale;

/**
 * <h1>9 Locale and Parameter Negotiation</h1><br>
 * <h2>9.2 Abstract Operations</h2>
 */
public final class IntlAbstractOperations {
    private IntlAbstractOperations() {
    }

    /**
     * 6.1 Case Sensitivity and Case Mapping
     * 
     * @param s
     *            the string
     * @return the upper case string
     */
    public static String ToUpperCase(String s) {
        char[] ca = s.toCharArray();
        for (int i = 0, len = ca.length; i < len; ++i) {
            char c = ca[i];
            if (c >= 'a' && c <= 'z') {
                c = (char) ('A' + (c - 'a'));
            }
            ca[i] = c;
        }
        return String.valueOf(ca);
    }

    /**
     * 6.2.1 Unicode Locale Extension Sequences
     * 
     * @param languageTag
     *            the canonicalized language tag
     * @return the unicode extension sequences
     */
    private static String[] UnicodeLocaleExtSequence(String languageTag) {
        unicodeExt: {
            if (languageTag.startsWith("x-")) {
                // privateuse-only case
                break unicodeExt;
            }
            int indexUnicode = languageTag.indexOf("-u-");
            if (indexUnicode == -1) {
                // no unicode extension
                break unicodeExt;
            }
            int indexPrivateUse = languageTag.lastIndexOf("-x-", indexUnicode);
            if (indexPrivateUse != -1) {
                // -u- in privateuse case
                break unicodeExt;
            }
            // found unicode extension, search end index
            int endIndex = languageTag.length();
            for (int i = indexUnicode + 3;;) {
                int sep = languageTag.indexOf('-', i);
                if (sep == -1) {
                    // end of string reached
                    break;
                }
                assert sep + 2 < languageTag.length() : languageTag;
                if (languageTag.charAt(sep + 2) == '-') {
                    // next singleton found
                    endIndex = sep;
                    break;
                }
                i = sep + 1;
            }
            String noExtension = languageTag.substring(0, indexUnicode)
                    + languageTag.substring(endIndex);
            String extension = languageTag.substring(indexUnicode, endIndex);
            return new String[] { noExtension, extension };
        }
        return new String[] { languageTag, "" };
    }

    /**
     * 6.2.2 IsStructurallyValidLanguageTag (locale)
     * 
     * @param locale
     *            the locale string
     * @return the parsed language tag
     */
    public static LanguageTag IsStructurallyValidLanguageTag(String locale) {
        return new LanguageTagParser(locale).parse();
    }

    /**
     * 6.2.3 CanonicalizeLanguageTag (locale)
     * 
     * @param locale
     *            the language tag
     * @return the canonicalized language tag
     */
    public static String CanonicalizeLanguageTag(LanguageTag locale) {
        return locale.canonicalize();
    }

    /**
     * 6.2.4 DefaultLocale ()
     * 
     * @param realm
     *            the realm instance
     * @return the default locale
     */
    public static String DefaultLocale(Realm realm) {
        return realm.getLocale().toLanguageTag();
    }

    /**
     * 6.3.1 IsWellFormedCurrencyCode (currency)
     * 
     * @param cx
     *            the execution context
     * @param currency
     *            the currency string
     * @return {@code true} if the currency string is well formed
     */
    public static boolean IsWellFormedCurrencyCode(ExecutionContext cx, Object currency) {
        String normalized = ToFlatString(cx, currency);
        if (normalized.length() != 3) {
            return false;
        }
        return isAlpha(normalized.charAt(0)) && isAlpha(normalized.charAt(1))
                && isAlpha(normalized.charAt(2));
    }

    private static final boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static final Set<String> JDK_TIMEZONE_NAMES = set("ACT", "AET", "AGT", "ART", "AST",
            "BET", "BST", "CAT", "CNT", "CST", "CTT", "EAT", "ECT", "IET", "IST", "JST", "MIT",
            "NET", "NST", "PLT", "PNT", "PRT", "PST", "SST", "VST");

    private static final Lazy<Map<String, String>> timezones = new Lazy<Map<String, String>>() {
        @Override
        protected Map<String, String> computeValue() {
            HashMap<String, String> map = new HashMap<>();
            Set<String> ids = TimeZone.getAvailableIDs(SystemTimeZoneType.ANY, null, null);
            for (String id : ids) {
                if (JDK_TIMEZONE_NAMES.contains(id)) {
                    // ignore non-IANA, JDK-specific timezones
                    continue;
                }
                map.put(ToUpperCase(id), id);
            }
            return map;
        }
    };

    /**
     * 6.4.1 IsValidTimeZoneName (timeZone)
     * 
     * @param timeZone
     *            the time zone name
     * @return {@code true} if the time zone name is valid
     */
    public static boolean IsValidTimeZoneName(String timeZone) {
        return timezones.get().containsKey(ToUpperCase(timeZone));
    }

    /**
     * 6.4.2 CanonicalizeTimeZoneName (timeZone)
     * 
     * @param timeZone
     *            the time zone name
     * @return the canonicalized time zone name
     */
    public static String CanonicalizeTimeZoneName(String timeZone) {
        /* step 1 */
        String ianaTimeZone = timezones.get().get(ToUpperCase(timeZone));
        /* step 2 */
        ianaTimeZone = TimeZone.getCanonicalID(ianaTimeZone);
        assert ianaTimeZone != null : "invalid timezone: " + timeZone;
        /* step 3 */
        if ("Etc/UTC".equals(ianaTimeZone) || "Etc/GMT".equals(ianaTimeZone)) {
            return "UTC";
        }
        /* step 4 */
        return ianaTimeZone;
    }

    /**
     * 6.4.3 DefaultTimeZone ()
     * 
     * @param realm
     *            the realm instance
     * @return the default time zone
     */
    public static String DefaultTimeZone(Realm realm) {
        return realm.getTimezone().getID();
    }

    private static final Map<String, String[]> oldStyleLanguageTags;
    static {
        // generated from CLDR-2.0.0
        HashMap<String, String[]> map = new HashMap<>();
        map.put("az-Latn-AZ", new String[] { "az-AZ" });
        map.put("ha-Latn-GH", new String[] { "ha-GH" });
        map.put("ha-Latn-NE", new String[] { "ha-NE" });
        map.put("ha-Latn-NG", new String[] { "ha-NG" });
        map.put("kk-Cyrl-KZ", new String[] { "kk-KZ" });
        map.put("ku-Arab-IQ", new String[] { "ku-IQ" });
        map.put("ku-Arab-IR", new String[] { "ku-IR" });
        map.put("ku-Latn-SY", new String[] { "ku-SY" });
        map.put("ku-Latn-TR", new String[] { "ku-TR" });
        map.put("mn-Mong-CN", new String[] { "mn-CN" });
        map.put("mn-Cyrl-MN", new String[] { "mn-MN" });
        map.put("pa-Guru-IN", new String[] { "pa-IN" });
        map.put("pa-Arab-PK", new String[] { "pa-PK" });
        map.put("shi-Latn-MA", new String[] { "shi-MA" });
        map.put("sr-Latn-BA", new String[] { "sh-BA" });
        map.put("sr-Latn-RS", new String[] { "sh-CS", "sh-YU" });
        map.put("sr-Cyrl-BA", new String[] { "sr-BA" });
        map.put("sr-Cyrl-RS", new String[] { "sr-CS", "sr-RS", "sr-YU" });
        map.put("sr-Latn-ME", new String[] { "sr-ME" });
        map.put("tg-Cyrl-TJ", new String[] { "tg-TJ" });
        map.put("fil-PH", new String[] { "tl-PH" });
        map.put("tzm-Latn-MA", new String[] { "tzm-MA" });
        map.put("uz-Arab-AF", new String[] { "uz-AF" });
        map.put("uz-Cyrl-UZ", new String[] { "uz-UZ" });
        map.put("vai-Vaii-LR", new String[] { "vai-LR" });
        map.put("zh-Hans-CN", new String[] { "zh-CN" });
        map.put("zh-Hant-HK", new String[] { "zh-HK" });
        map.put("zh-Hant-MO", new String[] { "zh-MO" });
        map.put("zh-Hans-SG", new String[] { "zh-SG" });
        map.put("zh-Hant-TW", new String[] { "zh-TW" });
        oldStyleLanguageTags = map;
    }

    /**
     * 9.1 Internal Properties of Service Constructors
     * 
     * @param locales
     *            the supported locales
     * @return the set of available locales
     */
    public static Set<String> GetAvailableLocales(ULocale[] locales) {
        Map<String, String[]> oldTags = oldStyleLanguageTags;
        HashSet<String> set = new HashSet<>(locales.length);
        for (ULocale locale : locales) {
            String tag = locale.toLanguageTag();
            set.add(tag);
            if (oldTags.containsKey(tag)) {
                for (String old : oldTags.get(tag)) {
                    set.add(old);
                }
            }
        }
        return set;
    }

    public enum ExtensionKey {
        // Collator
        co, kn, kf,
        // NumberFormat
        nu,
        // DateTimeFormat
        ca, /* nu */
        ;

        private static ExtensionKey forName(String name, int index) {
            assert index + 2 <= name.length();
            char c0 = name.charAt(index), c1 = name.charAt(index + 1);
            if (c0 == 'c') {
                return c1 == 'a' ? ca : c1 == 'o' ? co : null;
            }
            if (c0 == 'k') {
                return c1 == 'f' ? kf : c1 == 'n' ? kn : null;
            }
            return c0 == 'n' && c1 == 'u' ? nu : null;
        }
    }

    private static EnumMap<ExtensionKey, String> unicodeLocaleExtensions(String extension) {
        /*
         * http://unicode.org/reports/tr35/#Unicode_locale_identifier
         *
         * unicode_locale_extensions = sep "u" (1*(sep keyword) / 1*(sep attribute) *(sep keyword))
         * keyword = key [sep type]
         * key = 2alphanum
         * type = 3*8alphanum *(sep 3*8alphanum)
         * attribute = 3*8alphanum
         */
        final int KEY_LENGTH = 2;
        final int SEP_LENGTH = 1;
        final int KEY_SEP_LENGTH = KEY_LENGTH + SEP_LENGTH;

        assert extension.startsWith("-u-") && extension.length() >= 3 + KEY_LENGTH : extension;
        EnumMap<ExtensionKey, String> map = new EnumMap<>(ExtensionKey.class);
        ExtensionKey key = null;
        int start = 3, startKeyword = start, length = extension.length();
        for (int index = start; index < length; ++index) {
            char c = extension.charAt(index);
            if (c != '-') {
                continue;
            }
            int partLength = index - start;
            assert partLength >= KEY_LENGTH;
            if (partLength == KEY_LENGTH) {
                // found new keyword
                if (key != null && !map.containsKey(key)) {
                    // commit last keyword
                    int from = startKeyword + KEY_SEP_LENGTH;
                    int to = start - SEP_LENGTH;
                    String type = to >= from ? extension.substring(from, to) : "";
                    map.put(key, type);
                }
                key = ExtensionKey.forName(extension, start);
                startKeyword = start;
            }
            start = index + 1;
        }
        boolean trailingKeyword = length - start == KEY_LENGTH;
        if (key != null && !map.containsKey(key)) {
            // commit last keyword
            int from = startKeyword + KEY_SEP_LENGTH;
            int to = trailingKeyword ? start - SEP_LENGTH : length;
            String type = to >= from ? extension.substring(from, to) : "";
            map.put(key, type);
        }
        if (trailingKeyword) {
            key = ExtensionKey.forName(extension, start);
            if (key != null && !map.containsKey(key)) {
                map.put(key, "");
            }
        }
        return map;
    }

    /**
     * 9.1 Internal Properties of Service Constructors
     */
    public interface LocaleData {
        LocaleDataInfo info(ULocale locale);
    }

    /**
     * 9.1 Internal Properties of Service Constructors
     */
    public interface LocaleDataInfo {
        /**
         * Returns {@link #entries(IntlAbstractOperations.ExtensionKey)}.get(0).
         *
         * @param extensionKey
         *            the extension key
         * @return the extension key default value
         */
        String defaultValue(ExtensionKey extensionKey);

        /**
         * Returns [[sortLocaleData]], [[searchLocaleData]] or [[localeData]].
         * 
         * @param extensionKey
         *            the extension key
         * @return the list of extension key entries
         */
        List<String> entries(ExtensionKey extensionKey);
    }

    /**
     * 9.2.1 CanonicalizeLocaleList (locales)
     * 
     * @param cx
     *            the execution context
     * @param locales
     *            the locales array
     * @return the set of canonicalized locales
     */
    public static Set<String> CanonicalizeLocaleList(ExecutionContext cx, Object locales) {
        if (Type.isUndefined(locales)) {
            return emptySet();
        }
        if (Type.isString(locales)) {
            // handle the string-only case directly
            String tag = ToFlatString(cx, locales);
            LanguageTag langTag = IsStructurallyValidLanguageTag(tag);
            if (langTag == null) {
                throw newRangeError(cx, Messages.Key.IntlStructurallyInvalidLanguageTag, tag);
            }
            tag = CanonicalizeLanguageTag(langTag);
            return singleton(tag);
        }
        Set<String> seen = new LinkedHashSet<>();
        ScriptObject o = ToObject(cx, locales);
        Object lenValue = Get(cx, o, "length");
        long len = ToUint32(cx, lenValue);
        for (long k = 0; k < len; ++k) {
            String pk = ToString(k);
            boolean kPresent = HasProperty(cx, o, pk);
            if (kPresent) {
                Object kValue = Get(cx, o, pk);
                if (!(Type.isString(kValue) || Type.isObject(pk))) {
                    throw newTypeError(cx, Messages.Key.IncompatibleObject);
                }
                String tag = ToFlatString(cx, kValue);
                LanguageTag langTag = IsStructurallyValidLanguageTag(tag);
                if (langTag == null) {
                    throw newRangeError(cx, Messages.Key.IntlStructurallyInvalidLanguageTag, tag);
                }
                tag = CanonicalizeLanguageTag(langTag);
                if (!seen.contains(tag)) {
                    seen.add(tag);
                }
            }
        }
        return seen;
    }

    /**
     * 9.2.2 BestAvailableLocale (availableLocales, locale)
     * 
     * @param availableLocales
     *            the set of available locales
     * @param locale
     *            the requested locale
     * @return the best available locale
     */
    public static String BestAvailableLocale(Set<String> availableLocales, String locale) {
        String candidate = locale;
        while (true) {
            if (availableLocales.contains(candidate)) {
                return candidate;
            }
            int pos = candidate.lastIndexOf('-');
            if (pos == -1) {
                return null;
            }
            if (pos >= 2 && candidate.charAt(pos - 2) == '-') {
                pos -= 2;
            }
            candidate = candidate.substring(0, pos);
        }
    }

    private static final class LocaleMatch {
        String locale;
        String extension;
        int extensionIndex;
    }

    /**
     * 9.2.3 LookupMatcher (availableLocales, requestedLocales)
     * 
     * @param cx
     *            the execution context
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @return the lookup matcher
     */
    public static LocaleMatch LookupMatcher(ExecutionContext cx,
            Lazy<Set<String>> availableLocales, Set<String> requestedLocales) {
        for (String locale : requestedLocales) {
            String[] unicodeExt = UnicodeLocaleExtSequence(locale);
            String noExtensionsLocale = unicodeExt[0];
            String availableLocale = BestAvailableLocale(availableLocales.get(), noExtensionsLocale);
            if (availableLocale != null) {
                return LookupMatch(availableLocale, locale, unicodeExt);
            }
        }
        return LookupMatch(DefaultLocale(cx.getRealm()), null, null);
    }

    private static LocaleMatch LookupMatch(String availableLocale, String locale,
            String[] unicodeExt) {
        LocaleMatch result = new LocaleMatch();
        result.locale = availableLocale;
        if (locale != null && !locale.equals(unicodeExt[0])) {
            result.extension = unicodeExt[1];
            result.extensionIndex = locale.indexOf("-u-") + 2;
        }
        return result;
    }

    /**
     * 9.2.4 BestFitMatcher (availableLocales, requestedLocales)
     * 
     * @param cx
     *            the execution context
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @return the best-fit matcher
     */
    public static LocaleMatch BestFitMatcher(ExecutionContext cx,
            Lazy<Set<String>> availableLocales, Set<String> requestedLocales) {
        // fast path when no specific locale was requested
        if (requestedLocales.isEmpty()) {
            final String defaultLocale = DefaultLocale(cx.getRealm());
            return BestFitMatch(defaultLocale, defaultLocale);
        }

        LocaleMatcher matcher = CreateDefaultMatcher();
        Map<String, LocaleEntry> map = GetMaximizedLocales(matcher, availableLocales.get());

        // search for best fit match
        String bestMatchCandidate = null;
        Entry<String, Double> bestMatch = null;
        for (String locale : requestedLocales) {
            String[] unicodeExt = UnicodeLocaleExtSequence(locale);
            String noExtensionsLocale = unicodeExt[0];
            Entry<String, Double> match = BestFitAvailableLocale(matcher, map, noExtensionsLocale);
            if (bestMatch == null || match.getValue() > bestMatch.getValue()) {
                bestMatch = match;
                bestMatchCandidate = locale;
            }
        }
        // If no best fit match was found, fall back to lookup matcher algorithm
        if (bestMatch == null) {
            return LookupMatcher(cx, availableLocales, requestedLocales);
        }
        return BestFitMatch(bestMatch.getKey(), bestMatchCandidate);
    }

    private static LocaleMatch BestFitMatch(String locale, String bestMatchCandidate) {
        LocaleMatch result = new LocaleMatch();
        result.locale = locale;
        String[] unicodeExt = UnicodeLocaleExtSequence(bestMatchCandidate);
        String noExtensionsLocale = unicodeExt[0];
        if (!bestMatchCandidate.equals(noExtensionsLocale)) {
            result.extension = unicodeExt[1];
            result.extensionIndex = bestMatchCandidate.indexOf("-u-") + 2;
        }
        return result;
    }

    /**
     * Minimum match value for best fit matcher, currently set to 0.5 to match ICU4J's defaults
     */
    private static final double BEST_FIT_MIN_MATCH = 0.5;

    private static LocaleMatcher CreateDefaultMatcher() {
        LocalePriorityList priorityList = LocalePriorityList.add(ULocale.ROOT).build();
        LocaleMatcher matcher = new LocaleMatcher(priorityList);
        return matcher;
    }

    /**
     * Hard cache for this entries to reduce time required to finish intl-tests
     */
    private static final Map<String, LocaleEntry> maximizedLocales = new ConcurrentHashMap<>();

    private static final class LocaleEntry {
        private final ULocale canonicalized;
        private final ULocale maximized;

        public LocaleEntry(ULocale canonicalized, ULocale maximized) {
            this.canonicalized = canonicalized;
            this.maximized = maximized;
        }

        ULocale getCanonicalized() {
            return canonicalized;
        }

        ULocale getMaximized() {
            return maximized;
        }
    }

    private static Map<String, LocaleEntry> GetMaximizedLocales(LocaleMatcher matcher,
            Set<String> availableLocales) {
        Map<String, LocaleEntry> map = new LinkedHashMap<>();
        for (String available : availableLocales) {
            LocaleEntry entry = maximizedLocales.get(available);
            if (entry == null) {
                ULocale canonicalized = matcher.canonicalize(ULocale.forLanguageTag(available));
                ULocale maximized = addLikelySubtagsWithDefaults(canonicalized);
                entry = new LocaleEntry(canonicalized, maximized);
                maximizedLocales.put(available, entry);
            }
            map.put(available, entry);
        }
        return map;
    }

    private static Entry<String, Double> BestFitAvailableLocale(LocaleMatcher matcher,
            Map<String, LocaleEntry> availableLocales, String requestedLocale) {
        ULocale canonicalized = matcher.canonicalize(ULocale.forLanguageTag(requestedLocale));
        ULocale maximized = addLikelySubtagsWithDefaults(canonicalized);
        String bestMatchLocale = null;
        LocaleEntry bestMatchEntry = null;
        double bestMatch = Double.NEGATIVE_INFINITY;
        for (Entry<String, LocaleEntry> available : availableLocales.entrySet()) {
            LocaleEntry entry = available.getValue();
            double match = matcher.match(canonicalized, maximized, entry.getCanonicalized(),
                    entry.getMaximized());
            // if (match > 0.10) {
            // System.out.printf("[%s; %s, %s] -> [%s; %s, %s]  => %f%n", requestedLocale,
            // canonicalized, maximized, available.getKey(), entry.getCanonicalized(),
            // entry.getMaximized(), match);
            // }
            if (match > bestMatch
                    || (match == bestMatch && isBetterMatch(canonicalized, maximized,
                            bestMatchEntry, entry))) {
                bestMatchLocale = available.getKey();
                bestMatchEntry = entry;
                bestMatch = match;
            }
        }
        return new SimpleEntry<>(bestMatchLocale, bestMatch);
    }

    /**
     * Requests for "en-US" gives two results with '1.0' score:
     * <ul>
     * <li>[en; en, en_Latn_US]
     * <li>[en-US; en_US, en_Latn_US]
     * </ul>
     * Obviously it's the latter result we're interested in.
     * 
     * @param canonicalized
     *            the canonicalized locale
     * @param maximized
     *            the maximized locale
     * @param oldMatch
     *            the previous match
     * @param newMatch
     *            the new match
     * @return {@code true} if the new match is better than the previous one
     */
    private static boolean isBetterMatch(ULocale canonicalized, ULocale maximized,
            LocaleEntry oldMatch, LocaleEntry newMatch) {
        // prefer more detailled information over less
        ULocale oldCanonicalized = oldMatch.getCanonicalized();
        ULocale newCanonicalized = newMatch.getCanonicalized();
        String language = canonicalized.getLanguage();
        if (newCanonicalized.getLanguage().equals(language)
                && !oldCanonicalized.getLanguage().equals(language)) {
            return true;
        }
        String script = canonicalized.getScript();
        if (newCanonicalized.getScript().equals(script)
                && !oldCanonicalized.getScript().equals(script)) {
            return true;
        }
        String region = canonicalized.getCountry();
        if (newCanonicalized.getCountry().equals(region)
                && !oldCanonicalized.getCountry().equals(region)) {
            return true;
        }
        return false;
    }

    private static ULocale addLikelySubtagsWithDefaults(ULocale locale) {
        ULocale maximized = ULocale.addLikelySubtags(locale);
        if (maximized == locale) {
            // already in maximal form, or no data available for maximization, just make sure
            // language, script and region are not undefined (ICU4J expects all are defined)
            String language = locale.getLanguage();
            String script = locale.getScript();
            String region = locale.getCountry();
            if (language.isEmpty() || script.isEmpty() || region.isEmpty()) {
                language = !language.isEmpty() ? language : "und";
                script = !script.isEmpty() ? script : "Zzzz";
                region = !region.isEmpty() ? region : "ZZ";
                return new ULocale(language, script, region);
            }
        }
        return maximized;
    }

    public static final class OptionsRecord {
        public enum MatcherType {
            Lookup, BestFit;

            public static MatcherType forName(String name) {
                switch (name) {
                case "lookup":
                    return Lookup;
                case "best fit":
                    return BestFit;
                default:
                    throw new IllegalArgumentException(name);
                }
            }
        }

        public MatcherType localeMatcher = MatcherType.BestFit;
        public EnumMap<ExtensionKey, String> values = new EnumMap<>(ExtensionKey.class);
    }

    public static final class ResolvedLocale {
        public String dataLocale;
        public String locale;
        public EnumMap<ExtensionKey, String> values = new EnumMap<>(ExtensionKey.class);
    }

    /**
     * 9.2.5 ResolveLocale (availableLocales, requestedLocales, options, relevantExtensionKeys,
     * localeData)
     * 
     * @param cx
     *            the execution context
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @param options
     *            the options record
     * @param relevantExtensionKeys
     *            the list of relevant extension keys
     * @param localeData
     *            the locale data
     * @return the resolved locale
     */
    public static ResolvedLocale ResolveLocale(ExecutionContext cx,
            Lazy<Set<String>> availableLocales, Set<String> requestedLocales,
            OptionsRecord options, List<ExtensionKey> relevantExtensionKeys, LocaleData localeData) {
        /* steps 1-3 */
        MatcherType matcher = options.localeMatcher;
        LocaleMatch r;
        if (matcher == MatcherType.Lookup) {
            r = LookupMatcher(cx, availableLocales, requestedLocales);
        } else {
            r = BestFitMatcher(cx, availableLocales, requestedLocales);
        }
        /* step 4 */
        String foundLocale = r.locale;
        LocaleDataInfo foundLocaleData = localeData.info(ULocale.forLanguageTag(foundLocale));
        /* step 5 */
        // List<String> extensionSubtags = null;
        EnumMap<ExtensionKey, String> extensionSubtags = null;
        if (r.extension != null) {
            String extension = r.extension;
            // extensionSubtags = Arrays.asList(extension.split("-"));
            extensionSubtags = unicodeLocaleExtensions(extension);
        }
        /* steps 6-7 */
        ResolvedLocale result = new ResolvedLocale();
        result.dataLocale = foundLocale;
        // fast path for steps 8-14
        if (extensionSubtags == null && options.values.isEmpty()) {
            /* steps 8-11 */
            for (int i = 0, len = relevantExtensionKeys.size(); i < len; ++i) {
                ExtensionKey key = relevantExtensionKeys.get(i);
                String value = foundLocaleData.defaultValue(key);
                result.values.put(key, value);
            }
            /* step 12 (not applicable) */
            /* step 13 */
            result.locale = foundLocale;
            /* step 14 */
            return result;
        }
        /* steps 8-11 */
        String supportedExtension = "-u";
        for (int i = 0, len = relevantExtensionKeys.size(); i < len; ++i) {
            ExtensionKey key = relevantExtensionKeys.get(i);
            List<String> keyLocaleData = foundLocaleData.entries(key);
            String value = keyLocaleData.get(0);
            String supportedExtensionAddition = "";
            if (extensionSubtags != null) {
                // int keyPos = extensionSubtags.indexOf(key.name());
                if (extensionSubtags.containsKey(key)) {
                    if (!extensionSubtags.get(key).isEmpty()) {
                        String requestedValue = extensionSubtags.get(key);
                        int valuePos = keyLocaleData.indexOf(requestedValue);
                        if (valuePos != -1) {
                            value = requestedValue;
                            supportedExtensionAddition = "-" + key.name() + "-" + value;
                        }
                    } else {
                        int valuePos = keyLocaleData.indexOf("true");
                        if (valuePos != -1) {
                            value = "true";
                        }
                    }
                }
            }
            if (options.values.containsKey(key)) {
                String optionsValue = options.values.get(key);
                if (keyLocaleData.indexOf(optionsValue) != -1) {
                    if (!optionsValue.equals(value)) {
                        value = optionsValue;
                        supportedExtensionAddition = "";
                    }
                }
            }
            result.values.put(key, value);
            supportedExtension += supportedExtensionAddition;
        }
        /* step 12 */
        if (supportedExtension.length() > 2) {
            assert r.extension != null;
            int extensionIndex = r.extensionIndex;
            extensionIndex = Math.min(extensionIndex, foundLocale.length());
            String preExtension = foundLocale.substring(0, extensionIndex);
            String postExtension = foundLocale.substring(extensionIndex);
            foundLocale = preExtension + supportedExtension + postExtension;
        }
        /* step 13 */
        result.locale = foundLocale;
        /* step 14 */
        return result;
    }

    /**
     * 9.2.6 LookupSupportedLocales (availableLocales, requestedLocales)
     * 
     * @param cx
     *            the execution context
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @return the list of supported locales
     */
    public static List<String> LookupSupportedLocales(ExecutionContext cx,
            Set<String> availableLocales, Set<String> requestedLocales) {
        List<String> subset = new ArrayList<>();
        for (String locale : requestedLocales) {
            String noExtensionsLocale = UnicodeLocaleExtSequence(locale)[0];
            String availableLocale = BestAvailableLocale(availableLocales, noExtensionsLocale);
            if (availableLocale != null) {
                subset.add(locale);
            }
        }
        return subset;
    }

    /**
     * 9.2.7 BestFitSupportedLocales (availableLocales, requestedLocales)
     * 
     * @param cx
     *            the execution context
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @return the list of best-fit supported locales
     */
    public static List<String> BestFitSupportedLocales(ExecutionContext cx,
            Set<String> availableLocales, Set<String> requestedLocales) {
        LocaleMatcher matcher = CreateDefaultMatcher();
        Map<String, LocaleEntry> map = GetMaximizedLocales(matcher, availableLocales);
        List<String> subset = new ArrayList<>();
        for (String locale : requestedLocales) {
            String noExtensionsLocale = UnicodeLocaleExtSequence(locale)[0];
            Entry<String, Double> availableLocaleMatch = BestFitAvailableLocale(matcher, map,
                    noExtensionsLocale);
            if (availableLocaleMatch.getValue() >= BEST_FIT_MIN_MATCH) {
                subset.add(locale);
            } else {
                // If no best fit match was found, fall back to lookup matcher algorithm
                String availableLocale = BestAvailableLocale(availableLocales, noExtensionsLocale);
                if (availableLocale != null) {
                    subset.add(locale);
                }
            }
        }
        return subset;
    }

    /**
     * 9.2.8 SupportedLocales (availableLocales, requestedLocales, options)
     * 
     * @param cx
     *            the execution context
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @param options
     *            the options object
     * @return the supported locales array
     */
    public static ScriptObject SupportedLocales(ExecutionContext cx, Set<String> availableLocales,
            Set<String> requestedLocales, Object options) {
        String matcher = null;
        if (!Type.isUndefined(options)) {
            ScriptObject opts = ToObject(cx, options);
            // FIXME: spec issue? algorithm steps should use abstract operation GetOption()
            matcher = GetStringOption(cx, opts, "localeMatcher", set("lookup", "best fit"),
                    "best fit");
        }
        List<String> subset;
        if (matcher == null || "best fit".equals(matcher)) {
            subset = BestFitSupportedLocales(cx, availableLocales, requestedLocales);
        } else {
            subset = LookupSupportedLocales(cx, availableLocales, requestedLocales);
        }
        ScriptObject array = ArrayCreate(cx, subset.size());
        for (int i = 0, size = subset.size(); i < size; ++i) {
            String key = Integer.toString(i);
            Object value = subset.get(i);
            DefinePropertyOrThrow(cx, array, key, new PropertyDescriptor(value, false, true, false));
        }
        PropertyDescriptor nonConfigurableWritable = new PropertyDescriptor();
        nonConfigurableWritable.setConfigurable(false);
        nonConfigurableWritable.setWritable(false);
        DefinePropertyOrThrow(cx, array, "length", nonConfigurableWritable);
        return array;
    }

    /**
     * 9.2.9 GetOption (options, property, type, values, fallback)
     * 
     * @param cx
     *            the execution context
     * @param options
     *            the options object
     * @param property
     *            the property name
     * @param values
     *            the optional set of allowed property values
     * @param fallback
     *            the fallback value
     * @return the string option
     */
    public static String GetStringOption(ExecutionContext cx, ScriptObject options,
            String property, Set<String> values, String fallback) {
        Object value = Get(cx, options, property);
        if (!Type.isUndefined(value)) {
            String val = ToFlatString(cx, value);
            if (values != null && !values.contains(val)) {
                throw newRangeError(cx, Messages.Key.IntlInvalidOption, val);
            }
            return val;
        }
        return fallback;
    }

    /**
     * 9.2.9 GetOption (options, property, type, values, fallback)
     * 
     * @param cx
     *            the execution context
     * @param options
     *            the options object
     * @param property
     *            the property name
     * @param fallback
     *            the fallback value
     * @return the boolean option
     */
    public static Boolean GetBooleanOption(ExecutionContext cx, ScriptObject options,
            String property, Boolean fallback) {
        Object value = Get(cx, options, property);
        if (!Type.isUndefined(value)) {
            return ToBoolean(value);
        }
        return fallback;
    }

    /**
     * 9.2.10 GetNumberOption (options, property, minimum, maximum, fallback)
     * 
     * @param cx
     *            the execution context
     * @param options
     *            the options object
     * @param property
     *            the property name
     * @param minimum
     *            the minimum value
     * @param maximum
     *            the maximum value
     * @param fallback
     *            the fallback value
     * @return the number option
     */
    public static int GetNumberOption(ExecutionContext cx, ScriptObject options, String property,
            int minimum, int maximum, int fallback) {
        assert minimum <= maximum;
        assert minimum <= fallback && fallback <= maximum;
        Object value = Get(cx, options, property);
        if (!Type.isUndefined(value)) {
            double val = ToNumber(cx, value);
            if (Double.isNaN(val) || val < minimum || val > maximum) {
                throw newRangeError(cx, Messages.Key.IntlInvalidOption, Double.toString(val));
            }
            return (int) Math.floor(val);
        }
        return fallback;
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }
}
