/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsExtensible;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.*;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
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
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.ULocale;

/**
 * <h1>11 NumberFormat Objects</h1>
 * <ul>
 * <li>11.1 The Intl.NumberFormat Constructor
 * <li>11.2 Properties of the Intl.NumberFormat Constructor
 * </ul>
 */
public final class NumberFormatConstructor extends BuiltinConstructor implements Initialisable {
    /** [[availableLocales]] */
    private final Lazy<Set<String>> availableLocales = new Lazy<Set<String>>() {
        @Override
        protected Set<String> computeValue() {
            return GetAvailableLocales(NumberFormat.getAvailableULocales());
        }
    };

    public static Set<String> getAvailableLocales(ExecutionContext cx) {
        return getAvailableLocalesLazy(cx).get();
    }

    private static Lazy<Set<String>> getAvailableLocalesLazy(ExecutionContext cx) {
        NumberFormatConstructor numberFormat = (NumberFormatConstructor) cx
                .getIntrinsic(Intrinsics.Intl_NumberFormat);
        return numberFormat.availableLocales;
    }

    /** [[relevantExtensionKeys]] */
    private static List<ExtensionKey> relevantExtensionKeys = asList(ExtensionKey.nu);

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
                return getNumberInfo().get(0);
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
            return asList(localeNumberingSystem, "arab", "arabtext", "bali", "beng", "deva",
                    "fullwide", "gujr", "guru", "hanidec", "khmr", "knda", "laoo", "latn", "limb",
                    "mlym", "mong", "mymr", "orya", "tamldec", "telu", "thai", "tibt");
        }
    }

    public NumberFormatConstructor(Realm realm) {
        super(realm, "NumberFormat");
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
     * 11.1.1.1 InitializeNumberFormat (numberFormat, locales, options)
     */
    public static void InitializeNumberFormat(ExecutionContext cx, ScriptObject obj,
            Object locales, Object opts) {
        // spec allows any object to become a NumberFormat object, we don't allow this
        if (!(obj instanceof NumberFormatObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 1-2 */
        NumberFormatObject numberFormat = (NumberFormatObject) obj;
        if (numberFormat.isInitializedIntlObject()) {
            throw newTypeError(cx, Messages.Key.InitialisedObject);
        }
        numberFormat.setInitializedIntlObject(true);
        /* step 3 */
        Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
        /* steps 4-5 */
        ScriptObject options;
        if (Type.isUndefined(opts)) {
            options = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        } else {
            options = ToObject(cx, opts);
        }
        /* step 6 */
        OptionsRecord opt = new OptionsRecord();
        /* step 7 */
        String matcher = GetStringOption(cx, options, "localeMatcher", set("lookup", "best fit"),
                "best fit");
        /* step 8 */
        opt.localeMatcher = OptionsRecord.MatcherType.forName(matcher);
        /* steps 9-10 */
        NumberFormatLocaleData localeData = new NumberFormatLocaleData();
        /* step 11 */
        ResolvedLocale r = ResolveLocale(cx, getAvailableLocalesLazy(cx), requestedLocales, opt,
                relevantExtensionKeys, localeData);
        /* step 12 */
        numberFormat.setLocale(r.locale);
        /* step 13 */
        numberFormat.setNumberingSystem(r.values.get(ExtensionKey.nu));
        /* step 14 */
        @SuppressWarnings("unused")
        String dataLocale = r.dataLocale;
        /* steps 15-16 */
        String s = GetStringOption(cx, options, "style", set("decimal", "percent", "currency"),
                "decimal");
        numberFormat.setStyle(s);
        /* step 17 */
        String c = GetStringOption(cx, options, "currency", null, null);
        /* step 18 */
        if (c != null) {
            if (!IsWellFormedCurrencyCode(cx, c)) {
                throw newRangeError(cx, Messages.Key.IntlInvalidCurrency, c);
            }
        }
        /* step 19 */
        if ("currency".equals(s) && c == null) {
            throw newTypeError(cx, Messages.Key.IntlInvalidCurrency, "null");
        }
        /* step 20 */
        int cDigits = -1;
        if ("currency".equals(s)) {
            c = ToUpperCase(c);
            numberFormat.setCurrency(c);
            cDigits = CurrencyDigits(c);
        }
        /* steps 21-22 */
        String cd = GetStringOption(cx, options, "currencyDisplay", set("code", "symbol", "name"),
                "symbol");
        if ("currency".equals(s)) {
            numberFormat.setCurrencyDisplay(cd);
        }
        /* steps 23-24 */
        int mnid = GetNumberOption(cx, options, "minimumIntegerDigits", 1, 21, 1);
        numberFormat.setMinimumIntegerDigits(mnid);
        /* step 25 */
        int mnfdDefault = "currency".equals(s) ? cDigits : 0;
        /* steps 26-27 */
        int mnfd = GetNumberOption(cx, options, "minimumFractionDigits", 0, 20, mnfdDefault);
        numberFormat.setMinimumFractionDigits(mnfd);
        /* step 28 */
        int mxfdDefault = "currency".equals(s) ? Math.max(mnfd, cDigits)
                : "percent".equals(s) ? Math.max(mnfd, 0) : Math.max(mnfd, 3);
        /* steps 29-30 */
        int mxfd = GetNumberOption(cx, options, "maximumFractionDigits", mnfd, 20, mxfdDefault);
        numberFormat.setMaximumFractionDigits(mxfd);
        /* steps 31-32 */
        Object mnsd = Get(cx, options, "minimumSignificantDigits");
        Object mxsd = Get(cx, options, "maximumSignificantDigits");
        /* step 33 */
        if (!Type.isUndefined(mnsd) || !Type.isUndefined(mxsd)) {
            int _mnsd = GetNumberOption(cx, options, "minimumSignificantDigits", 1, 21, 1);
            int _mxsd = GetNumberOption(cx, options, "maximumSignificantDigits", _mnsd, 21, 21);
            numberFormat.setMinimumSignificantDigits(_mnsd);
            numberFormat.setMaximumSignificantDigits(_mxsd);
        }
        /* steps 34-35 */
        boolean g = GetBooleanOption(cx, options, "useGrouping", true);
        numberFormat.setUseGrouping(g);
        /* steps 36-41 */
        // not applicable
        /* step 42 */
        numberFormat.setBoundFormat(null);
        /* step 43 */
        numberFormat.setInitializedNumberFormat(true);
    }

    /**
     * Abstract Operation: CurrencyDigits
     */
    private static int CurrencyDigits(String c) {
        // http://www.currency-iso.org/dam/downloads/table_a1.xml
        switch (c) {
        case "BYR":
        case "XOF":
        case "BIF":
        case "XAF":
        case "CLF":
        case "CLP":
        case "KMF":
        case "DJF":
        case "XPF":
        case "GNF":
        case "ISK":
        case "JPY":
        case "KRW":
        case "PYG":
        case "RWF":
        case "UGX":
        case "UYI":
        case "VUV":
        case "VND":
            return 0;
        case "BHD":
        case "IQD":
        case "JOD":
        case "KWD":
        case "LYD":
        case "OMR":
        case "TND":
            return 3;
        default:
            return 2;
        }
    }

    /**
     * 11.1.2.1 Intl.NumberFormat.call (this [, locales [, options]])
     */
    @Override
    public ScriptObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        if (Type.isUndefined(thisValue) || thisValue == calleeContext.getIntrinsic(Intrinsics.Intl)) {
            return construct(calleeContext, args);
        }
        ScriptObject obj = ToObject(calleeContext, thisValue);
        if (!IsExtensible(calleeContext, obj)) {
            throw newTypeError(calleeContext, Messages.Key.NotExtensible);
        }
        InitializeNumberFormat(calleeContext, obj, locales, options);
        return obj;
    }

    /**
     * 11.1.3.1 new Intl.NumberFormat ([locales [, options]])
     */
    @Override
    public NumberFormatObject construct(ExecutionContext callerContext, Object... args) {
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        NumberFormatObject obj = new NumberFormatObject(callerContext.getRealm());
        obj.setPrototype(callerContext.getIntrinsic(Intrinsics.Intl_NumberFormatPrototype));
        InitializeNumberFormat(callerContext, obj, locales, options);
        return obj;
    }

    /**
     * 11.2 Properties of the Intl.NumberFormat Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "NumberFormat";

        /**
         * 11.2.1 Intl.NumberFormat.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_NumberFormatPrototype;

        /**
         * 11.2.2 Intl.NumberFormat.supportedLocalesOf (locales [, options])
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
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue,
                    Intrinsics.Intl_NumberFormatPrototype, NumberFormatObjectAllocator.INSTANCE);
        }
    }

    private static final class NumberFormatObjectAllocator implements
            ObjectAllocator<NumberFormatObject> {
        static final ObjectAllocator<NumberFormatObject> INSTANCE = new NumberFormatObjectAllocator();

        @Override
        public NumberFormatObject newInstance(Realm realm) {
            return new NumberFormatObject(realm);
        }
    }
}
