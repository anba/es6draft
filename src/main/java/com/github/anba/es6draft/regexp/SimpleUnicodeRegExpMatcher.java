/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * 
 */
final class SimpleUnicodeRegExpMatcher implements RegExpMatcher {
    private final String regex;
    private final Matcher matcher;

    SimpleUnicodeRegExpMatcher(String regex, Matcher matcher) {
        this.regex = regex;
        this.matcher = matcher;
    }

    static final class MatchRegion {
        final int start;
        final int end;

        MatchRegion(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    @FunctionalInterface
    interface Term {
        Optional<MatchRegion> match(String string, int start);
    }

    @FunctionalInterface
    interface Matcher {
        Optional<MatchRegion> match(String string, int start);
    }

    static Term character(IntPredicate predicate) {
        return (string, start) -> {
            if (start >= string.length() || !predicate.test(string.codePointAt(start))) {
                return Optional.empty();
            }
            return Optional.of(new MatchRegion(start, start + Character.charCount(string.codePointAt(start))));
        };
    }

    static Term plus(IntPredicate predicate) {
        return (string, start) -> {
            int i = start;
            while (i < string.length()) {
                int codePoint = string.codePointAt(i);
                if (predicate.test(codePoint)) {
                    i += Character.charCount(codePoint);
                    continue;
                }
                break;
            }
            if (i == start) {
                return Optional.empty();
            }
            return Optional.of(new MatchRegion(start, i));
        };
    }

    private static Term star(IntPredicate predicate) {
        return (string, start) -> {
            int i = start;
            while (i < string.length()) {
                int codePoint = string.codePointAt(i);
                if (predicate.test(codePoint)) {
                    i += Character.charCount(codePoint);
                    continue;
                }
                break;
            }
            return Optional.of(new MatchRegion(start, i));
        };
    }

    static Matcher stringMatches(Term term) {
        return (string, start) -> {
            return term.match(string, start).filter(r -> r.start == 0 && r.end == string.length());
        };
    }

    static Matcher startsWith(Term term) {
        return (string, start) -> {
            return term.match(string, start).filter(r -> r.start == 0);
        };
    }

    static Matcher contains(Term term, IntPredicate predicate) {
        return (string, start) -> {
            Optional<MatchRegion> prefix = star(predicate.negate()).match(string, start);
            int actualStart = prefix.get().end;
            return term.match(string, actualStart);
        };
    }

    @Override
    public MatcherState matcher(String input) {
        return new MatcherStateImpl(matcher, input);
    }

    @Override
    public MatcherState matcher(CharSequence input) {
        return new MatcherStateImpl(matcher, input.toString());
    }

    @Override
    public String toString() {
        return String.format("regex=%s", regex);
    }

    static final class MatcherStateImpl implements MatcherState {
        private final Matcher matcher;
        private final String string;
        private int begin = -1, end;

        MatcherStateImpl(Matcher matcher, String string) {
            this.matcher = matcher;
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
            return new MatcherResultImpl(string, begin, end);
        }

        @Override
        public boolean find(int start) {
            ensureValidIndex(start);
            Optional<MatchRegion> match = matcher.match(string, start);
            if (match.isPresent()) {
                this.begin = match.get().start;
                this.end = match.get().end;
                return true;
            }
            begin = end = -1;
            return false;
        }

        @Override
        public boolean matches(int start) {
            ensureValidIndex(start);
            Optional<MatchRegion> match = matcher.match(string, start);
            if (match.isPresent() && match.get().start == start) {
                this.begin = match.get().start;
                this.end = match.get().end;
                return true;
            }
            begin = end = -1;
            return false;
        }
    }

    static final class MatcherResultImpl implements MatcherResult {
        private final String string;
        private final int begin;
        private final int end;

        MatcherResultImpl(String string, int begin, int end) {
            assert begin >= 0;
            this.string = string;
            this.begin = begin;
            this.end = end;
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
            return end;
        }

        @Override
        public int end(int group) {
            ensureValidGroup(group);
            return end;
        }

        @Override
        public String group() {
            return string.substring(begin, end);
        }

        @Override
        public String group(int group) {
            ensureValidGroup(group);
            return string.substring(begin, end);
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
