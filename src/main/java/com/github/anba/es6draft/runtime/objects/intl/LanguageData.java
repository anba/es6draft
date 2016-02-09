/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;

/**
 * 
 */
final class LanguageData {
    private LanguageData() {
    }

    /**
     * Returns the set of available locales supported by the {@link Collator} class.
     * 
     * @return the set of available locales
     * @see Collator#getAvailableULocales()
     */
    static Set<String> getAvailableCollatorLocales() {
        return addDerivedLanguages(toLanguageTags(Collator.getAvailableULocales()));
    }

    /**
     * Returns the set of available locales supported by the {@link DateFormat} class.
     * 
     * @return the set of available locales
     * @see DateFormat#getAvailableULocales()
     */
    static List<String> getAvailableDateFormatLocales() {
        return toLanguageTags(DateFormat.getAvailableULocales());
    }

    /**
     * Returns the set of available locales supported by the {@link NumberFormat} class.
     * 
     * @return the set of available locales
     * @see NumberFormat#getAvailableULocales()
     */
    static List<String> getAvailableNumberFormatLocales() {
        return toLanguageTags(NumberFormat.getAvailableULocales());
    }

    /**
     * Returns the set of available locales supported by the {@link PluralRules} class.
     * 
     * @return the set of available locales
     * @see PluralRules#getAvailableULocales()
     */
    static List<String> getAvailablePluralRulesLocales() {
        return toLanguageTags(PluralRules.getAvailableULocales());
    }

    /**
     * Maps locales to BCP47 language tags.
     * 
     * @param locales
     *            array of locales
     * @return list of BCP47 language tags
     */
    private static List<String> toLanguageTags(ULocale[] locales) {
        ArrayList<String> list = new ArrayList<>(locales.length);
        for (ULocale locale : locales) {
            if (!(locale.getVariant().isEmpty() && locale.getUnicodeLocaleKeys().isEmpty())) {
                // Ignore locales with variants or unicode extension sequences.
                continue;
            }
            list.add(locale.toLanguageTag());
        }
        return list;
    }

    private static Set<String> addDerivedLanguages(List<String> available) {
        HashSet<String> availableSet = new HashSet<>(available);
        HashSet<String> derivedSet = new HashSet<>(available);
        for (ULocale locale : ULocale.getAvailableLocales()) {
            String languageTag = locale.toLanguageTag();
            if (derivedSet.contains(languageTag)) {
                continue;
            }
            if (!(locale.getVariant().isEmpty() && locale.getUnicodeLocaleKeys().isEmpty())) {
                // Ignore locales with variants or unicode extension sequences.
                continue;
            }
            String language = locale.getLanguage();
            if (availableSet.contains(language)) {
                derivedSet.add(languageTag);
                continue;
            }
            String script = locale.getScript();
            if (!script.isEmpty()) {
                String languageScript = language + "-" + script;
                if (availableSet.contains(languageScript)) {
                    derivedSet.add(languageTag);
                    continue;
                }
            }
            String country = locale.getCountry();
            if (!country.isEmpty()) {
                String languageCountry = language + "-" + country;
                if (availableSet.contains(languageCountry)) {
                    derivedSet.add(languageTag);
                    continue;
                }
            }
        }
        return derivedSet;
    }
}
