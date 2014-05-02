/**
 * Copyright (c) 2012-2014 André Bargull
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
    public BitSet getNegativeLookaheadGroups() {
        return negativeLAGroups;
    }
}
