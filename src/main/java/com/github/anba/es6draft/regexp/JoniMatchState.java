/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.regex.MatchResult;

import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;

/**
 * {@link MatchState} implementation for Joni {@link Regex} regular expressions
 */
final class JoniMatchState implements MatchState {
    private final Matcher matcher;
    private final String string;
    private final int byteLength;
    private int begin = -1, end = 0;
    private Region region;

    public JoniMatchState(Matcher matcher, String string) {
        this.matcher = matcher;
        this.string = string;
        this.byteLength = string.length() << 1;
    }

    private JoniMatchState(String string, int begin, int end, Region region) {
        this.matcher = null;
        this.string = string;
        this.byteLength = string.length() << 1;
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

    private void ensureResult() {
        if (begin < 0)
            throw new IllegalStateException("No match!");
    }

    private void ensureValidGroup(int group) {
        if (group < 0 || group > groupCount())
            throw new IndexOutOfBoundsException("Invalid group: " + group);
    }

    @Override
    public MatchResult toMatchResult() {
        return new JoniMatchState(string, begin, end, region != null ? region.clone() : null);
    }

    @Override
    public boolean find() {
        int start = end != begin ? end : end + 2;
        return update(matcher.search(start, byteLength, Option.NONE));
    }

    @Override
    public boolean find(int start) {
        return update(matcher.search(start << 1, byteLength, Option.NONE));
    }

    @Override
    public boolean matches(int start) {
        return update(matcher.match(start << 1, byteLength, Option.NONE));
    }

    @Override
    public int start() {
        ensureResult();
        return begin >> 1;
    }

    @Override
    public int start(int group) {
        ensureResult();
        ensureValidGroup(group);
        return (group == 0 ? begin : region.beg[group]) >> 1;
    }

    @Override
    public int end() {
        ensureResult();
        return end >> 1;
    }

    @Override
    public int end(int group) {
        ensureResult();
        ensureValidGroup(group);
        return (group == 0 ? end : region.end[group]) >> 1;
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
