/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>11 NumberFormat Objects</h1>
 * <ul>
 * <li>11.4 Properties of Intl.NumberFormat Instances
 * </ul>
 */
public class NumberFormatObject extends OrdinaryObject implements ScriptObject {
    /**
     * [[initializedIntlObject]]
     */
    private boolean initializedIntlObject = false;

    /**
     * [[initializedNumberFormat]]
     */
    private boolean initializedNumberFormat = false;

    /**
     * [[locale]]
     */
    private String locale;

    /**
     * [[numberingSystem]]
     */
    private String numberingSystem;

    /**
     * [[style]]
     */
    private Style style;

    public enum Style {
        Decimal, Currency, Percent;
    }

    /**
     * [[currency]]
     */
    private String currency;

    /**
     * [[currencyDisplay]]
     */
    private CurrencyDisplay currencyDisplay;

    public enum CurrencyDisplay {
        Code, Symbol, Name
    }

    /**
     * [[minimumIntegerDigits]]
     */
    private int minimumIntegerDigits;

    /**
     * [[minimumFractionDigits]]
     */
    private int minimumFractionDigits;

    /**
     * [[maximumFractionDigits]]
     */
    private int maximumFractionDigits;

    /**
     * [[minimumSignificantDigits]]
     */
    private int minimumSignificantDigits;

    /**
     * [[maximumSignificantDigits]]
     */
    private int maximumSignificantDigits;

    /**
     * [[useGrouping]]
     */
    private boolean useGrouping;

    /**
     * [[positivePattern]]
     */
    private String positivePattern;

    /**
     * [[negativePattern]]
     */
    private String negativePattern;

    /**
     * [[boundFormat]]
     */
    private Callable boundFormat;

    public NumberFormatObject(Realm realm) {
        super(realm);
    }

    /**
     * [[initializedIntlObject]]
     */
    public boolean isInitializedIntlObject() {
        return initializedIntlObject;
    }

    /**
     * [[initializedIntlObject]]
     */
    public void setInitializedIntlObject(boolean initializedIntlObject) {
        this.initializedIntlObject = initializedIntlObject;
    }

    /**
     * [[initializedNumberFormat]]
     */
    public boolean isInitializedNumberFormat() {
        return initializedNumberFormat;
    }

    /**
     * [[initializedNumberFormat]]
     */
    public void setInitializedNumberFormat(boolean initializedNumberFormat) {
        this.initializedNumberFormat = initializedNumberFormat;
    }

    /**
     * [[locale]]
     */
    public String getLocale() {
        return locale;
    }

    /**
     * [[locale]]
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * [[numberingSystem]]
     */
    public String getNumberingSystem() {
        return numberingSystem;
    }

    /**
     * [[numberingSystem]]
     */
    public void setNumberingSystem(String numberingSystem) {
        this.numberingSystem = numberingSystem;
    }

    /**
     * [[style]]
     */
    public Style getStyle() {
        return style;
    }

    /**
     * [[style]]
     */
    public void setStyle(Style style) {
        this.style = style;
    }

    /**
     * [[currency]]
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * [[currency]]
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * [[currencyDisplay]]
     */
    public CurrencyDisplay getCurrencyDisplay() {
        return currencyDisplay;
    }

    /**
     * [[currencyDisplay]]
     */
    public void setCurrencyDisplay(CurrencyDisplay currencyDisplay) {
        this.currencyDisplay = currencyDisplay;
    }

    /**
     * [[minimumIntegerDigits]]
     */
    public int getMinimumIntegerDigits() {
        return minimumIntegerDigits;
    }

    /**
     * [[minimumIntegerDigits]]
     */
    public void setMinimumIntegerDigits(int minimumIntegerDigits) {
        this.minimumIntegerDigits = minimumIntegerDigits;
    }

    /**
     * [[minimumFractionDigits]]
     */
    public int getMinimumFractionDigits() {
        return minimumFractionDigits;
    }

    /**
     * [[minimumFractionDigits]]
     */
    public void setMinimumFractionDigits(int minimumFractionDigits) {
        this.minimumFractionDigits = minimumFractionDigits;
    }

    /**
     * [[maximumFractionDigits]]
     */
    public int getMaximumFractionDigits() {
        return maximumFractionDigits;
    }

    /**
     * [[maximumFractionDigits]]
     */
    public void setMaximumFractionDigits(int maximumFractionDigits) {
        this.maximumFractionDigits = maximumFractionDigits;
    }

    /**
     * [[minimumSignificantDigits]]
     */
    public int getMinimumSignificantDigits() {
        return minimumSignificantDigits;
    }

    /**
     * [[minimumSignificantDigits]]
     */
    public void setMinimumSignificantDigits(int minimumSignificantDigits) {
        this.minimumSignificantDigits = minimumSignificantDigits;
    }

    /**
     * [[maximumSignificantDigits]]
     */
    public int getMaximumSignificantDigits() {
        return maximumSignificantDigits;
    }

    /**
     * [[maximumSignificantDigits]]
     */
    public void setMaximumSignificantDigits(int maximumSignificantDigits) {
        this.maximumSignificantDigits = maximumSignificantDigits;
    }

    /**
     * [[useGrouping]]
     */
    public boolean isUseGrouping() {
        return useGrouping;
    }

    /**
     * [[useGrouping]]
     */
    public void setUseGrouping(boolean useGrouping) {
        this.useGrouping = useGrouping;
    }

    /**
     * [[positivePattern]]
     */
    public String getPositivePattern() {
        return positivePattern;
    }

    /**
     * [[positivePattern]]
     */
    public void setPositivePattern(String positivePattern) {
        this.positivePattern = positivePattern;
    }

    /**
     * [[negativePattern]]
     */
    public String getNegativePattern() {
        return negativePattern;
    }

    /**
     * [[negativePattern]]
     */
    public void setNegativePattern(String negativePattern) {
        this.negativePattern = negativePattern;
    }

    /**
     * [[boundFormat]]
     */
    public Callable getBoundFormat() {
        return boundFormat;
    }

    /**
     * [[boundFormat]]
     */
    public void setBoundFormat(Callable boundFormat) {
        this.boundFormat = boundFormat;
    }
}
