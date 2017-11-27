/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.Set;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 */
public abstract class BreakableStatement extends Statement implements AbruptNode {
    private final EnumSet<Abrupt> abrupt;
    private final Set<String> labelSet;

    protected BreakableStatement(long beginPosition, long endPosition, EnumSet<Abrupt> abrupt, Set<String> labelSet) {
        super(beginPosition, endPosition);
        this.abrupt = abrupt;
        this.labelSet = labelSet;
    }

    @Override
    public final EnumSet<Abrupt> getAbrupt() {
        return abrupt;
    }

    @Override
    public final Set<String> getLabelSet() {
        return labelSet;
    }

    /**
     * Returns {@code true} if this node is the target of a <code>BreakStatement</code>.
     * 
     * @return {@code true} if this node is the target of a <code>BreakStatement</code>
     */
    public final boolean hasBreak() {
        return getAbrupt().contains(Abrupt.Break);
    }
}
