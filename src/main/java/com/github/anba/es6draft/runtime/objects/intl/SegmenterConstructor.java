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
 * <h1>Segmenter Objectss</h1>
 * <ul>
 * <li>The Intl.Segmenter Constructor
 * <li>Properties of the Intl.Segmenter Constructor
 * </ul>
 */
public final class SegmenterConstructor extends BuiltinConstructor implements Initializable {
    /** [[availableLocales]] */
    private final Lazy<Set<String>> availableLocales = Lazy
            .of(() -> GetAvailableLocales(LanguageData.getAvailableSegmenterLocales()));

    /**
     * [[availableLocales]]
     * 
     * @param cx
     *            the execution context
     * @return the set of available locales supported by {@code Intl.Segmenter}
     */
    public static Set<String> getAvailableLocales(ExecutionContext cx) {
        return getAvailableLocalesLazy(cx).get();
    }

    private static Lazy<Set<String>> getAvailableLocalesLazy(ExecutionContext cx) {
        SegmenterConstructor segmenter = (SegmenterConstructor) cx.getIntrinsic(Intrinsics.Intl_Segmenter);
        return segmenter.availableLocales;
    }

    /** [[relevantExtensionKeys]] */
    private static final List<ExtensionKey> relevantExtensionKeys = Collections.emptyList();

    private static final class SegmenterLocaleData implements LocaleData {
        @Override
        public LocaleDataInfo info(ULocale locale) {
            return new SegmenterLocaleDataInfo();
        }
    }

    private static final class SegmenterLocaleDataInfo implements LocaleDataInfo {
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
     * Constructs a new Segmenter constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public SegmenterConstructor(Realm realm) {
        super(realm, "Segmenter", 0);
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
     * Intl.Segmenter ([ locales [ , options ]])
     */
    @Override
    public ScriptObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "Intl.Segmenter");
    }

    /**
     * Intl.Segmenter ([ locales [ , options ]])
     */
    @Override
    public SegmenterObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object locales = argument(args, 0);
        Object opts = argument(args, 1);

        /* step 1 (not applicable) */
        /* step 2 */
        SegmenterObject segmenter = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.Intl_SegmenterPrototype, SegmenterObject::new);
        /* step 3 */
        Set<String> requestedLocals = CanonicalizeLocaleList(calleeContext, locales);
        /* steps 9-10 */
        // FIXME: spec bug - inserted at wrong position in spec
        ScriptObject options;
        if (Type.isUndefined(opts)) {
            options = ObjectCreate(calleeContext, (ScriptObject) null);
        } else {
            options = ToObject(calleeContext, opts);
        }
        /* step 5 */
        String matcher = GetStringOption(calleeContext, options, "localeMatcher", set("lookup", "best fit"),
                "best fit");
        /* steps 4, 6 */
        OptionsRecord opt = new OptionsRecord(OptionsRecord.MatcherType.forName(matcher));
        /* step 7 */
        SegmenterLocaleData localeData = new SegmenterLocaleData();
        ResolvedLocale r = ResolveLocale(calleeContext.getRealm(), getAvailableLocalesLazy(calleeContext),
                requestedLocals, opt, relevantExtensionKeys, localeData);
        /* step 8 */
        segmenter.setLocale(r.getLocale());
        /* step 11 */
        String granularity = GetStringOption(calleeContext, options, "granularity",
                set("grapheme", "word", "sentence", "line"), "grapheme");
        /* step 12 */
        segmenter.setGranularity(granularity);
        /* step 13 */
        if ("line".equals(granularity)) {
            /* step 13.a */
            String strictness = GetStringOption(calleeContext, options, "strictness", set("strict", "normal", "loose"),
                    "normal");
            /* step 13.b */
            segmenter.setStrictness(strictness);
        }
        /* step 14 */
        return segmenter;
    }

    /**
     * Properties of the Intl.Segmenter Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "Segmenter";

        /**
         * Intl.Segmenter.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_SegmenterPrototype;

        /**
         * Intl.Segmenter.supportedLocalesOf (locales [, options ])
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
