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
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

/**
 * <h1>11 NumberFormat Objects</h1>
 * <ul>
 * <li>11.5 Properties of Intl.NumberFormat Instances
 * </ul>
 */
public final class NumberFormatObject extends OrdinaryObject {
    /** [[Locale]] */
    private String locale;

    /** [[NumberingSystem]] */
    private String numberingSystem;

    /** [[Style]] */
    private String style;

    /** [[Currency]] */
    private String currency;

    /** [[CurrencyDisplay]] */
    private String currencyDisplay;

    /** [[MinimumIntegerDigits]] */
    private int minimumIntegerDigits;

    /** [[MinimumFractionDigits]] */
    private int minimumFractionDigits;

    /** [[MaximumFractionDigits]] */
    private int maximumFractionDigits;

    /** [[MinimumSignificantDigits]] */
    private int minimumSignificantDigits;

    /** [[MaximumSignificantDigits]] */
    private int maximumSignificantDigits;

    /** [[UseGrouping]] */
    private boolean useGrouping;

    /** [[BoundFormat]] */
    private Callable boundFormat;

    private NumberFormat numberFormat;

    /**
     * Constructs a new NumberFormat object.
     * 
     * @param realm
     *            the realm object
     */
    public NumberFormatObject(Realm realm) {
        super(realm);
    }

    /**
     * Returns the ICU {@link NumberFormat} instance.
     * 
     * @return the NumberFormat instance
     */
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
        DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getInstance(locale, choice);
        if ("currency".equals(style)) {
            numberFormat.setCurrency(Currency.getInstance(currency));
        }
        // numberingSystem is already handled in language-tag
        // assert locale.getKeywordValue("numbers").equals(numberingSystem);
        if (minimumSignificantDigits != 0 && maximumSignificantDigits != 0) {
            numberFormat.setSignificantDigitsUsed(true);
            numberFormat.setMinimumSignificantDigits(minimumSignificantDigits);
            numberFormat.setMaximumSignificantDigits(maximumSignificantDigits);
        } else {
            numberFormat.setSignificantDigitsUsed(false);
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
     * [[NumberingSystem]]
     * 
     * @return the numbering system
     */
    public String getNumberingSystem() {
        return numberingSystem;
    }

    /**
     * [[NumberingSystem]]
     * 
     * @param numberingSystem
     *            the new numbering system
     */
    public void setNumberingSystem(String numberingSystem) {
        this.numberingSystem = numberingSystem;
    }

    /**
     * [[Style]]
     * 
     * @return the number format style
     */
    public String getStyle() {
        return style;
    }

    /**
     * [[Style]]
     * 
     * @param style
     *            the new number format style
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * [[Currency]]
     * 
     * @return the currency
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * [[Currency]]
     * 
     * @param currency
     *            the new currency
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * [[CurrencyDisplay]]
     * 
     * @return the currency display value
     */
    public String getCurrencyDisplay() {
        return currencyDisplay;
    }

    /**
     * [[CurrencyDisplay]]
     * 
     * @param currencyDisplay
     *            the new currency display value
     */
    public void setCurrencyDisplay(String currencyDisplay) {
        this.currencyDisplay = currencyDisplay;
    }

    /**
     * [[MinimumIntegerDigits]]
     * 
     * @return the minimum number of integer digits
     */
    public int getMinimumIntegerDigits() {
        return minimumIntegerDigits;
    }

    /**
     * [[MinimumIntegerDigits]]
     * 
     * @param minimumIntegerDigits
     *            the new minimum number of integer digits
     */
    public void setMinimumIntegerDigits(int minimumIntegerDigits) {
        this.minimumIntegerDigits = minimumIntegerDigits;
    }

    /**
     * [[MinimumFractionDigits]]
     * 
     * @return the minimum number of fraction digits
     */
    public int getMinimumFractionDigits() {
        return minimumFractionDigits;
    }

    /**
     * [[MinimumFractionDigits]]
     * 
     * @param minimumFractionDigits
     *            the new minimum number of fraction digits
     */
    public void setMinimumFractionDigits(int minimumFractionDigits) {
        this.minimumFractionDigits = minimumFractionDigits;
    }

    /**
     * [[MaximumFractionDigits]]
     * 
     * @return the maximum number of fraction digits
     */
    public int getMaximumFractionDigits() {
        return maximumFractionDigits;
    }

    /**
     * [[MaximumFractionDigits]]
     * 
     * @param maximumFractionDigits
     *            the new maximum number of fraction digits
     */
    public void setMaximumFractionDigits(int maximumFractionDigits) {
        this.maximumFractionDigits = maximumFractionDigits;
    }

    /**
     * [[MinimumSignificantDigits]]
     * 
     * @return the minimum number of significant digits
     */
    public int getMinimumSignificantDigits() {
        return minimumSignificantDigits;
    }

    /**
     * [[MinimumSignificantDigits]]
     * 
     * @param minimumSignificantDigits
     *            the new minimum number of significant digits
     */
    public void setMinimumSignificantDigits(int minimumSignificantDigits) {
        this.minimumSignificantDigits = minimumSignificantDigits;
    }

    /**
     * [[MaximumSignificantDigits]]
     * 
     * @return the maximum number of significant digits
     */
    public int getMaximumSignificantDigits() {
        return maximumSignificantDigits;
    }

    /**
     * [[MaximumSignificantDigits]]
     * 
     * @param maximumSignificantDigits
     *            the new maximum number of significant digits
     */
    public void setMaximumSignificantDigits(int maximumSignificantDigits) {
        this.maximumSignificantDigits = maximumSignificantDigits;
    }

    /**
     * [[UseGrouping]]
     * 
     * @return the grouping flag
     */
    public boolean isUseGrouping() {
        return useGrouping;
    }

    /**
     * [[UseGrouping]]
     * 
     * @param useGrouping
     *            the new grouping flag
     */
    public void setUseGrouping(boolean useGrouping) {
        this.useGrouping = useGrouping;
    }

    /**
     * [[BoundFormat]]
     * 
     * @return the bound format function
     */
    public Callable getBoundFormat() {
        return boundFormat;
    }

    /**
     * [[BoundFormat]]
     * 
     * @param boundFormat
     *            the bound format function
     */
    public void setBoundFormat(Callable boundFormat) {
        this.boundFormat = boundFormat;
    }

    @Override
    public String toString() {
        return String.format(
                "%s, locale=%s, numberingSystem=%s, style=%s, currency=%s, currencyDisplay=%s, "
                        + "minimumIntegerDigits=%d, minimumFractionDigits=%d, maximumFractionDigits=%d, "
                        + "minimumSignificantDigits=%d, maximumSignificantDigits=%d, useGrouping=%b, numberFormat=%s",
                super.toString(), locale, numberingSystem, style, currency, currencyDisplay, minimumIntegerDigits,
                minimumFractionDigits, maximumFractionDigits, minimumSignificantDigits, maximumSignificantDigits,
                useGrouping, numberFormat);
    }
}
