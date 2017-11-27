/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.Set;
import java.util.regex.MatchResult;

/**
 * Extended {@link MatchResult} interface.
 */
public interface MatcherResult extends MatchResult {
    /**
     * Returns the input string.
     * 
     * @return the input string
     */
    CharSequence getInput();

    /**
     * Returns the set of named capturing groups.
     * 
     * @return the set of group names
     */
    Set<String> groups();

    /**
     * Returns the matched capturing group.
     * 
     * @param name
     *            the capturing group name
     * @return the matched capturing group
     */
    String group(String name);
}
