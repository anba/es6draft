/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;

import java.text.FieldPosition;
import java.text.ParsePosition;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.PluralType;
import com.ibm.icu.util.ULocale;

/**
 * <h1>PluralRules Objects</h1>
 * <ul>
 * <li>Properties of Intl.PluralRules Instances
 * </ul>
 */
public final class PluralRulesObject extends OrdinaryObject {
    /** [[Type]] */
    private String type;

    /** [[Locale]] */
    private String locale;

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

    /** [[PluralRule]] */
    private PluralRules pluralRules;
    private NumberFormat numberFormat;

    /** [[PluralCategories]] **/
    private ArrayObject pluralCategories;

    private final Realm realm;

    /**
     * Constructs a new PluralRules object.
     * 
     * @param realm
     *            the realm object
     */
    public PluralRulesObject(Realm realm) {
        super(realm);
        this.realm = realm;
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

    public NumberFormat getNumberFormat() {
        if (numberFormat == null) {
            numberFormat = createNumberFormat();
        }
        return numberFormat;
    }

    private NumberFormat createNumberFormat() {
        ULocale locale = ULocale.forLanguageTag(this.locale);
        locale = locale.setKeywordValue("numbers", "latn");

        DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getInstance(locale, NumberFormat.NUMBERSTYLE);
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
        // as required by ToRawPrecision/ToRawFixed
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
     * [[Type]]
     * 
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * [[Type]]
     * 
     * @param type
     *            the new type
     */
    public void setType(String type) {
        this.type = type;
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
     * [[PluralCategories]]
     * 
     * @return the plural categories
     */
    public ArrayObject getPluralCategories() {
        if (pluralCategories == null) {
            pluralCategories = CreateArrayFromList(realm.defaultContext(), getPluralRules().getKeywords());
        }
        return pluralCategories;
    }

    @SuppressWarnings("deprecation")
    com.ibm.icu.text.PluralRules.FixedDecimal toFixedDecimal(double n) {
        NumberFormat nf = getNumberFormat();

        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(DecimalFormat.FRACTION_FIELD);
        nf.format(n, sb, fp);

        int v = fp.getEndIndex() - fp.getBeginIndex();
        long f = 0;
        if (v > 0) {
            ParsePosition pp = new ParsePosition(fp.getBeginIndex());
            f = nf.parse(sb.toString(), pp).longValue();
        }
        return new com.ibm.icu.text.PluralRules.FixedDecimal(n, v, f);
    }

    @Override
    public String toString() {
        return String.format(
                "%s, type=%s, locale=%s, minimumIntegerDigits=%d, minimumFractionDigits=%d, maximumFractionDigits=%d, "
                        + "minimumSignificantDigits=%d, maximumSignificantDigits=%d, pluralRules=%s",
                super.toString(), type, locale, minimumIntegerDigits, minimumFractionDigits, maximumFractionDigits,
                minimumSignificantDigits, maximumSignificantDigits, pluralRules);
    }
}
