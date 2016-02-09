/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.regex.MatchResult;

/**
 * Common interface for regular expression match operations.
 */
public interface MatchState extends MatchResult {
    /**
     * Returns a {@link MatchResult} object.
     * 
     * @return the match result object
     */
    MatchResult toMatchResult();

    /**
     * Attempts to find the next match starting at {@code start}.
     * 
     * @param start
     *            the start position in the input string
     * @return {@code true} if a new match was found
     */
    boolean find(int start);

    /**
     * Matches the substring starting at {@code start} against this pattern.
     * 
     * @param start
     *            the start position in the input string
     * @return {@code true} if the substring matches the pattern
     */
    boolean matches(int start);
}
