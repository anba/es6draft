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
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

/**
 * <h1>11 NumberFormat Objects</h1>
 * <ul>
 * <li>11.4 Properties of Intl.NumberFormat Instances
 * </ul>
 */
public class NumberFormatObject extends OrdinaryObject {
    /** [[initializedIntlObject]] */
    private boolean initializedIntlObject;

    /** [[initializedNumberFormat]] */
    private boolean initializedNumberFormat;

    /** [[locale]] */
    private String locale;

    /** [[numberingSystem]] */
    private String numberingSystem;

    /** [[style]] */
    private String style;

    /** [[currency]] */
    private String currency;

    /** [[currencyDisplay]] */
    private String currencyDisplay;

    /** [[minimumIntegerDigits]] */
    private int minimumIntegerDigits;

    /** [[minimumFractionDigits]] */
    private int minimumFractionDigits;

    /** [[maximumFractionDigits]] */
    private int maximumFractionDigits;

    /** [[minimumSignificantDigits]] */
    private int minimumSignificantDigits;

    /** [[maximumSignificantDigits]] */
    private int maximumSignificantDigits;

    /** [[useGrouping]] */
    private boolean useGrouping;

    /** [[boundFormat]] */
    private Callable boundFormat;

    private NumberFormat numberFormat;

    public NumberFormatObject(Realm realm) {
        super(realm);
    }

    public NumberFormat getNumberFormat() {
        if (numberFormat == null) {
            numberFormat = createNumberFormat();
        }
        return numberFormat;
    }

    private NumberFormat createNumberFormat() {
        ULocale locale = ULocale.forLanguageTag(this.locale);
        int choice;
        if ("decimal".equals(style)) {
            choice = NumberFormat.NUMBERSTYLE;
        } else if ("percent".equals(style)) {
            choice = NumberFormat.PERCENTSTYLE;
        } else {
            if ("code".equals(currencyDisplay)) {
                choice = NumberFormat.ISOCURRENCYSTYLE;
            } else if ("symbol".equals(currencyDisplay)) {
                choice = NumberFormat.CURRENCYSTYLE;
            } else {
                choice = NumberFormat.PLURALCURRENCYSTYLE;
            }
        }
        DecimalFormat numberFormat;
        try {
            numberFormat = (DecimalFormat) NumberFormat.getInstance(locale, choice);
        } catch (IllegalMonitorStateException e) {
            throw new StackOverflowError();
        }
        if ("currency".equals(style)) {
            numberFormat.setCurrency(Currency.getInstance(currency));
        }
        // numberingSystem is already handled in language-tag
        // assert locale.getKeywordValue("numbers").equals(numberingSystem);
        if (minimumSignificantDigits != 0 && maximumSignificantDigits != 0) {
            numberFormat.setSignificantDigitsUsed(true);
            numberFormat.setMinimumSignificantDigits(minimumSignificantDigits);
            numberFormat.setMaximumFractionDigits(maximumSignificantDigits);
        } else {
            numberFormat.setMinimumIntegerDigits(minimumIntegerDigits);
            numberFormat.setMinimumFractionDigits(minimumFractionDigits);
            numberFormat.setMaximumFractionDigits(maximumFractionDigits);
        }
        numberFormat.setGroupingUsed(useGrouping);
        // as required by ToRawPrecision/ToRawFixed
        // FIXME: ICU4J bug:
        // new Intl.NumberFormat("en",{useGrouping:false}).format(111111111111111)
        // returns "111111111111111.02"
        numberFormat.setRoundingMode(BigDecimal.ROUND_HALF_UP);
        return numberFormat;
    }

    /** [[initializedIntlObject]] */
    public boolean isInitializedIntlObject() {
        return initializedIntlObject;
    }

    /** [[initializedIntlObject]] */
    public void setInitializedIntlObject(boolean initializedIntlObject) {
        this.initializedIntlObject = initializedIntlObject;
    }

    /** [[initializedNumberFormat]] */
    public boolean isInitializedNumberFormat() {
        return initializedNumberFormat;
    }

    /** [[initializedNumberFormat]] */
    public void setInitializedNumberFormat(boolean initializedNumberFormat) {
        this.initializedNumberFormat = initializedNumberFormat;
    }

    /** [[locale]] */
    public String getLocale() {
        return locale;
    }

    /** [[locale]] */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /** [[numberingSystem]] */
    public String getNumberingSystem() {
        return numberingSystem;
    }

    /** [[numberingSystem]] */
    public void setNumberingSystem(String numberingSystem) {
        this.numberingSystem = numberingSystem;
    }

    /** [[style]] */
    public String getStyle() {
        return style;
    }

    /** [[style]] */
    public void setStyle(String style) {
        this.style = style;
    }

    /** [[currency]] */
    public String getCurrency() {
        return currency;
    }

    /** [[currency]] */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /** [[currencyDisplay]] */
    public String getCurrencyDisplay() {
        return currencyDisplay;
    }

    /** [[currencyDisplay]] */
    public void setCurrencyDisplay(String currencyDisplay) {
        this.currencyDisplay = currencyDisplay;
    }

    /** [[minimumIntegerDigits]] */
    public int getMinimumIntegerDigits() {
        return minimumIntegerDigits;
    }

    /** [[minimumIntegerDigits]] */
    public void setMinimumIntegerDigits(int minimumIntegerDigits) {
        this.minimumIntegerDigits = minimumIntegerDigits;
    }

    /** [[minimumFractionDigits]] */
    public int getMinimumFractionDigits() {
        return minimumFractionDigits;
    }

    /** [[minimumFractionDigits]] */
    public void setMinimumFractionDigits(int minimumFractionDigits) {
        this.minimumFractionDigits = minimumFractionDigits;
    }

    /** [[maximumFractionDigits]] */
    public int getMaximumFractionDigits() {
        return maximumFractionDigits;
    }

    /** [[maximumFractionDigits]] */
    public void setMaximumFractionDigits(int maximumFractionDigits) {
        this.maximumFractionDigits = maximumFractionDigits;
    }

    /** [[minimumSignificantDigits]] */
    public int getMinimumSignificantDigits() {
        return minimumSignificantDigits;
    }

    /** [[minimumSignificantDigits]] */
    public void setMinimumSignificantDigits(int minimumSignificantDigits) {
        this.minimumSignificantDigits = minimumSignificantDigits;
    }

    /** [[maximumSignificantDigits]] */
    public int getMaximumSignificantDigits() {
        return maximumSignificantDigits;
    }

    /** [[maximumSignificantDigits]] */
    public void setMaximumSignificantDigits(int maximumSignificantDigits) {
        this.maximumSignificantDigits = maximumSignificantDigits;
    }

    /** [[useGrouping]] */
    public boolean isUseGrouping() {
        return useGrouping;
    }

    /** [[useGrouping]] */
    public void setUseGrouping(boolean useGrouping) {
        this.useGrouping = useGrouping;
    }

    /** [[boundFormat]] */
    public Callable getBoundFormat() {
        return boundFormat;
    }

    /** [[boundFormat]] */
    public void setBoundFormat(Callable boundFormat) {
        this.boundFormat = boundFormat;
    }
}
