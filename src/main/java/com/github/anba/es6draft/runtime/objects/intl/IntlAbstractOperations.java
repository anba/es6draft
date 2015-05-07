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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Lazy;
import com.github.anba.es6draft.runtime.internal.Messages;
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
                c = (char) (c - ('a' - 'A'));
            }
            ca[i] = c;
        }
        return String.valueOf(ca);
    }

    /**
     * 
     */
    public static final class LanguageTagUnicodeExtension {
        final String languageTag;
        final int startIndex;
        final int endIndex;

        LanguageTagUnicodeExtension(String languageTag) {
            this.languageTag = languageTag;
            this.startIndex = findStartIndex(languageTag);
            this.endIndex = findEndIndex(languageTag, startIndex);
        }

        /**
         * Returns the language tag with the unicode extension sequence removed.
         * 
         * @return the language tag with the unicode extension sequence removed
         */
        public String getLanguageTag() {
            return removeUnicodeExtension(languageTag, startIndex, endIndex);
        }

        /**
         * Returns the unicode extension sequence.
         * 
         * @return the unicode extension sequence
         */
        public String getUnicodeExtension() {
            if (startIndex == -1) {
                return "";
            }
            return languageTag.substring(startIndex, endIndex);
        }

        int getSubtagStartIndex() {
            if (startIndex == -1) {
                return -1;
            }
            return startIndex + 2;
        }

        static String stripExtension(String languageTag) {
            int startIndex = findStartIndex(languageTag);
            int endIndex = findEndIndex(languageTag, startIndex);
            return removeUnicodeExtension(languageTag, startIndex, endIndex);
        }

        private static String removeUnicodeExtension(String languageTag, int startIndex,
                int endIndex) {
            if (startIndex == -1) {
                return languageTag;
            }
            if (endIndex == languageTag.length()) {
                return languageTag.substring(0, startIndex);
            }
            return languageTag.substring(0, startIndex) + languageTag.substring(endIndex);
        }

        private static int findStartIndex(String languageTag) {
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

        private static int findEndIndex(String languageTag, int indexUnicode) {
            // found unicode extension, search end index
            if (indexUnicode == -1) {
                return -1;
            }
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
    }

    /**
     * 6.2.1 Unicode Locale Extension Sequences
     * 
     * @param languageTag
     *            the canonicalized language tag
     * @return the pair {@code [languageTagWithoutExtension, unicodeExtension]}
     */
    public static LanguageTagUnicodeExtension UnicodeLocaleExtSequence(String languageTag) {
        return new LanguageTagUnicodeExtension(languageTag);
    }

    /**
     * 6.2.1 Unicode Locale Extension Sequences
     * 
     * @param languageTag
     *            the canonicalized language tag
     * @return the language tag with the unicode extension sequence removed
     */
    public static String StripUnicodeLocaleExtension(String languageTag) {
        return LanguageTagUnicodeExtension.stripExtension(languageTag);
    }

    /**
     * 6.2.2 IsStructurallyValidLanguageTag (locale)
     * 
     * @param locale
     *            the locale string
     * @return the parsed language tag or {@code null} if the locale is not a valid language tag
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

    @SuppressWarnings("serial")
    private static final class ValidLanguageTags extends LinkedHashMap<String, String> {
        private static final int MAX_SIZE = 12;

        ValidLanguageTags() {
            super(16, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, String> eldest) {
            return size() > MAX_SIZE;
        }
    }

    private static final Map<String, String> validLanguageTags = new ValidLanguageTags();
    static {
        validLanguageTags.put("en", "en");
        validLanguageTags.put("en-GB", "en-GB");
        validLanguageTags.put("en-US", "en-US");
    }
    private static final String DEFAULT_LOCALE = "en";

    private static String sanitizeLanguageTag(String languageTag) {
        LanguageTag tag = IsStructurallyValidLanguageTag(languageTag);
        if (tag == null) {
            return DEFAULT_LOCALE;
        }
        String locale = StripUnicodeLocaleExtension(tag.canonicalize());
        locale = BestLocale(GetAvailableLocales(LanguageData.getAvailableCollatorLocales()), locale);
        if (locale == null) {
            return DEFAULT_LOCALE;
        }
        locale = BestLocale(GetAvailableLocales(LanguageData.getAvailableDateFormatLocales()),
                locale);
        if (locale == null) {
            return DEFAULT_LOCALE;
        }
        locale = BestLocale(GetAvailableLocales(LanguageData.getAvailableNumberFormatLocales()),
                locale);
        if (locale == null) {
            return DEFAULT_LOCALE;
        }
        return locale;
    }

    private static String BestLocale(Set<String> availableLocales, String locale) {
        if (BEST_FIT_SUPPORTED) {
            return BestFitAvailableLocale(availableLocales, locale);
        }
        return BestAvailableLocale(availableLocales, locale);
    }

    /**
     * 6.2.4 DefaultLocale ()
     * 
     * @param realm
     *            the realm instance
     * @return the default locale
     */
    public static String DefaultLocale(Realm realm) {
        String languageTag = realm.getLocale().toLanguageTag();
        synchronized (validLanguageTags) {
            String valid = validLanguageTags.get(languageTag);
            if (valid == null) {
                validLanguageTags.put(languageTag, valid = sanitizeLanguageTag(languageTag));
            }
            return valid;
        }
    }

    /**
     * 6.3.1 IsWellFormedCurrencyCode (currency)
     * 
     * @param currency
     *            the currency string
     * @return {@code true} if the currency string is well formed
     */
    public static boolean IsWellFormedCurrencyCode(String currency) {
        /* step 1 (case normalization omitted) */
        String normalized = currency;
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
        for (Map.Entry<String, String[]> oldTag : oldTags.entrySet()) {
            if (available.contains(oldTag.getKey())) {
                available.addAll(Arrays.asList(oldTag.getValue()));
            }
        }
        return available;
    }

    @SuppressWarnings("serial")
    private static final class LRUHashSet extends HashSet<String> {
        private transient String lastKey;
        private transient BestFitMatch lastValue;

        LRUHashSet(Collection<String> c) {
            super(c);
        }

        static LRUHashSet from(Set<String> set) {
            return set instanceof LRUHashSet ? (LRUHashSet) set : null;
        }

        static BestFitMatch get(LRUHashSet set, String key) {
            if (set != null && key.equals(set.lastKey)) {
                return set.lastValue;
            }
            return null;
        }

        static void set(LRUHashSet set, String key, BestFitMatch value) {
            if (set != null) {
                set.lastKey = key;
                set.lastValue = value;
            }
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

    /**
     * The result record type returned by BestFitMatcher and LookupMatcher.
     */
    public static final class LocaleMatch {
        /** [[locale]] */
        private final String locale;
        /** [[extension]] */
        private final String extension;
        /** [[extensionIndex]] */
        private final int extensionIndex;

        LocaleMatch(String locale) {
            this.locale = locale;
            this.extension = "";
            this.extensionIndex = -1;
        }

        LocaleMatch(String locale, LanguageTagUnicodeExtension unicodeExt) {
            this.locale = locale;
            this.extension = unicodeExt.getUnicodeExtension();
            this.extensionIndex = unicodeExt.getSubtagStartIndex();
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

        /**
         * [[extensionIndex]]
         * 
         * @return the start index of the language extension tag
         */
        public int getExtensionIndex() {
            return extensionIndex;
        }
    }

    /**
     * 9.2.3 LookupMatcher (availableLocales, requestedLocales)
     * 
     * @param realm
     *            the realm instance
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @return the lookup matcher
     */
    public static LocaleMatch LookupMatcher(Realm realm, Lazy<Set<String>> availableLocales,
            Set<String> requestedLocales) {
        /* steps 1-5 */
        for (String locale : requestedLocales) {
            /* steps 5.a-c (not applicable) */
            /* step 5.d */
            LanguageTagUnicodeExtension unicodeExt = UnicodeLocaleExtSequence(locale);
            String noExtensionsLocale = unicodeExt.getLanguageTag();
            /* step 5.e */
            String availableLocale = BestAvailableLocale(availableLocales.get(), noExtensionsLocale);
            if (availableLocale != null) {
                /* steps 6-7, 9 */
                return new LocaleMatch(availableLocale, unicodeExt);
            }
            /* step 5.d (not applicable) */
        }
        /* steps 6, 8-9 */
        return new LocaleMatch(DefaultLocale(realm));
    }

    /**
     * 9.2.4 BestFitMatcher (availableLocales, requestedLocales)
     * 
     * @param realm
     *            the realm instance
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @return the best-fit matcher
     */
    public static LocaleMatch BestFitMatcher(Realm realm, Lazy<Set<String>> availableLocales,
            Set<String> requestedLocales) {
        if (!BEST_FIT_SUPPORTED) {
            return LookupMatcher(realm, availableLocales, requestedLocales);
        }
        // fast path when no specific locale was requested
        if (requestedLocales.isEmpty()) {
            return new LocaleMatch(DefaultLocale(realm));
        }
        // fast path
        Set<String> available = availableLocales.get();
        for (String locale : requestedLocales) {
            LanguageTagUnicodeExtension unicodeExt = UnicodeLocaleExtSequence(locale);
            String noExtensionsLocale = unicodeExt.getLanguageTag();
            if (available.contains(noExtensionsLocale)) {
                return new LocaleMatch(noExtensionsLocale, unicodeExt);
            }
        }

        // search for best fit match
        LocaleMatcher matcher = CreateDefaultMatcher();
        BestFitMatch bestMatch = null;
        String bestMatchCandidate = null;
        for (String locale : requestedLocales) {
            String noExtLocale = StripUnicodeLocaleExtension(locale);
            BestFitMatch match = BestFitAvailableLocale(matcher, available, noExtLocale);
            double score = match.score;
            if (score >= BEST_FIT_MIN_MATCH) {
                if (bestMatch == null || score > bestMatch.score) {
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
            return LookupMatcher(realm, availableLocales, requestedLocales);
        }
        return new LocaleMatch(bestMatch.locale, UnicodeLocaleExtSequence(bestMatchCandidate));
    }

    /**
     * Minimum match value for best fit matcher, currently set to 0.5 to match ICU4J's defaults.
     */
    private static final double BEST_FIT_MIN_MATCH = 0.5;

    private static final boolean BEST_FIT_SUPPORTED = true;

    private static LocaleMatcher CreateDefaultMatcher() {
        LocalePriorityList priorityList = LocalePriorityList.add(ULocale.ROOT).build();
        LocaleMatcher matcher = new LocaleMatcher(priorityList);
        return matcher;
    }

    /**
     * Hard cache to reduce time required to finish intl-tests. The cache size is limited by the
     * total number of available locales.
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

    private static final class BestFitMatch {
        final String locale;
        final double score;

        BestFitMatch(String locale, double score) {
            this.locale = locale;
            this.score = score;
        }
    }

    private static BestFitMatch BestFitAvailableLocale(LocaleMatcher matcher,
            Set<String> availableLocales, String requestedLocale) {
        if (availableLocales.contains(requestedLocale)) {
            return new BestFitMatch(requestedLocale, 1.0);
        }
        LRUHashSet lruAvailableLocales = LRUHashSet.from(availableLocales);
        BestFitMatch lastResolved = LRUHashSet.get(lruAvailableLocales, requestedLocale);
        if (lastResolved != null) {
            return lastResolved;
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
            BestFitMatch result = new BestFitMatch(bestMatchLocale, bestMatch);
            LRUHashSet.set(lruAvailableLocales, requestedLocale, result);
            return result;
        }

        for (String available : availableLocales) {
            LocaleEntry entry = GetMaximizedLocale(matcher, available);
            double match = matcher.match(requested.getCanonicalized(), requested.getMaximized(),
                    entry.getCanonicalized(), entry.getMaximized());
            // if (match > 0.10) {
            // System.out.printf("[%s; %s, %s] -> [%s; %s, %s]  => %f%n", requestedLocale,
            // requested.getCanonicalized(), requested.getMaximized(), available,
            // entry.getCanonicalized(), entry.getMaximized(), match);
            // }
            if (match > bestMatch
                    || (match == bestMatch && isBetterMatch(requested, bestMatchEntry, entry))) {
                bestMatchLocale = available;
                bestMatchEntry = entry;
                bestMatch = match;
            }
        }
        BestFitMatch result = new BestFitMatch(bestMatchLocale, bestMatch);
        LRUHashSet.set(lruAvailableLocales, requestedLocale, result);
        return result;
    }

    private static String BestFitAvailableLocale(Set<String> availableLocales,
            String requestedLocale) {
        if (availableLocales.contains(requestedLocale)) {
            return requestedLocale;
        }
        LocaleMatcher matcher = CreateDefaultMatcher();
        BestFitMatch availableLocaleMatch = BestFitAvailableLocale(matcher, availableLocales,
                requestedLocale);
        if (availableLocaleMatch.score >= BEST_FIT_MIN_MATCH) {
            return availableLocaleMatch.locale;
        } else {
            // If no best fit match was found, fall back to lookup matcher algorithm
            String availableLocale = BestAvailableLocale(availableLocales, requestedLocale);
            if (availableLocale != null) {
                return availableLocale;
            }
        }
        return null;
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
     * @param realm
     *            the realm instance
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
    public static ResolvedLocale ResolveLocale(Realm realm, Lazy<Set<String>> availableLocales,
            Set<String> requestedLocales, OptionsRecord options,
            List<ExtensionKey> relevantExtensionKeys, LocaleData localeData) {
        /* steps 1-4 */
        OptionsRecord.MatcherType matcher = options.getLocaleMatcher();
        LocaleMatch r;
        if (matcher == OptionsRecord.MatcherType.Lookup) {
            r = LookupMatcher(realm, availableLocales, requestedLocales);
        } else {
            r = BestFitMatcher(realm, availableLocales, requestedLocales);
        }
        /* step 5 */
        String foundLocale = r.getLocale();
        LocaleDataInfo foundLocaleData = localeData.info(ULocale.forLanguageTag(foundLocale));
        EnumMap<ExtensionKey, String> values = new EnumMap<>(ExtensionKey.class);
        // fast path for steps 6-16
        if (r.getExtension().isEmpty() && options.getValues().isEmpty()) {
            /* steps 9-13 */
            for (ExtensionKey key : relevantExtensionKeys) {
                String value = foundLocaleData.defaultValue(key);
                values.put(key, value);
            }
            /* step 14 (not applicable) */
            /* steps 15-16 */
            return new ResolvedLocale(r.getLocale(), foundLocale, values);
        }
        /* step 6 */
        EnumMap<ExtensionKey, String> extensionSubtags = null;
        if (!r.getExtension().isEmpty()) {
            extensionSubtags = unicodeLocaleExtensions(r.getExtension());
        }
        /* steps 7-8 (not applicable) */
        /* steps 9-13 */
        StringBuilder supportedExtension = new StringBuilder("-u");
        for (ExtensionKey key : relevantExtensionKeys) {
            /* steps 13.a-d (not applicable) */
            /* steps 13.e-f */
            List<String> keyLocaleData = foundLocaleData.entries(key);
            /* steps 13.g-h */
            String value = keyLocaleData.get(0);
            /* step 13.i */
            String supportedExtensionAddition = "";
            /* step 13.j */
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
            /* step 13.k */
            if (options.getValues().containsKey(key)) {
                String optionsValue = options.getValues().get(key);
                if (keyLocaleData.contains(optionsValue) && !optionsValue.equals(value)) {
                    value = optionsValue;
                    supportedExtensionAddition = "";
                }
            }
            /* step 13.l */
            values.put(key, value);
            /* step 13.m */
            supportedExtension.append(supportedExtensionAddition);
            /* step 13.n (not applicable) */
        }
        /* step 14 */
        if (supportedExtension.length() > 2) {
            assert !r.getExtension().isEmpty();
            int extensionIndex = r.getExtensionIndex();
            extensionIndex = Math.min(extensionIndex, foundLocale.length());
            /* step 14.a */
            String preExtension = foundLocale.substring(0, extensionIndex);
            /* step 14.b */
            String postExtension = foundLocale.substring(extensionIndex);
            /* step 14.c */
            foundLocale = preExtension + supportedExtension + postExtension;
        }
        /* steps 15-16 */
        return new ResolvedLocale(r.getLocale(), foundLocale, values);
    }

    /**
     * 9.2.5 ResolveLocale (availableLocales, requestedLocales, options, relevantExtensionKeys,
     * localeData)
     * 
     * @param realm
     *            the realm record
     * @param relevantExtensionKeys
     *            the list of relevant extension keys
     * @param localeData
     *            the locale data
     * @return the resolved locale
     */
    public static ResolvedLocale ResolveDefaultLocale(Realm realm,
            List<ExtensionKey> relevantExtensionKeys, LocaleData localeData) {
        /* steps 1-4 */
        LocaleMatch r = new LocaleMatch(DefaultLocale(realm));
        /* step 5 */
        String foundLocale = r.getLocale();
        LocaleDataInfo foundLocaleData = localeData.info(ULocale.forLanguageTag(foundLocale));
        EnumMap<ExtensionKey, String> values = new EnumMap<>(ExtensionKey.class);
        /* steps 6-8 (not applicable) */
        /* steps 10-13 */
        for (ExtensionKey key : relevantExtensionKeys) {
            values.put(key, foundLocaleData.defaultValue(key));
        }
        /* step 14 (not applicable) */
        /* steps 15-16 */
        return new ResolvedLocale(r.getLocale(), foundLocale, values);
    }

    /**
     * 9.2.6 LookupSupportedLocales (availableLocales, requestedLocales)
     * 
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @return the list of supported locales
     */
    public static List<String> LookupSupportedLocales(Set<String> availableLocales,
            Set<String> requestedLocales) {
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
        /* steps 1-5 */
        ArrayList<String> subset = new ArrayList<>();
        for (String locale : requestedLocales) {
            /* steps 5.a-c (not applicable) */
            /* step 5.d */
            String noExtensionsLocale = StripUnicodeLocaleExtension(locale);
            /* step 5.e */
            String availableLocale = BestAvailableLocale(availableLocales, noExtensionsLocale);
            /* step 5.f */
            if (availableLocale != null) {
                subset.add(locale);
            }
            /* step 5.g (not applicable) */
        }
        /* steps 6-7 */
        return subset;
    }

    /**
     * 9.2.7 BestFitSupportedLocales (availableLocales, requestedLocales)
     * 
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @return the list of best-fit supported locales
     */
    public static List<String> BestFitSupportedLocales(Set<String> availableLocales,
            Set<String> requestedLocales) {
        if (!BEST_FIT_SUPPORTED) {
            return LookupSupportedLocales(availableLocales, requestedLocales);
        }
        int numberOfRequestedLocales = requestedLocales.size();
        if (numberOfRequestedLocales == 0) {
            return emptyList();
        }
        if (numberOfRequestedLocales == 1) {
            String locale = requestedLocales.iterator().next();
            String noExtensionsLocale = StripUnicodeLocaleExtension(locale);
            if (BestFitAvailableLocale(availableLocales, noExtensionsLocale) != null) {
                return singletonList(locale);
            }
            return emptyList();
        }
        LocaleMatcher matcher = CreateDefaultMatcher();
        ArrayList<String> subset = new ArrayList<>();
        for (String locale : requestedLocales) {
            String noExtensionsLocale = StripUnicodeLocaleExtension(locale);
            BestFitMatch availableLocaleMatch = BestFitAvailableLocale(matcher, availableLocales,
                    noExtensionsLocale);
            if (availableLocaleMatch.score >= BEST_FIT_MIN_MATCH) {
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
            // FIXME: spec issue - use GetOption() (bug 4011)
            matcher = GetStringOption(cx, opts, "localeMatcher", set("lookup", "best fit"),
                    "best fit");
        }
        /* steps 2-4 */
        List<String> subset;
        if (matcher == null || "best fit".equals(matcher)) {
            subset = BestFitSupportedLocales(availableLocales, requestedLocales);
        } else {
            subset = LookupSupportedLocales(availableLocales, requestedLocales);
        }
        /* step 5 */
        ArrayObject array = ArrayCreate(cx, subset.size());
        for (int i = 0, size = subset.size(); i < size; ++i) {
            Object value = subset.get(i);
            array.defineOwnProperty(cx, i, new PropertyDescriptor(value, false, true, false));
        }
        PropertyDescriptor nonConfigurableWritable = new PropertyDescriptor();
        nonConfigurableWritable.setConfigurable(false);
        nonConfigurableWritable.setWritable(false);
        array.defineOwnProperty(cx, "length", nonConfigurableWritable);
        /* step 6 */
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
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        Object value = Get(cx, options, property);
        /* step 5 */
        if (!Type.isUndefined(value)) {
            /* steps 5.a-b (not applicable) */
            /* step 5.c */
            String val = ToFlatString(cx, value);
            /* step 5.d */
            if (values != null && !values.contains(val)) {
                throw newRangeError(cx, Messages.Key.IntlInvalidOption, val);
            }
            /* step 5.e */
            return val;
        }
        /* step 6 */
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
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        Object value = Get(cx, options, property);
        /* step 5 */
        if (!Type.isUndefined(value)) {
            /* steps 5.a, c-d (not applicable) */
            /* steps 5.b, e */
            return ToBoolean(value);
        }
        /* step 6 */
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
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        Object value = Get(cx, options, property);
        /* step 5 */
        if (!Type.isUndefined(value)) {
            /* steps 5.a-b */
            double val = ToNumber(cx, value);
            /* step 5.c */
            if (Double.isNaN(val) || val < minimum || val > maximum) {
                throw newRangeError(cx, Messages.Key.IntlInvalidOption, Double.toString(val));
            }
            /* step 5.d */
            return (int) Math.floor(val);
        }
        /* step 6 */
        return fallback;
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }
}
