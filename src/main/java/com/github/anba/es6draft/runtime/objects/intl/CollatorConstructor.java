/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsExtensible;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.*;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static java.util.Arrays.asList;

import java.util.ArrayList;
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
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

/**
 * <h1>10 Collator Objects</h1>
 * <ul>
 * <li>10.1 The Intl.Collator Constructor
 * <li>10.2 Properties of the Intl.Collator Constructor
 * </ul>
 */
public final class CollatorConstructor extends BuiltinConstructor implements Initializable {
    /** [[availableLocales]] */
    private final Lazy<Set<String>> availableLocales = new Lazy<Set<String>>() {
        @Override
        protected Set<String> computeValue() {
            return GetAvailableLocales(Collator.getAvailableULocales());
        }
    };

    public static Set<String> getAvailableLocales(ExecutionContext cx) {
        return getAvailableLocalesLazy(cx).get();
    }

    private static Lazy<Set<String>> getAvailableLocalesLazy(ExecutionContext cx) {
        CollatorConstructor collator = (CollatorConstructor) cx
                .getIntrinsic(Intrinsics.Intl_Collator);
        return collator.availableLocales;
    }

    /** [[relevantExtensionKeys]] */
    private static List<ExtensionKey> relevantExtensionKeys = asList(ExtensionKey.co,
            ExtensionKey.kn, ExtensionKey.kf);

    /**
     * Collation type keys (BCP 47; CLDR, version 25)
     */
    private enum CollationType {/* @formatter:off */
        big5han("big5han"),
        dict("dict", "dictionary"),
        direct("direct"), // deprecated, not supported in ICU
        ducet("ducet"),
        eor("eor"),
        gb2312("gb2312", "gb2312han"),
        phonebk("phonebk", "phonebook"),
        phonetic("phonetic"),
        pinyin("pinyin"),
        reformed("reformed"),
        search("search"),
        searchjl("searchjl"),
        standard("standard"),
        stroke("stroke"),
        trad("trad", "traditional"),
        unihan("unihan"), // not supported in ICU
        zhuyin("zhuyin");
        /* @formatter:on */

        private final String name;
        private final String alias;

        private CollationType(String name) {
            this.name = name;
            this.alias = null;
        }

        private CollationType(String name, String alias) {
            this.name = name;
            this.alias = alias;
        }

        public String getName() {
            return name;
        }

        public static CollationType forName(String name) {
            for (CollationType co : values()) {
                if (name.equals(co.name) || name.equals(co.alias)) {
                    return co;
                }
            }
            throw new IllegalArgumentException(name);
        }
    }

    /** [[sortLocaleData]] + [[searchLocaleData]] */
    private static final class CollatorLocaleData implements LocaleData {
        private final String usage;

        public CollatorLocaleData(String usage) {
            this.usage = usage;
        }

        @Override
        public LocaleDataInfo info(ULocale locale) {
            return new CollatorLocaleDataInfo(usage, locale);
        }
    }

    /** [[sortLocaleData]] + [[searchLocaleData]] */
    private static final class CollatorLocaleDataInfo implements LocaleDataInfo {
        private final String usage;
        private final ULocale locale;

        public CollatorLocaleDataInfo(String usage, ULocale locale) {
            this.usage = usage;
            this.locale = locale;
        }

        @Override
        public String defaultValue(ExtensionKey extensionKey) {
            switch (extensionKey) {
            case co:
                return null; // null must be first value, cf. 10.2.3
            case kf:
                return getCaseFirstInfo().get(0);
            case kn:
                return getNumericInfo().get(0);
            default:
                throw new IllegalArgumentException(extensionKey.name());
            }
        }

        @Override
        public List<String> entries(ExtensionKey extensionKey) {
            switch (extensionKey) {
            case co:
                return getCollationInfo();
            case kf:
                return getCaseFirstInfo();
            case kn:
                return getNumericInfo();
            default:
                throw new IllegalArgumentException(extensionKey.name());
            }
        }

        private List<String> getCollationInfo() {
            String[] values = Collator.getKeywordValuesForLocale("collation", locale, false);
            List<String> result = new ArrayList<>(values.length);
            result.add(null); // null must be first value, cf. 10.2.3
            for (int i = 0, len = values.length; i < len; ++i) {
                CollationType type = CollationType.forName(values[i]);
                if (type == CollationType.standard || type == CollationType.search) {
                    // 'standard' and 'search' must not be elements of 'co' array, cf. 10.2.3
                    // FIXME: spec issue? this gives slightly akward results for `new
                    // Intl.Collator("de-u-co-phonebk",{usage:"search"}).resolvedOptions()`
                    // the resolved locale is "de-u-co-phonebk", that means co=phonebk, but for the
                    // actual collator co=search will be used...
                    continue;
                }
                result.add(type.getName());
            }
            return result;
        }

        private List<String> getNumericInfo() {
            return asList("false", "true");
        }

        private List<String> getCaseFirstInfo() {
            if ("sort".equals(usage)) {
                // special case 'sort' usage for Danish and Maltese to use uppercase first defaults
                String language = locale.getLanguage();
                if ("da".equals(language) || "mt".equals(language)) {
                    return asList("upper", "false", "lower");
                }
            }
            return asList("false", "lower", "upper");
        }
    }

    public CollatorConstructor(Realm realm) {
        super(realm, "Collator");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    @Override
    public CollatorConstructor clone() {
        return new CollatorConstructor(getRealm());
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(asList(elements));
    }

    /**
     * 10.1.1.1 InitializeCollator (collator, locales, options)
     * 
     * @param cx
     *            the execution context
     * @param obj
     *            the collator object
     * @param locales
     *            the locales array
     * @param opts
     *            the options object
     */
    public static void InitializeCollator(ExecutionContext cx, ScriptObject obj, Object locales,
            Object opts) {
        // spec allows any object to become a Collator object, we don't allow this
        if (!(obj instanceof CollatorObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 1-2 */
        CollatorObject collator = (CollatorObject) obj;
        if (collator.isInitializedIntlObject()) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        collator.setInitializedIntlObject(true);
        /* step 3 */
        Set<String> requestedLocals = CanonicalizeLocaleList(cx, locales);
        /* steps 4-5 */
        ScriptObject options;
        if (Type.isUndefined(opts)) {
            options = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        } else {
            options = ToObject(cx, opts);
        }
        /* step 6 */
        String u = GetStringOption(cx, options, "usage", set("sort", "search"), "sort");
        /* step 7 */
        collator.setUsage(u);
        /* steps 8-9 */
        CollatorLocaleData localeData = new CollatorLocaleData(u);
        /* step 10 */
        OptionsRecord opt = new OptionsRecord();
        /* step 11 */
        String matcher = GetStringOption(cx, options, "localeMatcher", set("lookup", "best fit"),
                "best fit");
        /* step 12 */
        opt.localeMatcher = OptionsRecord.MatcherType.forName(matcher);
        // FIXME: spec should propably define exact iteration order here
        /* step 13 (kn-numeric) */
        Boolean numeric = GetBooleanOption(cx, options, "numeric", null);
        if (numeric != null) {
            opt.values.put(ExtensionKey.kn, numeric.toString());
        }
        /* step 13 (kf-caseFirst) */
        String caseFirst = GetStringOption(cx, options, "caseFirst",
                set("upper", "lower", "false"), null);
        if (caseFirst != null) {
            opt.values.put(ExtensionKey.kf, caseFirst);
        }
        /* steps 14-15 */
        ResolvedLocale r = ResolveLocale(cx, getAvailableLocalesLazy(cx), requestedLocals, opt,
                relevantExtensionKeys, localeData);
        /* step 16 */
        collator.setLocale(r.locale);
        /* steps 17-19 (co-collation) */
        String collation = r.values.get(ExtensionKey.co);
        collator.setCollation(collation != null ? collation : "default");
        /* steps 17-19 (kn-numeric) */
        collator.setNumeric("true".equals(r.values.get(ExtensionKey.kn)));
        /* steps 17-19 (kf-caseFirst) */
        collator.setCaseFirst(r.values.get(ExtensionKey.kf));
        /* step 20 */
        String s = GetStringOption(cx, options, "sensitivity",
                set("base", "accent", "case", "variant"), null);
        /* step 21 */
        if (s == null) {
            // spec differentiates between "sort" and "search" usage, but effectively you'll end up
            // with "variant" in both cases, so take the short path here
            s = "variant";
        }
        /* step 22 */
        collator.setSensitivity(s);
        /* steps 23-24 */
        boolean ip = GetBooleanOption(cx, options, "ignorePunctuation", false);
        collator.setIgnorePunctuation(ip);
        /* step 25 */
        collator.setBoundCompare(null);
        /* step 26 */
        collator.setInitializedCollator(true);
    }

    /**
     * 10.1.2.1 Intl.Collator.call (this [, locales [, options]])
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
        InitializeCollator(calleeContext, obj, locales, options);
        return obj;
    }

    /**
     * 10.1.3.1 new Intl.Collator ([locales [, options]])
     */
    @Override
    public CollatorObject construct(ExecutionContext callerContext, Object... args) {
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        CollatorObject obj = new CollatorObject(callerContext.getRealm());
        obj.setPrototype(callerContext.getIntrinsic(Intrinsics.Intl_CollatorPrototype));
        InitializeCollator(callerContext, obj, locales, options);
        return obj;
    }

    /**
     * 10.2 Properties of the Intl.Collator Constructor
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
        public static final String name = "Collator";

        /**
         * 10.2.1 Intl.Collator.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_CollatorPrototype;

        /**
         * 10.2.2 Intl.Collator.supportedLocalesOf (locales [, options])
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
        public static Object supportedLocalesOf(ExecutionContext cx, Object thisValue,
                Object locales, Object options) {
            Set<String> availableLocales = getAvailableLocales(cx);
            Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
            return SupportedLocales(cx, availableLocales, requestedLocales, options);
        }

        /**
         * Extension: Make subclassable for ES6 classes
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the new uninitialized collator object
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.Intl_CollatorPrototype,
                    CollatorObjectAllocator.INSTANCE);
        }
    }

    private static final class CollatorObjectAllocator implements ObjectAllocator<CollatorObject> {
        static final ObjectAllocator<CollatorObject> INSTANCE = new CollatorObjectAllocator();

        @Override
        public CollatorObject newInstance(Realm realm) {
            return new CollatorObject(realm);
        }
    }
}
