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
import org.joni.Matcher;
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
            // -2 to account for null-terminating bytes in c-string
            pattern = new Regex(bytes, 0, bytes.length - 2, flags, enc, JoniSyntax.ECMAScript);
        }
        return pattern;
    }

    @Override
    public JoniMatchState matcher(String s) {
        if (s != lastInput) {
            lastInput = s;
            lastInputBytes = UCS2Encoding.toBytes(s);
        }
        // -2 to account for null-terminating bytes in c-string
        Matcher matcher = getPattern().matcher(lastInputBytes, 0, lastInputBytes.length - 2);
        return new JoniMatchState(matcher, s);
    }

    @Override
    public BitSet getNegativeLookaheadGroups() {
        return negativeLAGroups;
    }
}
