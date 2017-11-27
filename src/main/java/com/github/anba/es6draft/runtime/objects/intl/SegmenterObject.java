/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;

/**
 * <h1>Segmenter Objects</h1>
 * <ul>
 * <li>Internal slots of Intl.Segmenter Instances
 * </ul>
 */
public final class SegmenterObject extends OrdinaryObject {
    /** [[Locale]] */
    private String locale;

    /** [[SegmenterGranularity]] */
    private String granularity;

    /** [[SegmenterStrictness]] */
    private String strictness;

    private BreakIterator breakIterator;

    /**
     * Constructs a new Segmenter object.
     * 
     * @param realm
     *            the realm object
     */
    public SegmenterObject(Realm realm) {
        super(realm);
    }

    /**
     * Returns the ICU {@link BreakIterator} instance.
     * 
     * @return the BreakIterator instance
     */
    public BreakIterator getBreakIterator() {
        if (breakIterator == null) {
            breakIterator = createBreakIterator();
        }
        return (BreakIterator) breakIterator.clone();
    }

    private BreakIterator createBreakIterator() {
        ULocale locale = ULocale.forLanguageTag(this.locale);
        if ("line".equals(granularity)) {
            // "strictness" cannot be set through unicode extensions (u-lb-strict), handle here:
            locale = locale.setKeywordValue("lb", strictness);
        }
        BreakIterator breakIterator;
        switch (granularity) {
        case "grapheme":
            breakIterator = BreakIterator.getCharacterInstance(locale);
            break;
        case "word":
            breakIterator = BreakIterator.getWordInstance(locale);
            break;
        case "sentence":
            breakIterator = BreakIterator.getSentenceInstance(locale);
            break;
        case "line":
            breakIterator = BreakIterator.getLineInstance(locale);
            break;
        default:
            throw new AssertionError();
        }
        return breakIterator;
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
     * [[SegmenterGranularity]]
     * 
     * @return the granularity field
     */
    public String getGranularity() {
        return granularity;
    }

    /**
     * [[SegmenterGranularity]]
     * 
     * @param granularity
     *            the new granularity value
     */
    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    /**
     * [[SegmenterStrictness]]
     * 
     * @return the strictness field
     */
    public String getStrictness() {
        return strictness;
    }

    /**
     * [[SegmenterStrictness]]
     * 
     * @param strictness
     *            the new strictness value
     */
    public void setStrictness(String strictness) {
        this.strictness = strictness;
    }

    @Override
    public String toString() {
        return String.format("%s, locale=%s, granularity=%s, strictness=%s", super.toString(), locale, granularity,
                strictness);
    }

}
