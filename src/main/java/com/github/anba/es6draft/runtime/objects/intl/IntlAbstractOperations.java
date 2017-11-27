/**
 * Copyright (c) André Bargull
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
        for (int i = 0; i < ca.length; ++i) {
            char c = ca[i];
            if ('a' <= c && c <= 'z') {
                c = (char) (c - ('a' - 'A'));
            }
            ca[i] = c;
        }
        return new String(ca);
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

        static String removeExtension(String languageTag) {
            int startIndex = findStartIndex(languageTag);
            int endIndex = findEndIndex(languageTag, startIndex);
            return removeUnicodeExtension(languageTag, startIndex, endIndex);
        }

        private static String removeUnicodeExtension(String languageTag, int startIndex, int endIndex) {
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
    public static String RemoveUnicodeLocaleExtension(String languageTag) {
        return LanguageTagUnicodeExtension.removeExtension(languageTag);
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
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
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
        String locale = RemoveUnicodeLocaleExtension(CanonicalizeLanguageTag(tag));
        locale = BestLocale(GetAvailableLocales(LanguageData.getAvailableCollatorLocales()), locale);
        if (locale == null) {
            return DEFAULT_LOCALE;
        }
        locale = BestLocale(GetAvailableLocales(LanguageData.getAvailableDateFormatLocales()), locale);
        if (locale == null) {
            return DEFAULT_LOCALE;
        }
        locale = BestLocale(GetAvailableLocales(LanguageData.getAvailableNumberFormatLocales()), locale);
        if (locale == null) {
            return DEFAULT_LOCALE;
        }
        locale = BestLocale(GetAvailableLocales(LanguageData.getAvailablePluralRulesLocales()), locale);
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
        return isAlpha(normalized.charAt(0)) && isAlpha(normalized.charAt(1)) && isAlpha(normalized.charAt(2));
    }

    private static boolean isAlpha(char c) {
        return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
    }

    private static final Set<String> JDK_TIMEZONE_NAMES = set("ACT", "AET", "AGT", "ART", "AST", "BET", "BST", "CAT",
            "CNT", "CST", "CTT", "EAT", "ECT", "IET", "IST", "JST", "MIT", "NET", "NST", "PLT", "PNT", "PRT", "PST",
            "SST", "VST");

    private static final Lazy<HashMap<String, String>> timezones = Lazy.syncOf(() -> {
        HashMap<String, String> map = new HashMap<>();
        for (String id : TimeZone.getAvailableIDs(SystemTimeZoneType.ANY, null, null)) {
            if (JDK_TIMEZONE_NAMES.contains(id)) {
                // ignore non-IANA, JDK-specific timezones
                continue;
            }
            map.put(ToUpperCase(id), id);
        }
        return map;
    });

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
        // FIXME: spec bug - remove or reword this note from 6.4.2 CanonicalizeTimeZoneName: "Implementations shall
        // support UTC and the host environment’s current time zone (if different from UTC) in formatting." ?
    }

    private static final String DEFAULT_TIMEZONE = "UTC";

    /**
     * 6.4.3 DefaultTimeZone ()
     * 
     * @param realm
     *            the realm instance
     * @return the default time zone
     */
    public static String DefaultTimeZone(Realm realm) {
        String timeZone = realm.getTimeZone().getID();
        if (!IsValidTimeZoneName(timeZone)) {
            return DEFAULT_TIMEZONE;
        }
        return CanonicalizeTimeZoneName(timeZone);
    }

    // FIXME: spec issue - comment in 9.1 unclear:
    // "For locales that in current usage would include a script subtag (such as Chinese locales), old-style language
    // tags without script subtags must be included [...]"
    // Maybe provide list of 'old-style language tags'?
    /* @formatter:off */
    // Generated from CLDR 32
    private static final String[] oldStyleLanguageTags = {
        "az-Latn-AZ", "az-AZ",
        "bs-Latn-BA", "bs-BA",
        "pa-Guru-IN", "pa-IN",
        "pa-Arab-PK", "pa-PK",
        "shi-Tfng-MA", "shi-MA",
        "sr-Cyrl-BA", "sr-BA",
        "sr-Latn-ME", "sr-ME",
        "sr-Cyrl-RS", "sr-RS",
        "sr-Cyrl-XK", "sr-XK",
        "uz-Arab-AF", "uz-AF",
        "uz-Latn-UZ", "uz-UZ",
        "vai-Vaii-LR", "vai-LR",
        "yue-Hans-CN", "yue-CN",
        "yue-Hant-HK", "yue-HK",
        "zh-Hans-CN", "zh-CN",
        "zh-Hant-HK", "zh-HK",
        "zh-Hant-MO", "zh-MO",
        "zh-Hans-SG", "zh-SG",
        "zh-Hant-TW", "zh-TW",
    };
    /* @formatter:on */

    /**
     * 9.1 Internal Properties of Service Constructors
     * 
     * @param locales
     *            the supported locales
     * @return the set of available locales
     */
    public static Set<String> GetAvailableLocales(Collection<String> locales) {
        String[] oldTags = oldStyleLanguageTags;
        HashSet<String> available = new LRUHashSet(locales);
        for (int i = 0; i < oldTags.length; i += 2) {
            if (available.contains(oldTags[i])) {
                available.add(oldTags[i + 1]);
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
        /** calendar */
        ca,

        /** collation */
        co,

        /** hourCycle */
        hc,

        /** colCaseFirst */
        kf,

        /** colNumeric */
        kn,

        /** numbers */
        nu;

        static ExtensionKey forName(String name, int index) {
            assert index + 2 <= name.length();
            char c0 = name.charAt(index), c1 = name.charAt(index + 1);
            switch (c0) {
            case 'c':
                return c1 == 'a' ? ca : c1 == 'o' ? co : null;
            case 'h':
                return c1 == 'c' ? hc : null;
            case 'k':
                return c1 == 'f' ? kf : c1 == 'n' ? kn : null;
            case 'n':
                return c1 == 'u' ? nu : null;
            default:
                return null;
            }
        }
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
        /* steps 2-8 (string only) */
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
        long len = ToLength(cx, Get(cx, o, "length"));
        /* steps 6-7 */
        for (long k = 0; k < len; ++k) {
            /* step 7.a */
            long pk = k;
            /* step 7.b */
            boolean kPresent = HasProperty(cx, o, pk);
            /* step 7.c */
            if (kPresent) {
                /* step 7.c.i */
                Object kValue = Get(cx, o, pk);
                /* step 7.c.ii */
                if (!(Type.isString(kValue) || Type.isObject(kValue))) {
                    throw newTypeError(cx, Messages.Key.IntlInvalidLanguageTagType, Type.of(kValue).toString());
                }
                /* step 7.c.iii */
                String tag = ToFlatString(cx, kValue);
                /* step 7.c.iv */
                LanguageTag langTag = IsStructurallyValidLanguageTag(tag);
                if (langTag == null) {
                    throw newRangeError(cx, Messages.Key.IntlStructurallyInvalidLanguageTag, tag);
                }
                /* step 7.c.v */
                tag = CanonicalizeLanguageTag(langTag);
                /* step 7.c.vi */
                seen.add(tag);
            }
        }
        /* step 8 */
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
        /* step 2 */
        for (String locale : requestedLocales) {
            /* step 2.a */
            LanguageTagUnicodeExtension unicodeExt = UnicodeLocaleExtSequence(locale);
            String noExtensionsLocale = unicodeExt.getLanguageTag();
            /* step 2.b */
            String availableLocale = BestAvailableLocale(availableLocales.get(), noExtensionsLocale);
            /* step 2.c */
            if (availableLocale != null) {
                /* steps 1, 2.c.i-iii */
                return new LocaleMatch(availableLocale, unicodeExt);
            }
        }
        /* steps 1, 3-5 */
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
            String noExtLocale = RemoveUnicodeLocaleExtension(locale);
            BestFitMatch match = BestFitAvailableLocale(matcher, available, noExtLocale);
            double score = match.score;
            if (score >= BEST_FIT_MIN_MATCH && (bestMatch == null || score > bestMatch.score)) {
                // System.out.printf("%s -> %s [%f]%n", locale, match.locale, match.score);
                bestMatch = match;
                bestMatchCandidate = locale;
                if (score == 1.0) {
                    break;
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
        return new LocaleMatcher(priorityList);
    }

    /**
     * Hard cache to reduce time required to finish intl-tests. The cache size is limited by the total number of
     * available locales.
     */
    private static final ConcurrentHashMap<String, LocaleEntry> maximizedLocales = new ConcurrentHashMap<>();

    private static LocaleEntry GetMaximizedLocale(LocaleMatcher matcher, String locale) {
        LocaleEntry entry = maximizedLocales.get(locale);
        if (entry == null) {
            entry = createLocaleEntry(matcher, locale);
            maximizedLocales.putIfAbsent(locale, entry);
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

    private static BestFitMatch BestFitAvailableLocale(LocaleMatcher matcher, Set<String> availableLocales,
            String requestedLocale) {
        // Return early if requested locale is available as-is.
        if (availableLocales.contains(requestedLocale)) {
            return new BestFitMatch(requestedLocale, 1.0);
        }
        // Check cache next.
        LRUHashSet lruAvailableLocales = LRUHashSet.from(availableLocales);
        BestFitMatch lastResolved = LRUHashSet.get(lruAvailableLocales, requestedLocale);
        if (lastResolved != null) {
            return lastResolved;
        }

        // Perform two-passes to compute best-fit available locale:
        // 1) Compare maximized locales.
        // 2) If no match was found, compare all available locales.
        LocaleEntry requested = createLocaleEntry(matcher, requestedLocale);
        String bestMatchLocale = null;
        LocaleEntry bestMatchEntry = null;
        double bestMatch = Double.NEGATIVE_INFINITY;
        for (String available : availableLocales) {
            LocaleEntry entry = GetMaximizedLocale(matcher, available);
            if (requested.maximized.equals(entry.maximized)) {
                double match = matcher.match(requested.getCanonicalized(), requested.getMaximized(),
                        entry.getCanonicalized(), entry.getMaximized());
                if (match > bestMatch || (match == bestMatch && isBetterMatch(requested, bestMatchEntry, entry))) {
                    bestMatchLocale = available;
                    bestMatchEntry = entry;
                    bestMatch = match;
                }
            }
        }
        if (bestMatchLocale == null) {
            for (String available : availableLocales) {
                LocaleEntry entry = GetMaximizedLocale(matcher, available);
                double match = matcher.match(requested.getCanonicalized(), requested.getMaximized(),
                        entry.getCanonicalized(), entry.getMaximized());
                // if (match > 0.10) {
                // System.out.printf("[%s; %s, %s] -> [%s; %s, %s] => %f%n", requestedLocale,
                // requested.getCanonicalized(), requested.getMaximized(), available,
                // entry.getCanonicalized(), entry.getMaximized(), match);
                // }
                if (match > bestMatch || (match == bestMatch && isBetterMatch(requested, bestMatchEntry, entry))) {
                    bestMatchLocale = available;
                    bestMatchEntry = entry;
                    bestMatch = match;
                }
            }
        }

        // Create result object and store in cache.
        BestFitMatch result = new BestFitMatch(bestMatchLocale, bestMatch);
        LRUHashSet.set(lruAvailableLocales, requestedLocale, result);
        return result;
    }

    private static String BestFitAvailableLocale(Set<String> availableLocales, String requestedLocale) {
        if (availableLocales.contains(requestedLocale)) {
            return requestedLocale;
        }
        LocaleMatcher matcher = CreateDefaultMatcher();
        BestFitMatch availableLocaleMatch = BestFitAvailableLocale(matcher, availableLocales, requestedLocale);
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
    private static boolean isBetterMatch(LocaleEntry requested, LocaleEntry oldMatch, LocaleEntry newMatch) {
        ULocale canonicalized = requested.getCanonicalized();
        ULocale oldCanonicalized = oldMatch.getCanonicalized();
        ULocale newCanonicalized = newMatch.getCanonicalized();

        // Compare language.
        String language = canonicalized.getLanguage();
        String newLanguage = newCanonicalized.getLanguage();
        String oldLanguage = oldCanonicalized.getLanguage();
        assert !newLanguage.isEmpty() && !oldLanguage.isEmpty();
        if (newLanguage.equals(language) && !oldLanguage.equals(language)) {
            return true;
        }

        // Compare script.
        String script = canonicalized.getScript();
        String newScript = newCanonicalized.getScript();
        String oldScript = oldCanonicalized.getScript();
        if ((newScript.equals(script) && !oldScript.equals(script))
                || (newScript.isEmpty() && !oldScript.isEmpty() && !oldScript.equals(script))) {
            return true;
        }

        // Compare region.
        String region = canonicalized.getCountry();
        String newRegion = newCanonicalized.getCountry();
        String oldRegion = oldCanonicalized.getCountry();
        if ((newRegion.equals(script) && !oldRegion.equals(region))
                || (newRegion.isEmpty() && !oldRegion.isEmpty() && !oldRegion.equals(region))) {
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

        public int size() {
            return values.size();
        }

        public boolean contains(ExtensionKey key) {
            return values.containsKey(key);
        }

        public String get(ExtensionKey key) {
            return values.get(key);
        }

        public void set(ExtensionKey key, String value) {
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
     * 9.2.5 UnicodeExtensionValue ( extension, key )
     * 
     * @param extension
     *            the Unicode locale extension sequence
     * @return the map <code>&laquo; extension key &rarr; extension value &raquo;</code>
     */
    public static EnumMap<ExtensionKey, String> UnicodeExtensionValues(String extension) {
        // Return early if no extensions are present.
        if (extension.isEmpty()) {
            return null;
        }

        /*
         * http://unicode.org/reports/tr35/#Unicode_locale_identifier
         *
         * unicode_locale_extensions = sep "u" ((sep keyword)+ | (sep attribute)+ (sep keyword)*) ;
         * keyword = key (sep type)? ;
         * key = alphanum{2} ;
         * type = alphanum{3,8} (sep alphanum{3,8})* ;
         * attribute = alphanum{3,8} ;
         */
        final int KEY_LENGTH = 2;
        assert extension.startsWith("-u-") && extension.length() >= 3 + KEY_LENGTH : extension;
        final int length = extension.length();
        int startKeyword = 3;

        // Skip optional leading attributes.
        while (startKeyword < length) {
            int index = extension.indexOf('-', startKeyword);
            int endIndex = index != -1 ? index : extension.length();
            if (endIndex - startKeyword > KEY_LENGTH) {
                startKeyword = endIndex + 1;
            } else {
                break;
            }
        }

        // Read keywords.
        EnumMap<ExtensionKey, String> map = new EnumMap<>(ExtensionKey.class);
        while (startKeyword < length) {
            assert startKeyword + KEY_LENGTH <= length;
            assert startKeyword + KEY_LENGTH == length || extension.charAt(startKeyword + KEY_LENGTH) == '-';
            ExtensionKey key = ExtensionKey.forName(extension, startKeyword);
            int startType = startKeyword + KEY_LENGTH + 1, nextKeyword = startType;
            while (nextKeyword < length) {
                int index = extension.indexOf('-', nextKeyword);
                int endIndex = index != -1 ? index : extension.length();
                if (endIndex - nextKeyword > KEY_LENGTH) {
                    nextKeyword = endIndex + 1;
                } else {
                    break;
                }
            }
            // Supported extension key and not a duplicate keyword.
            if (key != null && !map.containsKey(key)) {
                if (startType < nextKeyword) {
                    map.put(key, extension.substring(startType, nextKeyword - 1));
                } else {
                    map.put(key, "");
                }
            }
            startKeyword = nextKeyword;
        }
        return map;
    }

    /**
     * 9.2.6 ResolveLocale (availableLocales, requestedLocales, options, relevantExtensionKeys, localeData)
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
            Set<String> requestedLocales, OptionsRecord options, List<ExtensionKey> relevantExtensionKeys,
            LocaleData localeData) {
        /* steps 1-3 */
        OptionsRecord.MatcherType matcher = options.getLocaleMatcher();
        LocaleMatch r;
        if (matcher == OptionsRecord.MatcherType.Lookup) {
            r = LookupMatcher(realm, availableLocales, requestedLocales);
        } else {
            r = BestFitMatcher(realm, availableLocales, requestedLocales);
        }
        /* step 4 */
        String foundLocale = r.getLocale();
        /* steps 5-6 (moved) */
        /* steps 8.a-b */
        LocaleDataInfo foundLocaleData = localeData.info(ULocale.forLanguageTag(foundLocale));

        // Fast path for steps 7-9 when no Unicode extensions and options are present.
        EnumMap<ExtensionKey, String> values = new EnumMap<>(ExtensionKey.class);
        if (r.getExtension().isEmpty() && options.size() == 0) {
            /* step 7 (not applicable) */
            /* step 8 */
            for (ExtensionKey key : relevantExtensionKeys) {
                values.put(key, foundLocaleData.defaultValue(key));
            }
            /* step 9 (not applicable) */
        } else {
            /* step 7 */
            StringBuilder supportedExtension = new StringBuilder("-u");
            /* step 8 */
            EnumMap<ExtensionKey, String> extensionValues = UnicodeExtensionValues(r.getExtension());
            for (ExtensionKey key : relevantExtensionKeys) {
                /* steps 8.c-d */
                List<String> keyLocaleData = foundLocaleData.entries(key);
                /* steps 8.e-f */
                String value = keyLocaleData.get(0);
                /* step 8.g */
                String supportedExtensionAddition = "";
                /* step 8.h */
                if (extensionValues != null) {
                    /* step 8.h.i */
                    String requestedValue = extensionValues.get(key);
                    /* step 8.h.ii */
                    if (requestedValue != null) {
                        /* steps 8.h.ii.1-2 */
                        if (!requestedValue.isEmpty()) {
                            /* step 8.h.ii.1 */
                            if (keyLocaleData.contains(requestedValue)) {
                                value = requestedValue;
                                supportedExtensionAddition = "-" + key.name() + "-" + value;
                            }
                        } else if (keyLocaleData.contains("true")) {
                            /* step 8.h.ii.2 */
                            value = "true";
                        }
                    }
                }
                /* step 8.i */
                if (options.contains(key)) {
                    /* steps 8.i.i-ii */
                    String optionsValue = options.get(key);
                    /* step 8.i.iii */
                    if (optionsValue != null && keyLocaleData.contains(optionsValue) && !optionsValue.equals(value)) {
                        value = optionsValue;
                        supportedExtensionAddition = "";
                    }
                }
                /* step 8.j */
                values.put(key, value);
                /* step 8.k */
                supportedExtension.append(supportedExtensionAddition);
            }
            /* step 9 */
            if (supportedExtension.length() > 2) {
                /* step 9.a */
                int privateIndex = foundLocale.indexOf("-x-");
                /* steps 9.b-c */
                if (privateIndex == -1) {
                    /* step 9.b */
                    foundLocale += supportedExtension;
                } else {
                    /* step 9.c */
                    /* step 9.c.i */
                    String preExtension = foundLocale.substring(0, privateIndex);
                    /* step 9.c.ii */
                    String postExtension = foundLocale.substring(privateIndex);
                    /* step 9.c.iii */
                    foundLocale = preExtension + supportedExtension + postExtension;
                }
                /* step 9.d */
                LanguageTag languageTag = IsStructurallyValidLanguageTag(foundLocale);
                assert languageTag != null : foundLocale;
                /* step 9.e */
                foundLocale = CanonicalizeLanguageTag(languageTag);
            }
        }
        /* steps 5-6, 10-11 */
        return new ResolvedLocale(r.getLocale(), foundLocale, values);
    }

    /**
     * 9.2.7 LookupSupportedLocales (availableLocales, requestedLocales)
     * 
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @return the list of supported locales
     */
    public static List<String> LookupSupportedLocales(Set<String> availableLocales, Set<String> requestedLocales) {
        int numberOfRequestedLocales = requestedLocales.size();
        // Fast path for none requested locale.
        if (numberOfRequestedLocales == 0) {
            return emptyList();
        }
        // Fast path for one requested locale.
        if (numberOfRequestedLocales == 1) {
            String locale = requestedLocales.iterator().next();
            String noExtensionsLocale = RemoveUnicodeLocaleExtension(locale);
            String availableLocale = BestAvailableLocale(availableLocales, noExtensionsLocale);
            if (availableLocale != null) {
                return singletonList(locale);
            }
            return emptyList();
        }
        /* step 1 */
        ArrayList<String> subset = new ArrayList<>();
        /* step 2 */
        for (String locale : requestedLocales) {
            /* step 2.a */
            String noExtensionsLocale = RemoveUnicodeLocaleExtension(locale);
            /* step 2.b */
            String availableLocale = BestAvailableLocale(availableLocales, noExtensionsLocale);
            /* step 2.c */
            if (availableLocale != null) {
                subset.add(locale);
            }
        }
        /* step 3 */
        return subset;
    }

    /**
     * 9.2.8 BestFitSupportedLocales (availableLocales, requestedLocales)
     * 
     * @param availableLocales
     *            the set of available locales
     * @param requestedLocales
     *            the set of requested locales
     * @return the list of best-fit supported locales
     */
    public static List<String> BestFitSupportedLocales(Set<String> availableLocales, Set<String> requestedLocales) {
        if (!BEST_FIT_SUPPORTED) {
            return LookupSupportedLocales(availableLocales, requestedLocales);
        }
        int numberOfRequestedLocales = requestedLocales.size();
        if (numberOfRequestedLocales == 0) {
            return emptyList();
        }
        if (numberOfRequestedLocales == 1) {
            String locale = requestedLocales.iterator().next();
            String noExtensionsLocale = RemoveUnicodeLocaleExtension(locale);
            if (BestFitAvailableLocale(availableLocales, noExtensionsLocale) != null) {
                return singletonList(locale);
            }
            return emptyList();
        }
        LocaleMatcher matcher = CreateDefaultMatcher();
        ArrayList<String> subset = new ArrayList<>();
        for (String locale : requestedLocales) {
            String noExtensionsLocale = RemoveUnicodeLocaleExtension(locale);
            BestFitMatch availableLocaleMatch = BestFitAvailableLocale(matcher, availableLocales, noExtensionsLocale);
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
     * 9.2.9 SupportedLocales (availableLocales, requestedLocales, options)
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
        /* steps 1-2 */
        String matcher;
        if (!Type.isUndefined(options)) {
            ScriptObject optionsObj = ToObject(cx, options);
            matcher = GetStringOption(cx, optionsObj, "localeMatcher", set("lookup", "best fit"), "best fit");
        } else {
            matcher = "best fit";
        }
        /* steps 3-4 */
        List<String> supportedLocales;
        if ("best fit".equals(matcher)) {
            supportedLocales = BestFitSupportedLocales(availableLocales, requestedLocales);
        } else {
            supportedLocales = LookupSupportedLocales(availableLocales, requestedLocales);
        }
        /* steps 5-7 */
        ArrayObject subset = ArrayCreate(cx, supportedLocales.size());
        int index = 0;
        for (Object value : supportedLocales) {
            subset.defineOwnProperty(cx, index++, new PropertyDescriptor(value, false, true, false));
        }
        PropertyDescriptor nonConfigurableWritable = new PropertyDescriptor();
        nonConfigurableWritable.setConfigurable(false);
        nonConfigurableWritable.setWritable(false);
        subset.defineOwnProperty(cx, "length", nonConfigurableWritable);
        /* step 8 */
        return subset;
    }

    /**
     * 9.2.10 GetOption (options, property, type, values, fallback)
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
    public static String GetStringOption(ExecutionContext cx, ScriptObject options, String property, Set<String> values,
            String fallback) {
        /* step 1 */
        Object value = Get(cx, options, property);
        /* step 2 */
        if (!Type.isUndefined(value)) {
            /* steps 2.a-b (not applicable) */
            /* step 2.c */
            String val = ToFlatString(cx, value);
            /* step 2.d */
            if (values != null && !values.contains(val)) {
                throw newRangeError(cx, Messages.Key.IntlInvalidOption, val, property);
            }
            /* step 2.e */
            return val;
        }
        /* step 3 */
        return fallback;
    }

    /**
     * 9.2.10 GetOption (options, property, type, values, fallback)
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
    public static Boolean GetBooleanOption(ExecutionContext cx, ScriptObject options, String property,
            Boolean fallback) {
        /* step 1 */
        Object value = Get(cx, options, property);
        /* step 2 */
        if (!Type.isUndefined(value)) {
            /* steps 2.a, c-d (not applicable) */
            /* steps 2.b, e */
            return ToBoolean(value);
        }
        /* step 3 */
        return fallback;
    }

    /**
     * 9.2.11 DefaultNumberOption ( value, minimum, maximum, fallback )
     * 
     * @param cx
     *            the execution context
     * @param property
     *            the property name
     * @param value
     *            the property value
     * @param minimum
     *            the minimum value
     * @param maximum
     *            the maximum value
     * @param fallback
     *            the fallback value
     * @return the number option
     */
    public static int DefaultNumberOption(ExecutionContext cx, String property, Object value, int minimum, int maximum,
            int fallback) {
        assert minimum <= maximum;
        assert minimum <= fallback && fallback <= maximum;
        /* step 1 */
        if (!Type.isUndefined(value)) {
            /* step 1.a */
            double val = ToNumber(cx, value);
            /* step 1.b */
            if (Double.isNaN(val) || val < minimum || val > maximum) {
                throw newRangeError(cx, Messages.Key.IntlInvalidValue, ToString(val), property);
            }
            /* step 1.c */
            return (int) Math.floor(val);
        }
        /* step 2 */
        return fallback;
    }

    /**
     * 9.2.12 GetNumberOption (options, property, minimum, maximum, fallback)
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
    public static int GetNumberOption(ExecutionContext cx, ScriptObject options, String property, int minimum,
            int maximum, int fallback) {
        /* step 1 */
        Object value = Get(cx, options, property);
        /* step 2 */
        return DefaultNumberOption(cx, property, value, minimum, maximum, fallback);
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }
}
