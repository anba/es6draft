/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.Collections;
import java.util.Set;

/**
 * 
 */
final class SimpleRegExpMatcher implements RegExpMatcher {
    private final String regex;

    SimpleRegExpMatcher(String regex) {
        this.regex = regex;
    }

    @Override
    public MatcherState matcher(String input) {
        return new MatcherStateImpl(regex, input);
    }

    @Override
    public MatcherState matcher(CharSequence input) {
        return new MatcherStateImpl(regex, input.toString());
    }

    @Override
    public String toString() {
        return String.format("regex=%s", regex);
    }

    static final class MatcherStateImpl implements MatcherState {
        private final String regex;
        private final String string;
        private int begin = -1;

        MatcherStateImpl(String regex, String string) {
            this.regex = regex;
            this.string = string;
        }

        private void ensureValidIndex(int index) {
            if (index < 0 || index > string.length())
                throw new IndexOutOfBoundsException("Invalid index: " + index);
        }

        @Override
        public MatcherResult toMatchResult() {
            if (begin < 0)
                throw new IllegalStateException("No match!");
            return new MatcherResultImpl(regex, string, begin);
        }

        @Override
        public boolean find(int start) {
            ensureValidIndex(start);
            int result = string.indexOf(regex, start);
            begin = result;
            return result >= 0;
        }

        @Override
        public boolean matches(int start) {
            ensureValidIndex(start);
            boolean result = string.regionMatches(start, regex, 0, regex.length());
            begin = result ? start : -1;
            return result;
        }
    }

    static final class MatcherResultImpl implements MatcherResult {
        private final String regex;
        private final String string;
        private final int begin;

        MatcherResultImpl(String regex, String string, int begin) {
            assert begin >= 0;
            this.regex = regex;
            this.string = string;
            this.begin = begin;
        }

        private void ensureValidGroup(int group) {
            if (group < 0 || group > groupCount())
                throw new IndexOutOfBoundsException("Invalid group: " + group);
        }

        @Override
        public String getInput() {
            return string;
        }

        @Override
        public int start() {
            return begin;
        }

        @Override
        public int start(int group) {
            ensureValidGroup(group);
            return begin;
        }

        @Override
        public int end() {
            return begin + regex.length();
        }

        @Override
        public int end(int group) {
            ensureValidGroup(group);
            return begin + regex.length();
        }

        @Override
        public String group() {
            return string.substring(begin, begin + regex.length());
        }

        @Override
        public String group(int group) {
            ensureValidGroup(group);
            return string.substring(begin, begin + regex.length());
        }

        @Override
        public int groupCount() {
            return 0;
        }

        @Override
        public Set<String> groups() {
            return Collections.emptySet();
        }

        @Override
        public String group(String name) {
            throw new IndexOutOfBoundsException("Invalid group: " + name);
        }
    }
}
