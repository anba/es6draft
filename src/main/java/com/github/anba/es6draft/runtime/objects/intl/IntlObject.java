/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.CanonicalizeLanguageTag;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.CanonicalizeLocaleList;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.IsStructurallyValidLanguageTag;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.RemoveUnicodeLocaleExtension;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;

import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.intl.LanguageTagParser.LanguageTag;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>8 The Intl Object</h1>
 */
public final class IntlObject extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Intl object.
     * 
     * @param realm
     *            the realm object
     */
    public IntlObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, PluralRulesProperty.class);
        createProperties(realm, this, LocaleProperties.class);
    }

    /**
     * 8.1 Properties of the Intl Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Value(name = "Collator")
        public static final Intrinsics Collator = Intrinsics.Intl_Collator;

        @Value(name = "NumberFormat")
        public static final Intrinsics NumberFormat = Intrinsics.Intl_NumberFormat;

        @Value(name = "DateTimeFormat")
        public static final Intrinsics DateTimeFormat = Intrinsics.Intl_DateTimeFormat;
    }

    /**
     * Intl.PluralRules property
     */
    @CompatibilityExtension(CompatibilityOption.PluralRules)
    public enum PluralRulesProperty {
        ;

        @Value(name = "PluralRules")
        public static final Intrinsics PluralRules = Intrinsics.Intl_PluralRules;
    }

    /**
     * Locale Operations
     */
    @CompatibilityExtension(CompatibilityOption.Locale)
    public enum LocaleProperties {
        ;

        /**
         * Intl.getCanonicalLocales (locales)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the locales array
         * @return the array of canonicalized locales
         */
        @Function(name = "getCanonicalLocales", arity = 1)
        public static Object getCanonicalLocales(ExecutionContext cx, Object thisValue, Object locales) {
            /* step 2 */
            Set<String> localesList = CanonicalizeLocaleList(cx, locales);
            // FIXME: spec issue - should use CreateArrayFromList abstract op.
            /* steps 1, 3-5 */
            return CreateArrayFromList(cx, localesList);
        }

        /**
         * Intl.getParentLocales (locale)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locale
         *            the locale
         * @return the array of parent locales
         */
        @Function(name = "getParentLocales", arity = 1)
        public static Object getParentLocales(ExecutionContext cx, Object thisValue, Object locale) {
            /* step 1 */
            ArrayObject localeChain = ArrayCreate(cx, 0);
            /* step 2 */
            // FIXME: spec bug - CanonicalizeLanguageTag expects a previously validated language tag.
            String tag = ToFlatString(cx, locale);
            LanguageTag langTag = IsStructurallyValidLanguageTag(tag);
            if (langTag == null) {
                throw newRangeError(cx, Messages.Key.IntlStructurallyInvalidLanguageTag, tag);
            }
            String candidate = CanonicalizeLanguageTag(langTag);
            // FIXME: spec bug - unicode locale extension sequences should be removed (?).
            // FIXME: spec issue - how to handle privateuse sequences?
            // FIXME: spec bug - how to handle certain special languages, like the grandfathered "i-enochian"?
            // FIXME: spec bug - handle privateuse only tags, like "x-foo-bar".
            candidate = RemoveUnicodeLocaleExtension(candidate);
            /* step 3 */
            int n = 0;
            /* step 4 */
            // FIXME: spec bug - `n` not incremented resp. use 0 here and start n with 1.
            CreateDataProperty(cx, localeChain, n++, candidate);
            /* step 5 */
            int pos = candidate.lastIndexOf('-');
            /* step 6 */
            while (pos > -1) {
                /* step 6.a */
                if (pos >= 2 && candidate.charAt(pos - 2) == '-') {
                    pos -= 2;
                }
                /* step 6.b */
                candidate = candidate.substring(0, pos);
                /* steps 6.c, 6.e */
                CreateDataProperty(cx, localeChain, n++, candidate);
                /* step 6.d */
                pos = candidate.lastIndexOf('-');
            }
            /* step 7 */
            return localeChain;
        }
    }
}
