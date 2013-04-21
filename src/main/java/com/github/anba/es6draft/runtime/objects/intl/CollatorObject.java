/**
 * Copyright (c) 2012-2013 André Bargull
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
        RuleBasedCollator collator = (RuleBasedCollator) Collator.getInstance(locale);
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        // usage?
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

    /** [[initializedIntlObject]] */
    public boolean isInitializedIntlObject() {
        return initializedIntlObject;
    }

    /** [[initializedIntlObject]] */
    public void setInitializedIntlObject(boolean initialized) {
        this.initializedIntlObject = initialized;
    }

    /** [[initializedCollator]] */
    public boolean isInitializedCollator() {
        return initializedCollator;
    }

    /** [[initializedCollator]] */
    public void setInitializedCollator(boolean initializedCollator) {
        this.initializedCollator = initializedCollator;
    }

    /** [[usage]] */
    public String getUsage() {
        return usage;
    }

    /** [[usage]] */
    public void setUsage(String usage) {
        this.usage = usage;
    }

    /** [[locale]] */
    public String getLocale() {
        return locale;
    }

    /** [[locale]] */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /** [[collation]] */
    public String getCollation() {
        return collation;
    }

    /** [[collation]] */
    public void setCollation(String collation) {
        this.collation = collation;
    }

    /** [[numeric]] */
    public boolean isNumeric() {
        return numeric;
    }

    /** [[numeric]] */
    public void setNumeric(boolean numeric) {
        this.numeric = numeric;
    }

    /** [[caseFirst]] */
    public String getCaseFirst() {
        return caseFirst;
    }

    /** [[caseFirst]] */
    public void setCaseFirst(String caseFirst) {
        this.caseFirst = caseFirst;
    }

    /** [[sensitivity]] */
    public String getSensitivity() {
        return sensitivity;
    }

    /** [[sensitivity]] */
    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }

    /** [[ignorePunctuation]] */
    public boolean isIgnorePunctuation() {
        return ignorePunctuation;
    }

    /** [[ignorePunctuation]] */
    public void setIgnorePunctuation(boolean ignorePunctuation) {
        this.ignorePunctuation = ignorePunctuation;
    }

    /** [[boundCompare]] */
    public Callable getBoundCompare() {
        return boundCompare;
    }

    /** [[boundCompare]] */
    public void setBoundCompare(Callable boundCompare) {
        this.boundCompare = boundCompare;
    }
}
