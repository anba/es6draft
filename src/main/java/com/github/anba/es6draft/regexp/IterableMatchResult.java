/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.Iterator;
import java.util.regex.MatchResult;

/**
 * Extended {@link MatchResult} interface.
 */
public interface IterableMatchResult extends MatchResult, Iterable<String> {
    /**
     * Returns an iterator over the captured groups of this {@link MatchResult} object.
     * 
     * @return the iterator over the captured groups
     */
    @Override
    Iterator<String> iterator();
}
