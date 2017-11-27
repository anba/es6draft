/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.BreakIterator;

/**
 * <h1>Segmenter Objects</h1>
 * <ul>
 * <li>Segment Iterators
 * </ul>
 */
public final class SegmentIteratorObject extends OrdinaryObject {
    /** [[SegmentIteratorString]] */
    private final String string;

    /** [[SegmentIteratorPosition]] */
    private int position;

    /** [[SegmentIteratorBreakType]] */
    private String breakType;

    private final String granularity;
    private final BreakIterator breakIterator;

    SegmentIteratorObject(Realm realm, SegmenterObject segmenter, String string, ScriptObject prototype) {
        super(realm, prototype);
        this.string = string;
        this.position = 0;
        this.granularity = segmenter.getGranularity();
        this.breakIterator = segmenter.getBreakIterator();
        breakIterator.setText(string);
    }

    String getString() {
        return string;
    }

    int getPosition() {
        return position;
    }

    void setPosition(int position) {
        this.position = position;
    }

    public String getGranularity() {
        return granularity;
    }

    BreakIterator getBreakIterator() {
        return breakIterator;
    }

    String getBreakType() {
        return breakType;
    }

    void setBreakType(String breakType) {
        this.breakType = breakType;
    }

    @Override
    public String toString() {
        return String.format("%s, string=%s, position=%d, breakType=%s", super.toString(), string, position, breakType);
    }
}
