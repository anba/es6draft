/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import java.util.Date;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Lazy;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * <h1>12 DateTimeFormat Objects</h1>
 * <ul>
 * <li>12.4 Properties of Intl.DateTimeFormat Instances
 * </ul>
 */
public class DateTimeFormatObject extends OrdinaryObject {
    /** [[locale]] */
    private String locale;

    /** [[calendar]] */
    private String calendar;

    /** [[numberingSystem]] */
    private String numberingSystem;

    /** [[timeZone]] */
    private String timeZone;

    /** [[boundFormat]] */
    private Callable boundFormat;

    private Lazy<String> pattern;

    private DateFormat dateFormat;

    /**
     * Constructs a new DateTimeFormat object.
     * 
     * @param realm
     *            the realm object
     */
    public DateTimeFormatObject(Realm realm) {
        super(realm);
    }

    /**
     * Returns the ICU {@link DateFormat} instance.
     * 
     * @return the DateFormat instance
     */
    public DateFormat getDateFormat() {
        if (dateFormat == null) {
            dateFormat = createDateFormat();
        }
        return dateFormat;
    }

    private DateFormat createDateFormat() {
        ULocale locale = ULocale.forLanguageTag(this.locale);
        // calendar and numberingSystem are already handled in language-tag
        // assert locale.getKeywordValue("calendar").equals(calendar);
        // assert locale.getKeywordValue("numbers").equals(numberingSystem);
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern.get(), locale);
        if (timeZone != null) {
            dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        }
        Calendar calendar = dateFormat.getCalendar();
        if (calendar instanceof GregorianCalendar) {
            // format uses a proleptic Gregorian calendar with no year 0
            GregorianCalendar gregorian = (GregorianCalendar) calendar;
            gregorian.setGregorianChange(new Date(Long.MIN_VALUE));
        }
        return dateFormat;
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
     * [[calendar]]
     * 
     * @return the calendar value
     */
    public String getCalendar() {
        return calendar;
    }

    /**
     * [[calendar]]
     * 
     * @param calendar
     *            the new calendar value
     */
    public void setCalendar(String calendar) {
        this.calendar = calendar;
    }

    /**
     * [[numberingSystem]]
     * 
     * @return the numbering system value
     */
    public String getNumberingSystem() {
        return numberingSystem;
    }

    /**
     * [[numberingSystem]]
     * 
     * @param numberingSystem
     *            the new numbering system
     */
    public void setNumberingSystem(String numberingSystem) {
        this.numberingSystem = numberingSystem;
    }

    /**
     * [[timeZone]]
     * 
     * @return the time zone
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * [[timeZone]]
     * 
     * @param timeZone
     *            the new time zone
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * [[boundFormat]]
     * 
     * @return the bound format function
     */
    public Callable getBoundFormat() {
        return boundFormat;
    }

    /**
     * [[boundFormat]]
     * 
     * @param boundFormat
     *            the bound format function
     */
    public void setBoundFormat(Callable boundFormat) {
        this.boundFormat = boundFormat;
    }

    public String getPattern() {
        return pattern.get();
    }

    public void setPattern(Lazy<String> pattern) {
        this.pattern = pattern;
    }
}
