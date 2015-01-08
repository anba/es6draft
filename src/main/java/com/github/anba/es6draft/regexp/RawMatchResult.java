/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.regex.MatchResult;

/**
 * 
 */
public interface RawMatchResult extends MatchResult {
    Object rawGroup();

    Object rawGroup(int group);
}
