/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;

/**
 * <h1>10 Collator Objects</h1>
 * <ul>
 * <li>10.4 Properties of Intl.Collator Instances
 * </ul>
 */
public final class CollatorObject extends OrdinaryObject {
    /** [[Usage]] */
    private String usage;

    /** [[Locale]] */
    private String locale;

    /** [[Collation]] */
    private String collation;

    /** [[Numeric]] */
    private boolean numeric;

    /** [[CaseFirst]] */
    private String caseFirst;

    /** [[Sensitivity]] */
    private String sensitivity;

    /** [[IgnorePunctuation]] */
    private boolean ignorePunctuation;

    /** [[BoundCompare]] */
    private Callable boundCompare;

    private Collator collator;

    /**
     * Constructs a new Collator object.
     * 
     * @param realm
     *            the realm object
     */
    public CollatorObject(Realm realm) {
        super(realm);
    }

    /**
     * Returns the ICU {@link Collator} instance.
     * 
     * @return the Collator instance
     */
    public Collator getCollator() {
        if (collator == null) {
            collator = createCollator();
        }
        return collator;
    }

    private Collator createCollator() {
        ULocale locale = ULocale.forLanguageTag(this.locale);
        if ("search".equals(usage)) {
            // "search" usage cannot be set through unicode extensions (u-co-search), handle here:
            locale = locale.setKeywordValue("collation", "search");
        }
        RuleBasedCollator collator = (RuleBasedCollator) Collator.getInstance(locale);
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        collator.setNumericCollation(numeric);
        switch (caseFirst) {
        case "upper":
            collator.setUpperCaseFirst(true);
            break;
        case "lower":
            collator.setLowerCaseFirst(true);
            break;
        case "false":
            if (collator.isLowerCaseFirst()) {
                collator.setLowerCaseFirst(false);
            }
            if (collator.isUpperCaseFirst()) {
                collator.setUpperCaseFirst(false);
            }
            break;
        default:
            throw new AssertionError();
        }
        switch (sensitivity) {
        case "base":
            collator.setStrength(Collator.PRIMARY);
            break;
        case "accent":
            collator.setStrength(Collator.SECONDARY);
            break;
        case "case":
            collator.setStrength(Collator.PRIMARY);
            collator.setCaseLevel(true);
            break;
        case "variant":
            collator.setStrength(Collator.TERTIARY);
            break;
        default:
            throw new AssertionError();
        }
        collator.setAlternateHandlingShifted(ignorePunctuation);
        return collator;
    }

    /**
     * [[Usage]]
     * 
     * @return the usage field
     */
    public String getUsage() {
        return usage;
    }

    /**
     * [[Usage]]
     * 
     * @param usage
     *            the new usage value
     */
    public void setUsage(String usage) {
        this.usage = usage;
    }

    /**
     * [[Locale]]
     * 
     * @return the locale
     */
    public String getLocale() {
        return locale;
    }

    /**
     * [[Locale]]
     * 
     * @param locale
     *            the new locale
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * [[Collation]]
     * 
     * @return the collation kind
     */
    public String getCollation() {
        return collation;
    }

    /**
     * [[Collation]]
     * 
     * @param collation
     *            the new collation
     */
    public void setCollation(String collation) {
        this.collation = collation;
    }

    /**
     * [[Numeric]]
     * 
     * @return {@code true} if numeric collation should be performed
     */
    public boolean isNumeric() {
        return numeric;
    }

    /**
     * [[Numeric]]
     * 
     * @param numeric
     *            the new numeric value
     */
    public void setNumeric(boolean numeric) {
        this.numeric = numeric;
    }

    /**
     * [[CaseFirst]]
     * 
     * @return the case-first value
     */
    public String getCaseFirst() {
        return caseFirst;
    }

    /**
     * [[CaseFirst]]
     * 
     * @param caseFirst
     *            the new case-first value
     */
    public void setCaseFirst(String caseFirst) {
        this.caseFirst = caseFirst;
    }

    /**
     * [[Sensitivity]]
     * 
     * @return the sensitivity value
     */
    public String getSensitivity() {
        return sensitivity;
    }

    /**
     * [[Sensitivity]]
     * 
     * @param sensitivity
     *            the new sensitivity value
     */
    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }

    /**
     * [[IgnorePunctuation]]
     * 
     * @return the ignorePunctuation flag
     */
    public boolean isIgnorePunctuation() {
        return ignorePunctuation;
    }

    /**
     * [[IgnorePunctuation]]
     * 
     * @param ignorePunctuation
     *            the new ignorePunctuation flag
     */
    public void setIgnorePunctuation(boolean ignorePunctuation) {
        this.ignorePunctuation = ignorePunctuation;
    }

    /**
     * [[BoundCompare]]
     * 
     * @return the bound compare function
     */
    public Callable getBoundCompare() {
        return boundCompare;
    }

    /**
     * [[BoundCompare]]
     * 
     * @param boundCompare
     *            the new bound compare function
     */
    public void setBoundCompare(Callable boundCompare) {
        this.boundCompare = boundCompare;
    }

    @Override
    public String toString() {
        return String.format(
                "%s, usage=%s, locale=%s, collation=%s, numeric=%b, caseFirst=%s, sensitivity=%s, ignorePunctuation=%b, collator=%s",
                super.toString(), usage, locale, collation, numeric, caseFirst, sensitivity, ignorePunctuation,
                collator);
    }
}
