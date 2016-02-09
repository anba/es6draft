/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.PluralType;
import com.ibm.icu.util.ULocale;

/**
 * <h1>PluralRules Objects</h1>
 * <ul>
 * <li>Properties of Intl.PluralRules Instances
 * </ul>
 */
public class PluralRulesObject extends OrdinaryObject {
    /** [[type]] */
    private String type;
    /** [[locale]] */
    private String locale;
    /** [[pluralRule]] */
    private PluralRules pluralRules;
    /** [[boundResolve]] */
    private Callable boundResolve;

    /**
     * Constructs a new PluralRules object.
     * 
     * @param realm
     *            the realm object
     */
    public PluralRulesObject(Realm realm) {
        super(realm);
    }

    /**
     * Returns the ICU {@link PluralRules} instance.
     * 
     * @return the PluralRules instance
     */
    public PluralRules getPluralRules() {
        if (pluralRules == null) {
            pluralRules = createPluralRules();
        }
        return pluralRules;
    }

    private PluralRules createPluralRules() {
        ULocale locale = ULocale.forLanguageTag(this.locale);
        PluralType pluralType = "cardinal".equals(type) ? PluralType.CARDINAL : PluralType.ORDINAL;
        return PluralRules.forLocale(locale, pluralType);
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
     * [[type]]
     * 
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * [[type]]
     * 
     * @param type
     *            the new type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * [[boundResolve]]
     * 
     * @return the bound resolve function
     */
    public Callable getBoundResolve() {
        return boundResolve;
    }

    /**
     * [[boundResolve]]
     * 
     * @param boundResolve
     *            the bound resolve function
     */
    public void setBoundResolve(Callable boundResolve) {
        this.boundResolve = boundResolve;
    }
}
