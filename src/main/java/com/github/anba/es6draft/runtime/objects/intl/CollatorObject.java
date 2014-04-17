/**
 * Copyright (c) 2012-2014 André Bargull
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
public class CollatorObject extends OrdinaryObject {
    /** [[initializedIntlObject]] */
    private boolean initializedIntlObject;

    /** [[initializedCollator]] */
    private boolean initializedCollator;

    /** [[usage]] */
    private String usage;

    /** [[locale]] */
    private String locale;

    /** [[collation]] */
    private String collation;

    /** [[numeric]] */
    private boolean numeric;

    /** [[caseFirst]] */
    private String caseFirst;

    /** [[sensitivity]] */
    private String sensitivity;

    /** [[ignorePunctuation]] */
    private boolean ignorePunctuation;

    /** [[boundCompare]] */
    private Callable boundCompare;

    private Collator collator;

    public CollatorObject(Realm realm) {
        super(realm);
    }

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
        RuleBasedCollator collator;
        try {
            collator = (RuleBasedCollator) Collator.getInstance(locale);
        } catch (IllegalMonitorStateException e) {
            throw new StackOverflowError();
        }
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        collator.setNumericCollation(numeric);
        switch (caseFirst) {
        case "upper":
            collator.setUpperCaseFirst(true);
            break;
        case "lower":
            collator.setLowerCaseFirst(true);
            break;
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
        }
        collator.setAlternateHandlingShifted(ignorePunctuation);
        return collator;
    }

    /**
     * [[initializedIntlObject]]
     * 
     * @return {@code true} if the Intl object is initialized
     */
    public boolean isInitializedIntlObject() {
        return initializedIntlObject;
    }

    /**
     * [[initializedIntlObject]]
     * 
     * @param initializedIntlObject
     *            the new initialization state
     */
    public void setInitializedIntlObject(boolean initializedIntlObject) {
        this.initializedIntlObject = initializedIntlObject;
    }

    /**
     * [[initializedCollator]]
     * 
     * @return {@code true} if the collator object is initialized
     */
    public boolean isInitializedCollator() {
        return initializedCollator;
    }

    /**
     * [[initializedCollator]]
     * 
     * @param initializedCollator
     *            the new initialization state
     */
    public void setInitializedCollator(boolean initializedCollator) {
        this.initializedCollator = initializedCollator;
    }

    /**
     * [[usage]]
     * 
     * @return the usage field
     */
    public String getUsage() {
        return usage;
    }

    /**
     * [[usage]]
     * 
     * @param usage
     *            the new usage value
     */
    public void setUsage(String usage) {
        this.usage = usage;
    }

    /**
     * [[locale]]
     * 
     * @return the locale
     */
    public String getLocale() {
        return locale;
    }

    /**
     * [[locale]]
     * 
     * @param locale
     *            the new locale
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * [[collation]]
     * 
     * @return the collation kind
     */
    public String getCollation() {
        return collation;
    }

    /**
     * [[collation]]
     * 
     * @param collation
     *            the new collation
     */
    public void setCollation(String collation) {
        this.collation = collation;
    }

    /**
     * [[numeric]]
     * 
     * @return {@code true} if numeric collation should be performed
     */
    public boolean isNumeric() {
        return numeric;
    }

    /**
     * [[numeric]]
     * 
     * @param numeric
     *            the new numeric value
     */
    public void setNumeric(boolean numeric) {
        this.numeric = numeric;
    }

    /**
     * [[caseFirst]]
     * 
     * @return the case-first value
     */
    public String getCaseFirst() {
        return caseFirst;
    }

    /**
     * [[caseFirst]]
     * 
     * @param caseFirst
     *            the new case-first value
     */
    public void setCaseFirst(String caseFirst) {
        this.caseFirst = caseFirst;
    }

    /**
     * [[sensitivity]]
     * 
     * @return the sensitivity value
     */
    public String getSensitivity() {
        return sensitivity;
    }

    /**
     * [[sensitivity]]
     * 
     * @param sensitivity
     *            the new sensitivity value
     */
    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }

    /**
     * [[ignorePunctuation]]
     * 
     * @return the ignorePunctuation flag
     */
    public boolean isIgnorePunctuation() {
        return ignorePunctuation;
    }

    /**
     * [[ignorePunctuation]]
     * 
     * @param ignorePunctuation
     *            the new ignorePunctuation flag
     */
    public void setIgnorePunctuation(boolean ignorePunctuation) {
        this.ignorePunctuation = ignorePunctuation;
    }

    /**
     * [[boundCompare]]
     * 
     * @return the bound compare function
     */
    public Callable getBoundCompare() {
        return boundCompare;
    }

    /**
     * [[boundCompare]]
     * 
     * @param boundCompare
     *            the new bound compare function
     */
    public void setBoundCompare(Callable boundCompare) {
        this.boundCompare = boundCompare;
    }
}
