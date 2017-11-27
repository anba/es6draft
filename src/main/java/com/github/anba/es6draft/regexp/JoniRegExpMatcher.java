/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;

/**
 * {@link RegExpMatcher} implementation for Joni {@link Regex} regular expressions.
 */
final class JoniRegExpMatcher implements RegExpMatcher {
    // Java pattern for the input RegExp
    private final String regex;
    // Java flags for the input RegExp
    private final int flags;
    private final BitSet negativeLAGroups;
    private final Map<String, Integer> namedGroups;
    private Regex pattern;
    // FIXME: Memory issue?
    private CharSequence lastInput = null;
    private byte[] lastInputBytes = null;

    public JoniRegExpMatcher(String regex, int flags, BitSet negativeLAGroups, Map<String, Integer> namedGroups) {
        this.regex = regex;
        this.flags = flags;
        this.negativeLAGroups = negativeLAGroups;
        this.namedGroups = namedGroups;
    }

    private UEncoding getEncoding() {
        if ((this.flags & Pattern.UNICODE_CASE) != 0) {
            return UTF32Encoding.INSTANCE;
        }
        return UCS2Encoding.INSTANCE;
    }

    private Regex getPattern() {
        if (pattern == null) {
            int flags = 0;
            if ((this.flags & Pattern.MULTILINE) != 0) {
                flags |= Option.NEGATE_SINGLELINE;
            } else {
                flags |= Option.SINGLELINE;
            }
            if ((this.flags & Pattern.CASE_INSENSITIVE) != 0) {
                flags |= Option.IGNORECASE;
            }
            UEncoding enc = getEncoding();
            byte[] bytes = enc.toBytes(regex);
            int length = bytes.length - enc.minLength();
            pattern = new Regex(bytes, 0, length, flags, enc, JoniSyntax.ECMAScript);
        }
        return pattern;
    }

    @Override
    public MatcherStateImpl matcher(String s) {
        UEncoding enc = getEncoding();
        if (s != lastInput) {
            lastInput = s;
            lastInputBytes = enc.toBytes(s);
        }
        int length = lastInputBytes.length - enc.minLength();
        Matcher matcher = getPattern().matcher(lastInputBytes, 0, length);
        return new MatcherStateImpl(new EncodedString(enc, s), matcher, negativeLAGroups, namedGroups);
    }

    @Override
    public MatcherStateImpl matcher(CharSequence s) {
        UEncoding enc = getEncoding();
        if (s != lastInput) {
            lastInput = s;
            lastInputBytes = enc.toBytes(s);
        }
        int length = lastInputBytes.length - enc.minLength();
        Matcher matcher = getPattern().matcher(lastInputBytes, 0, length);
        return new MatcherStateImpl(new EncodedString(enc, s), matcher, negativeLAGroups, namedGroups);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(3);
        if ((flags & Pattern.CASE_INSENSITIVE) != 0) {
            sb.append('i');
        }
        if ((flags & Pattern.MULTILINE) != 0) {
            sb.append('m');
        }
        if ((flags & Pattern.UNICODE_CASE) != 0) {
            sb.append('u');
        }
        return String.format("regex=%s, flags=%s", regex, sb);
    }

    static final class EncodedString {
        final UEncoding encoding;
        final CharSequence string;

        EncodedString(UEncoding encoding, CharSequence string) {
            this.encoding = encoding;
            this.string = string;
        }

        int byteLength() {
            return encoding.length(string);
        }

        int strLength(int start, int count) {
            return encoding.strLength(string, start, count);
        }

        int length(int start, int end) {
            return encoding.length(string, start, end);
        }
    }

    // This class is only used as an optimization to avoid a full string traversal to find the correct byte/string
    // position. If we ever manage to make Unicode RegExps work with UTF16Encoding instead of UTF32Encoding, this class
    // can be removed, because UTF16Encoding#length (and #strLength) can compute its result in constant time.
    static final class StringPosition {
        final EncodedString encString;
        final int begin, end, stringBegin, stringEnd;

        StringPosition(EncodedString encString, int begin, int end, int stringBegin, int stringEnd) {
            this.encString = encString;
            this.begin = begin;
            this.end = end;
            this.stringBegin = stringBegin;
            this.stringEnd = stringEnd;
        }

        StringPosition relativeTo(int begin, int end) {
            assert begin >= 0 && end >= 0 && begin <= end;
            return new StringPosition(encString, begin, end, stringIndex(begin), stringIndex(end));
        }

        /**
         * Returns the string index for a given byte index.
         * 
         * @param byteIndex
         *            the byte index
         * @return the string index
         */
        int stringIndex(int byteIndex) {
            if (byteIndex >= end) {
                return stringEnd + encString.strLength(stringEnd, byteIndex - end);
            }
            if (byteIndex >= begin) {
                return stringBegin + encString.strLength(stringBegin, byteIndex - begin);
            }
            return encString.strLength(0, byteIndex);
        }

        /**
         * Returns the byte index for a given string index.
         * 
         * @param stringIndex
         *            the string index
         * @return the byte index
         */
        int byteIndex(int stringIndex) {
            if (stringIndex >= stringEnd) {
                return end + encString.length(stringEnd, stringIndex);
            }
            if (stringIndex >= stringBegin) {
                return begin + encString.length(stringBegin, stringIndex);
            }
            return encString.length(0, stringIndex);
        }
    }

    static final class MatcherStateImpl implements MatcherState {
        private final EncodedString encString;
        private final Matcher matcher;
        private final BitSet negativeLAGroups;
        private final Map<String, Integer> namedGroups;
        private final int byteLength;
        private StringPosition position;
        private int begin = -1, end = 0;
        private Region region;

        MatcherStateImpl(EncodedString encString, Matcher matcher, BitSet negativeLAGroups,
                Map<String, Integer> namedGroups) {
            this.encString = encString;
            this.matcher = matcher;
            this.negativeLAGroups = negativeLAGroups;
            this.namedGroups = namedGroups;
            this.byteLength = encString.byteLength();
            this.position = new StringPosition(encString, 0, 0, 0, 0);
        }

        private boolean update(int r) {
            begin = matcher.getBegin();
            end = matcher.getEnd();
            region = matcher.getRegion();
            position = position.relativeTo(begin, end);
            return r > Matcher.FAILED;
        }

        private boolean isUnicode() {
            return !(encString.encoding instanceof UCS2Encoding);
        }

        private int byteIndex(int index) {
            if (index < 0 || index > encString.string.length())
                throw new IndexOutOfBoundsException("Invalid index: " + index);

            // Don't start matching in middle of a surrogate pair.
            if (index > 0 && Character.isSupplementaryCodePoint(Character.codePointAt(encString.string, index - 1))
                    && isUnicode()) {
                index -= 1;
            }
            return position.byteIndex(index);
        }

        @Override
        public String toString() {
            return String.format("%s: [string=%s, begin=%d, end=%d]", getClass().getSimpleName(), encString.string,
                    begin, end);
        }

        @Override
        public MatcherResult toMatchResult() {
            if (begin < 0)
                throw new IllegalStateException("No match!");
            return new MatcherResultImpl(encString.string, negativeLAGroups, namedGroups, position, begin, end,
                    region != null ? region.clone() : null);
        }

        @Override
        public boolean find(int start) {
            return update(matcher.search(byteIndex(start), byteLength, Option.NONE));
        }

        @Override
        public boolean matches(int start) {
            return update(matcher.match(byteIndex(start), byteLength, Option.NONE));
        }
    }

    static final class MatcherResultImpl implements MatcherResult {
        private final CharSequence string;
        private final BitSet negativeLAGroups;
        private final Map<String, Integer> namedGroups;
        private final StringPosition position;
        private final int begin, end;
        private final Region region;
        private boolean regionFixed;

        MatcherResultImpl(CharSequence string, BitSet negativeLAGroups, Map<String, Integer> namedGroups,
                StringPosition position, int begin, int end, Region region) {
            assert begin >= 0 && begin <= end;
            this.string = string;
            this.negativeLAGroups = negativeLAGroups;
            this.namedGroups = namedGroups;
            this.position = position;
            this.begin = begin;
            this.end = end;
            this.region = region;
        }

        private int stringIndex(int byteIndex) {
            if (byteIndex < 0) {
                return -1;
            }
            return position.stringIndex(byteIndex);
        }

        private void ensureValidGroup(int group) {
            if (group < 0 || group > groupCount())
                throw new IndexOutOfBoundsException("Invalid group: " + group);
        }

        private void ensureRegionFixed() {
            if (regionFixed)
                return;
            regionFixed = true;

            int groupCount = this.groupCount();
            if (groupCount == 0)
                return;

            // Start index of last valid group in matched string.
            int last = start();
            Region region = this.region;
            for (int group = 1; group <= groupCount; ++group) {
                int startIndex = start(group);
                if (startIndex >= last && !negativeLAGroups.get(group)) {
                    last = startIndex;
                } else {
                    // Invalidate group: Joni doesn't reset capturing groups in some lookaround contexts, so we need to
                    // handle these cases manually.
                    region.beg[group] = region.end[group] = -1;
                }
            }
        }

        @Override
        public String toString() {
            return String.format("%s: [string=%s, begin=%d, end=%d]", getClass().getSimpleName(), string, begin, end);
        }

        @Override
        public CharSequence getInput() {
            return string;
        }

        @Override
        public int start() {
            return position.stringBegin;
        }

        @Override
        public int start(int group) {
            ensureValidGroup(group);
            if (group == 0) {
                return position.stringBegin;
            }
            ensureRegionFixed();
            return stringIndex(region.beg[group]);
        }

        @Override
        public int end() {
            return position.stringEnd;
        }

        @Override
        public int end(int group) {
            ensureValidGroup(group);
            if (group == 0) {
                return position.stringEnd;
            }
            ensureRegionFixed();
            return stringIndex(region.end[group]);
        }

        @Override
        public String group() {
            return string.subSequence(position.stringBegin, position.stringEnd).toString();
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

        @Override
        public Set<String> groups() {
            return Collections.unmodifiableSet(namedGroups.keySet());
        }

        @Override
        public String group(String name) {
            return group(namedGroups.getOrDefault(name, -1));
        }
    }
}
