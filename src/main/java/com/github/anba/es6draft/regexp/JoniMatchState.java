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

import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;

/**
 * {@link MatchState} implementation for Joni {@link Regex} regular expressions
 */
final class JoniMatchState implements MatchState, IterableMatchResult {
    private final UEncoding encoding;
    private final Matcher matcher;
    private final CharSequence string;
    private final BitSet negativeLAGroups;
    private final StringPosition position;
    private int begin = -1, end = 0;
    private Region region;

    public JoniMatchState(UEncoding encoding, Matcher matcher, CharSequence string,
            BitSet negativeLAGroups) {
        this.encoding = encoding;
        this.matcher = matcher;
        this.string = string;
        this.negativeLAGroups = negativeLAGroups;
        this.position = new StringPosition(encoding.length(string));
    }

    private JoniMatchState(UEncoding encoding, CharSequence string, BitSet negativeLAGroups,
            StringPosition position, int begin, int end, Region region) {
        this.encoding = encoding;
        this.matcher = null;
        this.string = string;
        this.negativeLAGroups = negativeLAGroups;
        this.position = position;
        this.begin = begin;
        this.end = end;
        this.region = region;
    }

    private final class StringPosition implements Cloneable {
        final int length;
        int begin, end, stringBegin, stringEnd;

        StringPosition(int length) {
            this.length = length;
        }

        @Override
        public StringPosition clone() {
            StringPosition clone = new StringPosition(length);
            clone.begin = begin;
            clone.end = end;
            clone.stringBegin = stringBegin;
            clone.stringEnd = stringEnd;
            return clone;
        }

        void update(int begin, int end) {
            assert begin >= 0 && end >= 0 && begin <= end;
            this.stringBegin = stringIndex(begin);
            this.stringEnd = stringIndex(end);
            this.begin = begin;
            this.end = end;
        }

        int stringIndex(int byteIndex) {
            if (byteIndex >= end) {
                return encoding.stringIndex(string, stringEnd, byteIndex);
            }
            if (byteIndex >= begin) {
                return encoding.stringIndex(string, stringBegin, byteIndex);
            }
            return encoding.stringIndex(string, 0, byteIndex);
        }

        int byteIndex(int stringIndex) {
            if (stringIndex >= stringEnd) {
                return end + encoding.byteIndex(string, stringEnd, stringIndex);
            }
            if (stringIndex >= stringBegin) {
                return begin + encoding.byteIndex(string, stringBegin, stringIndex);
            }
            return encoding.byteIndex(string, 0, stringIndex);
        }
    }

    private boolean update(int r) {
        begin = matcher.getBegin();
        end = matcher.getEnd();
        region = matcher.getRegion();
        position.update(begin, end);
        return r > Matcher.FAILED;
    }

    private int stringLength() {
        return string.length();
    }

    private int byteLength() {
        return position.length;
    }

    private int toStringIndex(int byteIndex) {
        if (byteIndex < 0) {
            return -1;
        }
        return position.stringIndex(byteIndex);
    }

    private int toByteIndex(int stringIndex) {
        return position.byteIndex(stringIndex);
    }

    private void ensureResult() {
        if (begin < 0)
            throw new IllegalStateException("No match!");
    }

    private void ensureValidIndex(int index) {
        if (index < 0 || index > stringLength())
            throw new IndexOutOfBoundsException("Invalid index: " + index);
    }

    private void ensureValidGroup(int group) {
        if (group < 0 || group > groupCount())
            throw new IndexOutOfBoundsException("Invalid group: " + group);
    }

    @Override
    public String toString() {
        return String.format("%s: [string=%s, begin=%d, end=%d]", getClass().getSimpleName(),
                string, begin, end);
    }

    @Override
    public Iterator<String> iterator() {
        return new GroupIterator(this, negativeLAGroups);
    }

    @Override
    public MatchResult toMatchResult() {
        return new JoniMatchState(encoding, string, negativeLAGroups, position.clone(), begin, end,
                region != null ? region.clone() : null);
    }

    @Override
    public boolean find() {
        int start = end != begin ? end : end + encoding.length(string, end);
        return update(matcher.search(start, byteLength(), Option.NONE));
    }

    @Override
    public boolean find(int start) {
        ensureValidIndex(start);
        return update(matcher.search(toByteIndex(start), byteLength(), Option.NONE));
    }

    @Override
    public boolean matches(int start) {
        ensureValidIndex(start);
        return update(matcher.match(toByteIndex(start), byteLength(), Option.NONE));
    }

    @Override
    public int start() {
        ensureResult();
        return position.stringBegin;
    }

    @Override
    public int start(int group) {
        ensureResult();
        ensureValidGroup(group);
        if (group == 0) {
            return position.stringBegin;
        }
        return toStringIndex(region.beg[group]);
    }

    @Override
    public int end() {
        ensureResult();
        return position.stringEnd;
    }

    @Override
    public int end(int group) {
        ensureResult();
        ensureValidGroup(group);
        if (group == 0) {
            return position.stringEnd;
        }
        return toStringIndex(region.end[group]);
    }

    @Override
    public String group() {
        return group(0);
    }

    @Override
    public String group(int group) {
        int start = start(group), end = end(group);
        if (start == -1 || end == -1) {
            return null;
        }
        return string.subSequence(start, end).toString();
    }

    @Override
    public int groupCount() {
        Region region = this.region;
        return region != null ? region.numRegs - 1 : 0;
    }
}
