/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.regex.MatchResult;

/**
 * Common interface for regular expression match operations
 */
public interface MatchState extends MatchResult {
    /**
     * Returns {@link MatchResult} object
     */
    MatchResult toMatchResult();

    /**
     * Attempts to find the next match
     */
    boolean find();

    /**
     * Attempts to find the next match starting at {@code start}
     */
    boolean find(int start);

    /**
     * Matches the substring starting at {@code start} against this pattern
     */
    boolean matches(int start);
}
