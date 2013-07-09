/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.Set;

/**
 * Base interface for {@link Node} objects which contain abrupt completions
 */
public interface AbruptNode extends Node {
    /**
     * Returns the set of labels for this node
     */
    Set<String> getLabelSet();

    /**
     * Returns the set of abrupt completions for this node
     */
    EnumSet<Abrupt> getAbrupt();

    enum Abrupt {
        Break, Continue
    }
}
