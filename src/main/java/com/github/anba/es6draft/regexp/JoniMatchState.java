/**
 * Copyright (c) 2012-2014 André Bargull
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
    private final Matcher matcher;
    private final String string;
    private final BitSet negativeLAGroups;
    private int begin = -1, end = 0;
    private Region region;

    public JoniMatchState(Matcher matcher, String string, BitSet negativeLAGroups) {
        this.matcher = matcher;
        this.string = string;
        this.negativeLAGroups = negativeLAGroups;
    }

    private JoniMatchState(String string, BitSet negativeLAGroups, int begin, int end, Region region) {
        this.matcher = null;
        this.string = string;
        this.negativeLAGroups = negativeLAGroups;
        this.begin = begin;
        this.end = end;
        this.region = region;
    }

    private boolean update(int r) {
        begin = matcher.getBegin();
        end = matcher.getEnd();
        region = matcher.getRegion();
        return r > Matcher.FAILED;
    }

    private int stringLength() {
        return string.length();
    }

    private int byteLength() {
        return string.length() << 1;
    }

    private int toStringIndex(int byteIndex) {
        return byteIndex >> 1;
    }

    private int toByteIndex(int stringIndex) {
        return stringIndex << 1;
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
        return new JoniMatchState(string, negativeLAGroups, begin, end,
                region != null ? region.clone() : null);
    }

    @Override
    public boolean find() {
        int start = end != begin ? end : end + 2;
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
        return toStringIndex(begin);
    }

    @Override
    public int start(int group) {
        ensureResult();
        ensureValidGroup(group);
        return toStringIndex(group == 0 ? begin : region.beg[group]);
    }

    @Override
    public int end() {
        ensureResult();
        return toStringIndex(end);
    }

    @Override
    public int end(int group) {
        ensureResult();
        ensureValidGroup(group);
        return toStringIndex(group == 0 ? end : region.end[group]);
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
        return string.substring(start, end);
    }

    @Override
    public int groupCount() {
        Region region = this.region;
        return region != null ? region.numRegs - 1 : 0;
    }
}
