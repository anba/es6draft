/**
 * Copyright (c) 2012-2013 André Bargull
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
    /** [[initializedIntlObject]] */
    private boolean initializedIntlObject;

    /** [[initializedDateTimeFormat]] */
    private boolean initializedDateTimeFormat;

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

    public DateTimeFormatObject(Realm realm) {
        super(realm);
    }

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

    /** [[initializedIntlObject]] */
    public boolean isInitializedIntlObject() {
        return initializedIntlObject;
    }

    /** [[initializedIntlObject]] */
    public void setInitializedIntlObject(boolean initializedIntlObject) {
        this.initializedIntlObject = initializedIntlObject;
    }

    /** [[initializedDateTimeFormat]] */
    public boolean isInitializedDateTimeFormat() {
        return initializedDateTimeFormat;
    }

    /** [[initializedDateTimeFormat]] */
    public void setInitializedDateTimeFormat(boolean initializedDateTimeFormat) {
        this.initializedDateTimeFormat = initializedDateTimeFormat;
    }

    /** [[locale]] */
    public String getLocale() {
        return locale;
    }

    /** [[locale]] */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /** [[calendar]] */
    public String getCalendar() {
        return calendar;
    }

    /** [[calendar]] */
    public void setCalendar(String calendar) {
        this.calendar = calendar;
    }

    /** [[numberingSystem]] */
    public String getNumberingSystem() {
        return numberingSystem;
    }

    /** [[numberingSystem]] */
    public void setNumberingSystem(String numberingSystem) {
        this.numberingSystem = numberingSystem;
    }

    /** [[timeZone]] */
    public String getTimeZone() {
        return timeZone;
    }

    /** [[timeZone]] */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /** [[boundFormat]] */
    public Callable getBoundFormat() {
        return boundFormat;
    }

    /** [[boundFormat]] */
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
