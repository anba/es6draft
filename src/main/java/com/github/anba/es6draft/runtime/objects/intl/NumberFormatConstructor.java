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
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.language.Operators.InstanceofOperator;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.*;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;
import static java.util.Arrays.asList;

import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.CharacterIterator;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Lazy;
import com.github.anba.es6draft.runtime.internal.Messages;
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
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.ULocale;

/**
 * <h1>11 NumberFormat Objects</h1>
 * <ul>
 * <li>11.1 Abstract Operations For NumberFormat Objects
 * <li>11.2 The Intl.NumberFormat Constructor
 * <li>11.3 Properties of the Intl.NumberFormat Constructor
 * </ul>
 */
public final class NumberFormatConstructor extends BuiltinConstructor implements Initializable {
    /** [[availableLocales]] */
    private final Lazy<Set<String>> availableLocales = Lazy
            .of(() -> GetAvailableLocales(LanguageData.getAvailableNumberFormatLocales()));

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
        private static final boolean ICU_NUMBERING_SYSTEMS = true;
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
            // FIXME: spec issue - increase list of Table 2 entries (newer CLDR than v21).
            String localeNumberingSystem = NumberingSystem.getInstance(locale).getName();
            if (ICU_NUMBERING_SYSTEMS) {
                ArrayList<String> list = new ArrayList<>(ICUNumberingSystems.available);
                list.set(0, localeNumberingSystem);
                return list;
            }
            return asList(localeNumberingSystem, "arab", "arabext", "bali", "beng", "deva", "fullwide", "gujr", "guru",
                    "hanidec", "khmr", "knda", "laoo", "latn", "limb", "mlym", "mong", "mymr", "orya", "tamldec",
                    "telu", "thai", "tibt");
        }

        private static final class ICUNumberingSystems {
            private static final ArrayList<String> available;
            static {
                ArrayList<String> list = new ArrayList<>();
                list.add(null);
                for (String name : NumberingSystem.getAvailableNames()) {
                    NumberingSystem ns;
                    try {
                        ns = NumberingSystem.getInstanceByName(name);
                    } catch (IllegalArgumentException e) {
                        // ICU4J throws an IllegalArgumentException if the numbering system digits are outside of BMP.
                        continue;
                    }
                    if (!ns.isAlgorithmic()) {
                        list.add(name);
                    }
                }
                available = list;
            }
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

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(asList(elements));
    }

    /**
     * 11.1.1 SetNumberFormatDigitOptions ( intlObj, options, mnfdDefault )
     * 
     * @param cx
     *            the execution context
     * @param setMinimumIntegerDigits
     *            the minimum integer digits setter
     * @param setMinimumFractionDigits
     *            the minimum fraction digits setter
     * @param setMaximumFractionDigits
     *            the maximum fraction digits setter
     * @param setMinimumSignificantDigits
     *            the minimum significants digits setter
     * @param setMaximumSignificantDigits
     *            the maximum significants digits setter
     * @param options
     *            the options object
     * @param mnfdDefault
     *            the minimum fraction digits default value
     * @param mxfdDefault
     *            the maximum fraction digits default value
     */
    public static void SetNumberFormatDigitOptions(ExecutionContext cx, IntConsumer setMinimumIntegerDigits,
            IntConsumer setMinimumFractionDigits, IntConsumer setMaximumFractionDigits,
            IntConsumer setMinimumSignificantDigits, IntConsumer setMaximumSignificantDigits, ScriptObject options,
            int mnfdDefault, int mxfdDefault) {
        /* steps 1-4 (not applicable) */
        /* step 5 */
        int mnid = GetNumberOption(cx, options, "minimumIntegerDigits", 1, 21, 1);
        /* step 6 */
        int mnfd = GetNumberOption(cx, options, "minimumFractionDigits", 0, 20, mnfdDefault);
        /* step 7 */
        int mxfdActualDefault = Math.max(mnfd, mxfdDefault);
        /* step 8 */
        int mxfd = GetNumberOption(cx, options, "maximumFractionDigits", mnfd, 20, mxfdActualDefault);
        /* step 9 */
        Object mnsd = Get(cx, options, "minimumSignificantDigits");
        /* step 10 */
        Object mxsd = Get(cx, options, "maximumSignificantDigits");
        /* step 11 */
        setMinimumIntegerDigits.accept(mnid);
        /* step 12 */
        setMinimumFractionDigits.accept(mnfd);
        /* step 13 */
        setMaximumFractionDigits.accept(mxfd);
        /* step 14 */
        if (!Type.isUndefined(mnsd) || !Type.isUndefined(mxsd)) {
            /* step 14.a */
            int _mnsd = DefaultNumberOption(cx, "minimumSignificantDigits", mnsd, 1, 21, 1);
            /* step 14.b */
            int _mxsd = DefaultNumberOption(cx, "maximumSignificantDigits", mxsd, _mnsd, 21, 21);
            /* step 14.c */
            setMinimumSignificantDigits.accept(_mnsd);
            /* step 14.d */
            setMaximumSignificantDigits.accept(_mxsd);
        }
    }

    /**
     * 11.1.2 InitializeNumberFormat (numberFormat, locales, options)
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
        /* step 3 */
        Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
        /* steps 4-5 */
        ScriptObject options;
        if (Type.isUndefined(opts)) {
            options = ObjectCreate(cx, (ScriptObject) null);
        } else {
            options = ToObject(cx, opts);
        }
        /* step 7 */
        String matcher = GetStringOption(cx, options, "localeMatcher", set("lookup", "best fit"), "best fit");
        /* step 6, 8 */
        OptionsRecord opt = new OptionsRecord(OptionsRecord.MatcherType.forName(matcher));
        /* step 9 */
        NumberFormatLocaleData localeData = new NumberFormatLocaleData();
        /* step 10 */
        ResolvedLocale r = ResolveLocale(cx.getRealm(), getAvailableLocalesLazy(cx), requestedLocales, opt,
                relevantExtensionKeys, localeData);
        /* step 11 */
        numberFormat.setLocale(r.getLocale());
        /* step 12 */
        numberFormat.setNumberingSystem(r.getValue(ExtensionKey.nu));
        /* step 13 (not applicable) */
        /* step 14 */
        String style = GetStringOption(cx, options, "style", set("decimal", "percent", "currency"), "decimal");
        /* step 15 */
        numberFormat.setStyle(style);
        /* step 16 */
        String c = GetStringOption(cx, options, "currency", null, null);
        /* step 17 */
        if (c != null && !IsWellFormedCurrencyCode(c)) {
            throw newRangeError(cx, Messages.Key.IntlInvalidCurrency, c);
        }
        /* step 18 */
        if ("currency".equals(style) && c == null) {
            throw newTypeError(cx, Messages.Key.IntlMissingCurrency);
        }
        /* step 19 */
        int cDigits = -1;
        if ("currency".equals(style)) {
            c = ToUpperCase(c);
            numberFormat.setCurrency(c);
            cDigits = CurrencyDigits(c);
        }
        /* step 20 */
        String cd = GetStringOption(cx, options, "currencyDisplay", set("code", "symbol", "name"), "symbol");
        /* step 21 */
        if ("currency".equals(style)) {
            numberFormat.setCurrencyDisplay(cd);
        }
        /* steps 22-23 */
        int mnfdDefault = "currency".equals(style) ? cDigits : 0;
        int mxfdDefault = "currency".equals(style) ? cDigits : "percent".equals(style) ? 0 : 3;
        /* step 24 */
        SetNumberFormatDigitOptions(cx, numberFormat::setMinimumIntegerDigits, numberFormat::setMinimumFractionDigits,
                numberFormat::setMaximumFractionDigits, numberFormat::setMinimumSignificantDigits,
                numberFormat::setMaximumSignificantDigits, options, mnfdDefault, mxfdDefault);
        /* step 25 */
        boolean g = GetBooleanOption(cx, options, "useGrouping", true);
        /* step 26 */
        numberFormat.setUseGrouping(g);
        /* steps 27-32 (not applicable) */
        /* step 33 */
        numberFormat.setBoundFormat(null);
        /* step 34 (FIXME: spec bug - unnecessary internal slot) */
        /* step 35 (omitted) */
    }

    /**
     * 11.1.3 CurrencyDigits (currency)
     * 
     * @param c
     *            the currency
     * @return the number of currency digits
     */
    private static int CurrencyDigits(String c) {
        // https://www.currency-iso.org/dam/downloads/lists/list_one.xml
        // Last updated: 2017-01-01
        switch (c) {
        case "BIF":
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
     * 11.1.4 Number Format Functions
     */
    public static final class FormatFunction extends BuiltinFunction {
        public FormatFunction(Realm realm) {
            super(realm, "format", 1);
            createDefaultFunctionProperties();
        }

        @Override
        public String call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            /* steps 1-2 */
            assert thisValue instanceof NumberFormatObject;
            NumberFormatObject nf = (NumberFormatObject) thisValue;
            /* step 3 */
            Object value = argument(args, 0);
            /* step 4 */
            double x = ToNumber(calleeContext, value);
            /* step 5 */
            return FormatNumber(nf, x);
        }
    }

    /**
     * 11.1.5 FormatNumber(numberFormat, x)
     * 
     * @param numberFormat
     *            the number format object
     * @param x
     *            the number value
     * @return the formatted number string
     */
    public static String FormatNumber(NumberFormatObject numberFormat, double x) {
        if (x == -0.0) {
            // -0 is not considered to be negative, cf. step 3a
            x = +0.0;
        }
        /* steps 1-8 */
        return numberFormat.getNumberFormat().format(x);
    }

    /**
     * PartitionNumberPattern(numberFormat, x)
     * 
     * @param numberFormatObj
     *            the number format object
     * @param x
     *            the number value
     * @return the formatted number object
     */
    private static List<Map.Entry<String, String>> PartitionNumberPattern(NumberFormatObject numberFormat, double x) {
        if (x == -0.0) {
            // -0 is not considered to be negative, cf. step 3a
            x = +0.0;
        }
        ArrayList<Map.Entry<String, String>> parts = new ArrayList<>();
        NumberFormat numFormat = numberFormat.getNumberFormat();
        AttributedCharacterIterator iterator = numFormat.formatToCharacterIterator(x);
        StringBuilder sb = new StringBuilder();
        for (char ch = iterator.first(); ch != CharacterIterator.DONE; ch = iterator.next()) {
            sb.append(ch);
            if (iterator.getIndex() + 1 == iterator.getRunLimit()) {
                Iterator<Attribute> keyIterator = iterator.getAttributes().keySet().iterator();
                String key;
                if (keyIterator.hasNext()) {
                    key = fieldToString((NumberFormat.Field) keyIterator.next(), x);
                } else {
                    key = "literal";
                }
                String value = sb.toString();
                sb.setLength(0);
                parts.add(new AbstractMap.SimpleImmutableEntry<>(key, value));
            }
        }
        return parts;
    }

    private static String fieldToString(NumberFormat.Field field, double x) {
        if (field == NumberFormat.Field.SIGN) {
            if (Double.compare(x, +0) >= 0) {
                return "plusSign";
            }
            return "minusSign";
        }
        if (field == NumberFormat.Field.INTEGER) {
            if (Double.isNaN(x)) {
                return "nan";
            }
            if (Double.isInfinite(x)) {
                return "infinity";
            }
            return "integer";
        }
        if (field == NumberFormat.Field.FRACTION) {
            return "fraction";
        }
        if (field == NumberFormat.Field.EXPONENT) {
            return "literal";
        }
        if (field == NumberFormat.Field.EXPONENT_SIGN) {
            return "literal";
        }
        if (field == NumberFormat.Field.EXPONENT_SYMBOL) {
            return "literal";
        }
        if (field == NumberFormat.Field.DECIMAL_SEPARATOR) {
            return "decimal";
        }
        if (field == NumberFormat.Field.GROUPING_SEPARATOR) {
            return "group";
        }
        if (field == NumberFormat.Field.PERCENT) {
            return "percentSign";
        }
        if (field == NumberFormat.Field.PERMILLE) {
            return "permilleSign";
        }
        if (field == NumberFormat.Field.CURRENCY) {
            return "currency";
        }
        // Report unsupported/unexpected number fields as literal.
        return "literal";
    }

    /**
     * FormatNumberToParts(numberFormat, x)
     * 
     * @param cx
     *            the execution context
     * @param numberFormat
     *            the number format object
     * @param x
     *            the number value
     * @return the formatted number object
     */
    public static ArrayObject FormatNumberToParts(ExecutionContext cx, NumberFormatObject numberFormat, double x) {
        /* step 1 */
        List<Map.Entry<String, String>> parts = PartitionNumberPattern(numberFormat, x);
        /* step 2 */
        ArrayObject result = ArrayCreate(cx, 0);
        /* step 3 */
        int n = 0;
        /* step 4 */
        for (Map.Entry<String, String> part : parts) {
            /* step 4.a */
            OrdinaryObject o = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* step 4.b */
            CreateDataProperty(cx, o, "type", part.getKey());
            /* step 4.c */
            CreateDataProperty(cx, o, "value", part.getValue());
            /* steps 4.d-e */
            CreateDataProperty(cx, result, n++, o);
        }
        /* step 5 */
        return result;
    }

    /**
     * 11.1.11 UnwrapNumberFormat( nf )
     * 
     * @param cx
     *            the execution context
     * @param nf
     *            the number format object
     * @param method
     *            the caller method
     * @return the unwrapped number format object
     */
    public static NumberFormatObject UnwrapNumberFormat(ExecutionContext cx, Object nf, String method) {
        /* step 1 */
        if (cx.getRuntimeContext().isEnabled(CompatibilityOption.IntlConstructorLegacyFallback)) {
            if (Type.isObject(nf) && !(nf instanceof NumberFormatObject)
                    && InstanceofOperator(nf, cx.getIntrinsic(Intrinsics.Intl_NumberFormat), cx)) {
                nf = Get(cx, Type.objectValue(nf),
                        cx.getIntrinsic(Intrinsics.Intl, IntlObject.class).getFallbackSymbol());
            }
        }
        /* step 2 */
        if (!(nf instanceof NumberFormatObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(nf).toString());
        }
        /* step 3 */
        return (NumberFormatObject) nf;
    }

    /**
     * 11.2.1 Intl.NumberFormat([ locales [, options]])
     */
    @Override
    public ScriptObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object locales = argument(args, 0);
        Object options = argument(args, 1);

        /* step 1 (not applicable) */
        /* step 2 */
        NumberFormatObject numberFormat = OrdinaryCreateFromConstructor(calleeContext, this,
                Intrinsics.Intl_NumberFormatPrototype, NumberFormatObject::new);
        /* step 3 */
        InitializeNumberFormat(calleeContext, numberFormat, locales, options);
        /* steps 4-5 */
        if (calleeContext.getRuntimeContext().isEnabled(CompatibilityOption.IntlConstructorLegacyFallback)) {
            if (Type.isObject(thisValue) && InstanceofOperator(thisValue, this, calleeContext)) {
                PropertyDescriptor desc = new PropertyDescriptor(numberFormat, false, false, false);
                DefinePropertyOrThrow(calleeContext, Type.objectValue(thisValue),
                        calleeContext.getIntrinsic(Intrinsics.Intl, IntlObject.class).getFallbackSymbol(), desc);
                return Type.objectValue(thisValue);
            }
        }
        /* step 6 */
        return numberFormat;
    }

    /**
     * 11.2.1 Intl.NumberFormat([ locales [, options]])
     */
    @Override
    public NumberFormatObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object locales = argument(args, 0);
        Object options = argument(args, 1);

        /* step 1 (not applicable) */
        /* step 2 */
        NumberFormatObject obj = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.Intl_NumberFormatPrototype, NumberFormatObject::new);
        /* step 3 */
        InitializeNumberFormat(calleeContext, obj, locales, options);
        /* steps 4-5 (not applicable) */
        /* step 6 */
        return obj;
    }

    /**
     * 11.3 Properties of the Intl.NumberFormat Constructor
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
         * 11.3.1 Intl.NumberFormat.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_NumberFormatPrototype;

        /**
         * 11.3.2 Intl.NumberFormat.supportedLocalesOf (locales [, options ])
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
