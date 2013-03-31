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
import static com.github.anba.es6draft.runtime.types.builtins.ListIterator.FromListIterator;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllformedLocaleException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.ResolveLocaleOptions.MatcherType;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>9 Locale and Parameter Negotiation</h1><br>
 * <h2>9.2 Abstract Operations</h2>
 */
public final class IntlAbstractOperations {
    private IntlAbstractOperations() {
    }

    /**
     * 6.2.2 IsStructurallyValidLanguageTag (locale)
     */
    public static boolean IsStructurallyValidLanguageTag(String tag) {
        try {
            new Locale.Builder().setLanguageTag(tag).build();
            return true;
        } catch (IllformedLocaleException e) {
            return false;
        }
    }

    /**
     * 6.2.3 CanonicalizeLanguageTag (locale)
     */
    public static String CanonicalizeLanguageTag(String tag) {
        return new Locale.Builder().setLanguageTag(tag).build().toLanguageTag();
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

    public static abstract class ExtensionKey<Value> {

    }

    /**
     * 9.1 Internal Properties of Service Constructors
     */
    public static abstract class LocaleData {
        public <Value> Value[] getValue(Locale locale, ExtensionKey<Value> key) {
            return null;
        }
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
            locales = CreateArrayFromList(cx, singletonList(locales));
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
                    // TODO: error message
                    throwTypeError(cx, Messages.Key.IncompatibleObject);
                }
                String tag = ToFlatString(cx, kValue);
                if (!IsStructurallyValidLanguageTag(tag)) {
                    // TODO: error message
                    throwRangeError(cx, Messages.Key.InvalidDescriptor);
                }
                tag = CanonicalizeLanguageTag(tag);
                if (seen.contains(tag)) {
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

    public static final class LocaleMatch {
        private String locale;
        private String extension;
        private int extensionIndex;

        private LocaleMatch() {
        }

        public String getLocale() {
            return locale;
        }

        public String getExtension() {
            return extension;
        }

        public int getExtensionIndex() {
            return extensionIndex;
        }
    }

    /**
     * 9.2.3 LookupMatcher (availableLocales, requestedLocales)
     */
    public static LocaleMatch LookupMatcher(ExecutionContext cx, Set<String> availableLocales,
            Set<String> requestedLocales) {
        for (String locale : requestedLocales) {
            String noExtensionsLocale = removeUnicodeLocaleExtSequence(locale);
            String availableLocale = BestAvailableLocale(availableLocales, noExtensionsLocale);
            if (availableLocale != null) {
                LocaleMatch result = new LocaleMatch();
                result.locale = availableLocale;
                if (!locale.equals(noExtensionsLocale)) {
                    result.extension = "";
                    result.extensionIndex = -1;
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

    public static final class ResolveLocaleOptions {
        public enum MatcherType {
            Lookup, BestFit
        }

        public MatcherType localeMatcher;

    }

    public static final class ResolvedLocale {

        public String dataLocale;

    }

    /**
     * 9.2.5 ResolveLocale (availableLocales, requestedLocales, options, relevantExtensionKeys,
     * localeData)
     */
    public static ResolvedLocale ResolveLocale(ExecutionContext cx, Set<String> availableLocales,
            Set<String> requestedLocales, ResolveLocaleOptions options,
            ScriptObject relevantExtensionKeys, ScriptObject localeData) {
        MatcherType matcher = options.localeMatcher;
        LocaleMatch r;
        if (matcher == MatcherType.Lookup) {
            r = LookupMatcher(cx, availableLocales, requestedLocales);
        } else {
            r = BestFitMatcher(cx, availableLocales, requestedLocales);
        }
        String foundLocale = r.locale;
        List<String> extensionSubtags = null;
        if (r.extension != null) {
            String extension = r.extension;
            int extensionIndex = r.extensionIndex;
            extensionSubtags = Arrays.asList(extension.split("-"));
        }
        ResolvedLocale result = new ResolvedLocale();
        result.dataLocale = foundLocale;
        String supportedExtension = "-u";
        long len = ToUint32(cx, Get(cx, relevantExtensionKeys, "length"));
        for (long i = 0; i < len; ++i) {
            String key = (String) Get(cx, relevantExtensionKeys, ToString(i));
            ScriptObject foundLocaleData = (ScriptObject) Get(cx, localeData, foundLocale);
            ScriptObject keyLocaleData = (ScriptObject) Get(cx, foundLocaleData, key);
            Object value = Get(cx, keyLocaleData, "0");
            String supportedExtensionAddition = "";
            if (extensionSubtags != null) {
                int keyPos = extensionSubtags.indexOf(key);
                if (keyPos != -1) {
                    if (keyPos + 1 < extensionSubtags.size()
                            && extensionSubtags.get(keyPos + 1).length() > 2) {
                        String requestedValue = extensionSubtags.get(keyPos + 1);
                        // int valuePos = keyLocaleData
                    }
                }
            }
        }
        return null;
    }

    /**
     * 9.2.6 LookupSupportedLocales (availableLocales, requestedLocales)
     */
    public static ScriptObject LookupSupportedLocales(ExecutionContext cx,
            Set<String> availableLocales, Set<String> requestedLocales) {
        List<String> subset = new ArrayList<>();
        for (String locale : requestedLocales) {
            String noExtensionsLocale = removeUnicodeLocaleExtSequence(locale);
            String availableLocale = BestAvailableLocale(availableLocales, noExtensionsLocale);
            if (availableLocale != null) {
                subset.add(locale);
            }
        }
        ScriptObject subsetArray = AbstractOperations.CreateArrayFromList(cx, subset);
        return subsetArray;
    }

    /**
     * 9.2.7 BestFitSupportedLocales (availableLocales, requestedLocales)
     */
    public static ScriptObject BestFitSupportedLocales(ExecutionContext cx,
            Set<String> availableLocales, Set<String> requestedLocales) {
        return LookupSupportedLocales(cx, availableLocales, requestedLocales);
    }

    /**
     * 9.2.8 SupportedLocales (availableLocales, requestedLocales, options)
     */
    public static ScriptObject SupportedLocales(ExecutionContext cx, Set<String> availableLocales,
            Set<String> requestedLocales, Object options) {
        boolean useBestFit = true;
        if (!Type.isUndefined(options)) {
            ScriptObject opts = ToObject(cx, options);
            Object matcher = Get(cx, opts, "localeMatcher");
            if (!Type.isUndefined(matcher)) {
                String m = ToFlatString(cx, matcher);
                if ("lookup".equals(m)) {
                    useBestFit = false;
                } else if (!"best fit".equals(m)) {
                    throwRangeError(cx, Messages.Key.InvalidPrecision);
                }
            }
        }
        ScriptObject subset;
        if (useBestFit) {
            subset = BestFitSupportedLocales(cx, availableLocales, requestedLocales);
        } else {
            subset = LookupSupportedLocales(cx, availableLocales, requestedLocales);
        }
        PropertyDescriptor nonConfigurableWritable = new PropertyDescriptor();
        nonConfigurableWritable.setConfigurable(false);
        nonConfigurableWritable.setWritable(false);
        Iterator<?> keys = FromListIterator(cx, subset.ownPropertyKeys(cx));
        while (keys.hasNext()) {
            Object key = ToPropertyKey(cx, keys.next());
            if (key instanceof String) {
                DefinePropertyOrThrow(cx, subset, (String) key, nonConfigurableWritable);
            } else {
                assert key instanceof Symbol;
                DefinePropertyOrThrow(cx, subset, (Symbol) key, nonConfigurableWritable);
            }
        }
        return subset;
    }

    /**
     * 9.2.9 GetOption (options, property, type, values, fallback)
     */
    public static <T> T getOption(ExecutionContext cx, ScriptObject options, String property,
            Type type, Set<T> values, T fallback) {
        Object value = Get(cx, options, property);
        if (!Type.isUndefined(value)) {
            assert type == Type.Boolean || type == Type.String;
            if (type == Type.Boolean) {
                value = ToBoolean(value);
            }
            if (type == Type.String) {
                value = ToFlatString(cx, value);
            }
            if (values != null && values.contains(value)) {
                throwRangeError(cx, Messages.Key.InvalidPrecision);
            }
            @SuppressWarnings("unchecked")
            T val = (T) value;
            return val;
        }
        return fallback;
    }

    /**
     * 9.2.9 GetOption (options, property, type, values, fallback)
     */
    public static String getStringOption(ExecutionContext cx, ScriptObject options,
            String property, Set<String> values, String fallback) {
        Object value = Get(cx, options, property);
        if (!Type.isUndefined(value)) {
            String val = ToFlatString(cx, value);
            if (values != null && values.contains(val)) {
                throwRangeError(cx, Messages.Key.InvalidPrecision);
            }
            return val;
        }
        return fallback;
    }

    /**
     * 9.2.9 GetOption (options, property, type, values, fallback)
     */
    public static boolean getBooleanOption(ExecutionContext cx, ScriptObject options,
            String property, boolean fallback) {
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
                throwRangeError(cx, Messages.Key.InvalidPrecision);
            }
            return Math.floor(val);
        }
        return fallback;
    }

    private static String removeUnicodeLocaleExtSequence(String locale) {
        return locale;
    }
}
