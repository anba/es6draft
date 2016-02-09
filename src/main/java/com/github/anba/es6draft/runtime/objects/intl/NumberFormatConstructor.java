/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.*;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Lazy;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
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
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.ULocale;

/**
 * <h1>11 NumberFormat Objects</h1>
 * <ul>
 * <li>11.1 The Intl.NumberFormat Constructor
 * <li>11.2 Properties of the Intl.NumberFormat Constructor
 * </ul>
 */
public final class NumberFormatConstructor extends BuiltinConstructor implements Initializable {
    /** [[availableLocales]] */
    private final Lazy<Set<String>> availableLocales = new Lazy<Set<String>>() {
        @Override
        protected Set<String> computeValue() {
            return GetAvailableLocales(LanguageData.getAvailableNumberFormatLocales());
        }
    };

    /**
     * [[availableLocales]]
     * 
     * @param cx
     *            the execution context
     * @return the set of available locales supported by {@code Intl.NumberFormat}
     */
    public static Set<String> getAvailableLocales(ExecutionContext cx) {
        return getAvailableLocalesLazy(cx).get();
    }

    private static Lazy<Set<String>> getAvailableLocalesLazy(ExecutionContext cx) {
        NumberFormatConstructor numberFormat = (NumberFormatConstructor) cx.getIntrinsic(Intrinsics.Intl_NumberFormat);
        return numberFormat.availableLocales;
    }

    /** [[relevantExtensionKeys]] */
    private static final List<ExtensionKey> relevantExtensionKeys = asList(ExtensionKey.nu);

    /** [[localeData]] */
    private static final class NumberFormatLocaleData implements LocaleData {
        @Override
        public LocaleDataInfo info(ULocale locale) {
            return new NumberFormatLocaleDataInfo(locale);
        }
    }

    /** [[localeData]] */
    private static final class NumberFormatLocaleDataInfo implements LocaleDataInfo {
        private final ULocale locale;

        public NumberFormatLocaleDataInfo(ULocale locale) {
            this.locale = locale;
        }

        @Override
        public String defaultValue(ExtensionKey extensionKey) {
            switch (extensionKey) {
            case nu:
                return NumberingSystem.getInstance(locale).getName();
            default:
                throw new IllegalArgumentException(extensionKey.name());
            }
        }

        @Override
        public List<String> entries(ExtensionKey extensionKey) {
            switch (extensionKey) {
            case nu:
                return getNumberInfo();
            default:
                throw new IllegalArgumentException(extensionKey.name());
            }
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
     * Constructs a new NumberFormat constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public NumberFormatConstructor(Realm realm) {
        super(realm, "NumberFormat", 0);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public NumberFormatConstructor clone() {
        return new NumberFormatConstructor(getRealm());
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(asList(elements));
    }

    /**
     * 11.1.1 InitializeNumberFormat (numberFormat, locales, options)
     * 
     * @param cx
     *            the execution context
     * @param numberFormat
     *            the number format object
     * @param locales
     *            the locales array
     * @param opts
     *            the options object
     */
    public static void InitializeNumberFormat(ExecutionContext cx, NumberFormatObject numberFormat, Object locales,
            Object opts) {
        /* steps 1-2 (FIXME: spec bug - unnecessary internal slot) */
        /* steps 3-4 */
        Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
        /* steps 5-6 */
        ScriptObject options;
        if (Type.isUndefined(opts)) {
            options = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        } else {
            options = ToObject(cx, opts);
        }
        /* steps 8-9 */
        String matcher = GetStringOption(cx, options, "localeMatcher", set("lookup", "best fit"), "best fit");
        /* step 7, 10 */
        OptionsRecord opt = new OptionsRecord(OptionsRecord.MatcherType.forName(matcher));
        /* step 11 */
        NumberFormatLocaleData localeData = new NumberFormatLocaleData();
        /* step 12 */
        ResolvedLocale r = ResolveLocale(cx.getRealm(), getAvailableLocalesLazy(cx), requestedLocales, opt,
                relevantExtensionKeys, localeData);
        /* step 13 */
        numberFormat.setLocale(r.getLocale());
        /* step 14 */
        numberFormat.setNumberingSystem(r.getValue(ExtensionKey.nu));
        /* step 15 */
        @SuppressWarnings("unused")
        String dataLocale = r.getDataLocale();
        /* steps 16-17 */
        String s = GetStringOption(cx, options, "style", set("decimal", "percent", "currency"), "decimal");
        /* step 18 */
        numberFormat.setStyle(s);
        /* steps 19-20 */
        String c = GetStringOption(cx, options, "currency", null, null);
        /* step 21 */
        if (c != null && !IsWellFormedCurrencyCode(c)) {
            throw newRangeError(cx, Messages.Key.IntlInvalidCurrency, c);
        }
        /* step 22 */
        if ("currency".equals(s) && c == null) {
            throw newTypeError(cx, Messages.Key.IntlInvalidCurrency, "null");
        }
        /* step 23 */
        int cDigits = -1;
        if ("currency".equals(s)) {
            c = ToUpperCase(c);
            numberFormat.setCurrency(c);
            cDigits = CurrencyDigits(c);
        }
        /* steps 24-25 */
        String cd = GetStringOption(cx, options, "currencyDisplay", set("code", "symbol", "name"), "symbol");
        /* step 26 */
        if ("currency".equals(s)) {
            numberFormat.setCurrencyDisplay(cd);
        }
        /* steps 27-28 */
        int mnid = GetNumberOption(cx, options, "minimumIntegerDigits", 1, 21, 1);
        /* step 29 */
        numberFormat.setMinimumIntegerDigits(mnid);
        /* step 30 */
        int mnfdDefault = "currency".equals(s) ? cDigits : 0;
        /* steps 31-32 */
        int mnfd = GetNumberOption(cx, options, "minimumFractionDigits", 0, 20, mnfdDefault);
        /* step 33 */
        numberFormat.setMinimumFractionDigits(mnfd);
        /* step 34 */
        int mxfdDefault = "currency".equals(s) ? Math.max(mnfd, cDigits)
                : "percent".equals(s) ? Math.max(mnfd, 0) : Math.max(mnfd, 3);
        /* steps 35-36 */
        int mxfd = GetNumberOption(cx, options, "maximumFractionDigits", mnfd, 20, mxfdDefault);
        /* step 37 */
        numberFormat.setMaximumFractionDigits(mxfd);
        /* steps 38-39 */
        Object mnsd = Get(cx, options, "minimumSignificantDigits");
        /* steps 40-41 */
        Object mxsd = Get(cx, options, "maximumSignificantDigits");
        /* step 42 */
        if (!Type.isUndefined(mnsd) || !Type.isUndefined(mxsd)) {
            int _mnsd = GetNumberOption(cx, options, "minimumSignificantDigits", 1, 21, 1);
            int _mxsd = GetNumberOption(cx, options, "maximumSignificantDigits", _mnsd, 21, 21);
            numberFormat.setMinimumSignificantDigits(_mnsd);
            numberFormat.setMaximumSignificantDigits(_mxsd);
        }
        /* steps 43-44 */
        boolean g = GetBooleanOption(cx, options, "useGrouping", true);
        /* step 45 */
        numberFormat.setUseGrouping(g);
        /* steps 46-51 (not applicable) */
        /* step 52 */
        numberFormat.setBoundFormat(null);
        /* step 53 (FIXME: spec bug - unnecessary internal slot) */
        /* step 54 (omitted) */
    }

    /**
     * 11.1.1 InitializeNumberFormat (numberFormat, locales, options)
     * 
     * @param realm
     *            the realm instance
     * @param numberFormat
     *            the number format object
     */
    public static void InitializeDefaultNumberFormat(Realm realm, NumberFormatObject numberFormat) {
        /* steps 1-2 (FIXME: spec bug - unnecessary internal slot) */
        /* steps 3-10 (not applicable) */
        /* step 11 */
        NumberFormatLocaleData localeData = new NumberFormatLocaleData();
        /* step 12 */
        ResolvedLocale r = ResolveDefaultLocale(realm, relevantExtensionKeys, localeData);
        /* steps 13, 15 */
        numberFormat.setLocale(r.getLocale());
        /* step 14 */
        numberFormat.setNumberingSystem(r.getValue(ExtensionKey.nu));
        /* step 15 (moved) */
        /* steps 16-18 */
        numberFormat.setStyle("decimal");
        /* steps 19-26 (not applicable) */
        /* steps 27-29 */
        numberFormat.setMinimumIntegerDigits(1);
        /* steps 30-33 */
        numberFormat.setMinimumFractionDigits(0);
        /* steps 34-37 */
        numberFormat.setMaximumFractionDigits(3);
        /* steps 38-42 (not applicable) */
        /* steps 43-45 */
        numberFormat.setUseGrouping(true);
        /* steps 46-51 (not applicable) */
        /* step 52 */
        numberFormat.setBoundFormat(null);
        /* step 53 (FIXME: spec bug - unnecessary internal slot) */
        /* step 54 (omitted) */
    }

    /**
     * Abstract Operation: CurrencyDigits
     * 
     * @param c
     *            the currency
     * @return the number of currency digits
     */
    private static int CurrencyDigits(String c) {
        // http://www.currency-iso.org/dam/downloads/lists/list_one.xml
        // Last updated: 2015-06-19
        switch (c) {
        case "BIF":
        case "BYR":
        case "CLP":
        case "DJF":
        case "GNF":
        case "ISK":
        case "JPY":
        case "KMF":
        case "KRW":
        case "PYG":
        case "RWF":
        case "UGX":
        case "UYI":
        case "VND":
        case "VUV":
        case "XAF":
        case "XOF":
        case "XPF":
            return 0;
        case "BHD":
        case "IQD":
        case "JOD":
        case "KWD":
        case "LYD":
        case "OMR":
        case "TND":
            return 3;
        case "CLF":
            return 4;
        default:
            return 2;
        }
    }

    /**
     * 11.1.2 Intl.NumberFormat([ locales [, options]])
     */
    @Override
    public ScriptObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* steps 1-4 */
        return construct(callerContext, this, args);
    }

    /**
     * 11.1.2 Intl.NumberFormat([ locales [, options]])
     */
    @Override
    public NumberFormatObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object locales = argument(args, 0);
        Object options = argument(args, 1);

        /* step 1 (not applicable) */
        /* steps 2-3 */
        NumberFormatObject obj = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.Intl_NumberFormatPrototype, NumberFormatObjectAllocator.INSTANCE);
        /* step 4 */
        InitializeNumberFormat(calleeContext, obj, locales, options);
        return obj;
    }

    private static final class NumberFormatObjectAllocator
            implements ObjectAllocator<NumberFormatObject> {
        static final ObjectAllocator<NumberFormatObject> INSTANCE = new NumberFormatObjectAllocator();

        @Override
        public NumberFormatObject newInstance(Realm realm) {
            return new NumberFormatObject(realm);
        }
    }

    /**
     * 11.2 Properties of the Intl.NumberFormat Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "NumberFormat";

        /**
         * 11.2.1 Intl.NumberFormat.prototype
         */
        @Value(name = "prototype",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_NumberFormatPrototype;

        /**
         * 11.2.2 Intl.NumberFormat.supportedLocalesOf (locales [, options ])
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
            /* step 2 */
            Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
            /* step 3 */
            return SupportedLocales(cx, availableLocales, requestedLocales, options);
        }
    }
}
