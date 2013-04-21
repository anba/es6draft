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
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.OptionsRecord.MatcherType;
import com.github.anba.es6draft.runtime.objects.intl.LanguageTagParser.LanguageTag;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
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
     */
    public static LanguageTag IsStructurallyValidLanguageTag(String locale) {
        return new LanguageTagParser(locale).parse();
    }

    /**
     * 6.2.3 CanonicalizeLanguageTag (locale)
     */
    public static String CanonicalizeLanguageTag(LanguageTag locale) {
        return locale.canonicalize();
    }

    /**
     * 6.2.4 DefaultLocale ()
     */
    public static String DefaultLocale(Realm realm) {
        return realm.getLocale().toLanguageTag();
    }

    /**
     * 6.3.1 IsWellFormedCurrencyCode (currency)
     */
    public static boolean IsWellFormedCurrencyCode(ExecutionContext cx, Object currency) {
        String s = ToFlatString(cx, currency);
        if (s.length() != 3) {
            return false;
        }
        return isAlpha(s.charAt(0)) && isAlpha(s.charAt(1)) && isAlpha(s.charAt(2));
    }

    private static final boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static Map<String, String[]> oldStyleLanguageTags;
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
    }

    /**
     * 9.1 Internal Properties of Service Constructors
     */
    public static interface LocaleData {
        LocaleDataInfo info(ULocale locale);
    }

    /**
     * 9.1 Internal Properties of Service Constructors
     */
    public static interface LocaleDataInfo {
        List<String> entries(ExtensionKey extensionKey);
    }

    /**
     * 9.2.1 CanonicalizeLocaleList (locales)
     */
    public static Set<String> CanonicalizeLocaleList(ExecutionContext cx, Object locales) {
        if (Type.isUndefined(locales)) {
            return emptySet();
        }
        Set<String> seen = new LinkedHashSet<>();
        if (Type.isString(locales)) {
            // handle the string-only case directly
            String tag = ToFlatString(cx, locales);
            LanguageTag langTag = IsStructurallyValidLanguageTag(tag);
            if (langTag == null) {
                throwRangeError(cx, Messages.Key.IntlStructurallyInvalidLanguageTag, tag);
            }
            tag = CanonicalizeLanguageTag(langTag);
            return singleton(tag);
        }
        ScriptObject o = ToObject(cx, locales);
        Object lenValue = Get(cx, o, "length");
        long len = ToUint32(cx, lenValue);
        for (long k = 0; k < len; ++k) {
            String pk = ToString(k);
            boolean kPresent = HasProperty(cx, o, pk);
            if (kPresent) {
                Object kValue = Get(cx, o, pk);
                if (!(Type.isString(kValue) || Type.isObject(pk))) {
                    throwTypeError(cx, Messages.Key.IncompatibleObject);
                }
                String tag = ToFlatString(cx, kValue);
                LanguageTag langTag = IsStructurallyValidLanguageTag(tag);
                if (langTag == null) {
                    throwRangeError(cx, Messages.Key.IntlStructurallyInvalidLanguageTag, tag);
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
     */
    public static LocaleMatch LookupMatcher(ExecutionContext cx, Set<String> availableLocales,
            Set<String> requestedLocales) {
        for (String locale : requestedLocales) {
            String[] unicodeExt = UnicodeLocaleExtSequence(locale);
            String noExtensionsLocale = unicodeExt[0];
            String availableLocale = BestAvailableLocale(availableLocales, noExtensionsLocale);
            if (availableLocale != null) {
                LocaleMatch result = new LocaleMatch();
                result.locale = availableLocale;
                if (!locale.equals(noExtensionsLocale)) {
                    result.extension = unicodeExt[1];
                    result.extensionIndex = locale.indexOf("-u-");
                }
                return result;
            }
        }
        LocaleMatch result = new LocaleMatch();
        result.locale = DefaultLocale(cx.getRealm());
        return result;
    }

    /**
     * 9.2.4 BestFitMatcher (availableLocales, requestedLocales)
     */
    public static LocaleMatch BestFitMatcher(ExecutionContext cx, Set<String> availableLocales,
            Set<String> requestedLocales) {
        return LookupMatcher(cx, availableLocales, requestedLocales);
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
     */
    public static ResolvedLocale ResolveLocale(ExecutionContext cx, Set<String> availableLocales,
            Set<String> requestedLocales, OptionsRecord options,
            List<ExtensionKey> relevantExtensionKeys, LocaleData localeData) {
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
        List<String> extensionSubtags = null;
        if (r.extension != null) {
            String extension = r.extension;
            extensionSubtags = Arrays.asList(extension.split("-"));
        }
        /* steps 6-7 */
        ResolvedLocale result = new ResolvedLocale();
        result.dataLocale = foundLocale;
        /* steps 8-11 */
        String supportedExtension = "-u";
        for (int i = 0, len = relevantExtensionKeys.size(); i < len; ++i) {
            ExtensionKey key = relevantExtensionKeys.get(i);
            List<String> keyLocaleData = foundLocaleData.entries(key);
            String value = keyLocaleData.get(0);
            String supportedExtensionAddition = "";
            if (extensionSubtags != null) {
                int keyPos = extensionSubtags.indexOf(key.name());
                if (keyPos != -1) {
                    if (keyPos + 1 < extensionSubtags.size()
                            && extensionSubtags.get(keyPos + 1).length() > 2) {
                        String requestedValue = extensionSubtags.get(keyPos + 1);
                        int valuePos = keyLocaleData.indexOf(requestedValue);
                        if (valuePos != -1) {
                            value = requestedValue;
                            supportedExtensionAddition = "-" + key + "-" + value;
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
     */
    public static List<String> BestFitSupportedLocales(ExecutionContext cx,
            Set<String> availableLocales, Set<String> requestedLocales) {
        return LookupSupportedLocales(cx, availableLocales, requestedLocales);
    }

    /**
     * 9.2.8 SupportedLocales (availableLocales, requestedLocales, options)
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
     */
    public static String GetStringOption(ExecutionContext cx, ScriptObject options,
            String property, Set<String> values, String fallback) {
        Object value = Get(cx, options, property);
        if (!Type.isUndefined(value)) {
            String val = ToFlatString(cx, value);
            if (values != null && !values.contains(val)) {
                throwRangeError(cx, Messages.Key.IntlInvalidOption, val);
            }
            return val;
        }
        return fallback;
    }

    /**
     * 9.2.9 GetOption (options, property, type, values, fallback)
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
     */
    public static double GetNumberOption(ExecutionContext cx, ScriptObject options,
            String property, double minimum, double maximum, double fallback) {
        Object value = Get(cx, options, property);
        if (!Type.isUndefined(value)) {
            double val = ToNumber(cx, value);
            if (Double.isNaN(val) || val < minimum || val > maximum) {
                throwRangeError(cx, Messages.Key.IntlInvalidOption, Double.toString(val));
            }
            return Math.floor(val);
        }
        return fallback;
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }
}
