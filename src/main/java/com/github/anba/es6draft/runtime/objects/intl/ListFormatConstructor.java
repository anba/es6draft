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
 * <h1>ListFormat Objects</h1>
 * <ul>
 * <li>The Intl.ListFormat Constructor
 * <li>Properties of the Intl.ListFormat Constructor
 * </ul>
 */
public final class ListFormatConstructor extends BuiltinConstructor implements Initializable {
    /** [[availableLocales]] */
    private final Lazy<Set<String>> availableLocales = Lazy
            .of(() -> GetAvailableLocales(LanguageData.getAvailableListFormatLocales()));

    /**
     * [[availableLocales]]
     * 
     * @param cx
     *            the execution context
     * @return the set of available locales supported by {@code Intl.ListFormat}
     */
    public static Set<String> getAvailableLocales(ExecutionContext cx) {
        return getAvailableLocalesLazy(cx).get();
    }

    private static Lazy<Set<String>> getAvailableLocalesLazy(ExecutionContext cx) {
        ListFormatConstructor listFormat = (ListFormatConstructor) cx.getIntrinsic(Intrinsics.Intl_ListFormat);
        return listFormat.availableLocales;
    }

    /** [[relevantExtensionKeys]] */
    private static final List<ExtensionKey> relevantExtensionKeys = Collections.emptyList();

    private static final class ListFormatLocaleData implements LocaleData {
        @Override
        public LocaleDataInfo info(ULocale locale) {
            return new ListFormatLocaleDataInfo();
        }
    }

    private static final class ListFormatLocaleDataInfo implements LocaleDataInfo {
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
     * Constructs a new ListFormat constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public ListFormatConstructor(Realm realm) {
        super(realm, "ListFormat", 0);
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
     * InitializeListFormat (listFormat, locales, options)
     * 
     * @param cx
     *            the execution context
     * @param listFormat
     *            the ListFormat object
     * @param locales
     *            the locales array
     * @param opts
     *            the options object
     */
    public static void InitializeListFormat(ExecutionContext cx, ListFormatObject listFormat, Object locales,
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
        // FIXME: spec issue - localeMatcher should be defined, too!
        String matcher = GetStringOption(cx, options, "localeMatcher", set("lookup", "best fit"), "best fit");
        OptionsRecord opt = new OptionsRecord(OptionsRecord.MatcherType.forName(matcher));
        /* step 7 */
        // FIXME: spec issue - provide a special type for number formatting? (CLDR "numbers/symbols/list" entry is often
        // different when compared to the separator for "listPatterns".)
        String t = GetStringOption(cx, options, "type", set("regular", "unit"), "regular");
        /* step 8 */
        listFormat.setType(t);
        /* step 9 */
        // FIXME: spec issue - CLDR doesn't provide "regular-narrow".
        String s = GetStringOption(cx, options, "style", set("long", "short", "narrow"), "long");
        /* step 10 */
        listFormat.setStyle(s);
        /* step 12 */
        ResolvedLocale r = ResolveLocale(cx.getRealm(), getAvailableLocalesLazy(cx), requestedLocales, opt,
                relevantExtensionKeys, new ListFormatLocaleData());
        /* steps 13-19 (not applicable) */
        /* step 20 */
        listFormat.setLocale(r.getLocale());
        /* step 21 (TODO: Remove [[InitializedListFormat]] slot) */
        /* step 22 (return) */
    }

    /**
     * Intl.ListFormat ([ locales [ , options ]])
     */
    @Override
    public ScriptObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        // FIXME: spec issue - should throw TypeError when [[Call]]'ed.
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "Intl.ListFormat");
    }

    /**
     * Intl.ListFormat ([ locales [ , options ]])
     */
    @Override
    public ListFormatObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object locales = argument(args, 0);
        Object options = argument(args, 1);

        /* step 1 (not applicable) */
        /* step 2 */
        ListFormatObject listFormat = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.Intl_ListFormatPrototype, ListFormatObject::new);
        /* step 3 */
        InitializeListFormat(calleeContext, listFormat, locales, options);
        return listFormat;
    }

    /**
     * Properties of the Intl.ListFormat Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "ListFormat";

        /**
         * Intl.ListFormat.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_ListFormatPrototype;

        /**
         * Intl.ListFormat.supportedLocalesOf (locales [, options ])
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
