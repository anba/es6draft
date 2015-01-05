/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.BitSet;
import java.util.Iterator;
import java.util.regex.MatchResult;

/**
 * 
 */
final class GroupIterator implements Iterator<String> {
    private final MatchResult result;
    private final BitSet negativeLAGroups;
    private int group = 1;
    // start index of last valid group in matched string
    private int last;

    GroupIterator(MatchResult result, BitSet negativeLAGroups) {
        this.result = result;
        this.negativeLAGroups = negativeLAGroups;
        this.last = result.start();
    }

    @Override
    public boolean hasNext() {
        return group <= result.groupCount();
    }

    @Override
    public String next() {
        int group = this.group++;
        if (result.start(group) >= last && !negativeLAGroups.get(group)) {
            last = result.start(group);
            return result.group(group);
        } else {
            return null;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
