/**
 * Copyright (c) 2012-2015 André Bargull
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
     * Maps locales to BCP47 language tags.
     * 
     * @param locales
     *            array of locales
     * @return list of BCP47 language tags
     */
    private static List<String> toLanguageTags(ULocale[] locales) {
        ArrayList<String> list = new ArrayList<>(locales.length);
        for (ULocale locale : locales) {
            String languageTag = locale.toLanguageTag();
            if ("en-US-u-va-posix".equals(languageTag)) {
                // Language tags with unicode locale extension sequences are not allowed.
                continue;
            }
            list.add(languageTag);
        }
        return list;
    }

    private static Set<String> addDerivedLanguages(List<String> available) {
        HashSet<String> availableSet = new HashSet<>(available);
        HashSet<String> derivedSet = new HashSet<>(available);
        for (ULocale locale : ULocale.getAvailableLocales()) {
            String languageTag = locale.toLanguageTag();
            if ("en-US-u-va-posix".equals(languageTag)) {
                // Language tags with unicode locale extension sequences are not allowed.
                continue;
            }
            if (derivedSet.contains(languageTag)) {
                continue;
            }
            int languageSep = languageTag.indexOf('-');
            if (languageSep == -1) {
                continue;
            }
            String language = languageTag.substring(0, languageSep);
            if (availableSet.contains(language)) {
                derivedSet.add(languageTag);
                continue;
            }
            int scriptSep = languageTag.indexOf('-', languageSep + 1);
            if (scriptSep == -1) {
                continue;
            }
            String languageScript = languageTag.substring(0, scriptSep);
            if (availableSet.contains(languageScript)) {
                derivedSet.add(languageTag);
                continue;
            }
            String languageCountry = language + languageTag.substring(scriptSep);
            if (availableSet.contains(languageCountry)) {
                derivedSet.add(languageTag);
                continue;
            }
        }
        return derivedSet;
    }
}
