/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.*;
import static com.github.anba.es6draft.runtime.objects.intl.NumberFormatConstructor.SetNumberFormatDigitOptions;
import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
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
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.ibm.icu.util.ULocale;

/**
 * <h1>PluralRules Objects</h1>
 * <ul>
 * <li>The Intl.PluralRules Constructor
 * <li>Properties of the Intl.PluralRules Constructor
 * </ul>
 */
public final class PluralRulesConstructor extends BuiltinConstructor implements Initializable {
    /** [[availableLocales]] */
    private final Lazy<Set<String>> availableLocales = Lazy
            .of(() -> GetAvailableLocales(LanguageData.getAvailablePluralRulesLocales()));

    /**
     * [[availableLocales]]
     * 
     * @param cx
     *            the execution context
     * @return the set of available locales supported by {@code Intl.PluralRules}
     */
    public static Set<String> getAvailableLocales(ExecutionContext cx) {
        return getAvailableLocalesLazy(cx).get();
    }

    private static Lazy<Set<String>> getAvailableLocalesLazy(ExecutionContext cx) {
        PluralRulesConstructor pluralRules = (PluralRulesConstructor) cx.getIntrinsic(Intrinsics.Intl_PluralRules);
        return pluralRules.availableLocales;
    }

    /** [[relevantExtensionKeys]] */
    private static final List<ExtensionKey> relevantExtensionKeys = Collections.emptyList();

    private static final class PluralRulesLocaleData implements LocaleData {
        @Override
        public LocaleDataInfo info(ULocale locale) {
            return new PluralRulesLocaleDataInfo();
        }
    }

    private static final class PluralRulesLocaleDataInfo implements LocaleDataInfo {
        @Override
        public String defaultValue(ExtensionKey extensionKey) {
            throw new IllegalArgumentException(extensionKey.name());
        }

        @Override
        public List<String> entries(ExtensionKey extensionKey) {
            throw new IllegalArgumentException(extensionKey.name());
        }
    }

    /**
     * Constructs a new PluralRules constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public PluralRulesConstructor(Realm realm) {
        super(realm, "PluralRules", 0);
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
     * InitializePluralRules (pluralRules, locales, options)
     * 
     * @param cx
     *            the execution context
     * @param pluralRules
     *            the pluralRules object
     * @param locales
     *            the locales array
     * @param opts
     *            the options object
     */
    public static void InitializePluralRules(ExecutionContext cx, PluralRulesObject pluralRules, Object locales,
            Object opts) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
        /* steps 4-5 */
        ScriptObject options;
        if (Type.isUndefined(opts)) {
            options = ObjectCreate(cx, (ScriptObject) null);
        } else {
            options = ToObject(cx, opts);
        }
        /* step 6 */
        // FIXME: spec-issue: re-order after getting localeMatcher for consistency with other Intl constructors?
        String type = GetStringOption(cx, options, "type", set("cardinal", "ordinal"), "cardinal");
        /* step 7 */
        pluralRules.setType(type);
        /* step 9 */
        String matcher = GetStringOption(cx, options, "localeMatcher", set("lookup", "best fit"), "best fit");
        /* steps 8, 10 */
        OptionsRecord opt = new OptionsRecord(OptionsRecord.MatcherType.forName(matcher));
        /* step 11 */
        SetNumberFormatDigitOptions(cx, pluralRules::setMinimumIntegerDigits, pluralRules::setMinimumFractionDigits,
                pluralRules::setMaximumFractionDigits, pluralRules::setMinimumSignificantDigits,
                pluralRules::setMaximumSignificantDigits, options, 0, 3);
        /* step 12 (FIXME: update spec) */
        /* step 13 */
        ResolvedLocale r = ResolveLocale(cx.getRealm(), getAvailableLocalesLazy(cx), requestedLocales, opt,
                relevantExtensionKeys, new PluralRulesLocaleData());
        /* step 14 */
        pluralRules.setLocale(r.getLocale());
        /* step 15 (not applicable) */
        /* step 16 (return) */
    }

    /**
     * Intl.PluralRules ([ locales [ , options ]])
     */
    @Override
    public ScriptObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "Intl.PluralRules");
    }

    /**
     * Intl.PluralRules ([ locales [ , options ]])
     */
    @Override
    public PluralRulesObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object locales = argument(args, 0);
        Object options = argument(args, 1);

        /* step 1 (not applicable) */
        /* step 2 */
        PluralRulesObject pluralRules = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.Intl_PluralRulesPrototype, PluralRulesObject::new);
        /* step 3 */
        InitializePluralRules(calleeContext, pluralRules, locales, options);
        return pluralRules;
    }

    /**
     * Properties of the Intl.PluralRules Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "PluralRules";

        /**
         * Intl.PluralRules.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_PluralRulesPrototype;

        /**
         * Intl.PluralRules.supportedLocalesOf (locales [, options ])
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
            Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
            /* step 2 */
            return SupportedLocales(cx, getAvailableLocales(cx), requestedLocales, options);
        }
    }
}
