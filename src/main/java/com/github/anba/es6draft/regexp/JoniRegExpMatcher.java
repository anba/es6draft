/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.BitSet;
import java.util.regex.Pattern;

import org.jcodings.Encoding;
import org.joni.Option;
import org.joni.Regex;

/**
 * {@link RegExpMatcher} implementation for Joni {@link Regex} regular expressions
 */
final class JoniRegExpMatcher implements RegExpMatcher {
    // Java pattern for the input RegExp
    private final String regex;
    // Java flags for the input RegExp
    private final int flags;
    private final BitSet negativeLAGroups;
    private Regex pattern;
    private String lastInput = null;
    private byte[] lastInputBytes = null;

    public JoniRegExpMatcher(String regex, int flags, BitSet negativeLAGroups) {
        this.regex = regex;
        this.flags = flags;
        this.negativeLAGroups = negativeLAGroups;
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
            Encoding enc = UCS2Encoding.INSTANCE;
            byte[] bytes = UCS2Encoding.toBytes(regex);
            pattern = new Regex(bytes, 0, bytes.length, flags, enc, JoniSyntax.ECMAScript);
        }
        return pattern;
    }

    @Override
    public JoniMatchState matcher(String s) {
        if (s != lastInput) {
            lastInput = s;
            lastInputBytes = UCS2Encoding.toBytes(s);
        }
        return new JoniMatchState(getPattern().matcher(lastInputBytes), s);
    }

    @Override
    public BitSet getNegativeLookaheadGroups() {
        return negativeLAGroups;
    }
}
