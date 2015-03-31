/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.BitSet;
import java.util.regex.Pattern;

/**
 * {@link RegExpMatcher} implementation for standard JDK {@link Pattern} regular expressions
 */
final class JDKRegExpMatcher implements RegExpMatcher {
    // Java pattern for the input RegExp
    private final String regex;
    // Java flags for the input RegExp
    private final int flags;
    private final BitSet negativeLAGroups;
    private Pattern pattern;

    public JDKRegExpMatcher(String regex, int flags, BitSet negativeLAGroups) {
        this.regex = regex;
        this.flags = flags;
        this.negativeLAGroups = negativeLAGroups;
    }

    private Pattern getPattern() {
        if (pattern == null) {
            pattern = Pattern.compile(regex, flags);
        }
        return pattern;
    }

    @Override
    public JDKMatchState matcher(String s) {
        return new JDKMatchState(getPattern().matcher(s), negativeLAGroups);
    }

    @Override
    public JDKMatchState matcher(CharSequence s) {
        return new JDKMatchState(getPattern().matcher(s), negativeLAGroups);
    }

    @Override
    public JDKRegExpMatcher clone() {
        JDKRegExpMatcher clone = new JDKRegExpMatcher(regex, flags, negativeLAGroups);
        clone.pattern = pattern;
        return clone;
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
}
