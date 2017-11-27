/**
 * Copyright (c) André Bargull
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
 * <li>12.5 Properties of Intl.DateTimeFormat Instances
 * </ul>
 */
public final class DateTimeFormatObject extends OrdinaryObject {
    /** [[Locale]] */
    private String locale;

    /** [[Calendar]] */
    private String calendar;

    /** [[NumberingSystem]] */
    private String numberingSystem;

    /** [[TimeZone]] */
    private String timeZone;

    /** [[Pattern]] */
    private Lazy<String> pattern;

    /** [[BoundFormat]] */
    private Callable boundFormat;

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
     * [[Calendar]]
     * 
     * @return the calendar value
     */
    public String getCalendar() {
        return calendar;
    }

    /**
     * [[Calendar]]
     * 
     * @param calendar
     *            the new calendar value
     */
    public void setCalendar(String calendar) {
        this.calendar = calendar;
    }

    /**
     * [[NumberingSystem]]
     * 
     * @return the numbering system value
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
     * [[TimeZone]]
     * 
     * @return the time zone
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * [[TimeZone]]
     * 
     * @param timeZone
     *            the new time zone
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * [[Pattern]]
     * 
     * @return the pattern string
     */
    public String getPattern() {
        return pattern.get();
    }

    /**
     * [[Pattern]]
     * 
     * @param pattern
     *            the new pattern string
     */
    public void setPattern(Lazy<String> pattern) {
        this.pattern = pattern;
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
        return String.format("%s, locale=%s, calendar=%s, numberingSystem=%s, timeZone=%s, pattern=%s, dateFormat=%s",
                super.toString(), locale, calendar, numberingSystem, timeZone, pattern.get(), dateFormat);

    }
}
