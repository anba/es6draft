/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

/**
 * Common interface for regular expression matcher objects.
 */
public interface RegExpMatcher extends Cloneable {
    /**
     * Returns a {@link MatchState} object for {@code input}.
     * 
     * @param input
     *            the input string to match against
     * @return the match state
     */
    MatchState matcher(String input);

    /**
     * Returns a {@link MatchState} object for {@code input}.
     * 
     * @param input
     *            the input string to match against
     * @return the match state
     */
    MatchState matcher(CharSequence input);

    /**
     * Returns a clone of this {@link RegExpMatcher} object.
     * 
     * @return the new matcher
     * @throws CloneNotSupportedException
     *             if not supported
     */
    RegExpMatcher clone() throws CloneNotSupportedException;
}
