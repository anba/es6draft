/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.Set;

/**
 * Base interface for {@link Node} objects which can be exited resp. restarted with {@code break} or
 * {@code continue} statements.
 */
public interface AbruptNode extends Node {
    /**
     * Returns the label set for this node.
     * 
     * @return the label set
     */
    Set<String> getLabelSet();

    /**
     * Returns the set of abrupt completions for this node.
     * 
     * @return the set of abrupt completions
     */
    EnumSet<Abrupt> getAbrupt();

    /**
     * Abrupt completions
     */
    enum Abrupt {
        Break, Continue
    }
}
