/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import java.util.AbstractMap.SimpleImmutableEntry;
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
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
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
            if ('a' <= c && c <= 'z') {
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
     * @return the pair {@code [languageTagWithoutExtension, unicodeExtension]}
     */
    private static String[] UnicodeLocaleExtSequence(String languageTag) {
        int startIndex = findUnicodeExtensionStartIndex(languageTag);
        if (startIndex != -1) {
            int endIndex = findUnicodeExtensionEndIndex(languageTag, startIndex);
            String noExtension = removeUnicodeExtension(languageTag, startIndex, endIndex);
            String extension = languageTag.substring(startIndex, endIndex);
            return new String[] { noExtension, extension };
        }
        return new String[] { languageTag, "" };
    }

    /**
     * 6.2.1 Unicode Locale Extension Sequences
     * 
     * @param languageTag
     *            the canonicalized language tag
     * @return the language tag with the unicode extension sequence removed
     */
    private static String StripUnicodeLocaleExtension(String languageTag) {
        int startIndex = findUnicodeExtensionStartIndex(languageTag);
        if (startIndex != -1) {
            int endIndex = findUnicodeExtensionEndIndex(languageTag, startIndex);
            return removeUnicodeExtension(languageTag, startIndex, endIndex);
        }
        return languageTag;
    }

    private static String removeUnicodeExtension(String languageTag, int startIndex, int endIndex) {
        if (endIndex == languageTag.length()) {
            return languageTag.substring(0, startIndex);
        }
        return languageTag.substring(0, startIndex) + languageTag.substring(endIndex);
    }

    private static int findUnicodeExtensionStartIndex(String languageTag) {
        if (languageTag.startsWith("x-")) {
            // privateuse-only case
            return -1;
        }
        int indexUnicode = languageTag.indexOf("-u-");
        if (indexUnicode == -1) {
            // no unicode extension
            return -1;
        }
        int indexPrivateUse = languageTag.lastIndexOf("-x-", indexUnicode);
        if (indexPrivateUse != -1) {
            // -u- in privateuse case
            return -1;
        }
        return indexUnicode;
    }

    private static int findUnicodeExtensionEndIndex(String languageTag, int indexUnicode) {
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
        return endIndex;
    }

    /**
     * 6.2.2 IsStructurallyValidLanguageTag (locale)
     * 
     * @param locale
     *            the locale string
     * @return the parsed language tag
     */
    public static LanguageTag IsStructurallyValidLanguageTag(String locale) {
        return LanguageTagParser.parse(locale);
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
        /* step 1 */
        String normalized = ToFlatString(cx, currency);
        /* step 2 */
        if (normalized.length() != 3) {
            return false;
        }
        /* steps 3-4 */
        return isAlpha(normalized.charAt(0)) && isAlpha(normalized.charAt(1))
                && isAlpha(normalized.charAt(2));
    }

    private static final boolean isAlpha(char c) {
        return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
    }

    private static final Set<String> JDK_TIMEZONE_NAMES = set("ACT", "AET", "AGT", "ART", "AST",
            "BET", "BST", "CAT", "CNT", "CST", "CTT", "EAT", "ECT", "IET", "IST", "JST", "MIT",
            "NET", "NST", "PLT", "PNT", "PRT", "PST", "SST", "VST");

    private static final Lazy<Map<String, String>> timezones = new Lazy<Map<String, String>>() {
        @Override
        protected Map<String, String> computeValue() {
            HashMap<String, String> map = new HashMap<>();
            for (String id : TimeZone.getAvailableIDs(SystemTimeZoneType.ANY, null, null)) {
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
        return realm.getTimeZone().getID();
    }

    private static final HashMap<String, String[]> oldStyleLanguageTags;
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
    public static Set<String> GetAvailableLocales(Collection<String> locales) {
        HashMap<String, String[]> oldTags = oldStyleLanguageTags;
        HashSet<String> available = new LRUHashSet(locales);
        for (String oldTag : oldTags.keySet()) {
            if (available.contains(oldTag)) {
                available.addAll(Arrays.asList(oldTags.get(oldTag)));
            }
        }
        return available;
    }

    @SuppressWarnings("serial")
    private static final class LRUHashSet extends HashSet<String> {
        final transient LRUEntry<String, Entry<String, Double>> entry = new LRUEntry<>();

        LRUHashSet(Collection<String> c) {
            super(c);
        }

        static LRUHashSet from(Set<String> set) {
            return set instanceof LRUHashSet ? (LRUHashSet) set : null;
        }
    }

    private static final class LRUEntry<KEY, VALUE> {
        private KEY key;
        private VALUE value;

        VALUE get(KEY request) {
            if (request.equals(key)) {
                return value;
            }
            return null;
        }

        void set(KEY key, VALUE value) {
            this.key = key;
            this.value = value;
        }
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
        /* step 1 */
        if (Type.isUndefined(locales)) {
            return emptySet();
        }
        /* steps 2-9 (string only) */
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
        /* step 2 */
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        /* step 3 (not applicable) */
        /* step 4 */
        ScriptObject o = ToObject(cx, locales);
        /* step 5 */
        Object lenValue = Get(cx, o, "length");
        /* step 6 */
        long len = ToUint32(cx, lenValue);
        /* steps 7-8 */
        for (long k = 0; k < len; ++k) {
            long pk = k;
            boolean kPresent = HasProperty(cx, o, pk);
            if (kPresent) {
                Object kValue = Get(cx, o, pk);
                if (!(Type.isString(kValue) || Type.isObject(kValue))) {
                    throw newTypeError(cx, Messages.Key.IntlInvalidLanguageTagType, Type.of(kValue)
                            .toString());
                }
                String tag = ToFlatString(cx, kValue);
                LanguageTag langTag = IsStructurallyValidLanguageTag(tag);
                if (langTag == null) {
                    throw newRangeError(cx, Messages.Key.IntlStructurallyInvalidLanguageTag, tag);
                }
                tag = CanonicalizeLanguageTag(langTag);
                seen.add(tag);
            }
        }
        /* step 9 */
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
        /* step 1 */
        String candidate = locale;
        /* step 2 */
        while (true) {
            /* step 2.a */
            if (availableLocales.contains(candidate)) {
                return candidate;
            }
            /* step 2.b */
            int pos = candidate.lastIndexOf('-');
            if (pos == -1) {
                return null;
            }
            /* step 2.c */
            if (pos >= 2 && candidate.charAt(pos - 2) == '-') {
                pos -= 2;
            }
            /* step 2.d */
            candidate = candidate.substring(0, pos);
        }
    }

    public static final class LocaleMatch {
        /** [[locale]] */
        private final String locale;
        /** [[extension]] */
        private final String extension;
        private final int extensionIndex;

        LocaleMatch(String locale) {
            this(locale, null, -1);
        }

        LocaleMatch(String locale, String extension, int extensionIndex) {
            this.locale = locale;
            this.extension = extension;
            this.extensionIndex = extensionIndex;
        }

        /**
         * [[locale]]
         * 
         * @return the locale language tag
         */
        public String getLocale() {
            return locale;
        }

        /**
         * [[extension]]
         * 
         * @return the language extension tag
         */
        public String getExtension() {
            return extension;
        }

        public int getExtensionIndex() {
            return extensionIndex;
        }
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
        /* steps 1-4 */
        for (String locale : requestedLocales) {
            /* step 4.a (not applicable) */
            /* step 4.b */
            String[] unicodeExt = UnicodeLocaleExtSequence(locale);
            String noExtensionsLocale = unicodeExt[0];
            /* step 4.c */
            String availableLocale = BestAvailableLocale(availableLocales.get(), noExtensionsLocale);
            if (availableLocale != null) {
                /* steps 5-6, 8 */
                return LookupMatch(availableLocale, locale, unicodeExt);
            }
            /* step 4.d (not applicable) */
        }
        /* steps 5, 7-8 */
        return LookupMatch(DefaultLocale(cx.getRealm()), null, null);
    }

    private static LocaleMatch LookupMatch(String availableLocale, String locale,
            String[] unicodeExt) {
        if (locale == null || locale.equals(unicodeExt[0])) {
            return new LocaleMatch(availableLocale);
        }
        return new LocaleMatch(availableLocale, unicodeExt[1], locale.indexOf("-u-") + 2);
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
        if (!BEST_FIT_SUPPORTED) {
            return LookupMatcher(cx, availableLocales, requestedLocales);
        }
        // fast path when no specific locale was requested
        if (requestedLocales.isEmpty()) {
            final String defaultLocale = DefaultLocale(cx.getRealm());
            return BestFitMatch(defaultLocale, defaultLocale);
        }
        // fast path
        Set<String> available = availableLocales.get();
        for (String locale : requestedLocales) {
            String noExtensionsLocale = StripUnicodeLocaleExtension(locale);
            if (available.contains(noExtensionsLocale)) {
                return BestFitMatch(noExtensionsLocale, locale);
            }
        }

        // search for best fit match
        LocaleMatcher matcher = CreateDefaultMatcher();
        Entry<String, Double> bestMatch = null;
        String bestMatchCandidate = null;
        for (String locale : requestedLocales) {
            String noExtLocale = StripUnicodeLocaleExtension(locale);
            Entry<String, Double> match = BestFitAvailableLocale(matcher, available, noExtLocale);
            double score = match.getValue();
            if (score >= BEST_FIT_MIN_MATCH) {
                if (bestMatch == null || score > bestMatch.getValue()) {
                    bestMatch = match;
                    bestMatchCandidate = locale;
                    if (score == 1.0) {
                        break;
                    }
                }
            }
        }

        // If no best fit match was found, fall back to lookup matcher algorithm.
        if (bestMatch == null) {
            return LookupMatcher(cx, availableLocales, requestedLocales);
        }
        return BestFitMatch(bestMatch.getKey(), bestMatchCandidate);
    }

    private static LocaleMatch BestFitMatch(String locale, String bestMatchCandidate) {
        String[] unicodeExt = UnicodeLocaleExtSequence(bestMatchCandidate);
        String noExtensionsLocale = unicodeExt[0];
        if (bestMatchCandidate.equals(noExtensionsLocale)) {
            return new LocaleMatch(locale);
        }
        return new LocaleMatch(locale, unicodeExt[1], bestMatchCandidate.indexOf("-u-") + 2);
    }

    /**
     * Minimum match value for best fit matcher, currently set to 0.5 to match ICU4J's defaults
     */
    private static final double BEST_FIT_MIN_MATCH = 0.5;

    private static final boolean BEST_FIT_SUPPORTED = true;

    private static LocaleMatcher CreateDefaultMatcher() {
        LocalePriorityList priorityList = LocalePriorityList.add(ULocale.ROOT).build();
        LocaleMatcher matcher = new LocaleMatcher(priorityList);
        return matcher;
    }

    /**
     * Hard cache to reduce time required to finish intl-tests
     */
    private static final Map<String, LocaleEntry> maximizedLocales = new ConcurrentHashMap<>();

    private static LocaleEntry GetMaximizedLocale(LocaleMatcher matcher, String locale) {
        LocaleEntry entry = maximizedLocales.get(locale);
        if (entry == null) {
            entry = createLocaleEntry(matcher, locale);
            maximizedLocales.put(locale, entry);
        }
        return entry;
    }

    private static final class LocaleEntry {
        private final ULocale canonicalized;
        private final ULocale maximized;

        LocaleEntry(ULocale canonicalized, ULocale maximized) {
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

    private static LocaleEntry createLocaleEntry(LocaleMatcher matcher, String locale) {
        ULocale canonicalized = matcher.canonicalize(ULocale.forLanguageTag(locale));
        ULocale maximized = addLikelySubtagsWithDefaults(canonicalized);
        return new LocaleEntry(canonicalized, maximized);
    }

    private static Entry<String, Double> BestFitAvailableLocale(LocaleMatcher matcher,
            Set<String> availableLocales, String requestedLocale) {
        if (availableLocales.contains(requestedLocale)) {
            return new SimpleImmutableEntry<>(requestedLocale, 1.0);
        }
        LRUHashSet lruAvailableLocales = LRUHashSet.from(availableLocales);
        if (lruAvailableLocales != null) {
            Entry<String, Double> lastResolved = lruAvailableLocales.entry.get(requestedLocale);
            if (lastResolved != null) {
                return lastResolved;
            }
        }
        LocaleEntry requested = createLocaleEntry(matcher, requestedLocale);
        String bestMatchLocale = null;
        LocaleEntry bestMatchEntry = null;
        double bestMatch = Double.NEGATIVE_INFINITY;

        for (String available : availableLocales) {
            LocaleEntry entry = GetMaximizedLocale(matcher, available);
            if (requested.maximized.equals(entry.maximized)) {
                double match = matcher.match(requested.getCanonicalized(),
                        requested.getMaximized(), entry.getCanonicalized(), entry.getMaximized());
                if (match > bestMatch
                        || (match == bestMatch && isBetterMatch(requested, bestMatchEntry, entry))) {
                    bestMatchLocale = available;
                    bestMatchEntry = entry;
                    bestMatch = match;
                }
            }
        }
        if (bestMatchLocale != null) {
            Entry<String, Double> result = new SimpleImmutableEntry<>(bestMatchLocale, bestMatch);
            if (lruAvailableLocales != null) {
                lruAvailableLocales.entry.set(requestedLocale, result);
            }
            return result;
        }

        for (String available : availableLocales) {
            LocaleEntry entry = GetMaximizedLocale(matcher, available);
            double match = matcher.match(requested.getCanonicalized(), requested.getMaximized(),
                    entry.getCanonicalized(), entry.getMaximized());
            // if (match > 0.10) {
            // System.out.printf("[%s; %s, %s] -> [%s; %s, %s]  => %f%n", requestedLocale,
            // canonicalized, maximized, available.getKey(), entry.getCanonicalized(),
            // entry.getMaximized(), match);
            // }
            if (match > bestMatch
                    || (match == bestMatch && isBetterMatch(requested, bestMatchEntry, entry))) {
                bestMatchLocale = available;
                bestMatchEntry = entry;
                bestMatch = match;
            }
        }
        Entry<String, Double> result = new SimpleImmutableEntry<>(bestMatchLocale, bestMatch);
        if (lruAvailableLocales != null) {
            lruAvailableLocales.entry.set(requestedLocale, result);
        }
        return result;
    }

    /**
     * Requests for "en-US" give two results with a perfect score:
     * <ul>
     * <li>[en; en, en_Latn_US]
     * <li>[en-US; en_US, en_Latn_US]
     * </ul>
     * Prefer the more detailed result value, i.e. "en-US".
     * 
     * @param requested
     *            the requested locale
     * @param oldMatch
     *            the previous match
     * @param newMatch
     *            the new match
     * @return {@code true} if the new match is better than the previous one
     */
    private static boolean isBetterMatch(LocaleEntry requested, LocaleEntry oldMatch,
            LocaleEntry newMatch) {
        ULocale canonicalized = requested.getCanonicalized();
        ULocale oldCanonicalized = oldMatch.getCanonicalized();
        ULocale newCanonicalized = newMatch.getCanonicalized();
        String language = canonicalized.getLanguage();
        if ((newCanonicalized.getLanguage().equals(language) || newCanonicalized.getLanguage()
                .isEmpty()) && !oldCanonicalized.getLanguage().equals(language)) {
            return true;
        }
        String script = canonicalized.getScript();
        if ((newCanonicalized.getScript().equals(script) || newCanonicalized.getScript().isEmpty())
                && !oldCanonicalized.getScript().equals(script)) {
            return true;
        }
        String region = canonicalized.getCountry();
        if ((newCanonicalized.getCountry().equals(region) || newCanonicalized.getCountry()
                .isEmpty()) && !oldCanonicalized.getCountry().equals(region)) {
            return true;
        }
        return false;
    }

    private static ULocale addLikelySubtagsWithDefaults(ULocale locale) {
        ULocale maximized = ULocale.addLikelySubtags(locale);
        if (maximized == locale) {
            // If already in maximal form or no data available for maximization, make sure
            // language, script and region are not undefined (ICU4J expects all are defined).
            String language = locale.getLanguage();
            String script = locale.getScript();
            String region = locale.getCountry();
            if (language.isEmpty() || script.isEmpty() || region.isEmpty()) {
                return new ULocale(toLocaleId(language, script, region));
            }
        }
        return maximized;
    }

    private static String toLocaleId(String language, String script, String region) {
        StringBuilder sb = new StringBuilder();
        sb.append(!language.isEmpty() ? language : "und");
        sb.append('_');
        sb.append(!script.isEmpty() ? script : "Zzzz");
        sb.append('_');
        sb.append(!region.isEmpty() ? region : "ZZ");
        return sb.toString();
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

        /** [[localeMatcher]] */
        private final MatcherType localeMatcher;
        private final EnumMap<ExtensionKey, String> values = new EnumMap<>(ExtensionKey.class);

        public OptionsRecord(MatcherType localeMatcher) {
            this.localeMatcher = localeMatcher;
        }

        /**
         * [[localeMatcher]]
         * 
         * @return the matcher type
         */
        public MatcherType getLocaleMatcher() {
            return localeMatcher;
        }

        public EnumMap<ExtensionKey, String> getValues() {
            return values;
        }

        public void putValue(ExtensionKey key, String value) {
            values.put(key, value);
        }
    }

    public static final class ResolvedLocale {
        private final String dataLocale;
        private final String locale;
        private final EnumMap<ExtensionKey, String> values;

        ResolvedLocale(String dataLocale, String locale, EnumMap<ExtensionKey, String> values) {
            this.dataLocale = dataLocale;
            this.locale = locale;
            this.values = values;
        }

        public String getDataLocale() {
            return dataLocale;
        }

        public String getLocale() {
            return locale;
        }

        public String getValue(ExtensionKey key) {
            return values.get(key);
        }
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
        MatcherType matcher = options.getLocaleMatcher();
        LocaleMatch r;
        if (matcher == MatcherType.Lookup) {
            r = LookupMatcher(cx, availableLocales, requestedLocales);
        } else {
            r = BestFitMatcher(cx, availableLocales, requestedLocales);
        }
        /* step 4 */
        String foundLocale = r.getLocale();
        LocaleDataInfo foundLocaleData = localeData.info(ULocale.forLanguageTag(foundLocale));
        EnumMap<ExtensionKey, String> values = new EnumMap<>(ExtensionKey.class);
        // fast path for steps 5-14
        if (r.getExtension() == null && options.getValues().isEmpty()) {
            /* steps 8-11 */
            for (int i = 0, len = relevantExtensionKeys.size(); i < len; ++i) {
                ExtensionKey key = relevantExtensionKeys.get(i);
                String value = foundLocaleData.defaultValue(key);
                values.put(key, value);
            }
            /* step 12 (not applicable) */
            /* steps 13-14 */
            return new ResolvedLocale(r.getLocale(), foundLocale, values);
        }
        /* step 5 */
        EnumMap<ExtensionKey, String> extensionSubtags = null;
        if (r.getExtension() != null) {
            extensionSubtags = unicodeLocaleExtensions(r.getExtension());
        }
        /* steps 6-7 (not applicable) */
        /* steps 8-11 */
        StringBuilder supportedExtension = new StringBuilder("-u");
        for (int i = 0, len = relevantExtensionKeys.size(); i < len; ++i) {
            /* step 11.a */
            ExtensionKey key = relevantExtensionKeys.get(i);
            /* steps 11.b-11.c */
            List<String> keyLocaleData = foundLocaleData.entries(key);
            /* step 11.d */
            String value = keyLocaleData.get(0);
            /* step 11.e */
            String supportedExtensionAddition = "";
            /* step 11.f (not applicable) */
            /* step 11.g */
            if (extensionSubtags != null && extensionSubtags.containsKey(key)) {
                if (!extensionSubtags.get(key).isEmpty()) {
                    String requestedValue = extensionSubtags.get(key);
                    if (keyLocaleData.contains(requestedValue)) {
                        value = requestedValue;
                        supportedExtensionAddition = "-" + key.name() + "-" + value;
                    }
                } else if (keyLocaleData.contains("true")) {
                    value = "true";
                }
            }
            /* step 11.h */
            if (options.getValues().containsKey(key)) {
                String optionsValue = options.getValues().get(key);
                if (keyLocaleData.contains(optionsValue) && !optionsValue.equals(value)) {
                    value = optionsValue;
                    supportedExtensionAddition = "";
                }
            }
            /* step 11.i */
            values.put(key, value);
            /* step 11.j */
            supportedExtension.append(supportedExtensionAddition);
            /* step 11.k (not applicable) */
        }
        /* step 12 */
        if (supportedExtension.length() > 2) {
            assert r.getExtension() != null;
            int extensionIndex = r.getExtensionIndex();
            extensionIndex = Math.min(extensionIndex, foundLocale.length());
            String preExtension = foundLocale.substring(0, extensionIndex);
            String postExtension = foundLocale.substring(extensionIndex);
            foundLocale = preExtension + supportedExtension + postExtension;
        }
        /* steps 13-14 */
        return new ResolvedLocale(r.getLocale(), foundLocale, values);
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
        int numberOfRequestedLocales = requestedLocales.size();
        if (numberOfRequestedLocales == 0) {
            return emptyList();
        }
        if (numberOfRequestedLocales == 1) {
            String locale = requestedLocales.iterator().next();
            String noExtensionsLocale = StripUnicodeLocaleExtension(locale);
            String availableLocale = BestAvailableLocale(availableLocales, noExtensionsLocale);
            if (availableLocale != null) {
                return singletonList(locale);
            }
            return emptyList();
        }
        /* steps 1-4 */
        ArrayList<String> subset = new ArrayList<>();
        for (String locale : requestedLocales) {
            String noExtensionsLocale = StripUnicodeLocaleExtension(locale);
            String availableLocale = BestAvailableLocale(availableLocales, noExtensionsLocale);
            if (availableLocale != null) {
                subset.add(locale);
            }
        }
        /* steps 5-6 */
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
        if (!BEST_FIT_SUPPORTED) {
            return LookupSupportedLocales(cx, availableLocales, requestedLocales);
        }
        int numberOfRequestedLocales = requestedLocales.size();
        if (numberOfRequestedLocales == 0) {
            return emptyList();
        }
        if (numberOfRequestedLocales == 1) {
            String locale = requestedLocales.iterator().next();
            String noExtensionsLocale = StripUnicodeLocaleExtension(locale);
            if (availableLocales.contains(noExtensionsLocale)) {
                return singletonList(locale);
            }
            LocaleMatcher matcher = CreateDefaultMatcher();
            Entry<String, Double> availableLocaleMatch = BestFitAvailableLocale(matcher,
                    availableLocales, noExtensionsLocale);
            if (availableLocaleMatch.getValue() >= BEST_FIT_MIN_MATCH) {
                return singletonList(locale);
            } else {
                // If no best fit match was found, fall back to lookup matcher algorithm
                String availableLocale = BestAvailableLocale(availableLocales, noExtensionsLocale);
                if (availableLocale != null) {
                    return singletonList(locale);
                }
            }
            return emptyList();
        }
        LocaleMatcher matcher = CreateDefaultMatcher();
        ArrayList<String> subset = new ArrayList<>();
        for (String locale : requestedLocales) {
            String noExtensionsLocale = StripUnicodeLocaleExtension(locale);
            Entry<String, Double> availableLocaleMatch = BestFitAvailableLocale(matcher,
                    availableLocales, noExtensionsLocale);
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
        /* step 1 */
        String matcher = null;
        if (!Type.isUndefined(options)) {
            ScriptObject opts = ToObject(cx, options);
            // FIXME: spec issue? algorithm steps should use abstract operation GetOption()
            matcher = GetStringOption(cx, opts, "localeMatcher", set("lookup", "best fit"),
                    "best fit");
        }
        /* steps 2-3 */
        List<String> subset;
        if (matcher == null || "best fit".equals(matcher)) {
            subset = BestFitSupportedLocales(cx, availableLocales, requestedLocales);
        } else {
            subset = LookupSupportedLocales(cx, availableLocales, requestedLocales);
        }
        /* step 4 */
        ArrayObject array = ArrayCreate(cx, subset.size());
        for (int i = 0, size = subset.size(); i < size; ++i) {
            Object value = subset.get(i);
            DefinePropertyOrThrow(cx, array, i, new PropertyDescriptor(value, false, true, false));
        }
        PropertyDescriptor nonConfigurableWritable = new PropertyDescriptor();
        nonConfigurableWritable.setConfigurable(false);
        nonConfigurableWritable.setWritable(false);
        DefinePropertyOrThrow(cx, array, "length", nonConfigurableWritable);
        /* step 5 */
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
        /* step 1 */
        Object value = Get(cx, options, property);
        /* step 2 */
        if (!Type.isUndefined(value)) {
            String val = ToFlatString(cx, value);
            if (values != null && !values.contains(val)) {
                throw newRangeError(cx, Messages.Key.IntlInvalidOption, val);
            }
            return val;
        }
        /* step 3 */
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
        /* step 1 */
        Object value = Get(cx, options, property);
        /* step 2 */
        if (!Type.isUndefined(value)) {
            return ToBoolean(value);
        }
        /* step 3 */
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
        /* step 1 */
        Object value = Get(cx, options, property);
        /* step 2 */
        if (!Type.isUndefined(value)) {
            double val = ToNumber(cx, value);
            if (Double.isNaN(val) || val < minimum || val > maximum) {
                throw newRangeError(cx, Messages.Key.IntlInvalidOption, Double.toString(val));
            }
            return (int) Math.floor(val);
        }
        /* step 3 */
        return fallback;
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }
}
